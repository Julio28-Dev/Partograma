package com.jninfo.partograma.partograma;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.ListenerRegistration;
import com.jninfo.partograma.partograma.data.SolicitacaoAcesso;
import com.jninfo.partograma.partograma.data.SolicitacaoAcessoRepository;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Tela admin-only (Configuracoes -> Autorizacoes) que lista as solicitacoes de acesso
 * institucional pendentes e permite Aceitar/Rejeitar. So aparece/funciona para quem tem
 * documento em admins/{uid} -- mesmo que alguem chegue aqui manualmente, o Firestore nega
 * qualquer leitura/escrita em institutionAccessRequests para quem nao for admin (ver
 * firestore.rules), entao nao ha risco de seguranca dependente so desta tela.
 */
public class AutorizacoesActivity extends BaseActivity {

    private LinearLayout containerSolicitacoes;
    private SolicitacaoAcessoRepository repositorio;
    private ListenerRegistration listenerSolicitacoes;
    private final SimpleDateFormat formatoData = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autorizacoes);

        repositorio = new SolicitacaoAcessoRepository();
        containerSolicitacoes = findViewById(R.id.containerSolicitacoes);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenerSolicitacoes = repositorio.observarPendentes(this::preencherLista);
    }

    @Override
    protected void onStop() {
        if (listenerSolicitacoes != null) {
            listenerSolicitacoes.remove();
        }
        super.onStop();
    }

    private void preencherLista(List<SolicitacaoAcesso> solicitacoes) {
        containerSolicitacoes.removeAllViews();
        if (solicitacoes.isEmpty()) {
            TextView vazio = new TextView(this);
            vazio.setText(R.string.autorizacoes_vazio);
            vazio.setTextColor(getResources().getColor(R.color.textSecondary));
            vazio.setTextSize(13f);
            containerSolicitacoes.addView(vazio);
            return;
        }
        for (SolicitacaoAcesso solicitacao : solicitacoes) {
            View linha = LayoutInflater.from(this).inflate(R.layout.item_solicitacao_acesso, containerSolicitacoes, false);
            ((TextView) linha.findViewById(R.id.txtNomeInstituicaoSolicitacao)).setText(solicitacao.getNomeInstituicao());
            ((TextView) linha.findViewById(R.id.txtResponsavelSolicitacao)).setText(
                    getString(R.string.solicitar_acesso_responsavel) + ": " + solicitacao.getResponsavel());
            ((TextView) linha.findViewById(R.id.txtEmailSolicitacao)).setText(solicitacao.getEmail());

            TextView txtTelefone = linha.findViewById(R.id.txtTelefoneSolicitacao);
            if (TextUtils.isEmpty(solicitacao.getTelefone())) {
                txtTelefone.setVisibility(View.GONE);
            } else {
                txtTelefone.setText(solicitacao.getTelefone());
            }

            TextView txtData = linha.findViewById(R.id.txtDataSolicitacao);
            if (solicitacao.getCriadoEm() != null) {
                txtData.setText(formatoData.format(solicitacao.getCriadoEm()));
            } else {
                txtData.setVisibility(View.GONE);
            }

            linha.findViewById(R.id.btnAceitarSolicitacao).setOnClickListener(v -> mostrarDialogoAceitar(solicitacao));
            linha.findViewById(R.id.btnRejeitarSolicitacao).setOnClickListener(v -> mostrarDialogoRejeitar(solicitacao));
            containerSolicitacoes.addView(linha);
        }
    }

    private void mostrarDialogoAceitar(SolicitacaoAcesso solicitacao) {
        View corpo = LayoutInflater.from(this).inflate(R.layout.dialog_senha_inicial, null);
        EditText edtSenha = corpo.findViewById(R.id.edtSenhaInicial);
        TextView txtMensagem = corpo.findViewById(R.id.txtMensagemSenhaInicial);
        txtMensagem.setText(getString(R.string.autorizacoes_dialogo_aceitar_mensagem, solicitacao.getNomeInstituicao()));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.autorizacoes_dialogo_aceitar_titulo)
                .setView(corpo)
                .setNegativeButton(R.string.autorizacoes_cancelar, null)
                .setPositiveButton(R.string.autorizacoes_confirmar, null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String senha = edtSenha.getText().toString();
            if (senha.length() < 6) {
                Toast.makeText(this, R.string.autorizacoes_erro_senha_curta, Toast.LENGTH_LONG).show();
                return;
            }
            dialog.dismiss();
            repositorio.aprovar(this, solicitacao, senha, new SolicitacaoAcessoRepository.OperacaoCallback() {
                @Override
                public void onSucesso() {
                    Toast.makeText(AutorizacoesActivity.this, R.string.autorizacoes_sucesso_aceitar, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onErro(Exception erro) {
                    boolean emailEmUso = erro instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException;
                    Toast.makeText(AutorizacoesActivity.this,
                            emailEmUso ? R.string.autorizacoes_erro_email_em_uso : R.string.autorizacoes_erro_generico,
                            Toast.LENGTH_LONG).show();
                }
            });
        }));
        dialog.show();
    }

    private void mostrarDialogoRejeitar(SolicitacaoAcesso solicitacao) {
        View corpo = LayoutInflater.from(this).inflate(R.layout.dialog_motivo_rejeicao, null);
        EditText edtMotivo = corpo.findViewById(R.id.edtMotivoRejeicao);

        new AlertDialog.Builder(this)
                .setTitle(R.string.autorizacoes_dialogo_rejeitar_titulo)
                .setView(corpo)
                .setNegativeButton(R.string.autorizacoes_cancelar, null)
                .setPositiveButton(R.string.autorizacoes_confirmar, (dialog, which) ->
                        repositorio.rejeitar(solicitacao, edtMotivo.getText().toString(), new SolicitacaoAcessoRepository.OperacaoCallback() {
                            @Override
                            public void onSucesso() {
                                Toast.makeText(AutorizacoesActivity.this, R.string.autorizacoes_sucesso_rejeitar, Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onErro(Exception erro) {
                                Toast.makeText(AutorizacoesActivity.this, R.string.autorizacoes_erro_generico, Toast.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }
}
