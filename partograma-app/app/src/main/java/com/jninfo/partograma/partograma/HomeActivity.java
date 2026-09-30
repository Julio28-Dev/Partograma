package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.jninfo.partograma.partograma.data.SessaoUtil;
import com.jninfo.partograma.partograma.ui.BlobButton;
import com.jninfo.partograma.partograma.ui.LightRaysView;

/**
 * Tela inicial de marca ("Home") do Partograma Digital, reproduzindo a composicao da
 * referencia visual (ver /refe.jpg): logo, titulo, cartao de recursos, ilustracao e o
 * botao "INICIAR".
 *
 * Fluxo atual (autenticacao institucional agora e OBRIGATORIA para acessar dados
 * protegidos -- substitui a fase anterior, em que uma sessao anonima bastava): INICIAR
 * verifica se ja existe sessao valida e recente (ver {@link SessaoUtil}); se sim, vai
 * direto para Pacientes sem pedir nada de novo; se nao, manda para {@link LoginActivity}.
 * Nao tenta mais autenticar anonimamente aqui -- login anonimo ficou reservado
 * exclusivamente para quem ainda nao tem conta enviar uma solicitacao de acesso (ver
 * {@link SolicitarAcessoActivity}), nunca para entrar em Pacientes.
 */
public class HomeActivity extends AppCompatActivity {

    /** Mesma curva de easing do efeito de entrada da Splash, para os dois momentos
     *  do aplicativo parecerem parte de um unico sistema de movimento. */
    private static final PathInterpolator ENTRANCE_EASING = new PathInterpolator(0.625f, 0.05f, 0f, 1f);
    private static final long ENTRANCE_DURATION_MS = 420L;
    private static final long ENTRANCE_STAGGER_MS = 55L;
    private static final float ENTRANCE_RISE_DP = 22f;

    private LightRaysView lightRaysView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        lightRaysView = findViewById(R.id.lightRaysView);

        Typeface interRegular = ResourcesCompat.getFont(this, R.font.inter_regular);
        TextView tagline = findViewById(R.id.taglineText);
        if (interRegular != null) {
            tagline.setTypeface(Typeface.create(interRegular, Typeface.ITALIC));
        }

        bindFeature(R.id.feature1, R.drawable.ic_feature_clock, R.string.home_feature_registro);
        bindFeature(R.id.feature2, R.drawable.ic_feature_pulse, R.string.home_feature_avaliacao);
        bindFeature(R.id.feature3, R.drawable.ic_feature_chart, R.string.home_feature_dilatacao);
        bindFeature(R.id.feature4, R.drawable.ic_feature_rotate, R.string.home_feature_rotatividade);
        bindFeature(R.id.feature5, R.drawable.ic_feature_pie, R.string.home_feature_graficos);

        BlobButton btnIniciar = findViewById(R.id.btnIniciar);
        btnIniciar.setOnClickListener(v -> iniciar(btnIniciar));

        playEntranceAnimation();
    }

    /**
     * "INICIAR": sessao valida e recente -> Pacientes direto, sem pedir nada de novo.
     * Qualquer outro caso (nunca logou, fez logout, sessao anonima, expirada ha mais de
     * 30 dias) -> LoginActivity, que por sua vez decide entre mostrar o formulario ou
     * oferecer "Solicitar acesso".
     */
    private void iniciar(View origem) {
        origem.setEnabled(false);
        if (SessaoUtil.sessaoValidaLocalmente(this)) {
            SessaoUtil.registrarAcessoValido(this);
            irParaPacientes();
        } else {
            SessaoUtil.encerrarSessao(this);
            irParaLogin();
        }
    }

    private void irParaPacientes() {
        startActivity(new Intent(HomeActivity.this, PacientesActivity.class));
        finish();
    }

    private void irParaLogin() {
        startActivity(new Intent(HomeActivity.this, LoginActivity.class));
        finish();
    }

    /** Fade + leve deslocamento vertical, em cascata por secao, ao abrir a Home. */
    private void playEntranceAnimation() {
        ViewGroup content = findViewById(R.id.homeContent);
        float density = getResources().getDisplayMetrics().density;
        float risePx = ENTRANCE_RISE_DP * density;

        int childCount = content.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = content.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(risePx);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * ENTRANCE_STAGGER_MS)
                    .setDuration(ENTRANCE_DURATION_MS)
                    .setInterpolator(ENTRANCE_EASING)
                    .start();
        }
    }

    private void bindFeature(int includeId, int iconRes, int labelRes) {
        View root = findViewById(includeId);
        ImageView icon = root.findViewById(R.id.iconFeature);
        TextView label = root.findViewById(R.id.labelFeature);
        icon.setImageResource(iconRes);
        label.setText(labelRes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        lightRaysView.resumeAnimation();
    }

    @Override
    protected void onPause() {
        lightRaysView.pauseAnimation();
        super.onPause();
    }
}
