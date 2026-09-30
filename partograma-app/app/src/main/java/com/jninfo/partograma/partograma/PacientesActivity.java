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

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.jninfo.partograma.partograma.data.FirestorePatientRepository;
import com.jninfo.partograma.partograma.data.Paciente;
import com.jninfo.partograma.partograma.data.SessaoUtil;
import com.jninfo.partograma.partograma.data.SolicitacaoAcessoRepository;

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
public class PacientesActivity extends BaseActivity {

    private FirestorePatientRepository repositorio;
    private ListenerRegistration listenerPacientes;
    private SolicitacaoAcessoRepository solicitacaoAcessoRepositorio;
    private ListenerRegistration listenerContagemPendentes;
    private View badgeConfiguracoes;

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
        solicitacaoAcessoRepositorio = new SolicitacaoAcessoRepository();
        badgeConfiguracoes = findViewById(R.id.badgeConfiguracoes);

        txtTotalPacientes = findViewById(R.id.txtTotalPacientes);
        estadoVazio = findViewById(R.id.estadoVazio);
        progressCarregando = findViewById(R.id.progressCarregando);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        RecyclerView listaPacientes = findViewById(R.id.listaPacientes);
        listaPacientes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PacientesAdapter(this::abrirDetalhesPaciente, this::confirmarExclusao);
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

        View.OnClickListener abrirCadastro = v ->
                startActivity(new Intent(PacientesActivity.this, AdicionarPacienteActivity.class));
        findViewById(R.id.btnAdicionarPaciente).setOnClickListener(abrirCadastro);
        findViewById(R.id.btnAdicionarPacienteFab).setOnClickListener(abrirCadastro);
        findViewById(R.id.btnConfiguracoes).setOnClickListener(v ->
                startActivity(new Intent(PacientesActivity.this, ConfiguracoesActivity.class)));

        observarPacientes();
        configurarBadgeAutorizacoes();
        revalidarAutorizacaoInstituicao();
    }

    /**
     * Confere no Firestore se a instituicao logada continua autorizada (pega uma
     * desativacao feita pelo admin DEPOIS do login -- SessaoUtil.sessaoValidaLocalmente()
     * sozinho nao sabe disso, so olha o cache local do Firebase Auth + o timestamp de
     * inatividade). Falha aberto se offline/erro (ver javadoc de
     * SessaoUtil.verificarAutorizacaoInstituicao) -- so derruba a sessao numa negativa
     * CONFIRMADA, nunca por falta de conexao.
     */
    private void revalidarAutorizacaoInstituicao() {
        com.google.firebase.auth.FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            return;
        }
        SessaoUtil.verificarAutorizacaoInstituicao(usuario.getUid(), autorizada -> {
            if (!autorizada && !isFinishing()) {
                SessaoUtil.encerrarSessao(PacientesActivity.this);
                Toast.makeText(PacientesActivity.this, R.string.pacientes_acesso_revogado, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(PacientesActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    /**
     * So instituicoes com papel admin (documento em admins/{uid}, ver firestore.rules)
     * veem a contagem de solicitacoes pendentes -- para as demais, nem a checagem de
     * admin muda nada visualmente, nem o listener de contagem chega a ser aberto (evita
     * um "permission-denied" inutil a cada instituicao comum que abrir esta tela).
     */
    private void configurarBadgeAutorizacoes() {
        solicitacaoAcessoRepositorio.verificarAdmin(ehAdmin -> {
            if (!ehAdmin) {
                return;
            }
            listenerContagemPendentes = solicitacaoAcessoRepositorio.observarContagemPendentes(quantidade -> {
                if (quantidade > 0) {
                    ((TextView) badgeConfiguracoes).setText(String.valueOf(quantidade));
                    badgeConfiguracoes.setVisibility(View.VISIBLE);
                } else {
                    badgeConfiguracoes.setVisibility(View.GONE);
                }
            });
        });
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

    private void confirmarExclusao(Paciente paciente) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.excluir_paciente_titulo)
                .setMessage(getString(R.string.excluir_paciente_mensagem, paciente.getNome()))
                .setNegativeButton(R.string.excluir_paciente_cancelar, null)
                .setPositiveButton(R.string.excluir_paciente_confirmar, (dialog, which) -> excluirPaciente(paciente))
                .create()
                .show();
    }

    private void excluirPaciente(Paciente paciente) {
        // Identificador usado e o document ID do Firestore (paciente.getId()), nunca o
        // nome -- dois pacientes podem ter o mesmo nome, o ID e o que garante que so o
        // documento certo (e as subcolecoes certas) sejam apagados.
        repositorio.excluirPacienteCompleto(paciente.getId(), new FirestorePatientRepository.OperacaoCallback() {
            @Override
            public void onSucesso() {
                Toast.makeText(PacientesActivity.this, R.string.excluir_paciente_sucesso, Toast.LENGTH_LONG).show();
                // A lista se atualiza sozinha via observarPacientes() (listener em tempo
                // real) -- nao precisa remover manualmente do adapter aqui.
            }

            @Override
            public void onErro(Exception erro) {
                Toast.makeText(PacientesActivity.this, R.string.excluir_paciente_erro, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (listenerPacientes != null) {
            listenerPacientes.remove();
        }
        if (listenerContagemPendentes != null) {
            listenerContagemPendentes.remove();
        }
        super.onDestroy();
    }
}
