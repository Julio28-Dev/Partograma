package com.jninfo.partograma.partograma.data;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pedido de acesso institucional e aprovacao pelo admin (colecao
 * {@code institutionAccessRequests}) + verificacao de papel admin (colecao {@code admins},
 * ver firestore.rules). Nao substitui {@link InstituicaoRepository} -- pelo contrario, ao
 * aprovar uma solicitacao esta classe cria o mesmo tipo de documento em
 * {@code instituicoes/{uid}} que {@link InstituicaoRepository#garantirInstituicao} cria no
 * primeiro login, so que antecipado pelo admin.
 *
 * Nenhuma senha e persistida em nenhum momento: a senha inicial digitada pelo admin ao
 * aprovar e usada uma unica vez, na chamada ao Firebase Auth, e descartada da memoria logo
 * em seguida. Como o app nao tem backend/Cloud Functions, criar a conta da instituicao sem
 * derrubar a sessao do admin exige um FirebaseApp SECUNDARIO (mesmo projeto, instancia
 * paralela): {@code createUserWithEmailAndPassword} roda nessa instancia paralela, entao
 * quem "loga" nela e a instituicao nova, nunca o admin logado na instancia principal.
 */
public class SolicitacaoAcessoRepository {

    private static final String COLECAO_SOLICITACOES = "institutionAccessRequests";
    private static final String COLECAO_ADMINS = "admins";
    private static final String COLECAO_INSTITUICOES = "instituicoes";
    private static final String CAMPO_MENSAGEM_PADRAO = "mensagemPadraoAlerta";
    private static final String NOME_APP_PROVISIONAMENTO = "ProvisionamentoInstitucional";

    private final FirebaseFirestore db;

    public SolicitacaoAcessoRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface OperacaoCallback {
        void onSucesso();
        void onErro(Exception erro);
    }

    public interface AdminCallback {
        void onResultado(boolean ehAdmin);
    }

    public interface ContagemCallback {
        void onContagem(int quantidade);
    }

    public interface ListaSolicitacoesCallback {
        void onAtualizadas(List<SolicitacaoAcesso> solicitacoes);
    }

    /** Envia um novo pedido de acesso. Exige alguma sessao Firebase (anonima ja serve). */
    public void solicitarAcesso(String nomeInstituicao, String responsavel, String email,
                                 @Nullable String telefone, OperacaoCallback callback) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nomeInstituicao", nomeInstituicao);
        dados.put("responsavel", responsavel);
        dados.put("email", email);
        dados.put("telefone", telefone == null ? "" : telefone);
        dados.put("status", SolicitacaoAcesso.STATUS_PENDENTE);
        dados.put("criadoEm", FieldValue.serverTimestamp());
        db.collection(COLECAO_SOLICITACOES).add(dados)
                .addOnSuccessListener(docRef -> callback.onSucesso())
                .addOnFailureListener(callback::onErro);
    }

    /** Verifica se o usuario logado no momento e admin (existencia de admins/{uid}). */
    public void verificarAdmin(AdminCallback callback) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            callback.onResultado(false);
            return;
        }
        db.collection(COLECAO_ADMINS).document(usuario.getUid()).get()
                .addOnSuccessListener(doc -> callback.onResultado(doc.exists()))
                .addOnFailureListener(erro -> callback.onResultado(false));
    }

    /** Contagem em tempo real de solicitacoes pendentes, para o badge da engrenagem. */
    public ListenerRegistration observarContagemPendentes(ContagemCallback callback) {
        return db.collection(COLECAO_SOLICITACOES)
                .whereEqualTo("status", SolicitacaoAcesso.STATUS_PENDENTE)
                .addSnapshotListener((snapshot, erro) -> {
                    if (erro != null || snapshot == null) {
                        callback.onContagem(0);
                        return;
                    }
                    callback.onContagem(snapshot.size());
                });
    }

    /** Lista em tempo real as solicitacoes pendentes, mais antigas primeiro. */
    public ListenerRegistration observarPendentes(ListaSolicitacoesCallback callback) {
        return db.collection(COLECAO_SOLICITACOES)
                .whereEqualTo("status", SolicitacaoAcesso.STATUS_PENDENTE)
                .orderBy("criadoEm", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, erro) -> {
                    List<SolicitacaoAcesso> lista = new ArrayList<>();
                    if (snapshot != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            SolicitacaoAcesso solicitacao = doc.toObject(SolicitacaoAcesso.class);
                            if (solicitacao != null) {
                                solicitacao.setId(doc.getId());
                                lista.add(solicitacao);
                            }
                        }
                    }
                    callback.onAtualizadas(lista);
                });
    }

    /**
     * Aprova a solicitacao: cria a conta Firebase Auth da instituicao (via app secundario,
     * sem afetar a sessao do admin), cria o documento instituicoes/{uid} e marca a
     * solicitacao como aprovada. A senha inicial e definida agora pelo admin e nunca
     * chega ao Firestore -- a instituicao pode troca-la depois em Configuracoes.
     */
    public void aprovar(Context contexto, SolicitacaoAcesso solicitacao, String senhaInicial, OperacaoCallback callback) {
        FirebaseUser admin = FirebaseAuth.getInstance().getCurrentUser();
        if (admin == null) {
            callback.onErro(new IllegalStateException("Admin nao autenticado"));
            return;
        }
        String adminUid = admin.getUid();

        FirebaseApp appProvisionamento;
        try {
            appProvisionamento = FirebaseApp.getInstance(NOME_APP_PROVISIONAMENTO);
        } catch (IllegalStateException semInstancia) {
            appProvisionamento = FirebaseApp.initializeApp(
                    contexto.getApplicationContext(),
                    FirebaseApp.getInstance().getOptions(),
                    NOME_APP_PROVISIONAMENTO);
        }
        FirebaseAuth authProvisionamento = FirebaseAuth.getInstance(appProvisionamento);

        authProvisionamento.createUserWithEmailAndPassword(solicitacao.getEmail(), senhaInicial)
                .addOnSuccessListener(resultado -> {
                    String novoUid = resultado.getUser().getUid();
                    authProvisionamento.signOut();
                    criarInstituicaoEAtualizarSolicitacao(solicitacao, novoUid, adminUid, callback);
                })
                .addOnFailureListener(callback::onErro);
    }

    private void criarInstituicaoEAtualizarSolicitacao(SolicitacaoAcesso solicitacao, String novoUid,
                                                         String adminUid, OperacaoCallback callback) {
        Map<String, Object> instituicaoDados = new HashMap<>();
        instituicaoDados.put("criadoEm", FieldValue.serverTimestamp());
        instituicaoDados.put(CAMPO_MENSAGEM_PADRAO, "");
        instituicaoDados.put("nomeInstituicao", solicitacao.getNomeInstituicao());
        instituicaoDados.put("responsavel", solicitacao.getResponsavel());

        db.collection(COLECAO_INSTITUICOES).document(novoUid).set(instituicaoDados)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> atualizacao = new HashMap<>();
                    atualizacao.put("status", SolicitacaoAcesso.STATUS_APROVADA);
                    atualizacao.put("institutionUid", novoUid);
                    atualizacao.put("revisadoPor", adminUid);
                    atualizacao.put("atualizadoEm", FieldValue.serverTimestamp());
                    db.collection(COLECAO_SOLICITACOES).document(solicitacao.getId())
                            .update(atualizacao)
                            .addOnSuccessListener(unused2 -> callback.onSucesso())
                            .addOnFailureListener(callback::onErro);
                })
                .addOnFailureListener(callback::onErro);
    }

    /** Rejeita a solicitacao. Motivo e opcional. */
    public void rejeitar(SolicitacaoAcesso solicitacao, @Nullable String motivo, OperacaoCallback callback) {
        FirebaseUser admin = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> atualizacao = new HashMap<>();
        atualizacao.put("status", SolicitacaoAcesso.STATUS_REJEITADA);
        atualizacao.put("revisadoPor", admin != null ? admin.getUid() : null);
        atualizacao.put("atualizadoEm", FieldValue.serverTimestamp());
        if (motivo != null && !motivo.trim().isEmpty()) {
            atualizacao.put("motivoRejeicao", motivo.trim());
        }
        db.collection(COLECAO_SOLICITACOES).document(solicitacao.getId())
                .update(atualizacao)
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(callback::onErro);
    }
}
