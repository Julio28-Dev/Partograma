package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.jninfo.partograma.partograma.data.AppPreferences;

/**
 * Tela de Configurações do aplicativo. Nao e mais uma aba dentro dos detalhes de uma
 * paciente -- foi movida pra ca porque tema/fonte/notificacoes/cache sao preferencias do
 * aparelho, nao dados de uma paciente especifica, e a lista de abas exigida pelo cliente
 * para a tela de detalhes (Dados/Partograma/Relatório/Sinais vitais/Desfecho) e exaustiva
 * e nao inclui Configuracoes. Acessada a partir de um icone na tela "Pacientes".
 */
public class ConfiguracoesActivity extends BaseActivity {

    private AppPreferences preferencias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes);

        preferencias = new AppPreferences(this);
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        configurarTema();
        configurarFonte();
        configurarNotificacoes();
        configurarOutrasOpcoes();
    }

    private void configurarTema() {
        View btnClaro = findViewById(R.id.btnTemaClaro);
        View btnEscuro = findViewById(R.id.btnTemaEscuro);
        View btnAutomatico = findViewById(R.id.btnTemaAutomatico);
        View[] botoes = {btnClaro, btnEscuro, btnAutomatico};
        String[] valores = {AppPreferences.TEMA_CLARO, AppPreferences.TEMA_ESCURO, AppPreferences.TEMA_AUTOMATICO};
        aplicarSelecaoSegmento(botoes, indiceTema(preferencias.getTema()));
        for (int i = 0; i < botoes.length; i++) {
            int indice = i;
            botoes[i].setOnClickListener(v -> {
                preferencias.setTema(valores[indice]);
                aplicarSelecaoSegmento(botoes, indice);
                Toast.makeText(this, R.string.config_reiniciar_aviso, Toast.LENGTH_LONG).show();
            });
        }
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
        findViewById(R.id.btnAjudaSuporte).setOnClickListener(v -> startActivity(new Intent(this, TelaInicial.class)));
    }

    private int indiceTema(String tema) {
        if (AppPreferences.TEMA_ESCURO.equals(tema)) return 1;
        if (AppPreferences.TEMA_AUTOMATICO.equals(tema)) return 2;
        return 0;
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
