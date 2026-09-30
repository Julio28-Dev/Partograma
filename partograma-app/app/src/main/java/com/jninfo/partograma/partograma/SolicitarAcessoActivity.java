package com.jninfo.partograma.partograma;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.jninfo.partograma.partograma.data.SolicitacaoAcessoRepository;

/**
 * Tela publica (sem login previo) para uma instituicao pedir acesso ao app. Nao concede
 * acesso na hora -- so grava o pedido como "pendente" para o admin revisar em
 * Configuracoes -> Autorizacoes (ver SolicitacaoAcessoRepository).
 *
 * Exige alguma sessao Firebase para poder escrever no Firestore (ver firestore.rules);
 * usa a sessao anonima ja aberta pelo bootstrap do app (PartogramaApplication) ou tenta
 * abrir uma na hora, igual ao padrao ja usado em HomeActivity.
 */
public class SolicitarAcessoActivity extends BaseActivity {

    private EditText edtNomeInstituicao;
    private EditText edtResponsavel;
    private EditText edtEmailSolicitacao;
    private EditText edtTelefoneSolicitacao;
    private TextView txtErro;
    private View btnEnviar;
    private ProgressBar progress;
    private SolicitacaoAcessoRepository repositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitar_acesso);

        repositorio = new SolicitacaoAcessoRepository();
        edtNomeInstituicao = findViewById(R.id.edtNomeInstituicao);
        edtResponsavel = findViewById(R.id.edtResponsavel);
        edtEmailSolicitacao = findViewById(R.id.edtEmailSolicitacao);
        edtTelefoneSolicitacao = findViewById(R.id.edtTelefoneSolicitacao);
        txtErro = findViewById(R.id.txtErroSolicitarAcesso);
        btnEnviar = findViewById(R.id.btnEnviarSolicitacao);
        progress = findViewById(R.id.progressSolicitarAcesso);

        findViewById(R.id.btnVoltarSolicitarAcesso).setOnClickListener(v -> finish());
        btnEnviar.setOnClickListener(v -> validarEEnviar());
    }

    private void validarEEnviar() {
        String nomeInstituicao = edtNomeInstituicao.getText().toString().trim();
        String responsavel = edtResponsavel.getText().toString().trim();
        String email = edtEmailSolicitacao.getText().toString().trim();
        String telefone = edtTelefoneSolicitacao.getText().toString().trim();

        if (TextUtils.isEmpty(nomeInstituicao) || TextUtils.isEmpty(responsavel) || TextUtils.isEmpty(email)) {
            mostrarErro(getString(R.string.solicitar_acesso_erro_campos_vazios));
            return;
        }

        alternarCarregando(true);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            enviarSolicitacao(nomeInstituicao, responsavel, email, telefone);
        } else {
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener(resultado -> enviarSolicitacao(nomeInstituicao, responsavel, email, telefone))
                    .addOnFailureListener(erro -> {
                        alternarCarregando(false);
                        mostrarErro(getString(R.string.solicitar_acesso_erro_generico));
                    });
        }
    }

    private void enviarSolicitacao(String nomeInstituicao, String responsavel, String email, String telefone) {
        repositorio.solicitarAcesso(nomeInstituicao, responsavel, email, telefone, new SolicitacaoAcessoRepository.OperacaoCallback() {
            @Override
            public void onSucesso() {
                alternarCarregando(false);
                mostrarSucesso();
            }

            @Override
            public void onErro(Exception erro) {
                alternarCarregando(false);
                mostrarErro(getString(R.string.solicitar_acesso_erro_generico));
            }
        });
    }

    private void mostrarSucesso() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.solicitar_acesso_sucesso_titulo)
                .setMessage(R.string.solicitar_acesso_sucesso_mensagem)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                .show();
    }

    private void mostrarErro(String mensagem) {
        txtErro.setText(mensagem);
        txtErro.setVisibility(View.VISIBLE);
    }

    private void alternarCarregando(boolean carregando) {
        progress.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnEnviar.setEnabled(!carregando);
        btnEnviar.setAlpha(carregando ? 0.6f : 1f);
        if (carregando) {
            txtErro.setVisibility(View.GONE);
        }
    }
}
