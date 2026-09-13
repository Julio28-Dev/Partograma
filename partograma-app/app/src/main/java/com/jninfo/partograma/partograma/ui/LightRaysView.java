package com.jninfo.partograma.partograma.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Atmosfera de luz suave atras do conteudo da Home, inspirada no efeito "Light Rays"
 * (spell.sh/docs/light-rays), reimplementada nativamente para Android com foco em
 * performance: um unico SweepGradient (calculado uma vez) e rotacionado via matriz a
 * cada frame -- sem recriar shaders, sem blur, com um unico desenho de circulo por frame.
 *
 * A View nunca intercepta toque (nao define OnClickListener nem sobrescreve
 * onTouchEvent), entao fica naturalmente "atras" da interface sem bloquear cliques.
 */
public class LightRaysView extends View {

    private static final int[] RAY_COLORS = new int[]{
            0x00FFFFFF,
            0x3AF2A6C6, // rosa suave
            0x00FFFFFF,
            0x30C9A9E0, // lilas suave
            0x00FFFFFF,
            0x309B7FC7, // roxo suave
            0x00FFFFFF,
            0x305FB8AE, // teal suave
            0x00FFFFFF,
    };

    private long rotationDurationMs = 26000L;
    private float reach = 1.15f; // multiplicador do raio em relacao a maior dimensao da view

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix shaderMatrix = new Matrix();
    private SweepGradient sweepGradient;
    private float centerX;
    private float centerY;
    private float radius;
    private float rotationDeg;
    private ValueAnimator animator;

    public LightRaysView(Context context) {
        super(context);
    }

    public LightRaysView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** "intensity": opacidade global do efeito (0..1). Mantido discreto por padrao. */
    public void setIntensity(float intensity) {
        paint.setAlpha(Math.round(255 * Math.max(0f, Math.min(1f, intensity))));
        invalidate();
    }

    /** "reach": o quanto os raios se estendem para alem do centro da view. */
    public void setReach(float reach) {
        this.reach = reach;
        rebuildShader();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h * 0.35f;
        radius = Math.max(w, h) * reach;
        rebuildShader();
    }

    private void rebuildShader() {
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        sweepGradient = new SweepGradient(centerX, centerY, RAY_COLORS, null);
        paint.setShader(sweepGradient);
        if (paint.getAlpha() == 255) {
            paint.setAlpha(255);
        }
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    /** Deve ser chamado a partir do onPause() da Activity hospedeira para economizar bateria/GPU. */
    public void pauseAnimation() {
        if (animator != null) {
            animator.pause();
        }
    }

    /** Deve ser chamado a partir do onResume() da Activity hospedeira. */
    public void resumeAnimation() {
        if (animator == null) {
            startAnimation();
        } else if (animator.isPaused()) {
            animator.resume();
        }
    }

    private void startAnimation() {
        if (animator != null) {
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(rotationDurationMs);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            rotationDeg = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void stopAnimation() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sweepGradient == null) {
            return;
        }
        shaderMatrix.setRotate(rotationDeg, centerX, centerY);
        sweepGradient.setLocalMatrix(shaderMatrix);
        canvas.drawCircle(centerX, centerY, radius, paint);
    }
}
