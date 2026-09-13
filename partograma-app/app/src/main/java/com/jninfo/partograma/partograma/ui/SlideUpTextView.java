package com.jninfo.partograma.partograma.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;

/**
 * Efeito de abertura da Splash, inspirado no componente "Slide Up Text"
 * (spell.sh/docs/slide-up-text): o texto e dividido em palavras, e cada palavra
 * desliza de baixo para cima com fade-in, em cascata (stagger). Reimplementado
 * nativamente com Canvas (sem dependencias externas):
 *
 * - split: por palavra (2 palavras para "Partograma Digital" -> cascata rapida
 *   e legivel, ao contrario de uma animacao por letra, que ficaria longa demais
 *   para caber no orcamento de 2-3s da Splash).
 * - easing: PathInterpolator(0.625, 0.05, 0, 1), a mesma curva cubic-bezier da
 *   referencia.
 * - duracao 500ms por palavra, stagger de 120ms entre palavras.
 *
 * Corrige o bug do efeito anterior (assinatura desenhada via getTextPath), no
 * qual a letra inicial podia ficar cortada: a centralizacao horizontal aqui usa
 * os limites reais de tinta do texto ({@link Paint#getTextBounds}) em vez da
 * largura de "avanco" da fonte, que nao considera o "overshoot" de glifos
 * decorativos.
 */
public class SlideUpTextView extends View {

    private static final long PER_WORD_DURATION_MS = 500L;
    private static final long STAGGER_MS = 120L;

    private String text = "";
    private String[] words = new String[0];
    private float[] wordX = new float[0];
    private float[] wordProgress = new float[0];
    private Typeface typeface = Typeface.DEFAULT;
    private int[] gradientColors = new int[]{0xFFE8608F, 0xFF9B7FC7, 0xFF5FB8AE};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Interpolator easing = new PathInterpolator(0.625f, 0.05f, 0f, 1f);
    private final Rect textBounds = new Rect();

    private float originX;
    private float originY;
    private float riseDistancePx;
    private boolean layoutReady;
    private ValueAnimator animator;
    private Runnable onFinishedListener;

    public SlideUpTextView(Context context) {
        super(context);
    }

    public SlideUpTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        layoutReady = false;
        requestLayout();
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface != null ? typeface : Typeface.DEFAULT;
        layoutReady = false;
        invalidate();
    }

    public void setGradientColors(int[] colors) {
        this.gradientColors = colors;
        layoutReady = false;
    }

    public void setOnFinishedListener(Runnable listener) {
        this.onFinishedListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutReady = false;
        buildLayoutIfNeeded();
    }

    private void buildLayoutIfNeeded() {
        int width = getWidth();
        int height = getHeight();
        if (layoutReady || width <= 0 || height <= 0 || text.trim().isEmpty()) {
            return;
        }

        paint.setTypeface(typeface);

        float horizontalPadding = width * 0.08f;
        float targetWidth = width - (horizontalPadding * 2f);
        float textSize = height * 0.62f;
        paint.setTextSize(textSize);

        paint.getTextBounds(text, 0, text.length(), textBounds);
        float inkWidth = textBounds.width();
        if (inkWidth > targetWidth && inkWidth > 0) {
            textSize *= targetWidth / inkWidth;
            paint.setTextSize(textSize);
            paint.getTextBounds(text, 0, text.length(), textBounds);
            inkWidth = textBounds.width();
        }

        // Centraliza usando os limites reais de tinta (nao a largura de avanco),
        // garantindo que nenhum glifo com "overshoot" fique cortado na borda.
        originX = ((width - inkWidth) / 2f) - textBounds.left;

        Paint.FontMetrics metrics = paint.getFontMetrics();
        float textHeight = metrics.descent - metrics.ascent;
        originY = (height - textHeight) / 2f - metrics.ascent;

        paint.setShader(new LinearGradient(
                originX + textBounds.left, 0, originX + textBounds.right, 0,
                gradientColors, null, Shader.TileMode.CLAMP));

        words = text.trim().split("\\s+");
        wordX = new float[words.length];
        wordProgress = new float[words.length];
        int cursor = 0;
        for (int i = 0; i < words.length; i++) {
            int wordStart = text.indexOf(words[i], cursor);
            wordX[i] = originX + paint.measureText(text, 0, wordStart);
            cursor = wordStart + words[i].length();
        }

        riseDistancePx = textSize * 0.55f;
        layoutReady = true;
    }

    /** Inicia a cascata de entrada. Ao final (+ pequena pausa), chama o listener de conclusao. */
    public void start(long holdMs) {
        buildLayoutIfNeeded();
        if (words.length == 0) {
            return;
        }
        if (animator != null) {
            animator.cancel();
        }
        long totalDuration = STAGGER_MS * (words.length - 1) + PER_WORD_DURATION_MS;

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(totalDuration);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float elapsedMs = (float) animation.getAnimatedValue() * totalDuration;
            for (int i = 0; i < words.length; i++) {
                float local = (elapsedMs - i * STAGGER_MS) / PER_WORD_DURATION_MS;
                local = Math.max(0f, Math.min(1f, local));
                wordProgress[i] = easing.getInterpolation(local);
            }
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onFinishedListener != null) {
                    postDelayed(onFinishedListener, holdMs);
                }
            }
        });
        animator.start();
    }

    public void cancelAnimation() {
        if (animator != null) {
            animator.cancel();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!layoutReady) {
            buildLayoutIfNeeded();
        }
        for (int i = 0; i < words.length; i++) {
            float progress = wordProgress[i];
            if (progress <= 0f) {
                continue;
            }
            paint.setAlpha(Math.round(255 * progress));
            float y = originY + (1f - progress) * riseDistancePx;
            canvas.drawText(words[i], wordX[i], y, paint);
        }
    }
}
