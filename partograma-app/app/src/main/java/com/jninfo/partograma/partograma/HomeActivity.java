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

import com.jninfo.partograma.partograma.ui.BlobButton;
import com.jninfo.partograma.partograma.ui.LightRaysView;

/**
 * Tela inicial de marca ("Home") do Partograma Digital, reproduzindo a composicao da
 * referencia visual (ver /refe.jpg): logo, titulo, cartao de recursos, ilustracao e o
 * botao "INICIAR". Ao tocar em "INICIAR", segue para o fluxo original do aplicativo
 * ({@link TelaInicial}), que continua decidindo sozinho se mostra o tutorial ou vai
 * direto para o {@link Menu} -- nenhuma logica existente foi alterada.
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

        Typeface signatureTypeface = ResourcesCompat.getFont(this, R.font.lastoria_bold_regular);
        TextView tagline = findViewById(R.id.taglineText);
        if (signatureTypeface != null) {
            tagline.setTypeface(signatureTypeface);
        }

        bindFeature(R.id.feature1, R.drawable.ic_feature_clock, R.string.home_feature_registro);
        bindFeature(R.id.feature2, R.drawable.ic_feature_pulse, R.string.home_feature_avaliacao);
        bindFeature(R.id.feature3, R.drawable.ic_feature_chart, R.string.home_feature_dilatacao);
        bindFeature(R.id.feature4, R.drawable.ic_feature_rotate, R.string.home_feature_rotatividade);
        bindFeature(R.id.feature5, R.drawable.ic_feature_pie, R.string.home_feature_graficos);

        BlobButton btnIniciar = findViewById(R.id.btnIniciar);
        btnIniciar.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, TelaInicial.class));
            finish();
        });

        playEntranceAnimation();
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
