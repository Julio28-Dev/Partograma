package com.jninfo.partograma.partograma.data;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
 * A conta Firebase Auth da instituicao e criada JA NO MOMENTO DA SOLICITACAO, com a senha
 * que a propria instituicao escolhe (ver {@link #solicitarAcesso}) -- assim, depois de
 * aprovada, ela entra com o mesmo e-mail/senha que definiu ao pedir acesso, sem o admin
 * precisar inventar nenhuma senha. Isso NAO significa acesso automatico: enquanto a
 * solicitacao nao for aprovada, {@code instituicoes/{uid}} nao existe, e o login
 * (LoginActivity) so libera o resto do app quando esse documento existe (ou quando e
 * admin) -- ver LoginActivity.verificarAprovacaoEEntrar(). Nenhuma senha e persistida no
 * Firestore em nenhum momento; ela vive exclusivamente no Firebase Auth.
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
        void onErro(Exception erro);
    }

    /**
     * Cria a conta Firebase Auth da instituicao (via app secundario, para nao afetar
     * nenhuma sessao ja aberta neste aparelho) com o e-mail/senha escolhidos por ela, e
     * so depois grava o pedido no Firestore com status pendente. Se a gravacao no
     * Firestore falhar por qualquer motivo, desfaz a criacao da conta (rollback) para nao
     * deixar uma conta orfa sem pedido correspondente.
     */
    public void solicitarAcesso(Context contexto, String nomeInstituicao, String responsavel,
                                 String email, @Nullable String telefone, String senha,
                                 OperacaoCallback callback) {
        // A gravacao do pedido no Firestore (app PRIMARIO, mais abaixo) exige alguma
        // sessao (estaAutenticado()) -- quem chega nesta tela vindo do Login nunca tem
        // nenhuma, porque LoginActivity desloga qualquer sessao anonima ao mostrar o
        // formulario (para "Sair" funcionar de verdade, ver SessaoUtil.encerrarSessao).
        // Sem estabelecer uma sessao anonima aqui primeiro, a gravacao falhava sempre com
        // permission-denied, disfarcado de "verifique sua conexao" no app.
        FirebaseAuth authPrimario = FirebaseAuth.getInstance();
        if (authPrimario.getCurrentUser() != null) {
            prosseguirComSolicitacao(contexto, nomeInstituicao, responsavel, email, telefone, senha, callback);
        } else {
            authPrimario.signInAnonymously()
                    .addOnSuccessListener(resultado ->
                            prosseguirComSolicitacao(contexto, nomeInstituicao, responsavel, email, telefone, senha, callback))
                    .addOnFailureListener(callback::onErro);
        }
    }

    private void prosseguirComSolicitacao(Context contexto, String nomeInstituicao, String responsavel,
                                           String email, @Nullable String telefone, String senha,
                                           OperacaoCallback callback) {
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

        authProvisionamento.createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener(resultado -> {
                    String novoUid = resultado.getUser().getUid();
                    Map<String, Object> dados = new HashMap<>();
                    dados.put("nomeInstituicao", nomeInstituicao);
                    dados.put("responsavel", responsavel);
                    dados.put("email", email);
                    dados.put("telefone", telefone == null ? "" : telefone);
                    dados.put("status", SolicitacaoAcesso.STATUS_PENDENTE);
                    dados.put("criadoEm", FieldValue.serverTimestamp());
                    dados.put("institutionUid", novoUid);
                    // ID do documento = uid da conta (nao autogerado) de proposito: e o
                    // que permite ao firestore.rules negar auto-aprovacao (ver
                    // temSolicitacaoNaoAprovada() e o comentario em instituicoes/{uid}).
                    db.collection(COLECAO_SOLICITACOES).document(novoUid).set(dados)
                            .addOnSuccessListener(docRef -> {
                                authProvisionamento.signOut();
                                callback.onSucesso();
                            })
                            .addOnFailureListener(erro -> {
                                // Rollback: sem isso, ficaria uma conta Firebase Auth
                                // criada sem nenhum pedido de acesso correspondente.
                                if (authProvisionamento.getCurrentUser() != null) {
                                    authProvisionamento.getCurrentUser().delete();
                                }
                                callback.onErro(erro);
                            });
                })
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

    /**
     * Lista em tempo real as solicitacoes pendentes, mais antigas primeiro. A ordenacao e
     * feita EM MEMORIA (nao via orderBy() do Firestore) de proposito: um where() de
     * igualdade combinado com orderBy() em outro campo exige um indice composto que nao
     * existe neste projeto e precisaria ser criado manualmente no Console -- sem isso a
     * consulta falhava silenciosamente (o listener recebia so o erro, nunca os
     * documentos), e a tela de Autorizacoes aparecia sempre vazia mesmo com pedidos
     * pendentes de verdade (o badge, que so usa where() sem orderBy(), continuava
     * funcionando, por isso os dois nao batiam). Evitar o indice composto e mais simples
     * e nao depende de nenhuma configuracao adicional no Firebase.
     */
    public ListenerRegistration observarPendentes(ListaSolicitacoesCallback callback) {
        return db.collection(COLECAO_SOLICITACOES)
                .whereEqualTo("status", SolicitacaoAcesso.STATUS_PENDENTE)
                .addSnapshotListener((snapshot, erro) -> {
                    if (erro != null) {
                        callback.onErro(erro);
                        return;
                    }
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
                    Collections.sort(lista, Comparator.comparing(
                            SolicitacaoAcesso::getCriadoEm, Comparator.nullsLast(Comparator.naturalOrder())));
                    callback.onAtualizadas(lista);
                });
    }

    /**
     * Aprova a solicitacao: a conta Firebase Auth ja existe desde a solicitacao (ver
     * {@link #solicitarAcesso}) -- aprovar so cria o documento instituicoes/{uid}
     * (o que efetivamente libera o login, ver LoginActivity) e marca o pedido como
     * aprovado. Nao envolve senha nenhuma nesta etapa.
     */
    public void aprovar(SolicitacaoAcesso solicitacao, OperacaoCallback callback) {
        FirebaseUser admin = FirebaseAuth.getInstance().getCurrentUser();
        if (admin == null) {
            callback.onErro(new IllegalStateException("Admin nao autenticado"));
            return;
        }
        // O ID do documento e o uid da conta (ver solicitarAcesso) -- fonte de verdade,
        // mesmo que o campo institutionUid (redundante, so para facilitar leitura no
        // Console) por algum motivo estivesse ausente.
        criarInstituicaoEAtualizarSolicitacao(solicitacao, solicitacao.getId(), admin.getUid(), callback);
    }

    private void criarInstituicaoEAtualizarSolicitacao(SolicitacaoAcesso solicitacao, String institutionUid,
                                                         String adminUid, OperacaoCallback callback) {
        Map<String, Object> instituicaoDados = new HashMap<>();
        instituicaoDados.put("criadoEm", FieldValue.serverTimestamp());
        instituicaoDados.put(CAMPO_MENSAGEM_PADRAO, "");
        instituicaoDados.put("nomeInstituicao", solicitacao.getNomeInstituicao());
        instituicaoDados.put("responsavel", solicitacao.getResponsavel());
        instituicaoDados.put("ativo", true);

        db.collection(COLECAO_INSTITUICOES).document(institutionUid).set(instituicaoDados)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> atualizacao = new HashMap<>();
                    atualizacao.put("status", SolicitacaoAcesso.STATUS_APROVADA);
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
