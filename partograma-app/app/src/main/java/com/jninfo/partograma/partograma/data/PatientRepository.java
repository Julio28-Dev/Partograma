package com.jninfo.partograma.partograma.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Acesso a todos os dados persistidos pelo aplicativo original.
 *
 * O APK original nao usa banco de dados (SQLite/Room) nem arquivos: TODOS os dados sao
 * gravados em um unico SharedPreferences chamado "ArquivoPreferencia", com chaves
 * concatenadas manualmente (ex.: "nome1", "pessoa3dilatacao7"). Esta classe reproduz
 * exatamente o mesmo esquema de chaves para manter compatibilidade total de dados e
 * comportamento, apenas organizando o acesso em metodos em vez de repetir a mesma
 * chamada a SharedPreferences dezenas de vezes em cada Activity (como no original).
 *
 * Limites originais preservados: ate {@link #MAX_PACIENTES} pacientes ("leitos") em
 * paralelo, cada um com ate {@link #MAX_HORAS} registros horarios.
 */
public class PatientRepository {

    private static final String ARQUIVO_PREFERENCIA = "ArquivoPreferencia";

    public static final int MAX_PACIENTES = 10;
    public static final int MAX_HORAS = 16;

    private final SharedPreferences prefs;

    public PatientRepository(Context context) {
        this.prefs = context.getSharedPreferences(ARQUIVO_PREFERENCIA, Context.MODE_PRIVATE);
    }

    // ---- Tutorial (TelaInicial) ----------------------------------------------------------

    public boolean isTutorialCompleto() {
        return "completo".equals(prefs.getString("tutorial", "incompleto"));
    }

    public void marcarTutorialCompleto() {
        prefs.edit().putString("tutorial", "completo").apply();
    }

    // ---- Pacientes (Menu) -----------------------------------------------------------------

    public String getNomePaciente(int slot) {
        return prefs.getString("nome" + slot, "ainda sem");
    }

    public boolean existePacienteNoSlot(int slot) {
        return prefs.contains("nome" + slot);
    }

    /** Retorna o slot (1..MAX_PACIENTES) do paciente com esse nome, ou 0 se nao encontrado. */
    public int localizarSlotPorNome(String nome) {
        for (int slot = 1; slot <= MAX_PACIENTES; slot++) {
            if (existePacienteNoSlot(slot) && getNomePaciente(slot).equals(nome)) {
                return slot;
            }
        }
        return 0;
    }

    /** Retorna o primeiro slot livre (1..MAX_PACIENTES), ou 0 se todos os leitos estiverem ocupados. */
    public int localizarSlotLivre() {
        for (int slot = 1; slot <= MAX_PACIENTES; slot++) {
            if (!existePacienteNoSlot(slot)) {
                return slot;
            }
        }
        return 0;
    }

    /** Nomes de todos os pacientes cadastrados, na ordem dos slots (igual ao APK original). */
    public java.util.List<String> listarNomesPacientes() {
        java.util.List<String> nomes = new java.util.ArrayList<>();
        for (int slot = 1; slot <= MAX_PACIENTES; slot++) {
            if (existePacienteNoSlot(slot)) {
                nomes.add(getNomePaciente(slot));
            }
        }
        return nomes;
    }

    public void criarPaciente(int slot, String nome) {
        prefs.edit().putString("nome" + slot, nome).apply();
    }

    /** Remove o paciente do slot e TODOS os registros horarios associados a ele. */
    public void excluirPaciente(int slot) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("nome" + slot);
        editor.remove("pessoa" + slot + "hora");
        editor.remove("pessoa" + slot + "avisobatimento");
        editor.remove("pessoa" + slot + "avisolee");
        editor.remove("pessoa" + slot + "avisodilatacao");
        for (int hora = 1; hora <= MAX_HORAS; hora++) {
            String prefixo = "pessoa" + slot;
            editor.remove(prefixo + "dia" + hora);
            editor.remove(prefixo + "horadodia" + hora);
            editor.remove(prefixo + "batimentos" + hora);
            editor.remove(prefixo + "dilatacao" + hora);
            editor.remove(prefixo + "integridade" + hora);
            editor.remove(prefixo + "liquido" + hora);
            editor.remove(prefixo + "lee" + hora);
            editor.remove(prefixo + "freqcontracao" + hora);
            editor.remove(prefixo + "ocitosina" + hora);
            editor.remove(prefixo + "mesoprostol" + hora);
            editor.remove(prefixo + "remedios" + hora);
            editor.remove(prefixo + "examinador" + hora);
            editor.remove(prefixo + "posicao" + hora);
            editor.remove(prefixo + "temhora" + hora);
        }
        editor.apply();
    }

    // ---- Registro horario (Detalhes / relatorio) ------------------------------------------

    /** Numero da hora (1..16) atualmente selecionada/exibida para o paciente. */
    public String getHoraAtual(int slot) {
        return prefs.getString("pessoa" + slot + "hora", "ainda sem");
    }

    public boolean horaFoiRegistrada(int slot, int hora) {
        return "tem".equals(prefs.getString("pessoa" + slot + "temhora" + hora, "ainda sem"));
    }

    public String getCampo(int slot, String campo, int hora, String padrao) {
        return prefs.getString("pessoa" + slot + campo + hora, padrao);
    }

    public int getPosicaoLee(int slot, int hora, int padrao) {
        return prefs.getInt("pessoa" + slot + "posicao" + hora, padrao);
    }

    public String getAviso(String tipo, int slot, String padrao) {
        return prefs.getString("pessoa" + slot + "aviso" + tipo, padrao);
    }

    public void salvarAviso(String tipo, int slot, String texto) {
        prefs.edit().putString("pessoa" + slot + "aviso" + tipo, texto).apply();
    }

    /**
     * Grava um registro horario completo, com as mesmas chaves usadas pelo APK original
     * (dia, horadodia, batimentos, dilatacao, integridade, liquido, lee, freqcontracao,
     * ocitosina, mesoprostol, remedios, examinador, posicao) e marca "temhora{hora}=tem".
     */
    public void salvarRegistro(int slot, int hora, RegistroHorario r) {
        String prefixo = "pessoa" + slot;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(prefixo + "dia" + hora, r.dia);
        editor.putString(prefixo + "horadodia" + hora, r.horaDoDia);
        editor.putString(prefixo + "batimentos" + hora, r.batimentos);
        editor.putString(prefixo + "dilatacao" + hora, r.dilatacao);
        editor.putString(prefixo + "integridade" + hora, r.integridade);
        editor.putString(prefixo + "liquido" + hora, r.liquido);
        editor.putString(prefixo + "lee" + hora, r.lee);
        editor.putString(prefixo + "freqcontracao" + hora, r.freqContracao);
        editor.putString(prefixo + "ocitosina" + hora, r.ocitocina);
        editor.putString(prefixo + "mesoprostol" + hora, r.mesoprostol);
        editor.putString(prefixo + "remedios" + hora, r.remedios);
        editor.putString(prefixo + "examinador" + hora, r.examinador);
        editor.putString(prefixo + "hora", String.valueOf(hora));
        editor.putString(prefixo + "temhora" + hora, "tem");
        editor.putInt(prefixo + "posicao" + hora, r.posicaoLee);
        editor.apply();
    }

    /** Dados de um unico registro horario do partograma (uma "coluna" da tabela). */
    public static class RegistroHorario {
        public String dia;
        public String horaDoDia;
        public String batimentos;
        public String dilatacao;
        public String integridade;
        public String liquido;
        public String lee;
        public String freqContracao;
        public String ocitocina;
        public String mesoprostol;
        public String remedios;
        public String examinador;
        public int posicaoLee;
    }
}
