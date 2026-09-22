package com.jninfo.partograma.partograma.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Grafico de linha simples (dilatacao x horario), desenhado a mao com Canvas -- mesmo
 * padrao ja usado no resto do projeto para efeitos visuais (LightRaysView, BlobButton),
 * evitando depender de uma biblioteca externa de graficos so para esta tela.
 */
public class PartogramaChartView extends View {

    /** Um ponto do grafico: horario (rotulo do eixo X) e dilatacao em cm (eixo Y, 0-10). */
    public static class Ponto {
        public final String horario;
        public final double dilatacao;

        public Ponto(String horario, double dilatacao) {
            this.horario = horario;
            this.dilatacao = dilatacao;
        }
    }

    private static final float DILATACAO_MAX = 10f;

    private final List<Ponto> pontos = new ArrayList<>();
    private final Paint paintLinha = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPonto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGrade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintEixoTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPreenchimento = new Paint(Paint.ANTI_ALIAS_FLAG);

    public PartogramaChartView(Context context) {
        super(context);
        init();
    }

    public PartogramaChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintLinha.setStyle(Paint.Style.STROKE);
        paintLinha.setStrokeWidth(6f);
        paintLinha.setStrokeCap(Paint.Cap.ROUND);
        paintLinha.setStrokeJoin(Paint.Join.ROUND);
        paintLinha.setColor(Color.parseColor("#E8608F"));

        paintPonto.setStyle(Paint.Style.FILL);
        paintPonto.setColor(Color.parseColor("#9B7FC7"));

        paintGrade.setStyle(Paint.Style.STROKE);
        paintGrade.setStrokeWidth(1.5f);
        paintGrade.setColor(Color.parseColor("#EDE6F0"));

        paintEixoTexto.setColor(Color.parseColor("#7A7488"));
        paintEixoTexto.setTextSize(24f);

        paintPreenchimento.setStyle(Paint.Style.FILL);
    }

    public void setPontos(List<Ponto> novosPontos) {
        pontos.clear();
        pontos.addAll(novosPontos);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float largura = getWidth();
        float altura = getHeight();
        float margemEsquerda = 64f;
        float margemDireita = 24f;
        float margemTopo = 24f;
        float margemBaixo = 56f;

        float areaX = largura - margemEsquerda - margemDireita;
        float areaY = altura - margemTopo - margemBaixo;
        if (areaX <= 0 || areaY <= 0) {
            return;
        }

        // Linhas de grade horizontais (0, 2, 4, 6, 8, 10 cm)
        for (int cm = 0; cm <= 10; cm += 2) {
            float y = margemTopo + areaY - (cm / DILATACAO_MAX) * areaY;
            canvas.drawLine(margemEsquerda, y, largura - margemDireita, y, paintGrade);
            canvas.drawText(cm + "cm", 8f, y + 8f, paintEixoTexto);
        }

        if (pontos.isEmpty()) {
            return;
        }

        int n = pontos.size();
        float passoX = n > 1 ? areaX / (n - 1) : 0;

        Path linha = new Path();
        Path area = new Path();
        for (int i = 0; i < n; i++) {
            Ponto ponto = pontos.get(i);
            float x = margemEsquerda + (n > 1 ? i * passoX : areaX / 2f);
            float fracaoY = (float) (Math.min(Math.max(ponto.dilatacao, 0), DILATACAO_MAX) / DILATACAO_MAX);
            float y = margemTopo + areaY - fracaoY * areaY;

            if (i == 0) {
                linha.moveTo(x, y);
                area.moveTo(x, margemTopo + areaY);
                area.lineTo(x, y);
            } else {
                linha.lineTo(x, y);
                area.lineTo(x, y);
            }

            canvas.drawCircle(x, y, 8f, paintPonto);
            canvas.drawText(ponto.horario, x - 18f, altura - margemBaixo + 32f, paintEixoTexto);
        }
        if (n > 0) {
            float ultimoX = margemEsquerda + (n > 1 ? (n - 1) * passoX : areaX / 2f);
            area.lineTo(ultimoX, margemTopo + areaY);
            area.close();
        }

        paintPreenchimento.setShader(new LinearGradient(
                0, margemTopo, 0, margemTopo + areaY,
                Color.parseColor("#33E8608F"), Color.parseColor("#00E8608F"),
                Shader.TileMode.CLAMP));
        canvas.drawPath(area, paintPreenchimento);
        canvas.drawPath(linha, paintLinha);
    }
}
