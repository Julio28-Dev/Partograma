package com.jninfo.partograma.partograma.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Desfecho do parto de uma paciente. Diferente de {@link RegistroPartograma} e
 * {@link SinalVital} (varios registros ao longo do tempo), so existe UM desfecho por
 * paciente -- por isso e persistido como um mapa de campos no proprio documento
 * {@code pacientes/{pacienteId}} (campo "desfecho"), em vez de uma subcolecao. Isso
 * tambem simplifica a exclusao da paciente: nao existe subcolecao extra para limpar.
 *
 * Campos sem uma lista fechada explicita do cliente (apresentacao fetal, posicao/
 * variedade, condicao materna/RN, destino puerpera/RN) foram implementados como texto
 * livre em vez de opcoes fixas, para nao inventar taxonomia clinica que nao foi
 * fornecida. Os poucos campos com um conjunto padrao amplamente conhecido (laceracao
 * perineal, dequitacao, placenta) usam um seletor com as categorias-padrao da referencia.
 */
@IgnoreExtraProperties
public class DesfechoParto {

    // ---- Via de parto / informacoes do parto -----------------------------------------------
    private String viaDeParto;              // "Parto vaginal" | "Cesárea"
    private String dataParto;               // dd/MM/yyyy
    private String apresentacaoFetal;
    private String posicaoVariedade;
    private String laceracaoPerineal;       // Sem laceração | Grau I..IV
    private String dequitacao;              // Espontânea | Manual
    private String placenta;                // Íntegra | Incompleta
    private String intercorrencias;
    private String observacoes;

    // ---- Recem-nascido ------------------------------------------------------------------
    private String sexoRn;                  // Masculino | Feminino
    private Integer pesoRn;                 // g
    private Double comprimentoRn;           // cm
    private Double perimetroCefalicoRn;     // cm
    private String horarioNascimento;       // HH:mm
    private Integer apgar1;
    private Integer apgar5;
    private Integer apgar10;
    private Boolean contatoPeleAPele;
    private Boolean amamentacaoPrimeiraHora;
    private String intercorrenciasRn;

    // ---- Encerramento ---------------------------------------------------------------------
    private String condicaoMaterna;
    private String condicaoRn;
    private String destinoPuerpera;
    private String destinoRn;
    private String profissionalResponsavel;
    private String coren;
    private String observacoesFinais;

    @ServerTimestamp
    private Date dataHora;

    public DesfechoParto() {
        // Construtor vazio exigido pelo Firestore.
    }

    public String getViaDeParto() {
        return viaDeParto;
    }

    public void setViaDeParto(String viaDeParto) {
        this.viaDeParto = viaDeParto;
    }

    public String getDataParto() {
        return dataParto;
    }

    public void setDataParto(String dataParto) {
        this.dataParto = dataParto;
    }

    public String getApresentacaoFetal() {
        return apresentacaoFetal;
    }

    public void setApresentacaoFetal(String apresentacaoFetal) {
        this.apresentacaoFetal = apresentacaoFetal;
    }

    public String getPosicaoVariedade() {
        return posicaoVariedade;
    }

    public void setPosicaoVariedade(String posicaoVariedade) {
        this.posicaoVariedade = posicaoVariedade;
    }

    public String getLaceracaoPerineal() {
        return laceracaoPerineal;
    }

    public void setLaceracaoPerineal(String laceracaoPerineal) {
        this.laceracaoPerineal = laceracaoPerineal;
    }

    public String getDequitacao() {
        return dequitacao;
    }

    public void setDequitacao(String dequitacao) {
        this.dequitacao = dequitacao;
    }

    public String getPlacenta() {
        return placenta;
    }

    public void setPlacenta(String placenta) {
        this.placenta = placenta;
    }

    public String getIntercorrencias() {
        return intercorrencias;
    }

    public void setIntercorrencias(String intercorrencias) {
        this.intercorrencias = intercorrencias;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getSexoRn() {
        return sexoRn;
    }

    public void setSexoRn(String sexoRn) {
        this.sexoRn = sexoRn;
    }

    public Integer getPesoRn() {
        return pesoRn;
    }

    public void setPesoRn(Integer pesoRn) {
        this.pesoRn = pesoRn;
    }

    public Double getComprimentoRn() {
        return comprimentoRn;
    }

    public void setComprimentoRn(Double comprimentoRn) {
        this.comprimentoRn = comprimentoRn;
    }

    public Double getPerimetroCefalicoRn() {
        return perimetroCefalicoRn;
    }

    public void setPerimetroCefalicoRn(Double perimetroCefalicoRn) {
        this.perimetroCefalicoRn = perimetroCefalicoRn;
    }

    public String getHorarioNascimento() {
        return horarioNascimento;
    }

    public void setHorarioNascimento(String horarioNascimento) {
        this.horarioNascimento = horarioNascimento;
    }

    public Integer getApgar1() {
        return apgar1;
    }

    public void setApgar1(Integer apgar1) {
        this.apgar1 = apgar1;
    }

    public Integer getApgar5() {
        return apgar5;
    }

    public void setApgar5(Integer apgar5) {
        this.apgar5 = apgar5;
    }

    public Integer getApgar10() {
        return apgar10;
    }

    public void setApgar10(Integer apgar10) {
        this.apgar10 = apgar10;
    }

    public Boolean getContatoPeleAPele() {
        return contatoPeleAPele;
    }

    public void setContatoPeleAPele(Boolean contatoPeleAPele) {
        this.contatoPeleAPele = contatoPeleAPele;
    }

    public Boolean getAmamentacaoPrimeiraHora() {
        return amamentacaoPrimeiraHora;
    }

    public void setAmamentacaoPrimeiraHora(Boolean amamentacaoPrimeiraHora) {
        this.amamentacaoPrimeiraHora = amamentacaoPrimeiraHora;
    }

    public String getIntercorrenciasRn() {
        return intercorrenciasRn;
    }

    public void setIntercorrenciasRn(String intercorrenciasRn) {
        this.intercorrenciasRn = intercorrenciasRn;
    }

    public String getCondicaoMaterna() {
        return condicaoMaterna;
    }

    public void setCondicaoMaterna(String condicaoMaterna) {
        this.condicaoMaterna = condicaoMaterna;
    }

    public String getCondicaoRn() {
        return condicaoRn;
    }

    public void setCondicaoRn(String condicaoRn) {
        this.condicaoRn = condicaoRn;
    }

    public String getDestinoPuerpera() {
        return destinoPuerpera;
    }

    public void setDestinoPuerpera(String destinoPuerpera) {
        this.destinoPuerpera = destinoPuerpera;
    }

    public String getDestinoRn() {
        return destinoRn;
    }

    public void setDestinoRn(String destinoRn) {
        this.destinoRn = destinoRn;
    }

    public String getProfissionalResponsavel() {
        return profissionalResponsavel;
    }

    public void setProfissionalResponsavel(String profissionalResponsavel) {
        this.profissionalResponsavel = profissionalResponsavel;
    }

    public String getCoren() {
        return coren;
    }

    public void setCoren(String coren) {
        this.coren = coren;
    }

    public String getObservacoesFinais() {
        return observacoesFinais;
    }

    public void setObservacoesFinais(String observacoesFinais) {
        this.observacoesFinais = observacoesFinais;
    }

    public Date getDataHora() {
        return dataHora;
    }

    public void setDataHora(Date dataHora) {
        this.dataHora = dataHora;
    }
}
