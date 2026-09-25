package com.jninfo.partograma.partograma.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Uma avaliacao/registro do partograma de uma paciente (documento em
 * {@code pacientes/{pacienteId}/registros}).
 *
 * Nomenclatura corrigida nesta etapa (pedido explicito do cliente): o que antes estava
 * como um unico campo generico "posicao (1..8)" foi separado em dois conceitos clinicos
 * reais e distintos:
 *  - {@link #posicaoBebe}: apresentacao/situacao fetal (Cefalico/Pelvico/Oblíquo/
 *    Transversal) -- era chamado erroneamente de "Rotatividade do bebê" antes.
 *  - {@link #planoDeLee}: plano de De Lee de verdade (-5..+5), que nao existia antes.
 *
 * Compatibilidade: documentos antigos (criados antes desta correcao) tinham um campo
 * "posicaoLee" numerico 1..8 sem nenhuma correspondencia clinica valida com nenhum dos
 * dois campos novos -- inventar um mapeamento entre eles seria dado clinico incorreto,
 * entao NAO fazemos nenhuma conversao automatica. Esses registros antigos (todos de
 * teste, nenhum paciente real usou o app em producao ainda) simplesmente aparecem com
 * "Posição do bebê" e "Plano de De Lee" em branco -- sem erro, sem dado inventado.
 */
@IgnoreExtraProperties
public class RegistroPartograma {

    private String id;

    private String horario;              // "HH:mm", informado pelo usuario no momento do registro
    private Double dilatacao;            // cm
    private String posicaoBebe;          // Cefalico | Pelvico | Oblíquo | Transversal
    private Integer planoDeLee;          // -5..+5
    private Integer batimentos;          // BCF, bpm
    private String integridade;          // Íntegra | Rota
    private String liquido;              // so relevante quando integridade == Rota
    private String freqContracao;
    private String ocitocina;
    private String mesoprostol;
    private String remedios;             // "Outras medicações"
    private String examinador;
    private String faseTrabalhoParto;    // Em avaliação | Fase latente | Fase ativa | Período expulsivo | Dequitação | Período de Greenberg
    private List<String> metodosNaoFarmacologicos;
    private Boolean teveIntercorrencia;
    private String intercorrencia;       // so preenchido quando teveIntercorrencia == true
    private String observacaoAdicional;

    @ServerTimestamp
    private Date dataHora;

    public RegistroPartograma() {
        // Construtor vazio exigido pelo Firestore.
    }

    @Exclude
    public String getId() {
        return id;
    }

    @Exclude
    public void setId(String id) {
        this.id = id;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public Double getDilatacao() {
        return dilatacao;
    }

    public void setDilatacao(Double dilatacao) {
        this.dilatacao = dilatacao;
    }

    public String getPosicaoBebe() {
        return posicaoBebe;
    }

    public void setPosicaoBebe(String posicaoBebe) {
        this.posicaoBebe = posicaoBebe;
    }

    public Integer getPlanoDeLee() {
        return planoDeLee;
    }

    public void setPlanoDeLee(Integer planoDeLee) {
        this.planoDeLee = planoDeLee;
    }

    public Integer getBatimentos() {
        return batimentos;
    }

    public void setBatimentos(Integer batimentos) {
        this.batimentos = batimentos;
    }

    public String getIntegridade() {
        return integridade;
    }

    public void setIntegridade(String integridade) {
        this.integridade = integridade;
    }

    public String getLiquido() {
        return liquido;
    }

    public void setLiquido(String liquido) {
        this.liquido = liquido;
    }

    public String getFreqContracao() {
        return freqContracao;
    }

    public void setFreqContracao(String freqContracao) {
        this.freqContracao = freqContracao;
    }

    public String getOcitocina() {
        return ocitocina;
    }

    public void setOcitocina(String ocitocina) {
        this.ocitocina = ocitocina;
    }

    public String getMesoprostol() {
        return mesoprostol;
    }

    public void setMesoprostol(String mesoprostol) {
        this.mesoprostol = mesoprostol;
    }

    public String getRemedios() {
        return remedios;
    }

    public void setRemedios(String remedios) {
        this.remedios = remedios;
    }

    public String getExaminador() {
        return examinador;
    }

    public void setExaminador(String examinador) {
        this.examinador = examinador;
    }

    public String getFaseTrabalhoParto() {
        return faseTrabalhoParto;
    }

    public void setFaseTrabalhoParto(String faseTrabalhoParto) {
        this.faseTrabalhoParto = faseTrabalhoParto;
    }

    public List<String> getMetodosNaoFarmacologicos() {
        return metodosNaoFarmacologicos != null ? metodosNaoFarmacologicos : new ArrayList<>();
    }

    public void setMetodosNaoFarmacologicos(List<String> metodosNaoFarmacologicos) {
        this.metodosNaoFarmacologicos = metodosNaoFarmacologicos;
    }

    public Boolean getTeveIntercorrencia() {
        return teveIntercorrencia;
    }

    public void setTeveIntercorrencia(Boolean teveIntercorrencia) {
        this.teveIntercorrencia = teveIntercorrencia;
    }

    public String getIntercorrencia() {
        return intercorrencia;
    }

    public void setIntercorrencia(String intercorrencia) {
        this.intercorrencia = intercorrencia;
    }

    public String getObservacaoAdicional() {
        return observacaoAdicional;
    }

    public void setObservacaoAdicional(String observacaoAdicional) {
        this.observacaoAdicional = observacaoAdicional;
    }

    public Date getDataHora() {
        return dataHora;
    }

    public void setDataHora(Date dataHora) {
        this.dataHora = dataHora;
    }

    /** Rotulo de exibicao do plano de De Lee ("+2", "-3", "0"), ou "-" se nao informado. */
    @Exclude
    public String getRotuloPlanoDeLee() {
        if (planoDeLee == null) {
            return "-";
        }
        return planoDeLee > 0 ? "+" + planoDeLee : String.valueOf(planoDeLee);
    }
}
