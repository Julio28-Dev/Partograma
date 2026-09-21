package com.jninfo.partograma.partograma;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.jninfo.partograma.partograma.data.FirestorePatientRepository;
import com.jninfo.partograma.partograma.data.Paciente;

/**
 * Tela de detalhes da paciente, com as 4 abas do novo design (Dados/Partograma/
 * Relatório/Configurações). Os dados carregados na aba "Dados" sao reais, vindos do
 * Firestore. As demais abas ainda mostram um aviso "em desenvolvimento": a integracao
 * das funcionalidades originais de partograma/relatorio a esta tela e a proxima etapa
 * do projeto (a logica do {@link Detalhes} e do {@link relatorio} originais nao foi
 * alterada nem duplicada aqui).
 */
public class PacienteDetalhesActivity extends AppCompatActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";

    private FirestorePatientRepository repositorio;
    private String pacienteId;

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
        conteudoConfiguracoes = findViewById(R.id.conteudoConfiguracoes);
        conteudos = new View[]{conteudoDados, conteudoPartograma, conteudoRelatorio, conteudoConfiguracoes};

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

        if (pacienteId == null) {
            Toast.makeText(this, R.string.paciente_detalhes_erro_carregar, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        carregarPaciente();
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
}
