package com.jninfo.partograma.partograma;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jninfo.partograma.partograma.data.FirestorePatientRepository;
import com.jninfo.partograma.partograma.data.SinalVital;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Formulario de nova aferição de sinais vitais (aba "Sinais vitais" -> "+ Nova aferição").
 * Independente do Partograma -- o profissional pode registrar a qualquer momento.
 */
public class NovaAfericaoActivity extends BaseActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";
    public static final String EXTRA_NOME_PACIENTE = "extra_nome_paciente";

    private FirestorePatientRepository repositorio;
    private String pacienteId;

    private EditText edtDataAfericao;
    private EditText edtHorarioAfericao;
    private EditText edtPaSistolica;
    private EditText edtPaDiastolica;
    private EditText edtFrequenciaCardiaca;
    private EditText edtFrequenciaRespiratoria;
    private EditText edtTemperatura;
    private EditText edtSpo2;
    private EditText edtDor;
    private EditText edtHgt;
    private EditText edtObservacoesAfericao;
    private EditText edtExaminadorAfericao;
    private View btnSalvar;
    private ProgressBar progressAfericao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_afericao);

        pacienteId = getIntent().getStringExtra(EXTRA_PACIENTE_ID);
        String nomePaciente = getIntent().getStringExtra(EXTRA_NOME_PACIENTE);
        repositorio = new FirestorePatientRepository();

        if (nomePaciente != null) {
            ((TextView) findViewById(R.id.txtNomePacienteAfericao)).setText(
                    getString(R.string.sinais_vitais_nova_afericao) + " — " + nomePaciente);
        }

        edtDataAfericao = findViewById(R.id.edtDataAfericao);
        edtHorarioAfericao = findViewById(R.id.edtHorarioAfericao);
        edtPaSistolica = findViewById(R.id.edtPaSistolica);
        edtPaDiastolica = findViewById(R.id.edtPaDiastolica);
        edtFrequenciaCardiaca = findViewById(R.id.edtFrequenciaCardiaca);
        edtFrequenciaRespiratoria = findViewById(R.id.edtFrequenciaRespiratoria);
        edtTemperatura = findViewById(R.id.edtTemperatura);
        edtSpo2 = findViewById(R.id.edtSpo2);
        edtDor = findViewById(R.id.edtDor);
        edtHgt = findViewById(R.id.edtHgt);
        edtObservacoesAfericao = findViewById(R.id.edtObservacoesAfericao);
        edtExaminadorAfericao = findViewById(R.id.edtExaminadorAfericao);
        btnSalvar = findViewById(R.id.btnSalvarAfericao);
        progressAfericao = findViewById(R.id.progressAfericao);

        Date agora = new Date();
        edtDataAfericao.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(agora));
        edtHorarioAfericao.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(agora));

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancelarAfericao).setOnClickListener(v -> finish());
        btnSalvar.setOnClickListener(v -> validarESalvar());
    }

    private void validarESalvar() {
        String data = edtDataAfericao.getText().toString().trim();
        String horario = edtHorarioAfericao.getText().toString().trim();
        if (TextUtils.isEmpty(data) || TextUtils.isEmpty(horario)) {
            Toast.makeText(this, R.string.sinais_vitais_erro_obrigatorio, Toast.LENGTH_LONG).show();
            return;
        }

        SinalVital sinal = new SinalVital();
        sinal.setDataAfericao(data);
        sinal.setHorario(horario);
        sinal.setPaSistolica(inteiro(edtPaSistolica));
        sinal.setPaDiastolica(inteiro(edtPaDiastolica));
        sinal.setFrequenciaCardiaca(inteiro(edtFrequenciaCardiaca));
        sinal.setFrequenciaRespiratoria(inteiro(edtFrequenciaRespiratoria));
        sinal.setTemperatura(decimal(edtTemperatura));
        sinal.setSpo2(inteiro(edtSpo2));
        sinal.setDor(inteiro(edtDor));
        sinal.setHgt(decimal(edtHgt));
        sinal.setObservacoes(vazioParaNulo(edtObservacoesAfericao));
        sinal.setExaminador(vazioParaNulo(edtExaminadorAfericao));

        alternarCarregando(true);
        repositorio.adicionarSinalVital(pacienteId, sinal, new FirestorePatientRepository.OperacaoCallback() {
            @Override
            public void onSucesso() {
                finish();
            }

            @Override
            public void onErro(Exception erro) {
                alternarCarregando(false);
                Toast.makeText(NovaAfericaoActivity.this, R.string.sinais_vitais_erro_salvar, Toast.LENGTH_LONG).show();
            }
        });
    }

    private Integer inteiro(EditText campo) {
        try {
            String texto = campo.getText().toString().trim();
            return texto.isEmpty() ? null : Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double decimal(EditText campo) {
        try {
            String texto = campo.getText().toString().trim().replace(",", ".");
            return texto.isEmpty() ? null : Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String vazioParaNulo(EditText campo) {
        String texto = campo.getText().toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private void alternarCarregando(boolean carregando) {
        progressAfericao.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnSalvar.setEnabled(!carregando);
        btnSalvar.setAlpha(carregando ? 0.6f : 1f);
    }
}
