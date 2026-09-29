package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.jninfo.partograma.partograma.data.InstituicaoRepository;

/**
 * Login institucional: cada instituicao tem uma conta de e-mail/senha criada previamente no
 * Firebase Console (Authentication -> Email/senha) -- este app nao tem tela de cadastro
 * publico, so login, conforme pedido explicitamente ("nao inventar cadastro publico").
 *
 * Se ja existir uma sessao valida (nao anonima) salva pelo SDK do Firebase, pula direto para
 * Pacientes sem mostrar o formulario -- e a "sessao preservada" entre aberturas do app.
 */
public class LoginActivity extends BaseActivity {

    private EditText edtLogin;
    private EditText edtSenha;
    private TextView txtErro;
    private View btnEntrar;
    private ProgressBar progress;
    private InstituicaoRepository instituicaoRepositorio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser usuarioAtual = FirebaseAuth.getInstance().getCurrentUser();
        if (usuarioAtual != null && !usuarioAtual.isAnonymous()) {
            irParaPacientes();
            return;
        }

        setContentView(R.layout.activity_login);
        instituicaoRepositorio = new InstituicaoRepository();

        edtLogin = findViewById(R.id.edtLogin);
        edtSenha = findViewById(R.id.edtSenha);
        txtErro = findViewById(R.id.txtErroLogin);
        btnEntrar = findViewById(R.id.btnEntrar);
        progress = findViewById(R.id.progressLogin);

        btnEntrar.setOnClickListener(v -> tentarEntrar());
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
                    instituicaoRepositorio.garantirInstituicao(institutionId, new InstituicaoRepository.OperacaoCallback() {
                        @Override
                        public void onSucesso() {
                            irParaPacientes();
                        }

                        @Override
                        public void onErro(Exception erro) {
                            // A autenticacao ja foi bem-sucedida; nao bloquear o acesso so
                            // porque a criacao do documento da instituicao falhou agora --
                            // ela sera tentada de novo na proxima vez que algo precisar dela.
                            irParaPacientes();
                        }
                    });
                })
                .addOnFailureListener(erro -> {
                    alternarCarregando(false);
                    mostrarErro(getString(R.string.login_erro_credenciais));
                });
    }

    private void irParaPacientes() {
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
