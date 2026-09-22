package com.jninfo.partograma.partograma.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Uma avaliacao/registro do partograma de uma paciente (documento em
 * {@code pacientes/{pacienteId}/registros}). Equivalente, em conteudo, ao registro por
 * hora do APK original ("pessoa{slot}{campo}{hora}"): mesmos campos clinicos
 * (dilatacao, posicao de Lee, batimentos, integridade da bolsa, liquido amniotico,
 * frequencia de contracao, ocitocina, mesoprostol, remedios, examinador), apenas
 * persistidos no Firestore em vez de SharedPreferences e sem o limite fixo de 16
 * horas/slots do esquema antigo -- aqui cada avaliacao e um documento com ID proprio,
 * ordenado por {@link #dataHora} (timestamp do servidor).
 *
 * Campo novo em relacao ao original: {@link #intercorrencia}, usado pela aba
 * "Relatório" (Intercorrências) -- texto livre, opcional.
 */
@IgnoreExtraProperties
public class RegistroPartograma {

    private String id;

    private String horario;          // "HH:mm", informado pelo usuario no momento do registro
    private Double dilatacao;        // cm
    private Integer posicaoLee;      // 1..8, mesmo esquema de posicoes do APK original
    private Integer batimentos;      // bpm
    private String integridade;
    private String liquido;
    private String freqContracao;
    private String ocitocina;
    private String mesoprostol;
    private String remedios;
    private String examinador;
    private String intercorrencia;

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

    public Integer getPosicaoLee() {
        return posicaoLee;
    }

    public void setPosicaoLee(Integer posicaoLee) {
        this.posicaoLee = posicaoLee;
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

    public String getIntercorrencia() {
        return intercorrencia;
    }

    public void setIntercorrencia(String intercorrencia) {
        this.intercorrencia = intercorrencia;
    }

    public Date getDataHora() {
        return dataHora;
    }

    public void setDataHora(Date dataHora) {
        this.dataHora = dataHora;
    }

    /** Rotulo de exibicao da posicao de Lee ("Posição N"), sem inventar codigos obstetricos
     *  (ROP/OEA/etc.) que nao existem no esquema de imagens do APK original. */
    @Exclude
    public String getRotuloPosicaoLee() {
        return posicaoLee != null ? "Posição " + posicaoLee : "-";
    }
}
