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

    /** Resumo em uma linha (PA/FC/FR/T/SpO2/Dor), igual ao mostrado na lista de historico. */
    @Exclude
    public String getResumo() {
        StringBuilder sb = new StringBuilder();
        if (paSistolica != null && paDiastolica != null) {
            sb.append("PA: ").append(paSistolica).append("/").append(paDiastolica).append(" mmHg  |  ");
        }
        if (frequenciaCardiaca != null) {
            sb.append("FC: ").append(frequenciaCardiaca).append(" bpm  |  ");
        }
        if (frequenciaRespiratoria != null) {
            sb.append("FR: ").append(frequenciaRespiratoria).append(" irpm  |  ");
        }
        if (temperatura != null) {
            sb.append("T: ").append(temperatura).append(" °C  |  ");
        }
        if (spo2 != null) {
            sb.append("SpO2: ").append(spo2).append("%  |  ");
        }
        if (dor != null) {
            sb.append("Dor: ").append(dor).append("/10");
        }
        String resultado = sb.toString();
        return resultado.endsWith("  |  ") ? resultado.substring(0, resultado.length() - 5) : resultado;
    }
}
