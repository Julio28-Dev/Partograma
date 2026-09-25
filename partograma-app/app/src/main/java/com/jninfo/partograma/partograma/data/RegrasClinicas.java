package com.jninfo.partograma.partograma.data;

/**
 * Ponto de extensao para as regras de vigilancia/alerta pedidas pelo cliente (secao 11
 * do briefing): reavaliacao apos tempo sem novo registro, parametros fora do esperado, e
 * futuramente notificacoes push.
 *
 * NAO HA NENHUM LIMITE CLINICO IMPLEMENTADO AQUI DE PROPOSITO. O cliente foi explicito:
 * "NAO INVENTE limites clinicos, tempos clinicos ou valores de referencia" -- esses
 * numeros (ex.: "alertar se passarem X minutos sem avaliacao", "BCF normal e entre X e Y
 * bpm", "PA sistolica > X e alerta") precisam ser definidos pela equipe clinica do
 * cliente antes de qualquer alerta poder ser implementado com seguranca.
 *
 * O que existe hoje: os dados que uma regra futura vai precisar ja estao disponiveis e
 * com timestamp real --
 *   - {@link RegistroPartograma#getDataHora()} (avaliacoes do partograma)
 *   - {@link SinalVital#getDataHora()} (sinais vitais)
 *   - {@link Paciente#getUltimoAcesso()} (ultima interacao com a paciente)
 *
 * Quando as regras forem definidas pelo cliente, o lugar natural para implementa-las e
 * aqui: um metodo por regra, recebendo os dados ja carregados (nunca fazendo a UI
 * decidir "e hora de alertar"), retornando um resultado que a tela so precisa exibir.
 * Notificacoes push (FCM) exigiriam, alem disso, uma Cloud Function ou um job de
 * servidor observando o Firestore -- o app sozinho (cliente movel) nao consegue
 * notificar outros profissionais enquanto estiver fechado.
 */
public final class RegrasClinicas {

    private RegrasClinicas() {
    }

    /**
     * TODO (depende de definicao clinica do cliente): apos quantos minutos/horas sem um
     * novo {@link RegistroPartograma} uma paciente deveria ser sinalizada para
     * reavaliacao? Nenhum valor foi definido ainda -- nao inventar um numero aqui.
     */
    public static final Long LIMITE_MINUTOS_SEM_REAVALIACAO = null;

    /**
     * TODO (depende de definicao clinica do cliente): quais faixas de BCF, PA, FC, FR,
     * temperatura, SpO2 e dor devem ser consideradas "fora do esperado" o suficiente
     * para gerar um alerta? Nenhuma faixa foi definida ainda -- nao inventar valores aqui.
     */
    public static final String THRESHOLDS_SINAIS_VITAIS_PENDENTES =
            "Aguardando definicao clinica do cliente (BCF, PA, FC, FR, temperatura, SpO2, dor).";
}
