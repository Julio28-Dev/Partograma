package com.jninfo.partograma.partograma;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.ListenerRegistration;
import com.jninfo.partograma.partograma.data.FirestorePatientRepository;
import com.jninfo.partograma.partograma.data.Paciente;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Nova tela "Pacientes" -- segunda tela principal do fluxo (Splash -> Home -> Pacientes),
 * substituindo visualmente o {@link Menu} original. Lista pacientes em tempo real a partir
 * do Firestore ({@link FirestorePatientRepository}) e permite buscar, adicionar (via
 * {@link AdicionarPacienteActivity}) e abrir os detalhes ({@link PacienteDetalhesActivity}).
 *
 * O Menu/Detalhes/relatorio originais (baseados em SharedPreferences) continuam existindo
 * e funcionando sem nenhuma alteracao -- esta tela nao os substitui ainda, e a base da
 * nova arquitetura de dados hospedada.
 */
public class PacientesActivity extends AppCompatActivity {

    private FirestorePatientRepository repositorio;
    private ListenerRegistration listenerPacientes;

    private PacientesAdapter adapter;
    private final List<Paciente> todosPacientes = new ArrayList<>();

    private TextView txtTotalPacientes;
    private View estadoVazio;
    private ProgressBar progressCarregando;
    private SwipeRefreshLayout swipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pacientes);

        repositorio = new FirestorePatientRepository();

        txtTotalPacientes = findViewById(R.id.txtTotalPacientes);
        estadoVazio = findViewById(R.id.estadoVazio);
        progressCarregando = findViewById(R.id.progressCarregando);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        RecyclerView listaPacientes = findViewById(R.id.listaPacientes);
        listaPacientes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PacientesAdapter(this::abrirDetalhesPaciente);
        listaPacientes.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::observarPacientes);

        EditText edtBusca = findViewById(R.id.edtBusca);
        edtBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrar(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        findViewById(R.id.btnAdicionarPaciente).setOnClickListener(v ->
                startActivity(new Intent(PacientesActivity.this, AdicionarPacienteActivity.class)));

        observarPacientes();
    }

    private void observarPacientes() {
        if (listenerPacientes != null) {
            listenerPacientes.remove();
        }
        progressCarregando.setVisibility(todosPacientes.isEmpty() ? View.VISIBLE : View.GONE);

        listenerPacientes = repositorio.observarPacientes(new FirestorePatientRepository.ListaPacientesCallback() {
            @Override
            public void onPacientesAtualizados(List<Paciente> pacientes) {
                progressCarregando.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                todosPacientes.clear();
                todosPacientes.addAll(pacientes);
                txtTotalPacientes.setText(String.valueOf(pacientes.size()));
                adapter.atualizar(pacientes);
                estadoVazio.setVisibility(pacientes.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErro(Exception erro) {
                progressCarregando.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(PacientesActivity.this,
                        R.string.pacientes_erro_carregar, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filtrar(String consulta) {
        String termo = consulta.trim().toLowerCase(Locale.getDefault());
        if (termo.isEmpty()) {
            adapter.atualizar(todosPacientes);
            estadoVazio.setVisibility(todosPacientes.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }
        List<Paciente> filtrados = new ArrayList<>();
        for (Paciente paciente : todosPacientes) {
            boolean nomeCombina = paciente.getNome() != null
                    && paciente.getNome().toLowerCase(Locale.getDefault()).contains(termo);
            boolean prontuarioCombina = paciente.getProntuario() != null
                    && paciente.getProntuario().toLowerCase(Locale.getDefault()).contains(termo);
            if (nomeCombina || prontuarioCombina) {
                filtrados.add(paciente);
            }
        }
        adapter.atualizar(filtrados);
        estadoVazio.setVisibility(filtrados.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void abrirDetalhesPaciente(Paciente paciente) {
        repositorio.atualizarUltimoAcesso(paciente.getId());
        Intent intent = new Intent(this, PacienteDetalhesActivity.class);
        intent.putExtra(PacienteDetalhesActivity.EXTRA_PACIENTE_ID, paciente.getId());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (listenerPacientes != null) {
            listenerPacientes.remove();
        }
        super.onDestroy();
    }
}
