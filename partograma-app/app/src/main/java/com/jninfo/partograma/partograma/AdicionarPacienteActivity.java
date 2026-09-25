package com.jninfo.partograma.partograma;

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
import android.widget.Toast;

import com.jninfo.partograma.partograma.data.FirestorePatientRepository;
import com.jninfo.partograma.partograma.data.Paciente;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formulario real de cadastro de paciente (tela "Pacientes" -> "Adicionar nova paciente").
 * Valida, salva no Firestore e volta para a lista, que se atualiza sozinha via listener
 * em tempo real ({@link PacientesActivity#observarPacientes()}).
 */
public class AdicionarPacienteActivity extends BaseActivity {

    private FirestorePatientRepository repositorio;

    private EditText edtNome;
    private EditText edtProntuario;
    private EditText edtDataNascimento;
    private EditText edtDum;
    private Spinner spinnerTipoSanguineo;
    private EditText edtAlergias;
    private LinearLayout containerComorbidades;
    private EditText edtComorbidadesOutras;
    private Spinner spinnerClassificacaoRisco;
    private EditText edtProfissional;
    private EditText edtQueixa;
    private EditText edtConduta;
    private View btnSalvar;
    private ProgressBar progressSalvando;
    private final List<CheckBox> checkboxesComorbidades = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adicionar_paciente);

        repositorio = new FirestorePatientRepository();

        edtNome = findViewById(R.id.edtNome);
        edtProntuario = findViewById(R.id.edtProntuario);
        edtDataNascimento = findViewById(R.id.edtDataNascimento);
        edtDum = findViewById(R.id.edtDum);
        spinnerTipoSanguineo = findViewById(R.id.spinnerTipoSanguineo);
        edtAlergias = findViewById(R.id.edtAlergias);
        containerComorbidades = findViewById(R.id.containerComorbidades);
        edtComorbidadesOutras = findViewById(R.id.edtComorbidadesOutras);
        spinnerClassificacaoRisco = findViewById(R.id.spinnerClassificacaoRisco);
        edtProfissional = findViewById(R.id.edtProfissional);
        edtQueixa = findViewById(R.id.edtQueixa);
        edtConduta = findViewById(R.id.edtConduta);
        btnSalvar = findViewById(R.id.btnSalvar);
        progressSalvando = findViewById(R.id.progressSalvando);

        ArrayAdapter<CharSequence> adapterTipos = ArrayAdapter.createFromResource(
                this, R.array.tipos_sanguineos, android.R.layout.simple_spinner_item);
        adapterTipos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipoSanguineo.setAdapter(adapterTipos);

        ArrayAdapter<CharSequence> adapterRisco = ArrayAdapter.createFromResource(
                this, R.array.opcoes_classificacao_risco, android.R.layout.simple_spinner_item);
        adapterRisco.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClassificacaoRisco.setAdapter(adapterRisco);

        montarCheckboxesComorbidades();

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancelar).setOnClickListener(v -> finish());
        btnSalvar.setOnClickListener(v -> validarESalvar());
    }

    private void montarCheckboxesComorbidades() {
        String[] opcoes = getResources().getStringArray(R.array.opcoes_comorbidades);
        String opcaoOutras = opcoes[opcoes.length - 1];
        for (String opcao : opcoes) {
            CheckBox checkbox = (CheckBox) LayoutInflater.from(this)
                    .inflate(R.layout.item_checkbox_opcao, containerComorbidades, false);
            checkbox.setText(opcao);
            if (opcao.equals(opcaoOutras)) {
                checkbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                        edtComorbidadesOutras.setVisibility(isChecked ? View.VISIBLE : View.GONE));
            }
            containerComorbidades.addView(checkbox);
            checkboxesComorbidades.add(checkbox);
        }
    }

    private void validarESalvar() {
        String nome = edtNome.getText().toString().trim();
        String prontuario = edtProntuario.getText().toString().trim();
        String dataNascimento = edtDataNascimento.getText().toString().trim();
        String dum = edtDum.getText().toString().trim();

        if (TextUtils.isEmpty(nome) || TextUtils.isEmpty(prontuario) || TextUtils.isEmpty(dataNascimento)) {
            Toast.makeText(this, R.string.adicionar_paciente_erro_obrigatorio, Toast.LENGTH_LONG).show();
            return;
        }
        if (!dataValida(dataNascimento) || (!dum.isEmpty() && !dataValida(dum))) {
            Toast.makeText(this, R.string.adicionar_paciente_erro_data, Toast.LENGTH_LONG).show();
            return;
        }

        Paciente paciente = new Paciente();
        paciente.setNome(nome);
        paciente.setProntuario(prontuario);
        paciente.setDataNascimento(dataNascimento);
        paciente.setDum(dum.isEmpty() ? null : dum);
        paciente.setTipoSanguineo((String) spinnerTipoSanguineo.getSelectedItem());
        String alergias = edtAlergias.getText().toString().trim();
        paciente.setAlergias(alergias.isEmpty() ? "Nega" : alergias);

        List<String> comorbidadesSelecionadas = new ArrayList<>();
        for (CheckBox checkbox : checkboxesComorbidades) {
            if (checkbox.isChecked()) {
                comorbidadesSelecionadas.add(checkbox.getText().toString());
            }
        }
        paciente.setComorbidades(comorbidadesSelecionadas);
        paciente.setComorbidadesOutras(edtComorbidadesOutras.getVisibility() == View.VISIBLE
                ? edtComorbidadesOutras.getText().toString().trim() : null);
        paciente.setClassificacaoRisco((String) spinnerClassificacaoRisco.getSelectedItem());

        paciente.setProfissionalResponsavel(edtProfissional.getText().toString().trim());
        paciente.setQueixaPrincipal(edtQueixa.getText().toString().trim());
        paciente.setConduta(edtConduta.getText().toString().trim());
        paciente.setStatus(getString(R.string.pacientes_status_avaliacao));

        alternarCarregando(true);
        repositorio.adicionarPaciente(paciente, new FirestorePatientRepository.OperacaoCallback() {
            @Override
            public void onSucesso() {
                Toast.makeText(AdicionarPacienteActivity.this,
                        R.string.adicionar_paciente_sucesso, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onErro(Exception erro) {
                alternarCarregando(false);
                Toast.makeText(AdicionarPacienteActivity.this,
                        R.string.adicionar_paciente_erro_salvar, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean dataValida(String data) {
        try {
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            formato.setLenient(false);
            formato.parse(data);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    private void alternarCarregando(boolean carregando) {
        progressSalvando.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnSalvar.setEnabled(!carregando);
        btnSalvar.setAlpha(carregando ? 0.6f : 1f);
    }
}
