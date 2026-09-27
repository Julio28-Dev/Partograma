package com.jninfo.partograma.partograma.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Uma aferição de sinais vitais da paciente (documento em
 * {@code pacientes/{pacienteId}/sinaisVitais}). Independente do registro de avaliacao do
 * Partograma -- o profissional pode registrar sinais vitais a qualquer momento, nao so
 * junto de uma avaliacao do partograma.
 */
@IgnoreExtraProperties
public class SinalVital {

    private String id;

    private String dataAfericao;     // dd/MM/yyyy
    private String horario;          // HH:mm
    private Integer paSistolica;
    private Integer paDiastolica;
    private Integer frequenciaCardiaca;
    private Integer frequenciaRespiratoria;
    private Double temperatura;
    private Integer spo2;
    private Integer dor;             // 0..10
    private Double hgt;              // glicemia, mg/dL
    private String observacoes;
    private String examinador;

    @ServerTimestamp
    private Date dataHora;

    public SinalVital() {
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

    public String getDataAfericao() {
        return dataAfericao;
    }

    public void setDataAfericao(String dataAfericao) {
        this.dataAfericao = dataAfericao;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public Integer getPaSistolica() {
        return paSistolica;
    }

    public void setPaSistolica(Integer paSistolica) {
        this.paSistolica = paSistolica;
    }

    public Integer getPaDiastolica() {
        return paDiastolica;
    }

    public void setPaDiastolica(Integer paDiastolica) {
        this.paDiastolica = paDiastolica;
    }

    public Integer getFrequenciaCardiaca() {
        return frequenciaCardiaca;
    }

    public void setFrequenciaCardiaca(Integer frequenciaCardiaca) {
        this.frequenciaCardiaca = frequenciaCardiaca;
    }

    public Integer getFrequenciaRespiratoria() {
        return frequenciaRespiratoria;
    }

    public void setFrequenciaRespiratoria(Integer frequenciaRespiratoria) {
        this.frequenciaRespiratoria = frequenciaRespiratoria;
    }

    public Double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(Double temperatura) {
        this.temperatura = temperatura;
    }

    public Integer getSpo2() {
        return spo2;
    }

    public void setSpo2(Integer spo2) {
        this.spo2 = spo2;
    }

    public Integer getDor() {
        return dor;
    }

    public void setDor(Integer dor) {
        this.dor = dor;
    }

    public Double getHgt() {
        return hgt;
    }

    public void setHgt(Double hgt) {
        this.hgt = hgt;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getExaminador() {
        return examinador;
    }

    public void setExaminador(String examinador) {
        this.examinador = examinador;
    }

    public Date getDataHora() {
        return dataHora;
    }

    public void setDataHora(Date dataHora) {
        this.dataHora = dataHora;
    }

    /**
     * Resumo em varias linhas (uma por sinal vital: PA, FC, FR, T, SpO2, Dor e, se
     * preenchido, HGT), no formato pedido pela referencia visual da aba Sinais vitais.
     */
    @Exclude
    public String getResumo() {
        StringBuilder sb = new StringBuilder();
        if (paSistolica != null && paDiastolica != null) {
            adicionarLinha(sb, "PA " + paSistolica + "/" + paDiastolica + " mmHg");
        }
        if (frequenciaCardiaca != null) {
            adicionarLinha(sb, "FC " + frequenciaCardiaca + " bpm");
        }
        if (frequenciaRespiratoria != null) {
            adicionarLinha(sb, "FR " + frequenciaRespiratoria + " irpm");
        }
        if (temperatura != null) {
            adicionarLinha(sb, "T " + temperatura + " °C");
        }
        if (spo2 != null) {
            adicionarLinha(sb, "SpO2 " + spo2 + "%");
        }
        if (dor != null) {
            adicionarLinha(sb, "Dor " + dor + "/10");
        }
        if (hgt != null) {
            adicionarLinha(sb, "HGT " + hgt + " mg/dL");
        }
        return sb.toString();
    }

    private void adicionarLinha(StringBuilder sb, String linha) {
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(linha);
    }
}
