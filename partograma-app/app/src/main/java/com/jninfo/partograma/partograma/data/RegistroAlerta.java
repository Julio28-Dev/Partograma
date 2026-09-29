package com.jninfo.partograma.partograma.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

/**
 * Um alerta emitido manualmente por um profissional para uma paciente (documento em
 * {@code instituicoes/{institutionId}/alertas}). Registrado para rastreabilidade -- ver
 * RegrasClinicas.java para a distincao entre este alerta MANUAL e alertas clinicos
 * automaticos (nao implementados, dependem de definicao clinica).
 *
 * "status" e sempre "registrado" hoje: nao existe canal de envio real configurado
 * (push/SMS/WhatsApp/e-mail) nesta etapa, entao o app nunca afirma ter entregue o alerta a
 * ninguem -- apenas que ele foi registrado com os destinatarios que deveriam recebe-lo.
 */
@IgnoreExtraProperties
public class RegistroAlerta {

    private String id;
    private String pacienteId;
    private String pacienteNome;
    private String mensagem;
    private List<String> destinatariosNomes;
    private String status;

    @ServerTimestamp
    private Date dataHora;

    public RegistroAlerta() {
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

    public String getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(String pacienteId) {
        this.pacienteId = pacienteId;
    }

    public String getPacienteNome() {
        return pacienteNome;
    }

    public void setPacienteNome(String pacienteNome) {
        this.pacienteNome = pacienteNome;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public List<String> getDestinatariosNomes() {
        return destinatariosNomes;
    }

    public void setDestinatariosNomes(List<String> destinatariosNomes) {
        this.destinatariosNomes = destinatariosNomes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDataHora() {
        return dataHora;
    }

    public void setDataHora(Date dataHora) {
        this.dataHora = dataHora;
    }
}
