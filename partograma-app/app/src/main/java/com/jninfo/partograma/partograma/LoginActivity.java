package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jninfo.partograma.partograma.data.InstituicaoRepository;
import com.jninfo.partograma.partograma.data.SessaoUtil;

/**
 * Login institucional: cada instituicao tem uma conta de e-mail/senha, criada manualmente
 * no Firebase Console (o admin) ou pela aprovacao de uma solicitacao de acesso (ver
 * AutorizacoesActivity) -- este app nao tem cadastro publico direto, so login + pedido de
 * acesso sujeito a aprovacao.
 *
 * Tela EXCLUIDA do portao de sessao do BaseActivity (precisaSessaoValida() = false): e
 * exatamente a tela que precisa continuar acessivel para quem NAO tem sessao valida. Se
 * ja existir sessao valida e recente (ver SessaoUtil), pula direto para Pacientes sem
 * mostrar o formulario.
 */
public class LoginActivity extends BaseActivity {

    private EditText edtLogin;
    private EditText edtSenha;
    private TextView txtErro;
    private View btnEntrar;
    private ProgressBar progress;
    private InstituicaoRepository instituicaoRepositorio;

    @Override
    protected boolean precisaSessaoValida() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (SessaoUtil.sessaoValidaLocalmente(this)) {
            irParaPacientes();
            return;
        }
        // Sessao invalida/inexistente: garante estado limpo (ex.: sessao anonima de
        // bootstrap, ou expirada por inatividade) antes de mostrar o formulario.
        SessaoUtil.encerrarSessao(this);

        setContentView(R.layout.activity_login);
        instituicaoRepositorio = new InstituicaoRepository();

        edtLogin = findViewById(R.id.edtLogin);
        edtSenha = findViewById(R.id.edtSenha);
        txtErro = findViewById(R.id.txtErroLogin);
        btnEntrar = findViewById(R.id.btnEntrar);
        progress = findViewById(R.id.progressLogin);

        btnEntrar.setOnClickListener(v -> tentarEntrar());
        findViewById(R.id.btnSolicitarAcesso).setOnClickListener(v ->
                startActivity(new Intent(this, SolicitarAcessoActivity.class)));
        findViewById(R.id.btnEsqueciSenha).setOnClickListener(v -> enviarEmailRedefinicao());
    }

    /**
     * "Esqueci minha senha": usa o fluxo oficial do Firebase (e-mail com link de
     * redefinicao) -- nunca mostra, guarda ou envia a senha atual/nova por fora do
     * mecanismo do proprio Firebase Auth.
     */
    private void enviarEmailRedefinicao() {
        String email = edtLogin.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            mostrarErro(getString(R.string.login_esqueci_senha_informe_email));
            return;
        }
        alternarCarregando(true);
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    alternarCarregando(false);
                    Toast.makeText(this, R.string.login_esqueci_senha_enviado, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(erro -> {
                    alternarCarregando(false);
                    mostrarErro(getString(R.string.login_esqueci_senha_erro));
                });
    }

    private void tentarEntrar() {
        String email = edtLogin.getText().toString().trim();
        String senha = edtSenha.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(senha)) {
            mostrarErro(getString(R.string.login_erro_campos_vazios));
            return;
        }

        alternarCarregando(true);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, senha)
                .addOnSuccessListener(resultado -> {
                    String institutionId = InstituicaoRepository.instituicaoIdAtual();
                    if (institutionId == null) {
                        alternarCarregando(false);
                        mostrarErro(getString(R.string.login_erro_generico));
                        return;
                    }
                    verificarAprovacaoEEntrar(institutionId);
                })
                .addOnFailureListener(erro -> {
                    alternarCarregando(false);
                    mostrarErro(getString(R.string.login_erro_credenciais));
                });
    }

    /**
     * A autenticacao (e-mail/senha) por si so NAO significa acesso liberado: desde que a
     * conta da instituicao passou a ser criada ja no momento da solicitacao (ver
     * SolicitacaoAcessoRepository.solicitarAcesso), alguem com um pedido ainda pendente ou
     * ja rejeitado tambem consegue autenticar com sucesso aqui -- so nao deve conseguir
     * PASSAR daqui. Admin sempre passa (admins/{uid}); instituicao comum so passa quando
     * instituicoes/{uid} ja existe, o que so acontece quando o admin aprova (ver
     * AutorizacoesActivity). Pendente e rejeitada dao exatamente a mesma mensagem de
     * proposito -- nao expor pra quem esta tentando entrar qual dos dois casos e.
     */
    private void verificarAprovacaoEEntrar(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("admins").document(uid).get()
                .addOnSuccessListener(adminDoc -> {
                    if (adminDoc.exists()) {
                        garantirInstituicaoEEntrar(uid);
                        return;
                    }
                    db.collection("instituicoes").document(uid).get()
                            .addOnSuccessListener(instDoc -> {
                                if (instDoc.exists()) {
                                    irParaPacientes();
                                } else {
                                    alternarCarregando(false);
                                    FirebaseAuth.getInstance().signOut();
                                    mostrarErro(getString(R.string.login_erro_solicitacao_pendente));
                                }
                            })
                            .addOnFailureListener(erro -> {
                                alternarCarregando(false);
                                FirebaseAuth.getInstance().signOut();
                                mostrarErro(getString(R.string.login_erro_generico));
                            });
                })
                .addOnFailureListener(erro -> {
                    alternarCarregando(false);
                    FirebaseAuth.getInstance().signOut();
                    mostrarErro(getString(R.string.login_erro_generico));
                });
    }

    private void garantirInstituicaoEEntrar(String institutionId) {
        instituicaoRepositorio.garantirInstituicao(institutionId, new InstituicaoRepository.OperacaoCallback() {
            @Override
            public void onSucesso() {
                irParaPacientes();
            }

            @Override
            public void onErro(Exception erro) {
                // A autenticacao ja foi bem-sucedida; nao bloquear o acesso do admin so
                // porque a criacao do documento da instituicao falhou agora -- ela sera
                // tentada de novo na proxima vez que algo precisar dela.
                irParaPacientes();
            }
        });
    }

    private void irParaPacientes() {
        SessaoUtil.registrarAcessoValido(this);
        startActivity(new Intent(this, PacientesActivity.class));
        finish();
    }

    private void mostrarErro(String mensagem) {
        txtErro.setText(mensagem);
        txtErro.setVisibility(View.VISIBLE);
    }

    private void alternarCarregando(boolean carregando) {
        progress.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(!carregando);
        btnEntrar.setAlpha(carregando ? 0.6f : 1f);
        if (carregando) {
            txtErro.setVisibility(View.GONE);
        }
    }
}
