package com.jninfo.partograma.partograma.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.PathInterpolator;

/**
 * Efeito de abertura da Splash, inspirado no componente "Highlighted Text"
 * (spell.sh/docs/highlighted-text): uma barra de destaque (estilo marca-texto) desliza
 * a partir de uma direcao, revelando o texto por baixo. Reimplementado nativamente com
 * Canvas (sem dependencias externas):
 *
 * - "from": direcao de onde a barra de destaque surge (aqui, da esquerda, acompanhando
 *   o sentido natural de leitura).
 * - "delay"/duracao: controlados pela Activity que chama {@link #start}.
 *
 * O texto e desenhado em cor solida (alto contraste) com a fonte Inter -- ao contrario
 * do efeito anterior (fonte decorativa desenhada por contorno), aqui a legibilidade nao
 * depende de nenhum efeito: o texto so fica progressivamente visivel conforme a barra de
 * destaque avanca por cima dele (clip), garantindo que nunca fique cortado ou ilegivel.
 */
public class HighlightedTextView extends View {

    private static final long DEFAULT_DURATION_MS = 650L;

    private String text = "";
    private Typeface typeface = Typeface.DEFAULT_BOLD;
    private int textColor = 0xFF3A3149; // textPrimary
    private int[] highlightColors = new int[]{0xFFF6C9DA, 0xFFE3D3F0, 0xFFBFE6DF};

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();
    private final RectF highlightRect = new RectF();

    private float originX;
    private float originY;
    private float boxLeft, boxTop, boxRight, boxBottom;
    private float progress = 0f;
    private boolean layoutReady;
    private ValueAnimator animator;
    private Runnable onFinishedListener;

    public HighlightedTextView(Context context) {
        super(context);
    }

    public HighlightedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        layoutReady = false;
        requestLayout();
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface != null ? typeface : Typeface.DEFAULT_BOLD;
        layoutReady = false;
        invalidate();
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
        if (layoutReady || width <= 0 || height <= 0 || text.isEmpty()) {
            return;
        }

        textPaint.setTypeface(typeface);
        textPaint.setColor(textColor);

        float horizontalPadding = width * 0.1f;
        float targetWidth = width - (horizontalPadding * 2f);
        float textSize = height * 0.5f;
        textPaint.setTextSize(textSize);

        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        float inkWidth = textBounds.width();
        if (inkWidth > targetWidth && inkWidth > 0) {
            textSize *= targetWidth / inkWidth;
            textPaint.setTextSize(textSize);
            textPaint.getTextBounds(text, 0, text.length(), textBounds);
            inkWidth = textBounds.width();
        }

        // Centraliza pelos limites reais de tinta (nao pela largura de "avanco" da
        // fonte), para nunca cortar a primeira ou ultima letra.
        originX = ((width - inkWidth) / 2f) - textBounds.left;
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float textHeight = metrics.descent - metrics.ascent;
        originY = (height - textHeight) / 2f - metrics.ascent;

        float boxPadH = textSize * 0.22f;
        float boxPadV = textSize * 0.16f;
        boxLeft = originX + textBounds.left - boxPadH;
        boxRight = originX + textBounds.right + boxPadH;
        boxTop = originY + metrics.ascent - boxPadV;
        boxBottom = originY + metrics.descent + boxPadV;

        highlightPaint.setShader(new android.graphics.LinearGradient(
                boxLeft, 0, boxRight, 0, highlightColors, null, android.graphics.Shader.TileMode.CLAMP));

        layoutReady = true;
    }

    /** Inicia a animacao: a barra de destaque desliza da esquerda, revelando o texto. */
    public void start(long holdMs) {
        buildLayoutIfNeeded();
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DEFAULT_DURATION_MS);
        animator.setInterpolator(new PathInterpolator(0.625f, 0.05f, 0f, 1f));
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
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
        if (!layoutReady) {
            buildLayoutIfNeeded();
        }
        if (progress <= 0f || boxRight <= boxLeft) {
            return;
        }
        float revealRight = boxLeft + (boxRight - boxLeft) * progress;
        highlightRect.set(boxLeft, boxTop, revealRight, boxBottom);

        canvas.save();
        canvas.drawRoundRect(highlightRect, (boxBottom - boxTop) * 0.28f, (boxBottom - boxTop) * 0.28f, highlightPaint);
        canvas.clipRect(boxLeft, boxTop, revealRight, boxBottom);
        canvas.drawText(text, originX, originY, textPaint);
        canvas.restore();
    }
}
