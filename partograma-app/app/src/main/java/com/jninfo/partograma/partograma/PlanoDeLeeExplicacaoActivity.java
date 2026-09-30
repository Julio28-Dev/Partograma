package com.jninfo.partograma.partograma;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Modal "Saiba mais" do Plano de De Lee: explica o conceito e mostra os 11 valores
 * possiveis (-5..+5), cada um com a ilustracao e a legenda REAIS fornecidas pela cliente
 * (recortadas/transcritas de imag Lee.jpeg..imag Lee10.jpeg -- ver
 * res/drawable-nodpi/img_delee_*.png e as strings delee_legenda_*). Nada foi inventado:
 * cada imagem e legenda aqui corresponde exatamente ao arquivo que a cliente forneceu
 * para aquele valor.
 *
 * Toda a lista e navegavel/clicavel -- tocar em qualquer valor atualiza a ilustracao e a
 * legenda grandes no topo, nao so o valor que veio pre-selecionado do formulario.
 */
public class PlanoDeLeeExplicacaoActivity extends BaseActivity {

    public static final String EXTRA_VALOR_SELECIONADO = "extra_valor_selecionado";

    private static final int[] VALORES = {-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5};

    private ImageView imgGrande;
    private TextView txtValorGrande;
    private TextView txtLegendaGrande;
    private final View[] molduras = new View[VALORES.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delee_explicacao);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        imgGrande = findViewById(R.id.imgDeLeeGrande);
        txtValorGrande = findViewById(R.id.txtValorDeLeeGrande);
        txtLegendaGrande = findViewById(R.id.txtLegendaDeLeeGrande);

        int valorSelecionado = getIntent().getIntExtra(EXTRA_VALOR_SELECIONADO, 0);
        if (valorSelecionado < -5 || valorSelecionado > 5) {
            valorSelecionado = 0;
        }

        LinearLayout container = findViewById(R.id.containerValoresDeLee);
        for (int i = 0; i < VALORES.length; i++) {
            int valor = VALORES[i];
            View item = LayoutInflater.from(this).inflate(R.layout.item_delee_valor, container, false);
            TextView txtValor = item.findViewById(R.id.txtValorDeLee);
            ImageView imgValor = item.findViewById(R.id.imgValorDeLee);
            txtValor.setText(rotulo(valor));
            imgValor.setImageResource(drawableDoValor(valor));
            molduras[i] = item.findViewById(R.id.molduraValorDeLee);
            item.setOnClickListener(v -> selecionarValor(valor));
            container.addView(item);
        }

        selecionarValor(valorSelecionado);
    }

    private void selecionarValor(int valor) {
        imgGrande.setImageResource(drawableDoValor(valor));
        txtValorGrande.setText(rotulo(valor));
        txtLegendaGrande.setText(legendaDoValor(valor));
        for (int i = 0; i < VALORES.length; i++) {
            molduras[i].setBackgroundResource(
                    VALORES[i] == valor ? R.drawable.bg_selecionado_delee : R.drawable.bg_input_field);
        }
    }

    private String rotulo(int valor) {
        return valor > 0 ? "+" + valor : String.valueOf(valor);
    }

    private int drawableDoValor(int valor) {
        switch (valor) {
            case -5: return R.drawable.img_delee_m5;
            case -4: return R.drawable.img_delee_m4;
            case -3: return R.drawable.img_delee_m3;
            case -2: return R.drawable.img_delee_m2;
            case -1: return R.drawable.img_delee_m1;
            case 1: return R.drawable.img_delee_p1;
            case 2: return R.drawable.img_delee_p2;
            case 3: return R.drawable.img_delee_p3;
            case 4: return R.drawable.img_delee_p4;
            case 5: return R.drawable.img_delee_p5;
            default: return R.drawable.img_delee_0;
        }
    }

    private int legendaDoValor(int valor) {
        switch (valor) {
            case -5: return R.string.delee_legenda_m5;
            case -4: return R.string.delee_legenda_m4;
            case -3: return R.string.delee_legenda_m3;
            case -2: return R.string.delee_legenda_m2;
            case -1: return R.string.delee_legenda_m1;
            case 1: return R.string.delee_legenda_p1;
            case 2: return R.string.delee_legenda_p2;
            case 3: return R.string.delee_legenda_p3;
            case 4: return R.string.delee_legenda_p4;
            case 5: return R.string.delee_legenda_p5;
            default: return R.string.delee_legenda_0;
        }
    }
}
