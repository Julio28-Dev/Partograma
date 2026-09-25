package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.jninfo.partograma.partograma.data.FirestorePatientRepository;
import com.jninfo.partograma.partograma.data.RegistroPartograma;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formulario de uma nova avaliacao/registro do partograma (aba Partograma -> botao "+").
 * Campos e nomenclatura alinhados com o briefing do cliente: Posição do bebê (situacao
 * fetal) e Plano de De Lee (estacao) sao dois campos distintos -- ver
 * {@link RegistroPartograma} para o porque dessa separacao.
 */
public class NovaAvaliacaoActivity extends BaseActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";

    private static final String OPCAO_ROTA = "Rota";
    private static final String OPCAO_INTERCORRENCIA_SIM = "sim";
    private static final String OPCAO_INTERCORRENCIA_NAO = "nao";

    private FirestorePatientRepository repositorio;
    private String pacienteId;

    private EditText edtHorario;
    private Spinner spinnerFaseTrabalhoParto;
    private EditText edtDilatacao;
    private Spinner spinnerPosicaoBebe;
    private Spinner spinnerPlanoDeLee;
    private EditText edtBatimentos;
    private Spinner spinnerIntegridade;
    private View containerLiquido;
    private Spinner spinnerLiquido;
    private EditText edtFreqContracao;
    private EditText edtOcitocina;
    private EditText edtMesoprostol;
    private EditText edtRemedios;
    private EditText edtExaminador;
    private LinearLayout containerMetodos;
    private View btnIntercorrenciaSim;
    private View btnIntercorrenciaNao;
    private View containerDescricaoIntercorrencia;
    private EditText edtIntercorrencia;
    private EditText edtObservacaoAdicional;
    private View btnSalvar;
    private ProgressBar progressSalvando;

    private final List<CheckBox> checkboxesMetodos = new ArrayList<>();
    private String intercorrenciaEscolhida = OPCAO_INTERCORRENCIA_NAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_avaliacao);

        pacienteId = getIntent().getStringExtra(EXTRA_PACIENTE_ID);
        repositorio = new FirestorePatientRepository();

        edtHorario = findViewById(R.id.edtHorario);
        spinnerFaseTrabalhoParto = findViewById(R.id.spinnerFaseTrabalhoParto);
        edtDilatacao = findViewById(R.id.edtDilatacao);
        spinnerPosicaoBebe = findViewById(R.id.spinnerPosicaoBebe);
        spinnerPlanoDeLee = findViewById(R.id.spinnerPlanoDeLee);
        edtBatimentos = findViewById(R.id.edtBatimentos);
        spinnerIntegridade = findViewById(R.id.spinnerIntegridade);
        containerLiquido = findViewById(R.id.containerLiquido);
        spinnerLiquido = findViewById(R.id.spinnerLiquido);
        edtFreqContracao = findViewById(R.id.edtFreqContracao);
        edtOcitocina = findViewById(R.id.edtOcitocina);
        edtMesoprostol = findViewById(R.id.edtMesoprostol);
        edtRemedios = findViewById(R.id.edtRemedios);
        edtExaminador = findViewById(R.id.edtExaminador);
        containerMetodos = findViewById(R.id.containerMetodosNaoFarmacologicos);
        btnIntercorrenciaSim = findViewById(R.id.btnIntercorrenciaSim);
        btnIntercorrenciaNao = findViewById(R.id.btnIntercorrenciaNao);
        containerDescricaoIntercorrencia = findViewById(R.id.containerDescricaoIntercorrencia);
        edtIntercorrencia = findViewById(R.id.edtIntercorrencia);
        edtObservacaoAdicional = findViewById(R.id.edtObservacaoAdicional);
        btnSalvar = findViewById(R.id.btnSalvarAvaliacao);
        progressSalvando = findViewById(R.id.progressSalvando);

        edtHorario.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date()));

        configurarSpinner(spinnerFaseTrabalhoParto, R.array.opcoes_fase_trabalho_parto);
        configurarSpinner(spinnerPosicaoBebe, R.array.opcoes_posicao_bebe);
        configurarSpinner(spinnerIntegridade, R.array.opcoes_integridade_bolsa);
        configurarSpinner(spinnerLiquido, R.array.opcoes_liquido_amniotico);
        configurarSpinnerPlanoDeLee();
        montarCheckboxesMetodos();

        spinnerIntegridade.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                boolean bolsaRota = OPCAO_ROTA.equals(spinnerIntegridade.getSelectedItem());
                containerLiquido.setVisibility(bolsaRota ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                containerLiquido.setVisibility(View.GONE);
            }
        });

        btnIntercorrenciaNao.setOnClickListener(v -> selecionarIntercorrencia(OPCAO_INTERCORRENCIA_NAO));
        btnIntercorrenciaSim.setOnClickListener(v -> selecionarIntercorrencia(OPCAO_INTERCORRENCIA_SIM));

        findViewById(R.id.btnSaibaMaisDeLee).setOnClickListener(v -> {
            Intent intent = new Intent(this, PlanoDeLeeExplicacaoActivity.class);
            int indiceSelecionado = spinnerPlanoDeLee.getSelectedItemPosition();
            intent.putExtra(PlanoDeLeeExplicacaoActivity.EXTRA_VALOR_SELECIONADO, indiceSelecionado - 5);
            startActivity(intent);
        });

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        btnSalvar.setOnClickListener(v -> validarESalvar());
    }

    private void configurarSpinner(Spinner spinner, int arrayRes) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayRes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void configurarSpinnerPlanoDeLee() {
        List<String> valores = new ArrayList<>();
        for (int v = -5; v <= 5; v++) {
            valores.add(v > 0 ? "+" + v : String.valueOf(v));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, valores);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlanoDeLee.setAdapter(adapter);
        spinnerPlanoDeLee.setSelection(5); // "0", referencia das espinhas isquiaticas
    }

    private void montarCheckboxesMetodos() {
        String[] opcoes = getResources().getStringArray(R.array.opcoes_metodos_nao_farmacologicos);
        for (String opcao : opcoes) {
            CheckBox checkbox = (CheckBox) LayoutInflater.from(this)
                    .inflate(R.layout.item_checkbox_opcao, containerMetodos, false);
            checkbox.setText(opcao);
            containerMetodos.addView(checkbox);
            checkboxesMetodos.add(checkbox);
        }
    }

    private void selecionarIntercorrencia(String escolha) {
        intercorrenciaEscolhida = escolha;
        boolean sim = OPCAO_INTERCORRENCIA_SIM.equals(escolha);
        aplicarEstiloSegmento((TextView) btnIntercorrenciaSim, sim);
        aplicarEstiloSegmento((TextView) btnIntercorrenciaNao, !sim);
        containerDescricaoIntercorrencia.setVisibility(sim ? View.VISIBLE : View.GONE);
    }

    private void aplicarEstiloSegmento(TextView botao, boolean selecionado) {
        botao.setBackgroundResource(selecionado ? R.drawable.bg_segment_selected : R.drawable.bg_segment_unselected);
        botao.setTextColor(ContextCompat.getColor(this, selecionado ? R.color.primaryPink : R.color.textSecondary));
        botao.setTypeface(null, selecionado ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void validarESalvar() {
        String horario = edtHorario.getText().toString().trim();
        String dilatacaoTexto = edtDilatacao.getText().toString().trim();

        if (TextUtils.isEmpty(horario) || TextUtils.isEmpty(dilatacaoTexto)) {
            Toast.makeText(this, R.string.nova_avaliacao_erro_obrigatorio, Toast.LENGTH_LONG).show();
            return;
        }
        double dilatacao;
        try {
            dilatacao = Double.parseDouble(dilatacaoTexto.replace(",", "."));
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.nova_avaliacao_erro_obrigatorio, Toast.LENGTH_LONG).show();
            return;
        }

        RegistroPartograma registro = new RegistroPartograma();
        registro.setHorario(horario);
        registro.setDilatacao(dilatacao);
        registro.setFaseTrabalhoParto((String) spinnerFaseTrabalhoParto.getSelectedItem());
        registro.setPosicaoBebe((String) spinnerPosicaoBebe.getSelectedItem());
        registro.setPlanoDeLee(spinnerPlanoDeLee.getSelectedItemPosition() - 5);
        registro.setIntegridade((String) spinnerIntegridade.getSelectedItem());
        if (containerLiquido.getVisibility() == View.VISIBLE) {
            registro.setLiquido((String) spinnerLiquido.getSelectedItem());
        }

        String batimentosTexto = edtBatimentos.getText().toString().trim();
        if (!batimentosTexto.isEmpty()) {
            try {
                registro.setBatimentos(Integer.parseInt(batimentosTexto));
            } catch (NumberFormatException ignored) {
                // Campo opcional -- se vier num formato invalido, simplesmente nao grava o valor.
            }
        }
        registro.setFreqContracao(vazioParaNulo(edtFreqContracao));
        registro.setOcitocina(vazioParaNulo(edtOcitocina));
        registro.setMesoprostol(vazioParaNulo(edtMesoprostol));
        registro.setRemedios(vazioParaNulo(edtRemedios));
        registro.setExaminador(vazioParaNulo(edtExaminador));

        List<String> metodosSelecionados = new ArrayList<>();
        for (CheckBox checkbox : checkboxesMetodos) {
            if (checkbox.isChecked()) {
                metodosSelecionados.add(checkbox.getText().toString());
            }
        }
        registro.setMetodosNaoFarmacologicos(metodosSelecionados);

        boolean teveIntercorrencia = OPCAO_INTERCORRENCIA_SIM.equals(intercorrenciaEscolhida);
        registro.setTeveIntercorrencia(teveIntercorrencia);
        registro.setIntercorrencia(teveIntercorrencia ? vazioParaNulo(edtIntercorrencia) : null);
        registro.setObservacaoAdicional(vazioParaNulo(edtObservacaoAdicional));

        alternarCarregando(true);
        repositorio.adicionarRegistro(pacienteId, registro, new FirestorePatientRepository.OperacaoCallback() {
            @Override
            public void onSucesso() {
                Intent intent = new Intent(NovaAvaliacaoActivity.this, TelaFinalActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onErro(Exception erro) {
                alternarCarregando(false);
                Toast.makeText(NovaAvaliacaoActivity.this, R.string.nova_avaliacao_erro_salvar, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String vazioParaNulo(EditText campo) {
        String texto = campo.getText().toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private void alternarCarregando(boolean carregando) {
        progressSalvando.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnSalvar.setEnabled(!carregando);
        btnSalvar.setAlpha(carregando ? 0.6f : 1f);
    }
}
