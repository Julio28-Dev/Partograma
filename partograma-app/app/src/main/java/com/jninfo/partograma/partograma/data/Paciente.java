package com.jninfo.partograma.partograma.data;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Um paciente cadastrado no Firestore (colecao {@link FirestorePatientRepository#COLECAO_PACIENTES}).
 *
 * Campos que ja existiam no APK original (preservados 1:1, so trocando SharedPreferences por
 * Firestore): nome, horaAtual, avisoBatimento, avisoLee, avisoDilatacao.
 *
 * Campos NOVOS nesta etapa (nao existem no APK original -- necessarios para a tela "Pacientes"
 * e a aba "Dados" pedidas no novo design, ver auditoria/relatorio da evolucao): prontuario,
 * dataNascimento, dum, tipoSanguineo, alergias, profissionalResponsavel, queixaPrincipal,
 * conduta, status, ultimoAcesso, criadoEm.
 */
@IgnoreExtraProperties
public class Paciente {

    private String id;

    // ---- Preservados do APK original (equivalentes a "nome{slot}", "pessoa{slot}hora", etc.) --
    private String nome;
    private String horaAtual;
    private String avisoBatimento;
    private String avisoLee;
    private String avisoDilatacao;

    // ---- Novos nesta etapa (tela Pacientes / aba Dados) ----------------------------------------
    private String prontuario;
    private String dataNascimento;      // dd/MM/yyyy
    private String dum;                 // dd/MM/yyyy - data da ultima menstruacao
    private String tipoSanguineo;
    private String alergias;
    private String profissionalResponsavel;
    private String queixaPrincipal;
    private String conduta;
    private String status;              // "Em avaliação" | "Fase latente" | "Trabalho de parto ativo"

    @ServerTimestamp
    private Date criadoEm;

    @ServerTimestamp
    private Date ultimoAcesso;

    public Paciente() {
        // Construtor vazio exigido pelo Firestore para desserializacao automatica.
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

    public String getHoraAtual() {
        return horaAtual;
    }

    public void setHoraAtual(String horaAtual) {
        this.horaAtual = horaAtual;
    }

    public String getAvisoBatimento() {
        return avisoBatimento;
    }

    public void setAvisoBatimento(String avisoBatimento) {
        this.avisoBatimento = avisoBatimento;
    }

    public String getAvisoLee() {
        return avisoLee;
    }

    public void setAvisoLee(String avisoLee) {
        this.avisoLee = avisoLee;
    }

    public String getAvisoDilatacao() {
        return avisoDilatacao;
    }

    public void setAvisoDilatacao(String avisoDilatacao) {
        this.avisoDilatacao = avisoDilatacao;
    }

    public String getProntuario() {
        return prontuario;
    }

    public void setProntuario(String prontuario) {
        this.prontuario = prontuario;
    }

    public String getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(String dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getDum() {
        return dum;
    }

    public void setDum(String dum) {
        this.dum = dum;
    }

    public String getTipoSanguineo() {
        return tipoSanguineo;
    }

    public void setTipoSanguineo(String tipoSanguineo) {
        this.tipoSanguineo = tipoSanguineo;
    }

    public String getAlergias() {
        return alergias;
    }

    public void setAlergias(String alergias) {
        this.alergias = alergias;
    }

    public String getProfissionalResponsavel() {
        return profissionalResponsavel;
    }

    public void setProfissionalResponsavel(String profissionalResponsavel) {
        this.profissionalResponsavel = profissionalResponsavel;
    }

    public String getQueixaPrincipal() {
        return queixaPrincipal;
    }

    public void setQueixaPrincipal(String queixaPrincipal) {
        this.queixaPrincipal = queixaPrincipal;
    }

    public String getConduta() {
        return conduta;
    }

    public void setConduta(String conduta) {
        this.conduta = conduta;
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

    public Date getUltimoAcesso() {
        return ultimoAcesso;
    }

    public void setUltimoAcesso(Date ultimoAcesso) {
        this.ultimoAcesso = ultimoAcesso;
    }

    /** Idade em anos calculada a partir de {@link #dataNascimento} (formato dd/MM/yyyy). */
    @Exclude
    public int getIdadeAnos() {
        if (dataNascimento == null || dataNascimento.isEmpty()) {
            return -1;
        }
        try {
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            formato.setLenient(false);
            Date nascimento = formato.parse(dataNascimento);
            if (nascimento == null) {
                return -1;
            }
            Calendar nascimentoCal = Calendar.getInstance();
            nascimentoCal.setTime(nascimento);
            Calendar hoje = Calendar.getInstance();

            int idade = hoje.get(Calendar.YEAR) - nascimentoCal.get(Calendar.YEAR);
            if (hoje.get(Calendar.DAY_OF_YEAR) < nascimentoCal.get(Calendar.DAY_OF_YEAR)) {
                idade--;
            }
            return Math.max(idade, 0);
        } catch (Exception e) {
            return -1;
        }
    }

    /** Idade gestacional (semanas + dias) calculada a partir de {@link #dum}. */
    @Exclude
    public String getIdadeGestacional() {
        if (dum == null || dum.isEmpty()) {
            return null;
        }
        try {
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            formato.setLenient(false);
            Date dataUltimaMenstruacao = formato.parse(dum);
            if (dataUltimaMenstruacao == null) {
                return null;
            }
            long diffMs = new Date().getTime() - dataUltimaMenstruacao.getTime();
            long dias = diffMs / (1000L * 60 * 60 * 24);
            if (dias < 0) {
                return null;
            }
            long semanas = dias / 7;
            long diasRestantes = dias % 7;
            return semanas + "s + " + diasRestantes + "d";
        } catch (Exception e) {
            return null;
        }
    }
}
