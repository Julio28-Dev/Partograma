package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.jninfo.partograma.partograma.ui.SignatureTextView;

/**
 * Nova tela de abertura do aplicativo, exibida antes da Home. Mostra a assinatura
 * "Partograma Digital" sendo desenhada (ver {@link SignatureTextView}) com a fonte
 * Lastoria Bold e, ao concluir, navega para {@link HomeActivity}.
 *
 * Fluxo: SPLASH -> HOME -> (INICIAR) -> restante do aplicativo (TelaInicial/Menu),
 * sem alterar nenhuma tela ou logica de negocio existente.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long INITIAL_DELAY_MS = 300L;
    private static final long STROKE_DURATION_MS = 1600L;
    private static final long HOLD_DURATION_MS = 450L;

    private View root;
    private SignatureTextView signatureView;
    private boolean navigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        root = findViewById(R.id.splashRoot);
        signatureView = findViewById(R.id.signatureView);

        Typeface signatureTypeface = ResourcesCompat.getFont(this, R.font.lastoria_bold_regular);
        signatureView.setTypeface(signatureTypeface);
        signatureView.setText(getString(R.string.splash_signature));
        signatureView.setOnFinishedListener(this::goToHome);

        root.setAlpha(0f);
        root.animate().alpha(1f).setDuration(400).start();

        signatureView.postDelayed(
                () -> signatureView.start(STROKE_DURATION_MS, HOLD_DURATION_MS),
                INITIAL_DELAY_MS);
    }

    private void goToHome() {
        if (navigated || isFinishing()) {
            return;
        }
        navigated = true;
        startActivity(new Intent(this, HomeActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        signatureView.cancelAnimation();
        super.onDestroy();
    }
}
