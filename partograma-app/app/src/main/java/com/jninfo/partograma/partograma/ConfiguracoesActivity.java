package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.jninfo.partograma.partograma.data.AppPreferences;
import com.jninfo.partograma.partograma.data.InstituicaoRepository;
import com.jninfo.partograma.partograma.data.SessaoUtil;
import com.jninfo.partograma.partograma.data.SolicitacaoAcessoRepository;

/**
 * Tela de Configurações do aplicativo. Nao e mais uma aba dentro dos detalhes de uma
 * paciente -- foi movida pra ca porque tema/fonte/notificacoes/cache sao preferencias do
 * aparelho, nao dados de uma paciente especifica, e a lista de abas exigida pelo cliente
 * para a tela de detalhes (Dados/Partograma/Relatório/Sinais vitais/Desfecho) e exaustiva
 * e nao inclui Configuracoes. Acessada a partir de um icone na tela "Pacientes".
 */
public class ConfiguracoesActivity extends BaseActivity {

    private AppPreferences preferencias;
    private InstituicaoRepository instituicaoRepositorio;
    private SolicitacaoAcessoRepository solicitacaoAcessoRepositorio;
    private ListenerRegistration listenerContagemPendentes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes);

        preferencias = new AppPreferences(this);
        instituicaoRepositorio = new InstituicaoRepository();
        solicitacaoAcessoRepositorio = new SolicitacaoAcessoRepository();
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        configurarFonte();
        configurarNotificacoes();
        configurarOutrasOpcoes();
        configurarInstituicao();
        configurarAutorizacoes();
    }

    @Override
    protected void onDestroy() {
        if (listenerContagemPendentes != null) {
            listenerContagemPendentes.remove();
        }
        super.onDestroy();
    }

    /**
     * Secao "Autorizacoes" (aprovar/rejeitar acesso institucional): so aparece para quem
     * tem papel admin (admins/{uid} no Firestore, ver firestore.rules). Nao e so cosmetico
     * -- mesmo que essa checagem falhe por algum motivo, o Firestore recusa qualquer
     * leitura/escrita de institutionAccessRequests para quem nao for admin.
     */
    private void configurarAutorizacoes() {
        View divisor = findViewById(R.id.divisorAutorizacoes);
        View linha = findViewById(R.id.btnAutorizacoes);
        solicitacaoAcessoRepositorio.verificarAdmin(ehAdmin -> {
            if (!ehAdmin) {
                return;
            }
            divisor.setVisibility(View.VISIBLE);
            linha.setVisibility(View.VISIBLE);
            linha.setOnClickListener(v -> startActivity(new Intent(this, AutorizacoesActivity.class)));

            TextView badge = findViewById(R.id.badgeAutorizacoes);
            listenerContagemPendentes = solicitacaoAcessoRepositorio.observarContagemPendentes(quantidade -> {
                if (quantidade > 0) {
                    badge.setText(String.valueOf(quantidade));
                    badge.setVisibility(View.VISIBLE);
                } else {
                    badge.setVisibility(View.GONE);
                }
            });
        });
    }

    private void configurarFonte() {
        View btnMenor = findViewById(R.id.btnFonteMenor);
        View btnPadrao = findViewById(R.id.btnFontePadrao);
        View btnMaior = findViewById(R.id.btnFonteMaior);
        View[] botoes = {btnMenor, btnPadrao, btnMaior};
        float[] valores = {AppPreferences.ESCALA_FONTE_MENOR, AppPreferences.ESCALA_FONTE_PADRAO, AppPreferences.ESCALA_FONTE_MAIOR};
        aplicarSelecaoSegmento(botoes, indiceFonte(preferencias.getEscalaFonte()));
        for (int i = 0; i < botoes.length; i++) {
            int indice = i;
            botoes[i].setOnClickListener(v -> {
                preferencias.setEscalaFonte(valores[indice]);
                aplicarSelecaoSegmento(botoes, indice);
                recreate();
            });
        }
    }

    private void configurarNotificacoes() {
        Switch switchLembretes = findViewById(R.id.switchNotifLembretes);
        Switch switchDilatacao = findViewById(R.id.switchNotifDilatacao);
        Switch switchSistema = findViewById(R.id.switchNotifSistema);
        switchLembretes.setChecked(preferencias.isNotifLembretesAtivo());
        switchDilatacao.setChecked(preferencias.isNotifDilatacaoAtivo());
        switchSistema.setChecked(preferencias.isNotifSistemaAtivo());
        switchLembretes.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> preferencias.setNotifLembretesAtivo(checked));
        switchDilatacao.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> preferencias.setNotifDilatacaoAtivo(checked));
        switchSistema.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> preferencias.setNotifSistemaAtivo(checked));
    }

    private void configurarOutrasOpcoes() {
        findViewById(R.id.btnLimparCache).setOnClickListener(v -> limparCache());
        findViewById(R.id.btnSobreApp).setOnClickListener(v -> mostrarSobre());
        findViewById(R.id.btnGerenciarAlertas).setOnClickListener(v -> startActivity(new Intent(this, AlertasActivity.class)));
        findViewById(R.id.btnSair).setOnClickListener(v -> confirmarSaida());
    }

    /**
     * Login/senha da instituicao (secao 5.1 do briefing): se ja houver uma sessao
     * institucional real (nao anonima), mostra troca de senha; senao, mostra um mini-login
     * -- a mesma logica do LoginActivity, so que aqui a tela nao e obrigatoria para acessar
     * Pacientes, e sim um lugar para configurar a credencial quando/se a instituicao quiser.
     */
    private void configurarInstituicao() {
        View containerConectada = findViewById(R.id.containerInstituicaoConectada);
        View containerLogin = findViewById(R.id.containerInstituicaoLogin);
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        boolean logadaInstitucionalmente = usuario != null && !usuario.isAnonymous() && usuario.getEmail() != null;

        containerConectada.setVisibility(logadaInstitucionalmente ? View.VISIBLE : View.GONE);
        containerLogin.setVisibility(logadaInstitucionalmente ? View.GONE : View.VISIBLE);

        if (logadaInstitucionalmente) {
            ((TextView) findViewById(R.id.txtInstituicaoConectada))
                    .setText(getString(R.string.config_instituicao_conectado, usuario.getEmail()));
            findViewById(R.id.btnSalvarSenhaInstituicao).setOnClickListener(v -> salvarNovaSenhaInstituicao());
        } else {
            findViewById(R.id.btnEntrarInstituicao).setOnClickListener(v -> entrarInstituicao());
        }
    }

    private void entrarInstituicao() {
        EditText edtEmail = findViewById(R.id.edtLoginInstituicao);
        EditText edtSenha = findViewById(R.id.edtSenhaInstituicao);
        TextView txtErro = findViewById(R.id.txtErroInstituicao);
        ProgressBar progress = findViewById(R.id.progressInstituicao);

        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(senha)) {
            txtErro.setText(R.string.login_erro_campos_vazios);
            txtErro.setVisibility(View.VISIBLE);
            return;
        }

        txtErro.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, senha)
                .addOnSuccessListener(resultado -> {
                    String institutionId = InstituicaoRepository.instituicaoIdAtual();
                    SessaoUtil.registrarAcessoValido(this);
                    if (institutionId != null) {
                        instituicaoRepositorio.garantirInstituicao(institutionId, new InstituicaoRepository.OperacaoCallback() {
                            @Override
                            public void onSucesso() {
                                recreate();
                            }

                            @Override
                            public void onErro(Exception erro) {
                                recreate();
                            }
                        });
                    } else {
                        recreate();
                    }
                })
                .addOnFailureListener(erro -> {
                    progress.setVisibility(View.GONE);
                    txtErro.setText(R.string.login_erro_credenciais);
                    txtErro.setVisibility(View.VISIBLE);
                });
    }

    private void salvarNovaSenhaInstituicao() {
        EditText edtNova = findViewById(R.id.edtNovaSenhaInstituicao);
        EditText edtConfirmar = findViewById(R.id.edtConfirmarSenhaInstituicao);
        ProgressBar progress = findViewById(R.id.progressInstituicao);

        String novaSenha = edtNova.getText().toString();
        String confirmacao = edtConfirmar.getText().toString();
        if (novaSenha.length() < 6) {
            Toast.makeText(this, R.string.config_instituicao_senha_curta, Toast.LENGTH_LONG).show();
            return;
        }
        if (!novaSenha.equals(confirmacao)) {
            Toast.makeText(this, R.string.config_instituicao_senhas_diferentes, Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            return;
        }
        progress.setVisibility(View.VISIBLE);
        usuario.updatePassword(novaSenha)
                .addOnSuccessListener(unused -> {
                    progress.setVisibility(View.GONE);
                    edtNova.setText("");
                    edtConfirmar.setText("");
                    Toast.makeText(this, R.string.config_instituicao_senha_salva, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(erro -> {
                    progress.setVisibility(View.GONE);
                    if (erro instanceof com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                        Toast.makeText(this, R.string.config_instituicao_reautenticar, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.alertas_erro_salvar, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void confirmarSaida() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.config_sair_confirmar_titulo)
                .setMessage(R.string.config_sair_confirmar_mensagem)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.config_sair, (dialog, which) -> sair())
                .create()
                .show();
    }

    private void sair() {
        SessaoUtil.encerrarSessao(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int indiceFonte(float escala) {
        if (escala == AppPreferences.ESCALA_FONTE_MENOR) return 0;
        if (escala == AppPreferences.ESCALA_FONTE_MAIOR) return 2;
        return 1;
    }

    private void aplicarSelecaoSegmento(View[] botoes, int indiceSelecionado) {
        for (int i = 0; i < botoes.length; i++) {
            android.widget.TextView texto = (android.widget.TextView) botoes[i];
            boolean selecionado = i == indiceSelecionado;
            texto.setBackgroundResource(selecionado ? R.drawable.bg_segment_selected : R.drawable.bg_segment_unselected);
            texto.setTextColor(ContextCompat.getColor(this, selecionado ? R.color.primaryPink : R.color.textSecondary));
            texto.setTypeface(null, selecionado ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void limparCache() {
        try {
            deleteCacheDir(getCacheDir());
        } finally {
            Toast.makeText(this, R.string.config_limpar_cache_sucesso, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteCacheDir(java.io.File dir) {
        java.io.File[] arquivos = dir.listFiles();
        if (arquivos == null) {
            return;
        }
        for (java.io.File arquivo : arquivos) {
            if (arquivo.isDirectory()) {
                deleteCacheDir(arquivo);
            } else {
                arquivo.delete();
            }
        }
    }

    private void mostrarSobre() {
        String versao;
        try {
            versao = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            versao = "1.0";
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage("Partograma Digital\nVersão " + versao + "\n\nO apoio que o enfermeiro precisa para um parto seguro e humanizado.")
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }
}
