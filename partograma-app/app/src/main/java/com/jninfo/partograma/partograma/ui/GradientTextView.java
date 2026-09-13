package com.jninfo.partograma.partograma.ui;

import android.content.Context;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * TextView cujo texto e pintado com um gradiente linear horizontal (esquerda -> direita).
 * Usado no titulo "Partograma" da Home, reproduzindo o degrade rosa -> roxo -> teal da
 * referencia visual sem depender de nenhuma biblioteca externa.
 */
public class GradientTextView extends AppCompatTextView {

    private int[] gradientColors = new int[]{0xFFE8608F, 0xFF9B7FC7, 0xFF5FB8AE};

    public GradientTextView(Context context) {
        super(context);
    }

    public GradientTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setGradientColors(int[] colors) {
        this.gradientColors = colors;
        applyGradient();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        applyGradient();
    }

    private void applyGradient() {
        if (getWidth() == 0) {
            return;
        }
        Shader shader = new LinearGradient(
                0, 0, getWidth(), 0,
                gradientColors, null, Shader.TileMode.CLAMP);
        getPaint().setShader(shader);
        invalidate();
    }
}
