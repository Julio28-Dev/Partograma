package com.jninfo.partograma.partograma.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * View que desenha um texto como se estivesse sendo escrito a mao ("signature reveal"),
 * inspirada no componente Signature (spell.sh/docs/signature) da referencia, adaptada para
 * Android usando Canvas puro (sem dependencias externas):
 *
 * 1) O contorno do texto e obtido via {@link Paint#getTextPath}.
 * 2) Cada contorno (subpath) do texto e percorrido com {@link PathMeasure}.
 * 3) Um {@link ValueAnimator} revela progressivamente o comprimento total do traçado,
 *    criando o efeito de caneta desenhando o texto da esquerda para a direita.
 */
public class SignatureTextView extends View {

    private String text = "";
    private Typeface typeface = Typeface.DEFAULT;
    private int[] gradientColors = new int[]{0xFFE8608F, 0xFF9B7FC7, 0xFF5FB8AE};

    private final Paint textOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path fullPath = new Path();
    private final Path revealPath = new Path();
    private final PathMeasure pathMeasure = new PathMeasure();

    private float totalLength = 0f;
    private float progress = 0f;
    private boolean pathReady = false;
    private ValueAnimator animator;
    private Runnable onFinishedListener;

    public SignatureTextView(Context context) {
        super(context);
        init();
    }

    public SignatureTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        pathReady = false;
        requestLayout();
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface != null ? typeface : Typeface.DEFAULT;
        pathReady = false;
        invalidate();
    }

    public void setGradientColors(int[] colors) {
        this.gradientColors = colors;
        pathReady = false;
    }

    public void setOnFinishedListener(Runnable listener) {
        this.onFinishedListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        pathReady = false;
        buildPathIfNeeded();
    }

    /** Monta o path do texto ajustando o tamanho da fonte para caber na largura disponivel. */
    private void buildPathIfNeeded() {
        int width = getWidth();
        int height = getHeight();
        if (pathReady || width <= 0 || height <= 0 || text.isEmpty()) {
            return;
        }

        textOutlinePaint.setTypeface(typeface);

        float horizontalPadding = width * 0.08f;
        float targetWidth = width - (horizontalPadding * 2f);
        float textSize = height * 0.62f;
        textOutlinePaint.setTextSize(textSize);

        float measuredWidth = textOutlinePaint.measureText(text);
        if (measuredWidth > targetWidth && measuredWidth > 0) {
            textSize *= targetWidth / measuredWidth;
            textOutlinePaint.setTextSize(textSize);
            measuredWidth = textOutlinePaint.measureText(text);
        }

        Paint.FontMetrics metrics = textOutlinePaint.getFontMetrics();
        float textHeight = metrics.descent - metrics.ascent;
        float baselineX = (width - measuredWidth) / 2f;
        float baselineY = (height - textHeight) / 2f - metrics.ascent;

        fullPath.reset();
        textOutlinePaint.getTextPath(text, 0, text.length(), baselineX, baselineY, fullPath);

        totalLength = 0f;
        pathMeasure.setPath(fullPath, false);
        do {
            totalLength += pathMeasure.getLength();
        } while (pathMeasure.nextContour());

        strokePaint.setStrokeWidth(Math.max(2f, textSize * 0.045f));
        strokePaint.setShader(new LinearGradient(
                baselineX, 0, baselineX + measuredWidth, 0,
                gradientColors, null, Shader.TileMode.CLAMP));

        pathReady = true;
        updateRevealPath();
    }

    private void updateRevealPath() {
        revealPath.reset();
        if (!pathReady || totalLength <= 0f) {
            return;
        }
        float targetLength = progress * totalLength;
        float remaining = targetLength;
        pathMeasure.setPath(fullPath, false);
        do {
            float contourLength = pathMeasure.getLength();
            if (remaining <= 0f) {
                break;
            }
            float take = Math.min(contourLength, remaining);
            if (take > 0f) {
                pathMeasure.getSegment(0, take, revealPath, true);
            }
            remaining -= contourLength;
        } while (pathMeasure.nextContour());
    }

    /**
     * Inicia a animacao de "escrita". Duracao total (traçado + pequena pausa no final)
     * fica entre ~1.6s e ~2s, dentro da janela de 2-3s pedida para a Splash inteira
     * (o restante e consumido pela transicao de fade para a Home).
     */
    public void start(long strokeDurationMs, long holdMs) {
        buildPathIfNeeded();
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(strokeDurationMs);
        animator.setInterpolator(new DecelerateInterpolator(1.1f));
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            updateRevealPath();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
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
        if (!pathReady) {
            buildPathIfNeeded();
        }
        canvas.drawPath(revealPath, strokePaint);
    }
}
