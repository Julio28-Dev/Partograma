package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.jninfo.partograma.partograma.data.PatientRepository;

/**
 * Tela inicial / tutorial (4 passos), exibida apenas enquanto o tutorial nao tiver sido
 * concluido (flag "tutorial" no SharedPreferences). Ao concluir, ou se o tutorial ja
 * tiver sido concluido antes, navega direto para {@link Menu}.
 */
public class TelaInicial extends AppCompatActivity {

    private static final int ULTIMO_PASSO = 3;

    private PatientRepository repositorio;
    private Button btnAnterior;
    private Button btnProximo;
    /** Imagem de fundo em tela cheia (R.id.imagemFundo); muda a cada passo do tutorial. */
    private ImageView imagemFundo;
    /** Icone de "?" (R.id.imageView); so aparece no ultimo passo. */
    private ImageView iconeAjuda;
    private TextView textoTutorial;
    private int passo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_inicial);

        repositorio = new PatientRepository(this);
        if (repositorio.isTutorialCompleto()) {
            startActivity(new Intent(this, Menu.class));
        }

        iconeAjuda = findViewById(R.id.imageView);
        btnProximo = findViewById(R.id.botaoProximo);
        btnAnterior = findViewById(R.id.botaoAnterior);
        textoTutorial = findViewById(R.id.textTutorial);
        imagemFundo = findViewById(R.id.imagemFundo);
        passo = 0;

        btnProximo.setOnClickListener(v -> {
            if (passo < ULTIMO_PASSO) {
                passo++;
            }
            if (passo >= ULTIMO_PASSO) {
                repositorio.marcarTutorialCompleto();
                startActivity(new Intent(TelaInicial.this, Menu.class));
                passo = 0;
                return;
            }
            mostrarPasso();
        });

        btnAnterior.setOnClickListener(v -> {
            if (passo > 0) {
                passo--;
            }
            mostrarPasso();
        });
    }

    /**
     * Atualiza texto, imagem de fundo e visibilidade dos elementos para o passo atual.
     * Cada passo atualiza apenas o que o APK original atualizava -- por isso, por exemplo,
     * o texto "Pronto" do botao permanece caso o usuario volte do ultimo passo (comportamento
     * identico ao original, nao corrigido aqui).
     */
    private void mostrarPasso() {
        switch (passo) {
            case 0:
                textoTutorial.setText("Bem vindo ao aplicativo de Partograma");
                btnAnterior.setVisibility(View.INVISIBLE);
                imagemFundo.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.imagem1));
                iconeAjuda.setVisibility(View.INVISIBLE);
                break;
            case 1:
                btnProximo.setVisibility(View.VISIBLE);
                btnAnterior.setVisibility(View.VISIBLE);
                imagemFundo.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.imagem4));
                textoTutorial.setText("Para criar um novo paciente basta inserir o nome no campo e clicar em adicionar.");
                iconeAjuda.setVisibility(View.INVISIBLE);
                break;
            case 2:
                btnProximo.setVisibility(View.VISIBLE);
                btnAnterior.setVisibility(View.VISIBLE);
                imagemFundo.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.imagem3));
                textoTutorial.setText("Para excluir um paciente segure em cima do nome e escolha opção deletar.");
                iconeAjuda.setVisibility(View.INVISIBLE);
                break;
            case 3:
                btnProximo.setText("Pronto");
                imagemFundo.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.imagem5));
                textoTutorial.setText("Qualquer dúvida aperte neste interrogação");
                iconeAjuda.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }
}
