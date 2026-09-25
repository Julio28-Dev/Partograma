package com.jninfo.partograma.partograma;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.jninfo.partograma.partograma.data.FirestorePatientRepository;
import com.jninfo.partograma.partograma.data.RegistroPartograma;

import java.util.List;

/**
 * Tela generica de listagem usada pelos itens da aba Relatório (Evolução do trabalho de
 * parto / Intercorrências / Medicações / Observações) -- em vez de 4 telas quase
 * identicas, uma so, parametrizada pelo modo, todas lendo dados reais do Firestore.
 */
public class RelatorioListaActivity extends BaseActivity {

    public static final String EXTRA_PACIENTE_ID = "extra_paciente_id";
    public static final String EXTRA_MODO = "extra_modo";

    public static final String MODO_EVOLUCAO = "evolucao";
    public static final String MODO_INTERCORRENCIAS = "intercorrencias";
    public static final String MODO_MEDICACOES = "medicacoes";
    public static final String MODO_OBSERVACOES = "observacoes";

    private FirestorePatientRepository repositorio;
    private ListenerRegistration listenerRegistros;
    private String pacienteId;
    private String modo;
    private LinearLayout containerLista;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relatorio_lista);

        pacienteId = getIntent().getStringExtra(EXTRA_PACIENTE_ID);
        modo = getIntent().getStringExtra(EXTRA_MODO);
        repositorio = new FirestorePatientRepository();
        containerLista = findViewById(R.id.containerLista);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.txtTituloLista)).setText(tituloParaModo());

        if (MODO_OBSERVACOES.equals(modo)) {
            configurarFormObservacao();
            carregarObservacoes();
        } else {
            carregarRegistros();
        }
    }

    private int tituloParaModo() {
        if (MODO_INTERCORRENCIAS.equals(modo)) return R.string.relatorio_intercorrencias;
        if (MODO_MEDICACOES.equals(modo)) return R.string.relatorio_medicacoes;
        if (MODO_OBSERVACOES.equals(modo)) return R.string.relatorio_observacoes;
        return R.string.relatorio_evolucao;
    }

    // ---- Evolucao / Intercorrencias / Medicacoes (baseado em registros) --------------------

    private void carregarRegistros() {
        listenerRegistros = repositorio.observarRegistros(pacienteId, new FirestorePatientRepository.ListaRegistrosCallback() {
            @Override
            public void onRegistrosAtualizados(List<RegistroPartograma> registros) {
                exibirRegistros(registros);
            }

            @Override
            public void onErro(Exception erro) {
                mostrarVazio();
            }
        });
    }

    private void exibirRegistros(List<RegistroPartograma> registros) {
        containerLista.removeAllViews();
        boolean algumExibido = false;
        for (RegistroPartograma registro : registros) {
            if (MODO_INTERCORRENCIAS.equals(modo) && !Boolean.TRUE.equals(registro.getTeveIntercorrencia())) {
                continue;
            }
            if (MODO_MEDICACOES.equals(modo)
                    && TextUtils.isEmpty(registro.getOcitocina())
                    && TextUtils.isEmpty(registro.getMesoprostol())
                    && TextUtils.isEmpty(registro.getRemedios())) {
                continue;
            }
            adicionarBlocoRegistro(registro);
            algumExibido = true;
        }
        if (!algumExibido) {
            mostrarVazio();
        }
    }

    private void adicionarBlocoRegistro(RegistroPartograma registro) {
        TextView cabecalho = new TextView(this);
        cabecalho.setText(registro.getHorario() != null ? registro.getHorario() : "-");
        cabecalho.setTextColor(ContextCompat.getColor(this, R.color.primaryPink));
        cabecalho.setTextSize(13.5f);
        cabecalho.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams paramsCabecalho = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        paramsCabecalho.topMargin = containerLista.getChildCount() == 0 ? 0 : 18;
        containerLista.addView(cabecalho, paramsCabecalho);

        if (MODO_EVOLUCAO.equals(modo)) {
            adicionarLinha("Fase do trabalho de parto", registro.getFaseTrabalhoParto());
            adicionarLinha("Dilatação", registro.getDilatacao() != null ? registro.getDilatacao() + " cm" : null);
            adicionarLinha("Posição do bebê", registro.getPosicaoBebe());
            adicionarLinha("Plano de De Lee", registro.getRotuloPlanoDeLee());
            adicionarLinha("BCF", registro.getBatimentos() != null ? registro.getBatimentos() + " bpm" : null);
            adicionarLinha("Integridade da bolsa", registro.getIntegridade());
            adicionarLinha("Líquido amniótico", registro.getLiquido());
            adicionarLinha("Frequência de contração (10 min)", registro.getFreqContracao());
            adicionarLinha("Examinador", registro.getExaminador());
            if (!registro.getMetodosNaoFarmacologicos().isEmpty()) {
                adicionarLinha("Métodos não farmacológicos", TextUtils.join(", ", registro.getMetodosNaoFarmacologicos()));
            }
            adicionarLinha("Observação adicional", registro.getObservacaoAdicional());
        } else if (MODO_INTERCORRENCIAS.equals(modo)) {
            adicionarLinha("Intercorrência", registro.getIntercorrencia());
        } else if (MODO_MEDICACOES.equals(modo)) {
            adicionarLinha("Ocitocina", registro.getOcitocina());
            adicionarLinha("Misoprostol", registro.getMesoprostol());
            adicionarLinha("Outras medicações", registro.getRemedios());
        }
    }

    private void adicionarLinha(String rotulo, String valor) {
        if (TextUtils.isEmpty(valor)) {
            return;
        }
        TextView linha = new TextView(this);
        linha.setText(rotulo + ": " + valor);
        linha.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
        linha.setTextSize(13f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = 4;
        containerLista.addView(linha, params);
    }

    // ---- Observacoes (subcolecao evolucoes) -------------------------------------------------

    private void configurarFormObservacao() {
        findViewById(R.id.formNovaObservacao).setVisibility(View.VISIBLE);
        EditText edtObservacao = findViewById(R.id.edtNovaObservacao);
        findViewById(R.id.btnSalvarObservacao).setOnClickListener(v -> {
            String texto = edtObservacao.getText().toString().trim();
            if (texto.isEmpty()) {
                return;
            }
            repositorio.adicionarEvolucao(pacienteId, texto, new FirestorePatientRepository.OperacaoCallback() {
                @Override
                public void onSucesso() {
                    edtObservacao.setText("");
                    carregarObservacoes();
                }

                @Override
                public void onErro(Exception erro) {
                    Toast.makeText(RelatorioListaActivity.this, R.string.relatorio_pdf_erro, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void carregarObservacoes() {
        repositorio.evolucoesDoPaciente(pacienteId).orderBy("criadoEm", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    containerLista.removeAllViews();
                    if (snapshot.isEmpty()) {
                        mostrarVazio();
                        return;
                    }
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String texto = doc.getString("texto");
                        java.util.Date data = doc.getDate("criadoEm");
                        if (texto == null) {
                            continue;
                        }
                        TextView cabecalho = new TextView(this);
                        cabecalho.setText(data != null ? DateFormat.format("dd/MM/yyyy - HH:mm", data) : "");
                        cabecalho.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
                        cabecalho.setTextSize(11.5f);
                        LinearLayout.LayoutParams paramsCabecalho = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        paramsCabecalho.topMargin = containerLista.getChildCount() == 0 ? 0 : 16;
                        containerLista.addView(cabecalho, paramsCabecalho);

                        TextView txtObservacao = new TextView(this);
                        txtObservacao.setText(texto);
                        txtObservacao.setTextColor(ContextCompat.getColor(this, R.color.textPrimary));
                        txtObservacao.setTextSize(13.5f);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.topMargin = 2;
                        containerLista.addView(txtObservacao, params);
                    }
                })
                .addOnFailureListener(e -> mostrarVazio());
    }

    private void mostrarVazio() {
        containerLista.removeAllViews();
        TextView vazio = new TextView(this);
        vazio.setText(R.string.relatorio_lista_vazia);
        vazio.setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
        vazio.setTextSize(13.5f);
        containerLista.addView(vazio);
    }

    @Override
    protected void onDestroy() {
        if (listenerRegistros != null) {
            listenerRegistros.remove();
        }
        super.onDestroy();
    }
}
