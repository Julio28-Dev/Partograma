package com.jninfo.partograma.partograma;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

/**
 * Tela de confirmacao exibida depois que uma nova avaliacao do partograma e salva com
 * sucesso ({@link NovaAvaliacaoActivity}) -- reproduz a "tela final" da referencia visual
 * (ilustracao + "Cada registro é um passo a mais para um parto seguro."). Fecha sozinha
 * apos um instante (ou no toque), voltando para os detalhes da paciente.
 *
 * Interpretacao assumida: a referencia nao especifica em que momento essa tela aparece,
 * so o conteudo visual dela -- like a frase e a marca sao tematicamente sobre "registrar",
 * usa-la como confirmacao pos-registro foi a leitura mais direta.
 */
public class TelaFinalActivity extends BaseActivity {

    private static final long DURACAO_MS = 1800L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_final);

        findViewById(android.R.id.content).setOnClickListener(v -> finish());
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, DURACAO_MS);
    }
}
