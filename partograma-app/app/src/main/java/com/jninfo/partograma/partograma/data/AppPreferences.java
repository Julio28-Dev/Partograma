package com.jninfo.partograma.partograma.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Preferencias do APLICATIVO (tema, tamanho de fonte, notificacoes), separadas de
 * {@link PatientRepository} (dados do paciente original) e do Firestore (dados de
 * pacientes na nuvem). Estas sao configuracoes do aparelho/instalacao, nao dados
 * clinicos -- por isso ficam SOMENTE em SharedPreferences local, nunca no Firestore,
 * conforme pedido explicitamente na etapa de Configuracoes.
 */
public class AppPreferences {

    private static final String ARQUIVO = "ConfiguracoesApp";

    private static final String CHAVE_TEMA = "tema";
    private static final String CHAVE_ESCALA_FONTE = "escala_fonte";
    private static final String CHAVE_NOTIF_LEMBRETES = "notif_lembretes";
    private static final String CHAVE_NOTIF_DILATACAO = "notif_dilatacao";
    private static final String CHAVE_NOTIF_SISTEMA = "notif_sistema";

    public static final String TEMA_CLARO = "claro";
    public static final String TEMA_ESCURO = "escuro";
    public static final String TEMA_AUTOMATICO = "automatico";

    public static final float ESCALA_FONTE_MENOR = 0.88f;
    public static final float ESCALA_FONTE_PADRAO = 1.0f;
    public static final float ESCALA_FONTE_MAIOR = 1.15f;

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public String getTema() {
        return prefs.getString(CHAVE_TEMA, TEMA_CLARO);
    }

    public void setTema(String tema) {
        prefs.edit().putString(CHAVE_TEMA, tema).apply();
        aplicarTema(tema);
    }

    /** Aplica o modo noturno do AppCompat de acordo com a preferencia salva. */
    public static void aplicarTemaSalvo(Context context) {
        aplicarTema(new AppPreferences(context).getTema());
    }

    private static void aplicarTema(String tema) {
        switch (tema) {
            case TEMA_ESCURO:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case TEMA_AUTOMATICO:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case TEMA_CLARO:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }

    public float getEscalaFonte() {
        return prefs.getFloat(CHAVE_ESCALA_FONTE, ESCALA_FONTE_PADRAO);
    }

    public void setEscalaFonte(float escala) {
        prefs.edit().putFloat(CHAVE_ESCALA_FONTE, escala).apply();
    }

    public boolean isNotifLembretesAtivo() {
        return prefs.getBoolean(CHAVE_NOTIF_LEMBRETES, true);
    }

    public void setNotifLembretesAtivo(boolean ativo) {
        prefs.edit().putBoolean(CHAVE_NOTIF_LEMBRETES, ativo).apply();
    }

    public boolean isNotifDilatacaoAtivo() {
        return prefs.getBoolean(CHAVE_NOTIF_DILATACAO, true);
    }

    public void setNotifDilatacaoAtivo(boolean ativo) {
        prefs.edit().putBoolean(CHAVE_NOTIF_DILATACAO, ativo).apply();
    }

    public boolean isNotifSistemaAtivo() {
        return prefs.getBoolean(CHAVE_NOTIF_SISTEMA, false);
    }

    public void setNotifSistemaAtivo(boolean ativo) {
        prefs.edit().putBoolean(CHAVE_NOTIF_SISTEMA, ativo).apply();
    }
}
