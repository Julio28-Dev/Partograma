package com.jninfo.partograma.partograma;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Modal "Saiba mais" do Plano de De Lee (secao 5.7 do briefing): explica o conceito e
 * mostra os 11 valores possiveis (-5..+5), destacando o valor atualmente selecionado no
 * formulario de avaliacao.
 *
 * Ilustracoes: por enquanto todos os 11 valores apontam para o MESMO placeholder
 * generico ({@link R.drawable#ic_delee_placeholder}), porque as imagens clinicas
 * definitivas ainda nao foram fornecidas -- o cliente autorizou explicitamente usar
 * placeholders neste caso. A estrutura (um recurso de imagem por valor, mapeado aqui)
 * ja esta pronta para trocar cada uma individualmente sem mexer no restante da tela.
 */
public class PlanoDeLeeExplicacaoActivity extends BaseActivity {

    public static final String EXTRA_VALOR_SELECIONADO = "extra_valor_selecionado";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delee_explicacao);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        int valorSelecionado = getIntent().getIntExtra(EXTRA_VALOR_SELECIONADO, 0);
        LinearLayout container = findViewById(R.id.containerValoresDeLee);
        for (int valor = -5; valor <= 5; valor++) {
            View item = LayoutInflater.from(this).inflate(R.layout.item_delee_valor, container, false);
            TextView txtValor = item.findViewById(R.id.txtValorDeLee);
            FrameLayout moldura = item.findViewById(R.id.molduraValorDeLee);
            txtValor.setText(valor > 0 ? "+" + valor : String.valueOf(valor));
            if (valor == valorSelecionado) {
                moldura.setBackgroundResource(R.drawable.bg_selecionado_delee);
            }
            container.addView(item);
        }
    }
}
