package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.text.TextUtils;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.ListenerRegistration;
import com.jninfo.partograma.partograma.data.DesfechoParto;
import com.jninfo.partograma.partograma.data.FirestorePatientRepository;
import com.jninfo.partograma.partograma.data.Paciente;
import com.jninfo.partograma.partograma.data.RegistroPartograma;
import com.jninfo.partograma.partograma.data.SinalVital;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tela de detalhes da paciente, com as 5 abas pedidas pelo cliente, nesta ordem exata:
 * Dados / Partograma / Relatório / Sinais vitais / Desfecho do parto. "Configurações"
 * NAO e mais uma aba daqui -- e uma tela propria (ver {@link ConfiguracoesActivity}),
 * porque sao preferencias do aparelho/app, nao dados de uma paciente especifica, e a
 * lista de abas pedida pelo cliente e exaustiva (nao inclui Configuracoes).
 */
public class PacienteDetalhesActivity extends BaseActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";

    private FirestorePatientRepository repositorio;
    private String pacienteId;
    private String nomePaciente;
    private ListenerRegistration listenerRegistros;
    private ListenerRegistration listenerSinaisVitais;
    private List<RegistroPartograma> registrosAtuais = new ArrayList<>();
    private List<SinalVital> sinaisVitaisAtuais = new ArrayList<>();

    private TextView txtNomeCabecalho;
    private TextView txtSubtituloCabecalho;
    private LinearLayout containerDadosPaciente;
    private LinearLayout containerEvolucao;
    private ProgressBar progressDetalhes;

    private View conteudoDados;
    private View conteudoPartograma;
    private View conteudoRelatorio;
    private View conteudoSinaisVitais;
    private View conteudoDesfecho;

    private View[] abas;
    private View[] conteudos;

    // Partograma
    private View indicadorDilatacao;
    private View indicadorRotatividade;
    private TextView txtHoraUltimaAvaliacao;
    private LinearLayout containerHistorico;

    // Sinais vitais
    private LinearLayout containerHistoricoSinaisVitais;

    // Desfecho
    private LinearLayout containerDesfechoRegistrado;
    private View btnViaVaginal;
    private View btnViaCesarea;
    private String viaDePartoEscolhida = "Parto vaginal";
    private android.widget.EditText edtDataParto;
    private android.widget.EditText edtApresentacaoFetal;
    private android.widget.EditText edtPosicaoVariedade;
    private Spinner spinnerLaceracaoPerineal;
    private Spinner spinnerDequitacao;
    private Spinner spinnerPlacenta;
    private android.widget.EditText edtIntercorrenciasParto;
    private android.widget.EditText edtObservacoesParto;
    private View btnSexoMasculino;
    private View btnSexoFeminino;
    private String sexoRnEscolhido = "Masculino";
    private android.widget.EditText edtPesoRn;
    private android.widget.EditText edtComprimentoRn;
    private android.widget.EditText edtPerimetroCefalico;
    private android.widget.EditText edtHorarioNascimento;
    private android.widget.EditText edtApgar1;
    private android.widget.EditText edtApgar5;
    private android.widget.EditText edtApgar10;
    private View btnContatoPeleSim;
    private View btnContatoPeleNao;
    private boolean contatoPeleEscolhido = true;
    private View btnAmamentacaoSim;
    private View btnAmamentacaoNao;
    private boolean amamentacaoEscolhida = true;
    private android.widget.EditText edtIntercorrenciasRn;
    private android.widget.EditText edtCondicaoMaterna;
    private android.widget.EditText edtCondicaoRn;
    private android.widget.EditText edtDestinoPuerpera;
    private android.widget.EditText edtDestinoRn;
    private android.widget.EditText edtProfissionalDesfecho;
    private android.widget.EditText edtCorenDesfecho;
    private android.widget.EditText edtObservacoesFinais;
    private View btnSalvarDesfecho;
    private ProgressBar progressDesfecho;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente_detalhes);

        pacienteId = getIntent().getStringExtra(EXTRA_PACIENTE_ID);
        repositorio = new FirestorePatientRepository();

        txtNomeCabecalho = findViewById(R.id.txtNomeCabecalho);
        txtSubtituloCabecalho = findViewById(R.id.txtSubtituloCabecalho);
        containerDadosPaciente = findViewById(R.id.containerDadosPaciente);
        containerEvolucao = findViewById(R.id.containerEvolucao);
        progressDetalhes = findViewById(R.id.progressDetalhes);

        conteudoDados = findViewById(R.id.conteudoDados);
        conteudoPartograma = findViewById(R.id.conteudoPartograma);
        conteudoRelatorio = findViewById(R.id.conteudoRelatorio);
        conteudoSinaisVitais = findViewById(R.id.conteudoSinaisVitais);
        conteudoDesfecho = findViewById(R.id.conteudoDesfecho);
        conteudos = new View[]{conteudoDados, conteudoPartograma, conteudoRelatorio, conteudoSinaisVitais, conteudoDesfecho};

        indicadorDilatacao = findViewById(R.id.indicadorDilatacao);
        indicadorRotatividade = findViewById(R.id.indicadorRotatividade);
        txtHoraUltimaAvaliacao = findViewById(R.id.txtHoraUltimaAvaliacao);
        containerHistorico = findViewById(R.id.containerHistorico);
        containerHistoricoSinaisVitais = findViewById(R.id.containerHistoricoSinaisVitais);

        View tabDados = findViewById(R.id.tabDados);
        View tabPartograma = findViewById(R.id.tabPartograma);
        View tabRelatorio = findViewById(R.id.tabRelatorio);
        View tabSinaisVitais = findViewById(R.id.tabSinaisVitais);
        View tabDesfecho = findViewById(R.id.tabDesfecho);
        abas = new View[]{tabDados, tabPartograma, tabRelatorio, tabSinaisVitais, tabDesfecho};

        configurarAba(tabDados, R.string.paciente_detalhes_aba_dados);
        configurarAba(tabPartograma, R.string.paciente_detalhes_aba_partograma);
        configurarAba(tabRelatorio, R.string.paciente_detalhes_aba_relatorio);
        configurarAba(tabSinaisVitais, R.string.sinais_vitais_titulo);
        configurarAba(tabDesfecho, R.string.desfecho_titulo);

        for (int i = 0; i < abas.length; i++) {
            int indice = i;
            abas[i].setOnClickListener(v -> selecionarAba(indice));
        }
        selecionarAba(0);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnMenuPaciente).setOnClickListener(this::abrirMenuPaciente);

        configurarPartograma();
        configurarRelatorio();
        configurarSinaisVitais();
        configurarDesfecho();

        if (pacienteId == null) {
            Toast.makeText(this, R.string.paciente_detalhes_erro_carregar, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        carregarPaciente();
        observarRegistros();
        observarSinaisVitais();
        carregarDesfecho();
    }

    private void configurarAba(View aba, int textoRes) {
        TextView texto = aba.findViewById(R.id.txtAba);
        texto.setText(textoRes);
    }

    private void selecionarAba(int indiceSelecionado) {
        for (int i = 0; i < abas.length; i++) {
            TextView texto = abas[i].findViewById(R.id.txtAba);
            View indicador = abas[i].findViewById(R.id.indicadorAba);
            boolean selecionada = i == indiceSelecionado;
            texto.setTextColor(ContextCompat.getColor(this,
                    selecionada ? R.color.primaryPink : R.color.textSecondary));
            texto.setTypeface(null, selecionada ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            indicador.setVisibility(selecionada ? View.VISIBLE : View.INVISIBLE);
            conteudos[i].setVisibility(selecionada ? View.VISIBLE : View.GONE);
        }
    }

    // ---- Cabecalho / exclusao -----------------------------------------------------------

    private void abrirMenuPaciente(View ancora) {
        PopupMenu menu = new PopupMenu(this, ancora);
        menu.getMenu().add(getString(R.string.excluir_paciente_menu));
        menu.setOnMenuItemClickListener(item -> {
            confirmarExclusao();
            return true;
        });
        menu.show();
    }

    private void confirmarExclusao() {
        String nome = nomePaciente != null ? nomePaciente : "";
        new AlertDialog.Builder(this)
                .setTitle(R.string.excluir_paciente_titulo)
                .setMessage(getString(R.string.excluir_paciente_mensagem, nome))
                .setNegativeButton(R.string.excluir_paciente_cancelar, null)
                .setPositiveButton(R.string.excluir_paciente_confirmar, (dialog, which) -> excluirPaciente())
                .create()
                .show();
    }

    private void excluirPaciente() {
        repositorio.excluirPacienteCompleto(pacienteId, new FirestorePatientRepository.OperacaoCallback() {
            @Override
            public void onSucesso() {
                Toast.makeText(PacienteDetalhesActivity.this, R.string.excluir_paciente_sucesso, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onErro(Exception erro) {
                Toast.makeText(PacienteDetalhesActivity.this, R.string.excluir_paciente_erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- Dados ---------------------------------------------------------------------------

    private void carregarPaciente() {
        progressDetalhes.setVisibility(View.VISIBLE);
        repositorio.carregarPaciente(pacienteId, new FirestorePatientRepository.PacienteCallback() {
            @Override
            public void onCarregado(@Nullable Paciente paciente) {
                progressDetalhes.setVisibility(View.GONE);
                if (paciente == null) {
                    Toast.makeText(PacienteDetalhesActivity.this,
                            R.string.paciente_detalhes_erro_carregar, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                nomePaciente = paciente.getNome();
                preencherCabecalho(paciente);
                preencherDados(paciente);
                carregarEvolucao();
            }

            @Override
            public void onErro(Exception erro) {
                progressDetalhes.setVisibility(View.GONE);
                Toast.makeText(PacienteDetalhesActivity.this,
                        R.string.paciente_detalhes_erro_carregar, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void preencherCabecalho(Paciente paciente) {
        txtNomeCabecalho.setText(paciente.getNome());
        int idade = paciente.getIdadeAnos();
        String prontuario = paciente.getProntuario() != null ? paciente.getProntuario() : "-";
        String subtitulo = "Prontuário: " + prontuario;
        if (idade >= 0) {
            subtitulo += "  •  " + idade + " anos";
        }
        txtSubtituloCabecalho.setText(subtitulo);
    }

    private void preencherDados(Paciente paciente) {
        containerDadosPaciente.removeAllViews();
        adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_nome, paciente.getNome());
        adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_prontuario, paciente.getProntuario());

        int idade = paciente.getIdadeAnos();
        adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_idade,
                idade >= 0 ? idade + " anos" : null);

        String ig = paciente.getIdadeGestacional();
        if (ig != null) {
            adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_ig, ig);
        }
        adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_dum, paciente.getDum());
        adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_nascimento, paciente.getDataNascimento());
        adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_tipo_sanguineo, paciente.getTipoSanguineo());
        adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_alergias, paciente.getAlergias());

        if (!paciente.getComorbidades().isEmpty()) {
            String comorbidadesTexto = TextUtils.join(", ", paciente.getComorbidades());
            if (!TextUtils.isEmpty(paciente.getComorbidadesOutras())) {
                comorbidadesTexto += " (" + paciente.getComorbidadesOutras() + ")";
            }
            adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_comorbidades, comorbidadesTexto);
        }
        adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_classificacao_risco, paciente.getClassificacaoRisco());

        adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_profissional, paciente.getProfissionalResponsavel());
        adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_queixa, paciente.getQueixaPrincipal());
        adicionarLinhaDado(containerDadosPaciente, R.string.paciente_dados_rotulo_conduta, paciente.getConduta());
    }

    private void adicionarLinhaDado(LinearLayout container, int rotuloRes, @Nullable String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return;
        }
        View linha = LayoutInflater.from(this).inflate(R.layout.item_dado_paciente, container, false);
        TextView txtRotulo = linha.findViewById(R.id.txtRotuloDado);
        TextView txtValor = linha.findViewById(R.id.txtValorDado);
        txtRotulo.setText(rotuloRes);
        txtValor.setText(valor);
        container.addView(linha);
    }

    private void carregarEvolucao() {
        repositorio.evolucoesDoPaciente(pacienteId).orderBy("criadoEm", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    containerEvolucao.removeAllViews();
                    if (snapshot.isEmpty()) {
                        TextView vazio = new TextView(this);
                        vazio.setText(R.string.paciente_dados_evolucao_vazia);
                        vazio.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
                        vazio.setTextSize(13.5f);
                        containerEvolucao.addView(vazio);
                        return;
                    }
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        String texto = doc.getString("texto");
                        if (texto != null) {
                            adicionarLinhaDado(containerEvolucao, R.string.paciente_dados_secao_evolucao, texto);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    containerEvolucao.removeAllViews();
                    TextView erro = new TextView(this);
                    erro.setText(R.string.paciente_dados_evolucao_vazia);
                    erro.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
                    containerEvolucao.addView(erro);
                });
    }

    // ---- Partograma ------------------------------------------------------------------------

    private void configurarPartograma() {
        findViewById(R.id.btnNovaAvaliacao).setOnClickListener(v -> {
            Intent intent = new Intent(this, NovaAvaliacaoActivity.class);
            intent.putExtra(NovaAvaliacaoActivity.EXTRA_PACIENTE_ID, pacienteId);
            startActivity(intent);
        });
        findViewById(R.id.btnVerGraficoCompleto).setOnClickListener(v -> abrirGrafico());
    }

    private void observarRegistros() {
        listenerRegistros = repositorio.observarRegistros(pacienteId, new FirestorePatientRepository.ListaRegistrosCallback() {
            @Override
            public void onRegistrosAtualizados(List<RegistroPartograma> registros) {
                registrosAtuais = registros;
                preencherPartograma(registros);
            }

            @Override
            public void onErro(Exception erro) {
                Toast.makeText(PacienteDetalhesActivity.this, R.string.paciente_detalhes_erro_carregar, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void preencherPartograma(List<RegistroPartograma> registros) {
        preencherIndicador(indicadorDilatacao, R.drawable.ic_feature_chart, R.string.partograma_card_dilatacao,
                registros.isEmpty() || registros.get(0).getDilatacao() == null ? getString(R.string.partograma_sem_dados)
                        : formatarDilatacao(registros.get(0).getDilatacao()),
                registros.isEmpty() ? "" : registros.get(0).getHorario());

        preencherIndicador(indicadorRotatividade, R.drawable.ic_patient, R.string.partograma_card_rotatividade,
                registros.isEmpty() || TextUtils.isEmpty(registros.get(0).getPosicaoBebe()) ? getString(R.string.partograma_sem_dados)
                        : registros.get(0).getPosicaoBebe(),
                registros.isEmpty() ? "" : registros.get(0).getHorario());

        txtHoraUltimaAvaliacao.setText(getString(R.string.partograma_hora_ultima_avaliacao) + "\n"
                + (registros.isEmpty() ? "-" : registros.get(0).getHorario()));

        containerHistorico.removeAllViews();
        if (registros.isEmpty()) {
            TextView vazio = new TextView(this);
            vazio.setText(R.string.partograma_historico_vazio);
            vazio.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
            vazio.setTextSize(13f);
            containerHistorico.addView(vazio);
            return;
        }
        for (RegistroPartograma registro : registros) {
            View linha = LayoutInflater.from(this).inflate(R.layout.item_registro_historico, containerHistorico, false);
            TextView txtHora = linha.findViewById(R.id.txtHoraHistorico);
            TextView txtDilatacao = linha.findViewById(R.id.txtDilatacaoHistorico);
            TextView txtRotatividade = linha.findViewById(R.id.txtRotatividadeHistorico);
            TextView txtPlanoDeLee = linha.findViewById(R.id.txtPlanoDeLeeHistorico);
            txtHora.setText(registro.getHorario() != null ? registro.getHorario() : "-");
            txtDilatacao.setText("Dilatação: " + (registro.getDilatacao() != null ? formatarDilatacao(registro.getDilatacao()) : "-"));
            txtRotatividade.setText("Posição do bebê: " + (TextUtils.isEmpty(registro.getPosicaoBebe()) ? "-" : registro.getPosicaoBebe()));
            txtPlanoDeLee.setText("Plano de De Lee: " + registro.getRotuloPlanoDeLee());
            containerHistorico.addView(linha);
        }
    }

    private void preencherIndicador(View indicador, int iconeRes, int labelRes, String valor, String hora) {
        ((android.widget.ImageView) indicador.findViewById(R.id.iconIndicador)).setImageResource(iconeRes);
        ((TextView) indicador.findViewById(R.id.txtLabelIndicador)).setText(labelRes);
        ((TextView) indicador.findViewById(R.id.txtValorIndicador)).setText(valor);
        ((TextView) indicador.findViewById(R.id.txtCaptionIndicador)).setText(R.string.partograma_ultima_afericao);
        ((TextView) indicador.findViewById(R.id.txtHoraIndicador)).setText(hora);
    }

    private String formatarDilatacao(Double dilatacao) {
        if (dilatacao == Math.floor(dilatacao)) {
            return String.format(Locale.getDefault(), "%d cm", dilatacao.intValue());
        }
        return String.format(Locale.getDefault(), "%.1f cm", dilatacao);
    }

    private void abrirGrafico() {
        Intent intent = new Intent(this, GraficoPartogramaActivity.class);
        intent.putExtra(GraficoPartogramaActivity.EXTRA_PACIENTE_ID, pacienteId);
        intent.putExtra(GraficoPartogramaActivity.EXTRA_NOME_PACIENTE, nomePaciente);
        startActivity(intent);
    }

    // ---- Sinais vitais -----------------------------------------------------------------------

    private void configurarSinaisVitais() {
        findViewById(R.id.btnNovaAfericao).setOnClickListener(v -> {
            Intent intent = new Intent(this, NovaAfericaoActivity.class);
            intent.putExtra(NovaAfericaoActivity.EXTRA_PACIENTE_ID, pacienteId);
            intent.putExtra(NovaAfericaoActivity.EXTRA_NOME_PACIENTE, nomePaciente);
            startActivity(intent);
        });
    }

    private void observarSinaisVitais() {
        listenerSinaisVitais = repositorio.observarSinaisVitais(pacienteId, new FirestorePatientRepository.ListaSinaisVitaisCallback() {
            @Override
            public void onSinaisVitaisAtualizados(List<SinalVital> sinaisVitais) {
                sinaisVitaisAtuais = sinaisVitais;
                preencherSinaisVitais(sinaisVitais);
            }

            @Override
            public void onErro(Exception erro) {
                Toast.makeText(PacienteDetalhesActivity.this, R.string.paciente_detalhes_erro_carregar, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void preencherSinaisVitais(List<SinalVital> sinaisVitais) {
        containerHistoricoSinaisVitais.removeAllViews();
        if (sinaisVitais.isEmpty()) {
            TextView vazio = new TextView(this);
            vazio.setText(R.string.sinais_vitais_historico_vazio);
            vazio.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
            vazio.setTextSize(13f);
            containerHistoricoSinaisVitais.addView(vazio);
            return;
        }
        for (SinalVital sinal : sinaisVitais) {
            View linha = LayoutInflater.from(this).inflate(R.layout.item_sinal_vital_historico, containerHistoricoSinaisVitais, false);
            TextView txtDataHora = linha.findViewById(R.id.txtDataHoraSinal);
            TextView txtResumo = linha.findViewById(R.id.txtResumoSinal);
            String horario = sinal.getHorario() != null ? sinal.getHorario() : "";
            String data = sinal.getDataAfericao() != null ? sinal.getDataAfericao() : "";
            txtDataHora.setText(horario + (data.isEmpty() ? "" : " · " + data));
            txtResumo.setText(sinal.getResumo());
            containerHistoricoSinaisVitais.addView(linha);
        }
    }

    // ---- Relatorio -------------------------------------------------------------------------

    private void configurarRelatorio() {
        configurarLinhaRelatorio(R.id.itemRelatorioEvolucao, R.drawable.ic_feature_clock,
                R.string.relatorio_evolucao, RelatorioListaActivity.MODO_EVOLUCAO);
        configurarLinhaRelatorio(R.id.itemRelatorioGraficos, R.drawable.ic_feature_chart,
                R.string.relatorio_graficos, null);
        configurarLinhaRelatorio(R.id.itemRelatorioIntercorrencias, R.drawable.ic_feature_pulse,
                R.string.relatorio_intercorrencias, RelatorioListaActivity.MODO_INTERCORRENCIAS);
        configurarLinhaRelatorio(R.id.itemRelatorioMedicacoes, R.drawable.ic_feature_pie,
                R.string.relatorio_medicacoes, RelatorioListaActivity.MODO_MEDICACOES);
        configurarLinhaRelatorio(R.id.itemRelatorioObservacoes, R.drawable.ic_feature_rotate,
                R.string.relatorio_observacoes, RelatorioListaActivity.MODO_OBSERVACOES);

        View linhaSinaisVitais = findViewById(R.id.itemRelatorioSinaisVitais);
        ((android.widget.ImageView) linhaSinaisVitais.findViewById(R.id.iconRelatorio)).setImageResource(R.drawable.ic_feature_pulse);
        ((TextView) linhaSinaisVitais.findViewById(R.id.txtLabelRelatorio)).setText(R.string.relatorio_sinais_vitais);
        linhaSinaisVitais.setOnClickListener(v -> selecionarAba(3));

        View linhaDesfecho = findViewById(R.id.itemRelatorioDesfecho);
        ((android.widget.ImageView) linhaDesfecho.findViewById(R.id.iconRelatorio)).setImageResource(R.drawable.ic_patient);
        ((TextView) linhaDesfecho.findViewById(R.id.txtLabelRelatorio)).setText(R.string.relatorio_desfecho);
        linhaDesfecho.setOnClickListener(v -> selecionarAba(4));

        findViewById(R.id.btnGerarPdf).setOnClickListener(v -> gerarPdf());
    }

    private void configurarLinhaRelatorio(int includeId, int iconeRes, int labelRes, @Nullable String modo) {
        View linha = findViewById(includeId);
        ((android.widget.ImageView) linha.findViewById(R.id.iconRelatorio)).setImageResource(iconeRes);
        ((TextView) linha.findViewById(R.id.txtLabelRelatorio)).setText(labelRes);
        linha.setOnClickListener(v -> {
            if (modo == null) {
                abrirGrafico();
                return;
            }
            Intent intent = new Intent(this, RelatorioListaActivity.class);
            intent.putExtra(RelatorioListaActivity.EXTRA_PACIENTE_ID, pacienteId);
            intent.putExtra(RelatorioListaActivity.EXTRA_MODO, modo);
            startActivity(intent);
        });
    }

    private void gerarPdf() {
        if (nomePaciente == null) {
            return;
        }
        Toast.makeText(this, R.string.relatorio_pdf_gerando, Toast.LENGTH_SHORT).show();
        repositorio.carregarPaciente(pacienteId, new FirestorePatientRepository.PacienteCallback() {
            @Override
            public void onCarregado(@Nullable Paciente paciente) {
                if (paciente == null) {
                    Toast.makeText(PacienteDetalhesActivity.this, R.string.relatorio_pdf_erro, Toast.LENGTH_LONG).show();
                    return;
                }
                repositorio.evolucoesDoPaciente(pacienteId).orderBy("criadoEm", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener(evolucaoSnapshot -> {
                            List<String> observacoes = new ArrayList<>();
                            for (com.google.firebase.firestore.DocumentSnapshot doc : evolucaoSnapshot.getDocuments()) {
                                String texto = doc.getString("texto");
                                if (texto != null) {
                                    observacoes.add(texto);
                                }
                            }
                            repositorio.carregarDesfecho(pacienteId, new FirestorePatientRepository.DesfechoCallback() {
                                @Override
                                public void onCarregado(@Nullable DesfechoParto desfecho) {
                                    try {
                                        android.net.Uri uri = new PdfRelatorioGenerator(PacienteDetalhesActivity.this)
                                                .gerar(paciente, registrosAtuais, observacoes, sinaisVitaisAtuais, desfecho);
                                        abrirOuCompartilharPdf(uri);
                                    } catch (Exception e) {
                                        Toast.makeText(PacienteDetalhesActivity.this, R.string.relatorio_pdf_erro, Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onErro(Exception erro) {
                                    Toast.makeText(PacienteDetalhesActivity.this, R.string.relatorio_pdf_erro, Toast.LENGTH_LONG).show();
                                }
                            });
                        })
                        .addOnFailureListener(e -> Toast.makeText(PacienteDetalhesActivity.this, R.string.relatorio_pdf_erro, Toast.LENGTH_LONG).show());
            }

            @Override
            public void onErro(Exception erro) {
                Toast.makeText(PacienteDetalhesActivity.this, R.string.relatorio_pdf_erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void abrirOuCompartilharPdf(android.net.Uri uri) {
        Toast.makeText(this, R.string.relatorio_pdf_sucesso, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.relatorio_gerar_pdf)));
    }

    // ---- Desfecho do parto -------------------------------------------------------------------

    private void configurarDesfecho() {
        containerDesfechoRegistrado = findViewById(R.id.containerDesfechoRegistrado);
        btnViaVaginal = findViewById(R.id.btnViaVaginal);
        btnViaCesarea = findViewById(R.id.btnViaCesarea);
        edtDataParto = findViewById(R.id.edtDataParto);
        edtApresentacaoFetal = findViewById(R.id.edtApresentacaoFetal);
        edtPosicaoVariedade = findViewById(R.id.edtPosicaoVariedade);
        spinnerLaceracaoPerineal = findViewById(R.id.spinnerLaceracaoPerineal);
        spinnerDequitacao = findViewById(R.id.spinnerDequitacao);
        spinnerPlacenta = findViewById(R.id.spinnerPlacenta);
        edtIntercorrenciasParto = findViewById(R.id.edtIntercorrenciasParto);
        edtObservacoesParto = findViewById(R.id.edtObservacoesParto);
        btnSexoMasculino = findViewById(R.id.btnSexoMasculino);
        btnSexoFeminino = findViewById(R.id.btnSexoFeminino);
        edtPesoRn = findViewById(R.id.edtPesoRn);
        edtComprimentoRn = findViewById(R.id.edtComprimentoRn);
        edtPerimetroCefalico = findViewById(R.id.edtPerimetroCefalico);
        edtHorarioNascimento = findViewById(R.id.edtHorarioNascimento);
        edtApgar1 = findViewById(R.id.edtApgar1);
        edtApgar5 = findViewById(R.id.edtApgar5);
        edtApgar10 = findViewById(R.id.edtApgar10);
        btnContatoPeleSim = findViewById(R.id.btnContatoPeleSim);
        btnContatoPeleNao = findViewById(R.id.btnContatoPeleNao);
        btnAmamentacaoSim = findViewById(R.id.btnAmamentacaoSim);
        btnAmamentacaoNao = findViewById(R.id.btnAmamentacaoNao);
        edtIntercorrenciasRn = findViewById(R.id.edtIntercorrenciasRn);
        edtCondicaoMaterna = findViewById(R.id.edtCondicaoMaterna);
        edtCondicaoRn = findViewById(R.id.edtCondicaoRn);
        edtDestinoPuerpera = findViewById(R.id.edtDestinoPuerpera);
        edtDestinoRn = findViewById(R.id.edtDestinoRn);
        edtProfissionalDesfecho = findViewById(R.id.edtProfissionalDesfecho);
        edtCorenDesfecho = findViewById(R.id.edtCorenDesfecho);
        edtObservacoesFinais = findViewById(R.id.edtObservacoesFinais);
        btnSalvarDesfecho = findViewById(R.id.btnSalvarDesfecho);
        progressDesfecho = findViewById(R.id.progressDesfecho);

        configurarSpinnerSimples(spinnerLaceracaoPerineal, R.array.opcoes_laceracao_perineal);
        configurarSpinnerSimples(spinnerDequitacao, R.array.opcoes_dequitacao);
        configurarSpinnerSimples(spinnerPlacenta, R.array.opcoes_placenta);

        btnViaVaginal.setOnClickListener(v -> selecionarVia(getString(R.string.desfecho_via_vaginal)));
        btnViaCesarea.setOnClickListener(v -> selecionarVia(getString(R.string.desfecho_via_cesarea)));
        btnSexoMasculino.setOnClickListener(v -> selecionarSexo(getString(R.string.desfecho_sexo_masculino)));
        btnSexoFeminino.setOnClickListener(v -> selecionarSexo(getString(R.string.desfecho_sexo_feminino)));
        btnContatoPeleSim.setOnClickListener(v -> selecionarContatoPele(true));
        btnContatoPeleNao.setOnClickListener(v -> selecionarContatoPele(false));
        btnAmamentacaoSim.setOnClickListener(v -> selecionarAmamentacao(true));
        btnAmamentacaoNao.setOnClickListener(v -> selecionarAmamentacao(false));

        btnSalvarDesfecho.setOnClickListener(v -> salvarDesfecho());
    }

    private void configurarSpinnerSimples(Spinner spinner, int arrayRes) {
        android.widget.ArrayAdapter<CharSequence> adapter = android.widget.ArrayAdapter.createFromResource(
                this, arrayRes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void selecionarVia(String via) {
        viaDePartoEscolhida = via;
        boolean vaginal = getString(R.string.desfecho_via_vaginal).equals(via);
        aplicarEstiloSegmento((TextView) btnViaVaginal, vaginal);
        aplicarEstiloSegmento((TextView) btnViaCesarea, !vaginal);
    }

    private void selecionarSexo(String sexo) {
        sexoRnEscolhido = sexo;
        boolean masculino = getString(R.string.desfecho_sexo_masculino).equals(sexo);
        aplicarEstiloSegmento((TextView) btnSexoMasculino, masculino);
        aplicarEstiloSegmento((TextView) btnSexoFeminino, !masculino);
    }

    private void selecionarContatoPele(boolean sim) {
        contatoPeleEscolhido = sim;
        aplicarEstiloSegmento((TextView) btnContatoPeleSim, sim);
        aplicarEstiloSegmento((TextView) btnContatoPeleNao, !sim);
    }

    private void selecionarAmamentacao(boolean sim) {
        amamentacaoEscolhida = sim;
        aplicarEstiloSegmento((TextView) btnAmamentacaoSim, sim);
        aplicarEstiloSegmento((TextView) btnAmamentacaoNao, !sim);
    }

    private void aplicarEstiloSegmento(TextView botao, boolean selecionado) {
        botao.setBackgroundResource(selecionado ? R.drawable.bg_segment_selected : R.drawable.bg_segment_unselected);
        botao.setTextColor(ContextCompat.getColor(this, selecionado ? R.color.primaryPink : R.color.textSecondary));
        botao.setTypeface(null, selecionado ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void carregarDesfecho() {
        repositorio.carregarDesfecho(pacienteId, new FirestorePatientRepository.DesfechoCallback() {
            @Override
            public void onCarregado(@Nullable DesfechoParto desfecho) {
                if (desfecho == null) {
                    containerDesfechoRegistrado.setVisibility(View.GONE);
                    return;
                }
                preencherFormularioDesfecho(desfecho);
                preencherResumoDesfecho(desfecho);
            }

            @Override
            public void onErro(Exception erro) {
                // Sem desfecho ainda ou sem conexao -- o formulario continua disponivel para preencher.
            }
        });
    }

    private void preencherResumoDesfecho(DesfechoParto d) {
        containerDesfechoRegistrado.removeAllViews();
        TextView titulo = new TextView(this);
        titulo.setText(R.string.desfecho_registrado);
        titulo.setTextColor(ContextCompat.getColor(this, R.color.primaryPink));
        titulo.setTypeface(null, android.graphics.Typeface.BOLD);
        titulo.setTextSize(13.5f);
        containerDesfechoRegistrado.addView(titulo);

        StringBuilder resumo = new StringBuilder();
        if (!TextUtils.isEmpty(d.getViaDeParto())) resumo.append(d.getViaDeParto());
        if (!TextUtils.isEmpty(d.getSexoRn())) resumo.append(" • RN ").append(d.getSexoRn().toLowerCase(Locale.getDefault()));
        if (d.getPesoRn() != null) resumo.append(" • ").append(d.getPesoRn()).append(" g");
        TextView linha = new TextView(this);
        linha.setText(resumo.toString());
        linha.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
        linha.setTextSize(12.5f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 4;
        containerDesfechoRegistrado.addView(linha, params);
        containerDesfechoRegistrado.setVisibility(View.VISIBLE);
    }

    private void preencherFormularioDesfecho(DesfechoParto d) {
        if (!TextUtils.isEmpty(d.getViaDeParto())) {
            selecionarVia(d.getViaDeParto());
        }
        edtDataParto.setText(d.getDataParto());
        edtApresentacaoFetal.setText(d.getApresentacaoFetal());
        edtPosicaoVariedade.setText(d.getPosicaoVariedade());
        selecionarValorSpinner(spinnerLaceracaoPerineal, d.getLaceracaoPerineal());
        selecionarValorSpinner(spinnerDequitacao, d.getDequitacao());
        selecionarValorSpinner(spinnerPlacenta, d.getPlacenta());
        edtIntercorrenciasParto.setText(d.getIntercorrencias());
        edtObservacoesParto.setText(d.getObservacoes());
        if (!TextUtils.isEmpty(d.getSexoRn())) {
            selecionarSexo(d.getSexoRn());
        }
        if (d.getPesoRn() != null) edtPesoRn.setText(String.valueOf(d.getPesoRn()));
        if (d.getComprimentoRn() != null) edtComprimentoRn.setText(String.valueOf(d.getComprimentoRn()));
        if (d.getPerimetroCefalicoRn() != null) edtPerimetroCefalico.setText(String.valueOf(d.getPerimetroCefalicoRn()));
        edtHorarioNascimento.setText(d.getHorarioNascimento());
        if (d.getApgar1() != null) edtApgar1.setText(String.valueOf(d.getApgar1()));
        if (d.getApgar5() != null) edtApgar5.setText(String.valueOf(d.getApgar5()));
        if (d.getApgar10() != null) edtApgar10.setText(String.valueOf(d.getApgar10()));
        if (d.getContatoPeleAPele() != null) selecionarContatoPele(d.getContatoPeleAPele());
        if (d.getAmamentacaoPrimeiraHora() != null) selecionarAmamentacao(d.getAmamentacaoPrimeiraHora());
        edtIntercorrenciasRn.setText(d.getIntercorrenciasRn());
        edtCondicaoMaterna.setText(d.getCondicaoMaterna());
        edtCondicaoRn.setText(d.getCondicaoRn());
        edtDestinoPuerpera.setText(d.getDestinoPuerpera());
        edtDestinoRn.setText(d.getDestinoRn());
        edtProfissionalDesfecho.setText(d.getProfissionalResponsavel());
        edtCorenDesfecho.setText(d.getCoren());
        edtObservacoesFinais.setText(d.getObservacoesFinais());
    }

    private void selecionarValorSpinner(Spinner spinner, @Nullable String valor) {
        if (valor == null || spinner.getAdapter() == null) {
            return;
        }
        for (int i = 0; i < spinner.getAdapter().getCount(); i++) {
            if (valor.equals(spinner.getAdapter().getItem(i))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void salvarDesfecho() {
        DesfechoParto desfecho = new DesfechoParto();
        desfecho.setViaDeParto(viaDePartoEscolhida);
        desfecho.setDataParto(textoOuNulo(edtDataParto));
        desfecho.setApresentacaoFetal(textoOuNulo(edtApresentacaoFetal));
        desfecho.setPosicaoVariedade(textoOuNulo(edtPosicaoVariedade));
        desfecho.setLaceracaoPerineal((String) spinnerLaceracaoPerineal.getSelectedItem());
        desfecho.setDequitacao((String) spinnerDequitacao.getSelectedItem());
        desfecho.setPlacenta((String) spinnerPlacenta.getSelectedItem());
        desfecho.setIntercorrencias(textoOuNulo(edtIntercorrenciasParto));
        desfecho.setObservacoes(textoOuNulo(edtObservacoesParto));
        desfecho.setSexoRn(sexoRnEscolhido);
        desfecho.setPesoRn(inteiroOuNulo(edtPesoRn));
        desfecho.setComprimentoRn(doubleOuNulo(edtComprimentoRn));
        desfecho.setPerimetroCefalicoRn(doubleOuNulo(edtPerimetroCefalico));
        desfecho.setHorarioNascimento(textoOuNulo(edtHorarioNascimento));
        desfecho.setApgar1(inteiroOuNulo(edtApgar1));
        desfecho.setApgar5(inteiroOuNulo(edtApgar5));
        desfecho.setApgar10(inteiroOuNulo(edtApgar10));
        desfecho.setContatoPeleAPele(contatoPeleEscolhido);
        desfecho.setAmamentacaoPrimeiraHora(amamentacaoEscolhida);
        desfecho.setIntercorrenciasRn(textoOuNulo(edtIntercorrenciasRn));
        desfecho.setCondicaoMaterna(textoOuNulo(edtCondicaoMaterna));
        desfecho.setCondicaoRn(textoOuNulo(edtCondicaoRn));
        desfecho.setDestinoPuerpera(textoOuNulo(edtDestinoPuerpera));
        desfecho.setDestinoRn(textoOuNulo(edtDestinoRn));
        desfecho.setProfissionalResponsavel(textoOuNulo(edtProfissionalDesfecho));
        desfecho.setCoren(textoOuNulo(edtCorenDesfecho));
        desfecho.setObservacoesFinais(textoOuNulo(edtObservacoesFinais));

        progressDesfecho.setVisibility(View.VISIBLE);
        btnSalvarDesfecho.setEnabled(false);
        repositorio.salvarDesfecho(pacienteId, desfecho, new FirestorePatientRepository.OperacaoCallback() {
            @Override
            public void onSucesso() {
                progressDesfecho.setVisibility(View.GONE);
                btnSalvarDesfecho.setEnabled(true);
                Toast.makeText(PacienteDetalhesActivity.this, R.string.desfecho_sucesso, Toast.LENGTH_LONG).show();
                preencherResumoDesfecho(desfecho);
            }

            @Override
            public void onErro(Exception erro) {
                progressDesfecho.setVisibility(View.GONE);
                btnSalvarDesfecho.setEnabled(true);
                Toast.makeText(PacienteDetalhesActivity.this, R.string.desfecho_erro_salvar, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String textoOuNulo(android.widget.EditText campo) {
        String texto = campo.getText().toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private Integer inteiroOuNulo(android.widget.EditText campo) {
        try {
            String texto = campo.getText().toString().trim();
            return texto.isEmpty() ? null : Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double doubleOuNulo(android.widget.EditText campo) {
        try {
            String texto = campo.getText().toString().trim().replace(",", ".");
            return texto.isEmpty() ? null : Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        if (listenerRegistros != null) {
            listenerRegistros.remove();
        }
        if (listenerSinaisVitais != null) {
            listenerSinaisVitais.remove();
        }
        super.onDestroy();
    }
}
