package com.jninfo.partograma.partograma.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

/**
 * Pessoa autorizada a receber alertas emitidos pela instituicao (documento em
 * {@code instituicoes/{institutionId}/destinatarios}). Sem cargo fixo (medico, coordenador
 * etc.) porque o cliente nao pediu papeis pre-definidos -- so uma lista de pessoas.
 */
@IgnoreExtraProperties
public class Destinatario {

    private String id;
    private String nome;
    private String contato;      // e-mail ou telefone, conforme tipoContato
    private String tipoContato;  // "email" | "telefone"
    private Boolean ativo;

    public Destinatario() {
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getContato() {
        return contato;
    }

    public void setContato(String contato) {
        this.contato = contato;
    }

    public String getTipoContato() {
        return tipoContato;
    }

    public void setTipoContato(String tipoContato) {
        this.tipoContato = tipoContato;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
