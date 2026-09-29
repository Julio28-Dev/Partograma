package com.jninfo.partograma.partograma.data;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dados da instituicao autenticada: mensagem padrao de alerta, destinatarios autorizados a
 * receber alertas e o historico de alertas emitidos. Tudo debaixo de
 * {@code instituicoes/{institutionId}}, onde institutionId e o uid do Firebase Auth de quem
 * fez login (ver LoginActivity) -- cada conta de e-mail/senha criada no Firebase Console
 * para uma instituicao vira, na pratica, uma instituicao isolada aqui.
 *
 * Isolamento entre instituicoes: real e garantido por firestore.rules (cada uid so le/escreve
 * o proprio documento "instituicoes/{uid}" e suas subcolecoes) -- diferente da colecao
 * "pacientes", que por enquanto continua sem esse isolamento reforçado nas rules (ver
 * comentario em firestore.rules e o relatorio final desta etapa para o motivo).
 */
public class InstituicaoRepository {

    private static final String COLECAO_INSTITUICOES = "instituicoes";
    private static final String SUBCOLECAO_DESTINATARIOS = "destinatarios";
    private static final String SUBCOLECAO_ALERTAS = "alertas";
    private static final String CAMPO_MENSAGEM_PADRAO = "mensagemPadraoAlerta";

    private final FirebaseFirestore db;

    public InstituicaoRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /** Uid do Firebase Auth logado no momento, ou null se ninguem estiver logado. */
    @Nullable
    public static String instituicaoIdAtual() {
        com.google.firebase.auth.FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        return usuario != null ? usuario.getUid() : null;
    }

    public interface OperacaoCallback {
        void onSucesso();
        void onErro(Exception erro);
    }

    public interface TextoCallback {
        void onCarregado(@Nullable String texto);
        void onErro(Exception erro);
    }

    public interface ListaDestinatariosCallback {
        void onAtualizados(List<Destinatario> destinatarios);
        void onErro(Exception erro);
    }

    /** Garante que o documento da instituicao existe (chamado apos login bem-sucedido). */
    public void garantirInstituicao(String institutionId, OperacaoCallback callback) {
        db.collection(COLECAO_INSTITUICOES).document(institutionId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onSucesso();
                        return;
                    }
                    Map<String, Object> dados = new HashMap<>();
                    dados.put("criadoEm", FieldValue.serverTimestamp());
                    dados.put(CAMPO_MENSAGEM_PADRAO, "");
                    db.collection(COLECAO_INSTITUICOES).document(institutionId).set(dados)
                            .addOnSuccessListener(unused -> callback.onSucesso())
                            .addOnFailureListener(callback::onErro);
                })
                .addOnFailureListener(callback::onErro);
    }

    public void carregarMensagemPadrao(String institutionId, TextoCallback callback) {
        db.collection(COLECAO_INSTITUICOES).document(institutionId).get()
                .addOnSuccessListener(doc -> callback.onCarregado(doc.getString(CAMPO_MENSAGEM_PADRAO)))
                .addOnFailureListener(callback::onErro);
    }

    public void salvarMensagemPadrao(String institutionId, String mensagem, OperacaoCallback callback) {
        db.collection(COLECAO_INSTITUICOES).document(institutionId)
                .update(CAMPO_MENSAGEM_PADRAO, mensagem)
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(callback::onErro);
    }

    public ListenerRegistration observarDestinatarios(String institutionId, ListaDestinatariosCallback callback) {
        return db.collection(COLECAO_INSTITUICOES).document(institutionId).collection(SUBCOLECAO_DESTINATARIOS)
                .addSnapshotListener((snapshot, erro) -> {
                    if (erro != null) {
                        callback.onErro(erro);
                        return;
                    }
                    List<Destinatario> lista = new ArrayList<>();
                    if (snapshot != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            Destinatario destinatario = doc.toObject(Destinatario.class);
                            if (destinatario != null) {
                                destinatario.setId(doc.getId());
                                lista.add(destinatario);
                            }
                        }
                    }
                    callback.onAtualizados(lista);
                });
    }

    public void adicionarDestinatario(String institutionId, Destinatario destinatario, OperacaoCallback callback) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", destinatario.getNome());
        dados.put("contato", destinatario.getContato());
        dados.put("ativo", true);
        db.collection(COLECAO_INSTITUICOES).document(institutionId).collection(SUBCOLECAO_DESTINATARIOS)
                .add(dados)
                .addOnSuccessListener(docRef -> callback.onSucesso())
                .addOnFailureListener(callback::onErro);
    }

    public void removerDestinatario(String institutionId, String destinatarioId, OperacaoCallback callback) {
        db.collection(COLECAO_INSTITUICOES).document(institutionId).collection(SUBCOLECAO_DESTINATARIOS)
                .document(destinatarioId).delete()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(callback::onErro);
    }

    /**
     * Registra um alerta emitido para uma paciente. NAO envia nada de verdade (sem canal de
     * push/SMS/e-mail configurado) -- so grava o registro com os destinatarios que deveriam
     * ser avisados, para rastreabilidade e para quando um canal real for definido.
     */
    public void registrarAlerta(String institutionId, RegistroAlerta alerta, OperacaoCallback callback) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("pacienteId", alerta.getPacienteId());
        dados.put("pacienteNome", alerta.getPacienteNome());
        dados.put("mensagem", alerta.getMensagem());
        dados.put("destinatariosNomes", alerta.getDestinatariosNomes());
        dados.put("status", "registrado");
        dados.put("dataHora", FieldValue.serverTimestamp());
        db.collection(COLECAO_INSTITUICOES).document(institutionId).collection(SUBCOLECAO_ALERTAS)
                .add(dados)
                .addOnSuccessListener(docRef -> callback.onSucesso())
                .addOnFailureListener(callback::onErro);
    }
}
