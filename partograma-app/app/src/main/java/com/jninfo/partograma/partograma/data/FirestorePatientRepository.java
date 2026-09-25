package com.jninfo.partograma.partograma.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acesso aos pacientes cadastrados na nuvem (Firestore), usados pela nova tela "Pacientes"
 * e pelas abas de detalhes (Dados/Partograma/Relatório/Configurações).
 *
 * Isto substitui, para os pacientes cadastrados a partir desta evolucao, o esquema de
 * "slots" fixos (1..10) do {@link PatientRepository} original -- aqui cada paciente tem
 * um documento proprio com ID gerado pelo Firestore, sem limite artificial de 10.
 *
 * O fluxo antigo (Menu/Detalhes/relatorio, baseado em SharedPreferences) continua existindo
 * e funcionando exatamente como antes; esta classe NAO o substitui ainda -- ela e a base
 * para as novas telas. A migracao da logica de partograma (Detalhes.java) para usar este
 * repositorio e um passo seguinte, ainda pendente.
 */
public class FirestorePatientRepository {

    public static final String COLECAO_PACIENTES = "pacientes";
    public static final String SUBCOLECAO_REGISTROS = "registros";
    public static final String SUBCOLECAO_EVOLUCOES = "evolucoes";
    public static final String SUBCOLECAO_SINAIS_VITAIS = "sinaisVitais";
    public static final String CAMPO_DESFECHO = "desfecho";

    private final FirebaseFirestore db;

    public FirestorePatientRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface ListaPacientesCallback {
        void onPacientesAtualizados(List<Paciente> pacientes);
        void onErro(Exception erro);
    }

    public interface OperacaoCallback {
        void onSucesso();
        void onErro(Exception erro);
    }

    public interface PacienteCallback {
        void onCarregado(@Nullable Paciente paciente);
        void onErro(Exception erro);
    }

    public interface ListaRegistrosCallback {
        void onRegistrosAtualizados(List<RegistroPartograma> registros);
        void onErro(Exception erro);
    }

    public interface ListaSinaisVitaisCallback {
        void onSinaisVitaisAtualizados(List<SinalVital> sinaisVitais);
        void onErro(Exception erro);
    }

    public interface DesfechoCallback {
        void onCarregado(@Nullable DesfechoParto desfecho);
        void onErro(Exception erro);
    }

