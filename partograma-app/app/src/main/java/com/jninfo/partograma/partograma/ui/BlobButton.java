package com.jninfo.partograma.partograma.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;

import com.jninfo.partograma.partograma.R;

/**
 * Botao principal "INICIAR", inspirado no efeito Blob Button (brasilcode.com.br), porem
 * adaptado a identidade do Partograma Digital e ao ambiente Android nativo:
 *
 * - Estado de repouso: pilula com gradiente rosa -> roxo -> teal, identica em espirito
 *   a referencia visual (icone de seta em circulo + texto "INICIAR").
 * - Feedback de toque (mobile-first, sem depender de hover): ao pressionar, alem do
 *   encolhimento elastico do botao, algumas "bolhas" translucidas pulsam atras do
 *   conteudo usando BlurMaskFilter (efeito gooey), simulando o filtro SVG "goo" da
 *   referencia. O BlurMaskFilter so funciona em camada de software, entao a view
 *   alterna para LAYER_TYPE_SOFTWARE apenas durante a curta animacao do pulso.
 * - Fallback: caso o blur nao seja suportado pelo dispositivo, os circulos ainda sao
 *   desenhados solidos (sem o efeito de fusao), preservando o gradiente, o icone, o
 *   texto e o clique -- ou seja, o botao nunca depende do efeito gooey para funcionar.
 */
public class BlobButton extends LinearLayout {

    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float blobProgress = 0f;
    private ValueAnimator pulseAnimator;

    public BlobButton(Context context) {
        super(context);
        init(context, null);
    }

    public BlobButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setWillNotDraw(false);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setClickable(true);
        setFocusable(true);

        float density = context.getResources().getDisplayMetrics().density;
        int vPad = Math.round(14 * density);
        setPadding(vPad, vPad, vPad, vPad);

        GradientDrawable pill = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFFE8608F, 0xFF9B7FC7, 0xFF5FB8AE});
        pill.setCornerRadius(200 * density);
        setBackground(pill);

        blobPaint.setColor(0x55FFFFFF);
        blobPaint.setStyle(Paint.Style.FILL);

        int circleSize = Math.round(30 * density);
        FrameLayoutCircle circleBg = new FrameLayoutCircle(context);
        LayoutParams circleParams = new LayoutParams(circleSize, circleSize);
        circleParams.setMarginEnd(Math.round(10 * density));
        addView(circleBg, circleParams);

        ImageView arrow = new AppCompatImageView(context);
        arrow.setImageResource(R.drawable.ic_arrow_forward_thin);
        circleBg.addView(arrow, new FrameLayoutCircle.LayoutParams(
                FrameLayoutCircle.LayoutParams.MATCH_PARENT,
                FrameLayoutCircle.LayoutParams.MATCH_PARENT));

        TextView label = new TextView(context);
        label.setText(R.string.home_cta_iniciar);
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(18);
        label.setLetterSpacing(0.08f);
        label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
        addView(label, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                animatePress();
                return true;
            case MotionEvent.ACTION_UP:
                animateRelease();
                if (event.getX() >= 0 && event.getX() <= getWidth()
                        && event.getY() >= 0 && event.getY() <= getHeight()) {
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                animateRelease();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void animatePress() {
        animate().scaleX(0.96f).scaleY(0.96f).setDuration(120).start();
        startBlobPulse();
    }

    private void animateRelease() {
        animate().scaleX(1f).scaleY(1f).setDuration(180)
                .setInterpolator(new OvershootInterpolator(3f)).start();
    }

    /** Pulso "gooey" translucido, contido a area do botao, com fallback silencioso sem blur. */
    private void startBlobPulse() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        try {
            blobPaint.setMaskFilter(new BlurMaskFilter(
                    8 * getResources().getDisplayMetrics().density, BlurMaskFilter.Blur.NORMAL));
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } catch (Throwable ignored) {
            // Dispositivo sem suporte a BlurMaskFilter: segue sem o efeito de fusao.
            blobPaint.setMaskFilter(null);
        }

        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(360);
        pulseAnimator.addUpdateListener(animation -> {
            blobProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setLayerType(View.LAYER_TYPE_NONE, null);
                blobProgress = 0f;
                invalidate();
            }
        });
        pulseAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (blobProgress <= 0f) {
            return;
        }
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float maxRadius = Math.max(getWidth(), getHeight()) * 0.55f;
        float radius = maxRadius * blobProgress;
        int alpha = Math.round(90 * (1f - blobProgress));
        blobPaint.setAlpha(alpha);
        canvas.drawCircle(cx, cy, radius, blobPaint);
    }

    /** FrameLayout simples com fundo circular branco translucido para o icone de seta. */
    private static class FrameLayoutCircle extends android.widget.FrameLayout {
        FrameLayoutCircle(Context context) {
            super(context);
            android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setStroke(Math.round(1.6f * context.getResources().getDisplayMetrics().density), 0xFFFFFFFF);
            setBackground(circle);
        }
    }
}
