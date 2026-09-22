package com.jninfo.partograma.partograma;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.firestore.ListenerRegistration;
import com.jninfo.partograma.partograma.data.FirestorePatientRepository;
import com.jninfo.partograma.partograma.data.RegistroPartograma;
import com.jninfo.partograma.partograma.ui.PartogramaChartView;

import java.util.ArrayList;
import java.util.List;

/**
 * "Ver gráfico completo" -- grafico de dilatacao x horario com os registros reais da
 * paciente, atualizado em tempo real (mesma fonte de dados que o historico da aba
 * Partograma: {@link FirestorePatientRepository#observarRegistros}).
 */
public class GraficoPartogramaActivity extends BaseActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";
    public static final String EXTRA_NOME_PACIENTE = "extra_nome_paciente";

    private FirestorePatientRepository repositorio;
    private ListenerRegistration listener;
    private PartogramaChartView chartView;
    private View txtVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grafico_partograma);

        String pacienteId = getIntent().getStringExtra(EXTRA_PACIENTE_ID);
        String nome = getIntent().getStringExtra(EXTRA_NOME_PACIENTE);
        repositorio = new FirestorePatientRepository();

        chartView = findViewById(R.id.chartView);
        txtVazio = findViewById(R.id.txtGraficoVazio);
        TextView txtTitulo = findViewById(R.id.txtTituloGrafico);
        if (nome != null) {
            txtTitulo.setText(nome);
        }

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        if (pacienteId != null) {
            listener = repositorio.observarRegistros(pacienteId, new FirestorePatientRepository.ListaRegistrosCallback() {
                @Override
                public void onRegistrosAtualizados(List<RegistroPartograma> registros) {
                    exibirPontos(registros);
                }

                @Override
                public void onErro(Exception erro) {
                    txtVazio.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void exibirPontos(List<RegistroPartograma> registrosDescendentes) {
        if (registrosDescendentes.isEmpty()) {
            txtVazio.setVisibility(View.VISIBLE);
            chartView.setPontos(new ArrayList<>());
            return;
        }
        txtVazio.setVisibility(View.GONE);
        // O grafico deve andar da esquerda (mais antigo) para a direita (mais recente);
        // observarRegistros() retorna do mais recente para o mais antigo, entao invertemos.
        List<PartogramaChartView.Ponto> pontos = new ArrayList<>();
        for (int i = registrosDescendentes.size() - 1; i >= 0; i--) {
            RegistroPartograma registro = registrosDescendentes.get(i);
            if (registro.getDilatacao() != null) {
                pontos.add(new PartogramaChartView.Ponto(
                        registro.getHorario() != null ? registro.getHorario() : "",
                        registro.getDilatacao()));
            }
        }
        chartView.setPontos(pontos);
    }

    @Override
    protected void onDestroy() {
        if (listener != null) {
            listener.remove();
        }
        super.onDestroy();
    }
}