    /**
     * Observa a lista de pacientes em tempo real (ordenados por criacao, mais recente primeiro).
     * Retorna o {@link ListenerRegistration} para poder cancelar em onStop/onDestroy.
     */
    public ListenerRegistration observarPacientes(ListaPacientesCallback callback) {
        Query query = db.collection(COLECAO_PACIENTES).orderBy("criadoEm", Query.Direction.DESCENDING);
        return query.addSnapshotListener((QuerySnapshot snapshot, com.google.firebase.firestore.FirebaseFirestoreException erro) -> {
            if (erro != null) {
                callback.onErro(erro);
                return;
            }
            List<Paciente> pacientes = new ArrayList<>();
            if (snapshot != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                    Paciente paciente = doc.toObject(Paciente.class);
                    if (paciente != null) {
                        paciente.setId(doc.getId());
                        pacientes.add(paciente);
                    }
                }
            }
            callback.onPacientesAtualizados(pacientes);
        });
    }

    public void carregarPaciente(String pacienteId, PacienteCallback callback) {
        db.collection(COLECAO_PACIENTES).document(pacienteId).get()
                .addOnSuccessListener(doc -> {
                    Paciente paciente = doc.toObject(Paciente.class);
                    if (paciente != null) {
                        paciente.setId(doc.getId());
                    }
                    callback.onCarregado(paciente);
                })
                .addOnFailureListener(callback::onErro);
    }

    /** Cria um novo paciente. {@code paciente.getId()} e ignorado -- o ID e gerado pelo Firestore. */
    public void adicionarPaciente(@NonNull Paciente paciente, OperacaoCallback callback) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", paciente.getNome());
        dados.put("prontuario", paciente.getProntuario());
        dados.put("dataNascimento", paciente.getDataNascimento());
        dados.put("dum", paciente.getDum());
        dados.put("tipoSanguineo", paciente.getTipoSanguineo());
        dados.put("alergias", paciente.getAlergias());
        dados.put("profissionalResponsavel", paciente.getProfissionalResponsavel());
        dados.put("queixaPrincipal", paciente.getQueixaPrincipal());
        dados.put("conduta", paciente.getConduta());
        dados.put("status", paciente.getStatus());
        dados.put("comorbidades", paciente.getComorbidades());
        dados.put("comorbidadesOutras", paciente.getComorbidadesOutras());
        dados.put("classificacaoRisco", paciente.getClassificacaoRisco());
        dados.put("horaAtual", "ainda sem");
        dados.put("avisoBatimento", "ainda sem");
        dados.put("avisoLee", "ainda sem");
        dados.put("avisoDilatacao", "ainda sem");
        dados.put("criadoEm", FieldValue.serverTimestamp());
        dados.put("ultimoAcesso", FieldValue.serverTimestamp());

        db.collection(COLECAO_PACIENTES).add(dados)
                .addOnSuccessListener(docRef -> callback.onSucesso())
                .addOnFailureListener(callback::onErro);
    }

    public void atualizarUltimoAcesso(String pacienteId) {
        db.collection(COLECAO_PACIENTES).document(pacienteId)
                .update("ultimoAcesso", FieldValue.serverTimestamp());
    }

    public void excluirPaciente(String pacienteId, OperacaoCallback callback) {
        DocumentReference doc = db.collection(COLECAO_PACIENTES).document(pacienteId);
        doc.delete()
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(callback::onErro);
    }

    /**
     * Exclusao em cascata: o Firestore NAO apaga subcolecoes automaticamente ao apagar o
     * documento pai -- se so déssemos doc.delete() no paciente, os documentos de
     * "registros", "evolucoes" e "sinaisVitais" continuariam existindo, orfaos
     * (acessiveis por quem soubesse o caminho, mas invisiveis nas telas). Por isso
     * buscamos os IDs de cada subcolecao primeiro e apagamos tudo -- registros,
     * evolucoes, sinais vitais e o proprio paciente (que carrega o desfecho embutido,
     * ver {@link #salvarDesfecho}) -- num unico WriteBatch atomico (tudo ou nada, sem
     * risco de exclusao parcial).
     */
    public void excluirPacienteCompleto(String pacienteId, OperacaoCallback callback) {
        DocumentReference docPaciente = db.collection(COLECAO_PACIENTES).document(pacienteId);
        registrosDoPaciente(pacienteId).get()
                .addOnSuccessListener(registrosSnapshot ->
                        evolucoesDoPaciente(pacienteId).get()
                                .addOnSuccessListener(evolucoesSnapshot ->
                                        sinaisVitaisDoPaciente(pacienteId).get()
                                                .addOnSuccessListener(sinaisSnapshot -> {
                                                    WriteBatch batch = db.batch();
                                                    for (DocumentSnapshot doc : registrosSnapshot.getDocuments()) {
                                                        batch.delete(doc.getReference());
                                                    }
                                                    for (DocumentSnapshot doc : evolucoesSnapshot.getDocuments()) {
                                                        batch.delete(doc.getReference());
                                                    }
                                                    for (DocumentSnapshot doc : sinaisSnapshot.getDocuments()) {
                                                        batch.delete(doc.getReference());
                                                    }
                                                    batch.delete(docPaciente);
                                                    batch.commit()
                                                            .addOnSuccessListener(unused -> callback.onSucesso())
                                                            .addOnFailureListener(callback::onErro);
                                                })
                                                .addOnFailureListener(callback::onErro))
                                .addOnFailureListener(callback::onErro))
                .addOnFailureListener(callback::onErro);
    }

    public CollectionReference registrosDoPaciente(String pacienteId) {
        return db.collection(COLECAO_PACIENTES).document(pacienteId).collection(SUBCOLECAO_REGISTROS);
    }

    public CollectionReference evolucoesDoPaciente(String pacienteId) {
        return db.collection(COLECAO_PACIENTES).document(pacienteId).collection(SUBCOLECAO_EVOLUCOES);
    }

    public CollectionReference sinaisVitaisDoPaciente(String pacienteId) {
        return db.collection(COLECAO_PACIENTES).document(pacienteId).collection(SUBCOLECAO_SINAIS_VITAIS);
    }

    /** Observa os registros (avaliacoes) do partograma em tempo real, mais recente primeiro. */
    public ListenerRegistration observarRegistros(String pacienteId, ListaRegistrosCallback callback) {
        Query query = registrosDoPaciente(pacienteId).orderBy("dataHora", Query.Direction.DESCENDING);
        return query.addSnapshotListener((snapshot, erro) -> {
            if (erro != null) {
                callback.onErro(erro);
                return;
            }
            List<RegistroPartograma> registros = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    RegistroPartograma registro = doc.toObject(RegistroPartograma.class);
                    if (registro != null) {
                        registro.setId(doc.getId());
                        registros.add(registro);
                    }
                }
            }
            callback.onRegistrosAtualizados(registros);
        });
    }

    /**
     * Adiciona uma nova avaliacao (registro) do partograma e atualiza o resumo no
     * documento do paciente (horaAtual + status = fase do trabalho de parto informada),
     * para a tag da lista de pacientes refletir a fase mais recente de verdade.
     */
    public void adicionarRegistro(String pacienteId, RegistroPartograma registro, OperacaoCallback callback) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("horario", registro.getHorario());
        dados.put("dilatacao", registro.getDilatacao());
        dados.put("posicaoBebe", registro.getPosicaoBebe());
        dados.put("planoDeLee", registro.getPlanoDeLee());
        dados.put("batimentos", registro.getBatimentos());
        dados.put("integridade", registro.getIntegridade());
        dados.put("liquido", registro.getLiquido());
        dados.put("freqContracao", registro.getFreqContracao());
        dados.put("ocitocina", registro.getOcitocina());
        dados.put("mesoprostol", registro.getMesoprostol());
        dados.put("remedios", registro.getRemedios());
        dados.put("examinador", registro.getExaminador());
        dados.put("faseTrabalhoParto", registro.getFaseTrabalhoParto());
        dados.put("metodosNaoFarmacologicos", registro.getMetodosNaoFarmacologicos());
        dados.put("teveIntercorrencia", registro.getTeveIntercorrencia());
        dados.put("intercorrencia", registro.getIntercorrencia());
        dados.put("observacaoAdicional", registro.getObservacaoAdicional());
        dados.put("dataHora", FieldValue.serverTimestamp());

        registrosDoPaciente(pacienteId).add(dados)
                .addOnSuccessListener(docRef -> {
                    Map<String, Object> atualizacaoPaciente = new HashMap<>();
                    atualizacaoPaciente.put("horaAtual", registro.getHorario());
                    atualizacaoPaciente.put("ultimoAcesso", FieldValue.serverTimestamp());
                    if (registro.getFaseTrabalhoParto() != null) {
                        atualizacaoPaciente.put("status", registro.getFaseTrabalhoParto());
                    }
                    db.collection(COLECAO_PACIENTES).document(pacienteId).update(atualizacaoPaciente);
                    callback.onSucesso();
                })
                .addOnFailureListener(callback::onErro);
    }

    /** Adiciona uma nota de evolucao/observacao da paciente. */
    public void adicionarEvolucao(String pacienteId, String texto, OperacaoCallback callback) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("texto", texto);
        dados.put("criadoEm", FieldValue.serverTimestamp());
        evolucoesDoPaciente(pacienteId).add(dados)
                .addOnSuccessListener(docRef -> callback.onSucesso())
                .addOnFailureListener(callback::onErro);
    }

    // ---- Sinais vitais -----------------------------------------------------------------------

    public ListenerRegistration observarSinaisVitais(String pacienteId, ListaSinaisVitaisCallback callback) {
        Query query = sinaisVitaisDoPaciente(pacienteId).orderBy("dataHora", Query.Direction.DESCENDING);
        return query.addSnapshotListener((snapshot, erro) -> {
            if (erro != null) {
                callback.onErro(erro);
                return;
            }
            List<SinalVital> lista = new ArrayList<>();
            if (snapshot != null) {
                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                    SinalVital sinal = doc.toObject(SinalVital.class);
                    if (sinal != null) {
                        sinal.setId(doc.getId());
                        lista.add(sinal);
                    }
                }
            }
            callback.onSinaisVitaisAtualizados(lista);
        });
    }

    public void adicionarSinalVital(String pacienteId, SinalVital sinal, OperacaoCallback callback) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("dataAfericao", sinal.getDataAfericao());
        dados.put("horario", sinal.getHorario());
        dados.put("paSistolica", sinal.getPaSistolica());
        dados.put("paDiastolica", sinal.getPaDiastolica());
        dados.put("frequenciaCardiaca", sinal.getFrequenciaCardiaca());
        dados.put("frequenciaRespiratoria", sinal.getFrequenciaRespiratoria());
        dados.put("temperatura", sinal.getTemperatura());
        dados.put("spo2", sinal.getSpo2());
        dados.put("dor", sinal.getDor());
        dados.put("hgt", sinal.getHgt());
        dados.put("observacoes", sinal.getObservacoes());
        dados.put("examinador", sinal.getExaminador());
        dados.put("dataHora", FieldValue.serverTimestamp());

        sinaisVitaisDoPaciente(pacienteId).add(dados)
                .addOnSuccessListener(docRef -> {
                    db.collection(COLECAO_PACIENTES).document(pacienteId)
                            .update("ultimoAcesso", FieldValue.serverTimestamp());
                    callback.onSucesso();
                })
                .addOnFailureListener(callback::onErro);
    }

    // ---- Desfecho do parto (embutido no documento do paciente, 1:1) --------------------------

    public void salvarDesfecho(String pacienteId, DesfechoParto desfecho, OperacaoCallback callback) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("viaDeParto", desfecho.getViaDeParto());
        dados.put("dataParto", desfecho.getDataParto());
        dados.put("apresentacaoFetal", desfecho.getApresentacaoFetal());
        dados.put("posicaoVariedade", desfecho.getPosicaoVariedade());
        dados.put("laceracaoPerineal", desfecho.getLaceracaoPerineal());
        dados.put("dequitacao", desfecho.getDequitacao());
        dados.put("placenta", desfecho.getPlacenta());
        dados.put("intercorrencias", desfecho.getIntercorrencias());
        dados.put("observacoes", desfecho.getObservacoes());
        dados.put("sexoRn", desfecho.getSexoRn());
        dados.put("pesoRn", desfecho.getPesoRn());
        dados.put("comprimentoRn", desfecho.getComprimentoRn());
        dados.put("perimetroCefalicoRn", desfecho.getPerimetroCefalicoRn());
        dados.put("horarioNascimento", desfecho.getHorarioNascimento());
        dados.put("apgar1", desfecho.getApgar1());
        dados.put("apgar5", desfecho.getApgar5());
        dados.put("apgar10", desfecho.getApgar10());
        dados.put("contatoPeleAPele", desfecho.getContatoPeleAPele());
        dados.put("amamentacaoPrimeiraHora", desfecho.getAmamentacaoPrimeiraHora());
        dados.put("intercorrenciasRn", desfecho.getIntercorrenciasRn());
        dados.put("condicaoMaterna", desfecho.getCondicaoMaterna());
        dados.put("condicaoRn", desfecho.getCondicaoRn());
        dados.put("destinoPuerpera", desfecho.getDestinoPuerpera());
        dados.put("destinoRn", desfecho.getDestinoRn());
        dados.put("profissionalResponsavel", desfecho.getProfissionalResponsavel());
        dados.put("coren", desfecho.getCoren());
        dados.put("observacoesFinais", desfecho.getObservacoesFinais());
        dados.put("dataHora", FieldValue.serverTimestamp());

        Map<String, Object> atualizacao = new HashMap<>();
        atualizacao.put(CAMPO_DESFECHO, dados);
        db.collection(COLECAO_PACIENTES).document(pacienteId).update(atualizacao)
                .addOnSuccessListener(unused -> callback.onSucesso())
                .addOnFailureListener(callback::onErro);
    }

    public void carregarDesfecho(String pacienteId, DesfechoCallback callback) {
        db.collection(COLECAO_PACIENTES).document(pacienteId).get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> mapa = (Map<String, Object>) doc.get(CAMPO_DESFECHO);
                    if (mapa == null) {
                        callback.onCarregado(null);
                        return;
                    }
                    DesfechoParto desfecho = new DesfechoParto();
                    desfecho.setViaDeParto((String) mapa.get("viaDeParto"));
                    desfecho.setDataParto((String) mapa.get("dataParto"));
                    desfecho.setApresentacaoFetal((String) mapa.get("apresentacaoFetal"));
                    desfecho.setPosicaoVariedade((String) mapa.get("posicaoVariedade"));
                    desfecho.setLaceracaoPerineal((String) mapa.get("laceracaoPerineal"));
                    desfecho.setDequitacao((String) mapa.get("dequitacao"));
                    desfecho.setPlacenta((String) mapa.get("placenta"));
                    desfecho.setIntercorrencias((String) mapa.get("intercorrencias"));
                    desfecho.setObservacoes((String) mapa.get("observacoes"));
                    desfecho.setSexoRn((String) mapa.get("sexoRn"));
                    desfecho.setPesoRn(paraInteiro(mapa.get("pesoRn")));
                    desfecho.setComprimentoRn(paraDouble(mapa.get("comprimentoRn")));
                    desfecho.setPerimetroCefalicoRn(paraDouble(mapa.get("perimetroCefalicoRn")));
                    desfecho.setHorarioNascimento((String) mapa.get("horarioNascimento"));
                    desfecho.setApgar1(paraInteiro(mapa.get("apgar1")));
                    desfecho.setApgar5(paraInteiro(mapa.get("apgar5")));
                    desfecho.setApgar10(paraInteiro(mapa.get("apgar10")));
                    desfecho.setContatoPeleAPele((Boolean) mapa.get("contatoPeleAPele"));
                    desfecho.setAmamentacaoPrimeiraHora((Boolean) mapa.get("amamentacaoPrimeiraHora"));
                    desfecho.setIntercorrenciasRn((String) mapa.get("intercorrenciasRn"));
                    desfecho.setCondicaoMaterna((String) mapa.get("condicaoMaterna"));
                    desfecho.setCondicaoRn((String) mapa.get("condicaoRn"));
                    desfecho.setDestinoPuerpera((String) mapa.get("destinoPuerpera"));
                    desfecho.setDestinoRn((String) mapa.get("destinoRn"));
                    desfecho.setProfissionalResponsavel((String) mapa.get("profissionalResponsavel"));
                    desfecho.setCoren((String) mapa.get("coren"));
                    desfecho.setObservacoesFinais((String) mapa.get("observacoesFinais"));
                    callback.onCarregado(desfecho);
                })
                .addOnFailureListener(callback::onErro);
    }

    private Integer paraInteiro(Object valor) {
        return valor instanceof Number ? ((Number) valor).intValue() : null;
    }

    private Double paraDouble(Object valor) {
        return valor instanceof Number ? ((Number) valor).doubleValue() : null;
    }
}
