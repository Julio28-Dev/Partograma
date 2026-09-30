package com.jninfo.partograma.partograma.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.Date;

/**
 * Pedido de acesso institucional (documento em {@code institutionAccessRequests}), criado
 * pela tela "Solicitar acesso" e revisado pelo admin em Configuracoes -> Autorizacoes.
 * Nunca guarda senha -- a credencial so e criada no momento em que o admin aprova (ver
 * SolicitacaoAcessoRepository.aprovar).
 */
@IgnoreExtraProperties
public class SolicitacaoAcesso {

    public static final String STATUS_PENDENTE = "pendente";
    public static final String STATUS_APROVADA = "aprovada";
    public static final String STATUS_REJEITADA = "rejeitada";

    private String id;
    private String nomeInstituicao;
    private String responsavel;
    private String email;
    private String telefone;
    private String status;
    private Date criadoEm;
    private Date atualizadoEm;
    private String revisadoPor;
    private String motivoRejeicao;
    private String institutionUid;

    public SolicitacaoAcesso() {
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

    public String getNomeInstituicao() {
        return nomeInstituicao;
    }

    public void setNomeInstituicao(String nomeInstituicao) {
        this.nomeInstituicao = nomeInstituicao;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Date criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Date getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Date atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public String getRevisadoPor() {
        return revisadoPor;
    }

    public void setRevisadoPor(String revisadoPor) {
        this.revisadoPor = revisadoPor;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public void setMotivoRejeicao(String motivoRejeicao) {
        this.motivoRejeicao = motivoRejeicao;
    }

    public String getInstitutionUid() {
        return institutionUid;
    }

    public void setInstitutionUid(String institutionUid) {
        this.institutionUid = institutionUid;
    }
}
