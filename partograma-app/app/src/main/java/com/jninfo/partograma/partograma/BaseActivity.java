package com.jninfo.partograma.partograma;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.jninfo.partograma.partograma.data.AppPreferences;
import com.jninfo.partograma.partograma.data.SessaoUtil;

/**
 * Superclasse das telas NOVAS (Pacientes e tudo que abre a partir dela) que precisam
 * respeitar o "Tamanho da fonte" escolhido em Configuracoes. Aplicada apenas as telas
 * novas de proposito -- Splash/Home/TelaInicial/Menu/Detalhes/relatorio continuam
 * extends AppCompatActivity diretamente, sem nenhuma alteracao, para nao arriscar
 * nenhum efeito colateral nas telas que ja funcionam.
 *
 * TAMBEM e o unico ponto onde o "portao" de sessao (ver {@link SessaoUtil}) e checado,
 * de proposito centralizado aqui em vez de duplicado em cada Activity: qualquer tela
 * nova que passe a estender BaseActivity fica protegida automaticamente, inclusive
 * quando reaberta pela pilha de back stack ou por um estado restaurado (o Android chama
 * onCreate() de novo nesses casos, entao a checagem roda de novo). LoginActivity e
 * SolicitarAcessoActivity sao as duas excecoes (precisam continuar acessiveis SEM sessao
 * valida) e sobrescrevem precisaSessaoValida() para false.
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!precisaSessaoValida()) {
            return;
        }
        if (!SessaoUtil.sessaoValidaLocalmente(this)) {
            SessaoUtil.encerrarSessao(this);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        // Sessao valida: renova a janela de 30 dias enquanto a pessoa continuar
        // acessando telas protegidas normalmente.
        SessaoUtil.registrarAcessoValido(this);
    }

    /**
     * true (padrao) para toda tela protegida. LoginActivity e SolicitarAcessoActivity
     * sobrescrevem para false -- precisam continuar acessiveis sem nenhuma sessao.
     */
    protected boolean precisaSessaoValida() {
        return true;
    }
}
