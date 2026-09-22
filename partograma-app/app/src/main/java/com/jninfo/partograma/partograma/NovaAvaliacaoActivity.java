package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.jninfo.partograma.partograma.data.FirestorePatientRepository;
import com.jninfo.partograma.partograma.data.RegistroPartograma;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Formulario de uma nova avaliacao/registro do partograma (aba Partograma -> botao "+").
 * Campos equivalentes aos que o {@link Detalhes} original coletava por hora, so que
 * persistidos no Firestore ({@link FirestorePatientRepository#adicionarRegistro}) em vez
 * de SharedPreferences por slot.
 */
public class NovaAvaliacaoActivity extends BaseActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";

    private FirestorePatientRepository repositorio;
    private String pacienteId;

    private EditText edtHorario;
    private EditText edtDilatacao;
    private Spinner spinnerPosicaoLee;
    private EditText edtBatimentos;
    private Spinner spinnerIntegridade;
    private Spinner spinnerLiquido;
    private EditText edtFreqContracao;
    private EditText edtOcitocina;
    private EditText edtMesoprostol;
    private EditText edtRemedios;
    private EditText edtExaminador;
    private EditText edtIntercorrencia;
    private View btnSalvar;
    private ProgressBar progressSalvando;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_avaliacao);

        pacienteId = getIntent().getStringExtra(EXTRA_PACIENTE_ID);
        repositorio = new FirestorePatientRepository();

        edtHorario = findViewById(R.id.edtHorario);
        edtDilatacao = findViewById(R.id.edtDilatacao);
        spinnerPosicaoLee = findViewById(R.id.spinnerPosicaoLee);
        edtBatimentos = findViewById(R.id.edtBatimentos);
        spinnerIntegridade = findViewById(R.id.spinnerIntegridade);
        spinnerLiquido = findViewById(R.id.spinnerLiquido);
        edtFreqContracao = findViewById(R.id.edtFreqContracao);
        edtOcitocina = findViewById(R.id.edtOcitocina);
        edtMesoprostol = findViewById(R.id.edtMesoprostol);
        edtRemedios = findViewById(R.id.edtRemedios);
        edtExaminador = findViewById(R.id.edtExaminador);
        edtIntercorrencia = findViewById(R.id.edtIntercorrencia);
        btnSalvar = findViewById(R.id.btnSalvarAvaliacao);
        progressSalvando = findViewById(R.id.progressSalvando);

        edtHorario.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date()));

        configurarSpinner(spinnerPosicaoLee, R.array.opcoes_posicao_lee);
        configurarSpinner(spinnerIntegridade, R.array.opcoes_integridade_bolsa);
        configurarSpinner(spinnerLiquido, R.array.opcoes_liquido_amniotico);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        btnSalvar.setOnClickListener(v -> validarESalvar());
    }

    private void configurarSpinner(Spinner spinner, int arrayRes) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayRes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
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
        registro.setPosicaoLee(spinnerPosicaoLee.getSelectedItemPosition() + 1);
        registro.setIntegridade((String) spinnerIntegridade.getSelectedItem());
        registro.setLiquido((String) spinnerLiquido.getSelectedItem());

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
        registro.setIntercorrencia(vazioParaNulo(edtIntercorrencia));

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
