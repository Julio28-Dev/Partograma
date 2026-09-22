package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.ListenerRegistration;
import com.jninfo.partograma.partograma.data.AppPreferences;
import com.jninfo.partograma.partograma.data.FirestorePatientRepository;
import com.jninfo.partograma.partograma.data.Paciente;
import com.jninfo.partograma.partograma.data.RegistroPartograma;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela de detalhes da paciente, com as 4 abas do novo design (Dados/Partograma/
 * Relatório/Configurações). Dados, Partograma e Relatório sao alimentados em tempo real
 * pelo Firestore; Configuracoes e local (SharedPreferences via {@link AppPreferences}).
 *
 * O fluxo antigo (Menu/Detalhes/relatorio, baseado em SharedPreferences por slot)
 * continua existindo e funcionando sem nenhuma alteracao -- esta tela e a nova area de
 * partograma, com seu proprio esquema de dados (ver {@link RegistroPartograma}).
 */
public class PacienteDetalhesActivity extends BaseActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";

    private FirestorePatientRepository repositorio;
    private AppPreferences preferencias;
    private String pacienteId;
    private String nomePaciente;
    private ListenerRegistration listenerRegistros;
    private List<RegistroPartograma> registrosAtuais = new ArrayList<>();

    private TextView txtNomeCabecalho;
    private TextView txtSubtituloCabecalho;
    private LinearLayout containerDadosPaciente;
    private LinearLayout containerEvolucao;
    private ProgressBar progressDetalhes;

    private View conteudoDados;
    private View conteudoPartograma;
    private View conteudoRelatorio;
    private View conteudoConfiguracoes;

    private View[] abas;
    private View[] conteudos;

    // Partograma
    private View indicadorDilatacao;
    private View indicadorRotatividade;
    private TextView txtHoraUltimaAvaliacao;
    private LinearLayout containerHistorico;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paciente_detalhes);

        pacienteId = getIntent().getStringExtra(EXTRA_PACIENTE_ID);
        repositorio = new FirestorePatientRepository();
        preferencias = new AppPreferences(this);

        txtNomeCabecalho = findViewById(R.id.txtNomeCabecalho);
        txtSubtituloCabecalho = findViewById(R.id.txtSubtituloCabecalho);
        containerDadosPaciente = findViewById(R.id.containerDadosPaciente);
        containerEvolucao = findViewById(R.id.containerEvolucao);
        progressDetalhes = findViewById(R.id.progressDetalhes);

        conteudoDados = findViewById(R.id.conteudoDados);
        conteudoPartograma = findViewById(R.id.conteudoPartograma);
        conteudoRelatorio = findViewById(R.id.conteudoRelatorio);
        conteudoConfiguracoes = findViewById(R.id.conteudoConfiguracoes);
        conteudos = new View[]{conteudoDados, conteudoPartograma, conteudoRelatorio, conteudoConfiguracoes};

        indicadorDilatacao = findViewById(R.id.indicadorDilatacao);
        indicadorRotatividade = findViewById(R.id.indicadorRotatividade);
        txtHoraUltimaAvaliacao = findViewById(R.id.txtHoraUltimaAvaliacao);
        containerHistorico = findViewById(R.id.containerHistorico);

        View tabDados = findViewById(R.id.tabDados);
        View tabPartograma = findViewById(R.id.tabPartograma);
        View tabRelatorio = findViewById(R.id.tabRelatorio);
        View tabConfiguracoes = findViewById(R.id.tabConfiguracoes);
        abas = new View[]{tabDados, tabPartograma, tabRelatorio, tabConfiguracoes};

        configurarAba(tabDados, R.string.paciente_detalhes_aba_dados);
        configurarAba(tabPartograma, R.string.paciente_detalhes_aba_partograma);
        configurarAba(tabRelatorio, R.string.paciente_detalhes_aba_relatorio);
        configurarAba(tabConfiguracoes, R.string.paciente_detalhes_aba_configuracoes);

        for (int i = 0; i < abas.length; i++) {
            int indice = i;
            abas[i].setOnClickListener(v -> selecionarAba(indice));
        }
        selecionarAba(0);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnMenuPaciente).setOnClickListener(this::abrirMenuPaciente);

        configurarPartograma();
        configurarRelatorio();
        configurarConfiguracoes();

        if (pacienteId == null) {
            Toast.makeText(this, R.string.paciente_detalhes_erro_carregar, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        carregarPaciente();
        observarRegistros();
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
                registros.isEmpty() || registros.get(0).getPosicaoLee() == null ? getString(R.string.partograma_sem_dados)
                        : registros.get(0).getRotuloPosicaoLee(),
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
            txtHora.setText(registro.getHorario() != null ? registro.getHorario() : "-");
            txtDilatacao.setText("Dilatação: " + (registro.getDilatacao() != null ? formatarDilatacao(registro.getDilatacao()) : "-"));
            txtRotatividade.setText("Rotatividade: " + registro.getRotuloPosicaoLee());
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
            return String.format(java.util.Locale.getDefault(), "%d cm", dilatacao.intValue());
        }
        return String.format(java.util.Locale.getDefault(), "%.1f cm", dilatacao);
    }

    private void abrirGrafico() {
        Intent intent = new Intent(this, GraficoPartogramaActivity.class);
        intent.putExtra(GraficoPartogramaActivity.EXTRA_PACIENTE_ID, pacienteId);
        intent.putExtra(GraficoPartogramaActivity.EXTRA_NOME_PACIENTE, nomePaciente);
        startActivity(intent);
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
                            try {
                                android.net.Uri uri = new PdfRelatorioGenerator(PacienteDetalhesActivity.this)
                                        .gerar(paciente, registrosAtuais, observacoes);
                                abrirOuCompartilharPdf(uri);
                            } catch (Exception e) {
                                Toast.makeText(PacienteDetalhesActivity.this, R.string.relatorio_pdf_erro, Toast.LENGTH_LONG).show();
                            }
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

    // ---- Configuracoes ---------------------------------------------------------------------

    private void configurarConfiguracoes() {
        View btnClaro = findViewById(R.id.btnTemaClaro);
        View btnEscuro = findViewById(R.id.btnTemaEscuro);
        View btnAutomatico = findViewById(R.id.btnTemaAutomatico);
        View[] botoesTema = {btnClaro, btnEscuro, btnAutomatico};
        String[] valoresTema = {AppPreferences.TEMA_CLARO, AppPreferences.TEMA_ESCURO, AppPreferences.TEMA_AUTOMATICO};
        aplicarSelecaoSegmento(botoesTema, indiceTema(preferencias.getTema()));
        for (int i = 0; i < botoesTema.length; i++) {
            int indice = i;
            botoesTema[i].setOnClickListener(v -> {
                preferencias.setTema(valoresTema[indice]);
                aplicarSelecaoSegmento(botoesTema, indice);
                Toast.makeText(this, R.string.config_reiniciar_aviso, Toast.LENGTH_LONG).show();
            });
        }

        View btnMenor = findViewById(R.id.btnFonteMenor);
        View btnPadrao = findViewById(R.id.btnFontePadrao);
        View btnMaior = findViewById(R.id.btnFonteMaior);
        View[] botoesFonte = {btnMenor, btnPadrao, btnMaior};
        float[] valoresFonte = {AppPreferences.ESCALA_FONTE_MENOR, AppPreferences.ESCALA_FONTE_PADRAO, AppPreferences.ESCALA_FONTE_MAIOR};
        aplicarSelecaoSegmento(botoesFonte, indiceFonte(preferencias.getEscalaFonte()));
        for (int i = 0; i < botoesFonte.length; i++) {
            int indice = i;
            botoesFonte[i].setOnClickListener(v -> {
                preferencias.setEscalaFonte(valoresFonte[indice]);
                aplicarSelecaoSegmento(botoesFonte, indice);
                recreate();
            });
        }

        Switch switchLembretes = findViewById(R.id.switchNotifLembretes);
        Switch switchDilatacao = findViewById(R.id.switchNotifDilatacao);
        Switch switchSistema = findViewById(R.id.switchNotifSistema);
        switchLembretes.setChecked(preferencias.isNotifLembretesAtivo());
        switchDilatacao.setChecked(preferencias.isNotifDilatacaoAtivo());
        switchSistema.setChecked(preferencias.isNotifSistemaAtivo());
        switchLembretes.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> preferencias.setNotifLembretesAtivo(checked));
        switchDilatacao.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> preferencias.setNotifDilatacaoAtivo(checked));
        switchSistema.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> preferencias.setNotifSistemaAtivo(checked));

        findViewById(R.id.btnLimparCache).setOnClickListener(v -> limparCache());
        findViewById(R.id.btnSobreApp).setOnClickListener(v -> mostrarSobre());
        findViewById(R.id.btnAjudaSuporte).setOnClickListener(v -> startActivity(new Intent(this, TelaInicial.class)));
    }

    private int indiceTema(String tema) {
        if (AppPreferences.TEMA_ESCURO.equals(tema)) return 1;
        if (AppPreferences.TEMA_AUTOMATICO.equals(tema)) return 2;
        return 0;
    }

    private int indiceFonte(float escala) {
        if (escala == AppPreferences.ESCALA_FONTE_MENOR) return 0;
        if (escala == AppPreferences.ESCALA_FONTE_MAIOR) return 2;
        return 1;
    }

    private void aplicarSelecaoSegmento(View[] botoes, int indiceSelecionado) {
        for (int i = 0; i < botoes.length; i++) {
            TextView texto = (TextView) botoes[i];
            boolean selecionado = i == indiceSelecionado;
            texto.setBackgroundResource(selecionado ? R.drawable.bg_segment_selected : R.drawable.bg_segment_unselected);
            texto.setTextColor(ContextCompat.getColor(this, selecionado ? R.color.primaryPink : R.color.textSecondary));
            texto.setTypeface(null, selecionado ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private void limparCache() {
        try {
            deleteCacheDir(getCacheDir());
            Toast.makeText(this, R.string.config_limpar_cache_sucesso, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.config_limpar_cache_sucesso, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteCacheDir(java.io.File dir) {
        java.io.File[] arquivos = dir.listFiles();
        if (arquivos == null) {
            return;
        }
        for (java.io.File arquivo : arquivos) {
            if (arquivo.isDirectory()) {
                deleteCacheDir(arquivo);
            } else {
                arquivo.delete();
            }
        }
    }

    private void mostrarSobre() {
        String versao;
        try {
            versao = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            versao = "1.0";
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage("Partograma Digital\nVersão " + versao + "\n\nO apoio que o enfermeiro precisa para um parto seguro e humanizado.")
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    @Override
    protected void onDestroy() {
        if (listenerRegistros != null) {
            listenerRegistros.remove();
        }
        super.onDestroy();
    }
}
