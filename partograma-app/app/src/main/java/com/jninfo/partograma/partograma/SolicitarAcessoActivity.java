package com.jninfo.partograma.partograma;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.jninfo.partograma.partograma.data.SolicitacaoAcessoRepository;

/**
 * Tela publica (sem login previo) para uma instituicao pedir acesso ao app. A conta
 * Firebase Auth (e-mail + senha escolhidos aqui) e criada JA NESTA ETAPA -- ver
 * {@link SolicitacaoAcessoRepository#solicitarAcesso} -- para que, depois de aprovada, a
 * instituicao entre com a MESMA credencial que definiu aqui, sem o admin precisar inventar
 * senha nenhuma. Isso nao concede acesso na hora: enquanto o pedido nao for aprovado, o
 * login continua bloqueado (ver LoginActivity), mesmo a conta ja existindo.
 */
public class SolicitarAcessoActivity extends BaseActivity {

    @Override
    protected boolean precisaSessaoValida() {
        return false;
    }

    private EditText edtNomeInstituicao;
    private EditText edtResponsavel;
    private EditText edtEmailSolicitacao;
    private EditText edtTelefoneSolicitacao;
    private EditText edtSenhaSolicitacao;
    private EditText edtConfirmarSenhaSolicitacao;
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
        edtSenhaSolicitacao = findViewById(R.id.edtSenhaSolicitacao);
        edtConfirmarSenhaSolicitacao = findViewById(R.id.edtConfirmarSenhaSolicitacao);
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
        String senha = edtSenhaSolicitacao.getText().toString();
        String confirmarSenha = edtConfirmarSenhaSolicitacao.getText().toString();

        if (TextUtils.isEmpty(nomeInstituicao) || TextUtils.isEmpty(responsavel) || TextUtils.isEmpty(email)) {
            mostrarErro(getString(R.string.solicitar_acesso_erro_campos_vazios));
            return;
        }
        if (senha.length() < 6) {
            mostrarErro(getString(R.string.solicitar_acesso_erro_senha_curta));
            return;
        }
        if (!senha.equals(confirmarSenha)) {
            mostrarErro(getString(R.string.solicitar_acesso_erro_senhas_diferentes));
            return;
        }

        alternarCarregando(true);
        repositorio.solicitarAcesso(this, nomeInstituicao, responsavel, email, telefone, senha,
                new SolicitacaoAcessoRepository.OperacaoCallback() {
                    @Override
                    public void onSucesso() {
                        alternarCarregando(false);
                        mostrarSucesso();
                    }

                    @Override
                    public void onErro(Exception erro) {
                        alternarCarregando(false);
                        mostrarErro(getString(mensagemDeErro(erro)));
                    }
                });
    }

    private int mensagemDeErro(Exception erro) {
        if (erro instanceof FirebaseAuthUserCollisionException) {
            return R.string.solicitar_acesso_erro_email_em_uso;
        }
        if (erro instanceof FirebaseAuthWeakPasswordException) {
            return R.string.solicitar_acesso_erro_senha_curta;
        }
        if (erro instanceof FirebaseAuthInvalidCredentialsException) {
            return R.string.solicitar_acesso_erro_email_invalido;
        }
        return R.string.solicitar_acesso_erro_generico;
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
