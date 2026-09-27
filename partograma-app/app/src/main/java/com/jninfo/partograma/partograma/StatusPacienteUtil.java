package com.jninfo.partograma.partograma;

import android.content.Context;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/**
 * Cor do badge de "Fase do trabalho de parto", compartilhada entre a lista de Pacientes e o
 * cabecalho do Registro da Paciente para as duas telas ficarem sempre consistentes.
 *
 * As 6 fases clinicas definidas pelo cliente (Em avaliação / Fase latente / Fase ativa /
 * Período expulsivo / Dequitação / Período de Greenberg) sao agrupadas em 3 cores ja
 * existentes na identidade visual do app -- nenhum valor clinico e alterado, so a cor de
 * apresentacao. O agrupamento segue a referencia mais recente da cliente (teal = trabalho
 * de parto ativo/avancado, lilas = fase latente, laranja = em avaliacao) e mantem o antigo
 * "Trabalho de parto ativo" (status padrao de cadastros anteriores a essa fase) como teal.
 */
final class StatusPacienteUtil {

    private StatusPacienteUtil() {
    }

    static void aplicar(TextView badge, String status) {
        Context context = badge.getContext();
        int corFundo;
        int corTexto;

        boolean faseAvancada = "Fase ativa".equals(status)
                || "Período expulsivo".equals(status)
                || "Dequitação".equals(status)
                || "Período de Greenberg".equals(status)
                || context.getString(R.string.pacientes_status_trabalho_ativo).equals(status);
        boolean faseLatente = "Fase latente".equals(status);

        if (faseAvancada) {
            corFundo = R.drawable.bg_badge_teal;
            corTexto = R.color.softTeal;
        } else if (faseLatente) {
            corFundo = R.drawable.bg_badge_lilac;
            corTexto = R.color.softPurple;
        } else {
            corFundo = R.drawable.bg_badge_orange;
            corTexto = R.color.softAmber;
        }

        badge.setBackgroundResource(corFundo);
        badge.setTextColor(ContextCompat.getColor(context, corTexto));
    }
}
