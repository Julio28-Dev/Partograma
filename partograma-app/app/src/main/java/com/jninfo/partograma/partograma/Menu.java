package com.jninfo.partograma.partograma;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jninfo.partograma.partograma.data.PatientRepository;
import com.jninfo.partograma.partograma.notifications.ExamReminderScheduler;

import java.util.ArrayList;

/**
 * Tela "Menu": cadastro e lista de pacientes ("leitos"). Ate {@link PatientRepository#MAX_PACIENTES}
 * pacientes podem estar em acompanhamento simultaneo -- ao tentar cadastrar um a mais, o
 * APK original exibe o dialogo "Leito cheio", reproduzido aqui.
 *
 * Toque simples em um paciente -> abre {@link Detalhes}. Toque longo -> confirma exclusao.
 */
public class Menu extends AppCompatActivity {

    private PatientRepository repositorio;
    private ExamReminderScheduler agendador;

    private EditText txtNome;
    private ListView listaPaciente;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        repositorio = new PatientRepository(this);
        agendador = new ExamReminderScheduler(this);

        txtNome = findViewById(R.id.textoNome);
        Button btnAdicionar = findViewById(R.id.botaoAdicionar);
        Button btnHelp = findViewById(R.id.buttonHelp);
        listaPaciente = findViewById(R.id.listViewID);

        arrayList = new ArrayList<>(repositorio.listarNomesPacientes());
        adapter = new ArrayAdapter<>(this, R.layout.item_patient_row, R.id.nomePacienteRow, arrayList);
        listaPaciente.setAdapter(adapter);

        btnHelp.setOnClickListener(v -> finish());
        btnAdicionar.setOnClickListener(v -> adicionarPaciente());

        listaPaciente.setOnItemClickListener((parent, view, position, id) -> {
            String nome = (String) parent.getItemAtPosition(position);
            Intent detalhes = new Intent(Menu.this, Detalhes.class);
            detalhes.putExtra("nomeClicado", nome);
            startActivity(detalhes);
        });

        listaPaciente.setOnItemLongClickListener((parent, view, position, id) -> {
            String nome = (String) parent.getItemAtPosition(position);
            confirmarExclusao(nome, position);
            return true;
        });
    }

    private void adicionarPaciente() {
        String nome = txtNome.getText().toString();
        if (!nome.isEmpty()) {
            int slotLivre = repositorio.localizarSlotLivre();
            if (slotLivre == 0) {
                new AlertDialog.Builder(this)
                        .setMessage("Leito cheio")
                        .setCancelable(true)
                        .setPositiveButton("Ok", (dialog, which) -> dialog.cancel())
                        .create()
                        .show();
            } else {
                repositorio.criarPaciente(slotLivre, nome);
                arrayList.add(nome);
                adapter.notifyDataSetChanged();
                agendador.agendarParaPaciente(slotLivre, nome);
            }
        }
        txtNome.setText("");
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        View foco = getCurrentFocus();
        if (foco != null) {
            inputManager.hideSoftInputFromWindow(foco.getWindowToken(), 0);
        }
    }

    private void confirmarExclusao(String nome, int posicaoNaLista) {
        new AlertDialog.Builder(this)
                .setTitle("DELETAR PACIENTE?")
                .setMessage("DESEJA DELETAR O PACIENTE " + nome + "?")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> excluirPaciente(nome, posicaoNaLista))
                .create()
                .show();
    }

    private void excluirPaciente(String nome, int posicaoNaLista) {
        int slot = repositorio.localizarSlotPorNome(nome);
        if (slot == 0) {
            return;
        }
        agendador.cancelarParaPaciente(slot);
        repositorio.excluirPaciente(slot);
        Toast.makeText(getApplicationContext(), "Paciente Deleteado", Toast.LENGTH_LONG).show();
        arrayList.remove(posicaoNaLista);
        adapter.notifyDataSetChanged();
    }
}
