package com.jninfo.partograma.partograma.report;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import com.jninfo.partograma.partograma.data.PatientRepository;

/**
 * Preenche o grafico do partograma na tela de relatorio.
 *
 * O layout activity_relatorio.xml contem centenas de Views ja posicionadas (coordenadas
 * fixas em dip), uma para cada combinacao possivel de (valor, hora) -- exatamente como no
 * APK original. Esta classe reproduz a mesma logica de "qual View mostrar" que, no
 * original, estava escrita como centenas de blocos praticamente identicos dentro da
 * Activity "relatorio" (funcoes dilatacao(), batimentos(), lee(), frequencia(),
 * barradilatacao()). Os NOMES dos ids (ex.: "triangulo3_1", "pontoBatimento7_4") sao os
 * mesmos do layout original (apenas com "_" no lugar do "-", exigido pelas ferramentas
 * atuais -- ver REVERSE_ENGINEERING.md).
 */
public class ReportGridBinder {

    /** Linha 1..9 do grid de Lee, na mesma ordem de cima para baixo do layout original. */
    private static final String[] LEE_LINHAS = {"+4", "+3", "+2", "+1", "0", "-1", "-2", "-3", "- AM"};

    /** Faixas de BCF (bpm) -> linha 1..11 do grid, identicas ao APK original. */
    private static final int[][] FAIXAS_BCF = {
            {175, 200}, {165, 174}, {155, 164}, {145, 154}, {135, 144},
            {125, 134}, {115, 124}, {105, 114}, {95, 104}, {85, 94}, {0, 84}
    };

    private final Activity activity;
    private final PatientRepository repositorio;
    private final int slot;

    public ReportGridBinder(Activity activity, PatientRepository repositorio, int slot) {
        this.activity = activity;
        this.repositorio = repositorio;
        this.slot = slot;
    }

    public void aplicarGraficoCompleto() {
        aplicarDilatacao();
        aplicarBatimentos();
        aplicarLee();
        aplicarFrequencia();
        aplicarBarraDeDilatacaoInicial();
    }

    /** Um triangulo em (dilatacao, hora) para cada hora com dilatacao entre 3 e 10 cm. */
    private void aplicarDilatacao() {
        for (int hora = 1; hora <= PatientRepository.MAX_HORAS; hora++) {
            int valor = parseOuZero(repositorio.getCampo(slot, "dilatacao", hora, "0"));
            if (valor >= 3 && valor <= 10) {
                mostrar("triangulo" + valor + "_" + hora);
            }
        }
    }

    /** Um ponto de BCF por hora, na linha correspondente a faixa de batimentos. */
    private void aplicarBatimentos() {
        for (int hora = 1; hora <= PatientRepository.MAX_HORAS; hora++) {
            int bcf = parseOuZero(repositorio.getCampo(slot, "batimentos", hora, "-1"));
            for (int linha = 1; linha <= FAIXAS_BCF.length; linha++) {
                int[] faixa = FAIXAS_BCF[linha - 1];
                if (bcf >= faixa[0] && bcf <= faixa[1]) {
                    mostrar("pontoBatimento" + linha + "_" + hora);
                    break;
                }
            }
        }
    }

    /**
     * Um icone de posicao fetal por hora (escolhido pelo botao "Girar" em Detalhes),
     * posicionado na linha que corresponde ao Plano de Lee registrado naquela hora.
     */
    private void aplicarLee() {
        for (int hora = 1; hora <= PatientRepository.MAX_HORAS; hora++) {
            String valorLee = repositorio.getCampo(slot, "lee", hora, "");
            int linha = indiceDe(LEE_LINHAS, valorLee);
            if (linha < 0) {
                continue;
            }
            int posicao = repositorio.getPosicaoLee(slot, hora, 0);
            String drawableIcone = nomeDrawablePosicao(posicao);
            if (drawableIcone != null) {
                setImagem("lee" + linha + "_" + hora, drawableIcone);
            }
            mostrar("lee" + linha + "_" + hora);
        }
    }

    /** Um icone (fraca/media/forte) por hora, conforme a frequencia de contracao registrada. */
    private void aplicarFrequencia() {
        for (int hora = 1; hora <= PatientRepository.MAX_HORAS; hora++) {
            String freq = repositorio.getCampo(slot, "freqcontracao", hora, "");
            String drawable = "LEVE".equals(freq) ? "fraca" : "MODERADA".equals(freq) ? "media" : "ALTA".equals(freq) ? "forte" : null;
            if (drawable == null) {
                continue;
            }
            setImagem("frequencia1_" + hora, drawable);
            mostrar("frequencia1_" + hora);
        }
    }

    /** Linha de alerta/acao do partograma, posicionada pela dilatacao registrada na hora 1. */
    private void aplicarBarraDeDilatacaoInicial() {
        int valor = parseOuZero(repositorio.getCampo(slot, "dilatacao", 1, "0"));
        if (valor >= 3 && valor <= 9) {
            mostrar("barradilatacao" + valor);
            mostrar("alertabarradilatacao" + valor);
        }
    }

    private static String nomeDrawablePosicao(int posicao) {
        if (posicao < 1 || posicao > 8) {
            return null;
        }
        return posicao == 1 ? "lee" : "lee" + posicao;
    }

    private static int indiceDe(String[] opcoes, String valor) {
        for (int i = 0; i < opcoes.length; i++) {
            if (opcoes[i].equals(valor)) {
                return i + 1;
            }
        }
        return -1;
    }

    private static int parseOuZero(String valor) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void mostrar(String nomeId) {
        View view = encontrarViewPorNome(nomeId);
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private void setImagem(String nomeId, String nomeDrawable) {
        View view = encontrarViewPorNome(nomeId);
        int drawableId = activity.getResources().getIdentifier(nomeDrawable, "drawable", activity.getPackageName());
        if (view instanceof ImageView && drawableId != 0) {
            ((ImageView) view).setImageDrawable(ContextCompat.getDrawable(activity, drawableId));
        }
    }

    private View encontrarViewPorNome(String nomeId) {
        int id = activity.getResources().getIdentifier(nomeId, "id", activity.getPackageName());
        return id == 0 ? null : activity.findViewById(id);
    }
}
