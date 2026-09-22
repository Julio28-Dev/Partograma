package com.jninfo.partograma.partograma;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;

import com.jninfo.partograma.partograma.data.AppPreferences;

/**
 * Superclasse das telas NOVAS (Pacientes e tudo que abre a partir dela) que precisam
 * respeitar o "Tamanho da fonte" escolhido em Configuracoes. Aplicada apenas as telas
 * novas de proposito -- Splash/Home/TelaInicial/Menu/Detalhes/relatorio continuam
 * extends AppCompatActivity diretamente, sem nenhuma alteracao, para nao arriscar
 * nenhum efeito colateral nas telas que ja funcionam.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        float escala = new AppPreferences(newBase).getEscalaFonte();
        Configuration configuracao = new Configuration(newBase.getResources().getConfiguration());
        configuracao.fontScale = escala;
        Context contextoEscalado = newBase.createConfigurationContext(configuracao);
        super.attachBaseContext(contextoEscalado);
    }
}
