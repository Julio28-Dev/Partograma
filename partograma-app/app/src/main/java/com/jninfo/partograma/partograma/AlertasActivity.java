package com.jninfo.partograma.partograma;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.ListenerRegistration;
import com.jninfo.partograma.partograma.data.Destinatario;
import com.jninfo.partograma.partograma.data.InstituicaoRepository;

import java.util.List;

/**
 * Gerenciamento de alertas da instituição logada: mensagem padrão + destinatários
 * autorizados a receber alertas (secao 17 do briefing). Acessada a partir de
 * Configurações -- é configuração da instituição, nao de uma paciente especifica, por isso
 * nao fica dentro das cinco abas do Registro da Paciente.
 */
public class AlertasActivity extends BaseActivity {

    private InstituicaoRepository repositorio;
    private String institutionId;
    private ListenerRegistration listenerDestinatarios;

    private EditText edtMensagemPadrao;
    private View containerDestinatarios;
    private EditText edtNovoNome;
    private View btnTipoEmail;
    private View btnTipoTelefone;
    private EditText edtNovoContato;
    private ProgressBar progress;
    private String tipoContatoEscolhido = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertas);

        repositorio = new InstituicaoRepository();
        institutionId = InstituicaoRepository.instituicaoIdAtual();

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        edtMensagemPadrao = findViewById(R.id.edtMensagemPadrao);
        containerDestinatarios = findViewById(R.id.containerDestinatarios);
        edtNovoNome = findViewById(R.id.edtNovoDestinatarioNome);
        btnTipoEmail = findViewById(R.id.btnTipoEmail);
        btnTipoTelefone = findViewById(R.id.btnTipoTelefone);
        edtNovoContato = findViewById(R.id.edtNovoDestinatarioContato);
        progress = findViewById(R.id.progressAlertas);

        findViewById(R.id.btnSalvarMensagemPadrao).setOnClickListener(v -> salvarMensagemPadrao());
        findViewById(R.id.btnAdicionarDestinatario).setOnClickListener(v -> adicionarDestinatario());
        btnTipoEmail.setOnClickListener(v -> selecionarTipoContato("email"));
        btnTipoTelefone.setOnClickListener(v -> selecionarTipoContato("telefone"));

        if (institutionId == null) {
            Toast.makeText(this, R.string.login_erro_generico, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        carregarMensagemPadrao();
        observarDestinatarios();
    }

    private void carregarMensagemPadrao() {
        repositorio.carregarMensagemPadrao(institutionId, new InstituicaoRepository.TextoCallback() {
            @Override
            public void onCarregado(String texto) {
                if (texto != null) {
                    edtMensagemPadrao.setText(texto);
                }
            }

            @Override
            public void onErro(Exception erro) {
                // Campo continua vazio/editavel -- sem bloquear o resto da tela.
            }
        });
    }

    private void salvarMensagemPadrao() {
        String mensagem = edtMensagemPadrao.getText().toString().trim();
        repositorio.salvarMensagemPadrao(institutionId, mensagem, new InstituicaoRepository.OperacaoCallback() {
            @Override
            public void onSucesso() {
                Toast.makeText(AlertasActivity.this, R.string.alertas_mensagem_salva, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onErro(Exception erro) {
                Toast.makeText(AlertasActivity.this, R.string.alertas_erro_salvar, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void observarDestinatarios() {
        listenerDestinatarios = repositorio.observarDestinatarios(institutionId, new InstituicaoRepository.ListaDestinatariosCallback() {
            @Override
            public void onAtualizados(List<Destinatario> destinatarios) {
                preencherDestinatarios(destinatarios);
            }

            @Override
            public void onErro(Exception erro) {
                Toast.makeText(AlertasActivity.this, R.string.alertas_erro_salvar, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void preencherDestinatarios(List<Destinatario> destinatarios) {
        android.widget.LinearLayout container = (android.widget.LinearLayout) containerDestinatarios;
        container.removeAllViews();
        if (destinatarios.isEmpty()) {
            TextView vazio = new TextView(this);
            vazio.setText(R.string.alertas_destinatarios_vazio);
            vazio.setTextColor(getResources().getColor(R.color.textSecondary));
            vazio.setTextSize(13f);
            container.addView(vazio);
            return;
        }
        for (Destinatario destinatario : destinatarios) {
            View linha = LayoutInflater.from(this).inflate(R.layout.item_destinatario_alerta, container, false);
            ((TextView) linha.findViewById(R.id.txtNomeDestinatario)).setText(destinatario.getNome());
            ((TextView) linha.findViewById(R.id.txtContatoDestinatario)).setText(
                    TextUtils.isEmpty(destinatario.getContato()) ? "-" : destinatario.getContato());
            boolean telefone = "telefone".equals(destinatario.getTipoContato());
            ((android.widget.ImageView) linha.findViewById(R.id.imgTipoDestinatario))
                    .setImageResource(telefone ? R.drawable.ic_phone : R.drawable.ic_email);
            linha.findViewById(R.id.btnRemoverDestinatario).setOnClickListener(v -> removerDestinatario(destinatario.getId()));
            container.addView(linha);
        }
    }

    private void selecionarTipoContato(String tipo) {
        tipoContatoEscolhido = tipo;
        boolean email = "email".equals(tipo);
        aplicarEstiloSegmento((TextView) btnTipoEmail, email);
        aplicarEstiloSegmento((TextView) btnTipoTelefone, !email);
        edtNovoContato.setText("");
        if (email) {
            edtNovoContato.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | android.text.InputType.TYPE_CLASS_TEXT);
            edtNovoContato.setHint(R.string.alertas_novo_destinatario_contato_hint);
        } else {
            edtNovoContato.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            edtNovoContato.setHint(R.string.alertas_novo_destinatario_contato_hint_telefone);
        }
    }

    private void aplicarEstiloSegmento(TextView botao, boolean selecionado) {
        botao.setBackgroundResource(selecionado ? R.drawable.bg_segment_selected : R.drawable.bg_segment_unselected);
        botao.setTextColor(getResources().getColor(selecionado ? R.color.primaryPink : R.color.textSecondary));
        botao.setTypeface(null, selecionado ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void removerDestinatario(String destinatarioId) {
        repositorio.removerDestinatario(institutionId, destinatarioId, new InstituicaoRepository.OperacaoCallback() {
            @Override
            public void onSucesso() {
                // Lista atualiza sozinha via listener.
            }

            @Override
            public void onErro(Exception erro) {
                Toast.makeText(AlertasActivity.this, R.string.alertas_erro_salvar, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void adicionarDestinatario() {
        String nome = edtNovoNome.getText().toString().trim();
        String contato = edtNovoContato.getText().toString().trim();
        if (TextUtils.isEmpty(nome)) {
            Toast.makeText(this, R.string.alertas_erro_nome_obrigatorio, Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(contato)) {
            Toast.makeText(this, R.string.alertas_erro_contato_obrigatorio, Toast.LENGTH_LONG).show();
            return;
        }
        Destinatario destinatario = new Destinatario();
        destinatario.setNome(nome);
        destinatario.setContato(contato);
        destinatario.setTipoContato(tipoContatoEscolhido);

        progress.setVisibility(View.VISIBLE);
        repositorio.adicionarDestinatario(institutionId, destinatario, new InstituicaoRepository.OperacaoCallback() {
            @Override
            public void onSucesso() {
                progress.setVisibility(View.GONE);
                edtNovoNome.setText("");
                edtNovoContato.setText("");
            }

            @Override
            public void onErro(Exception erro) {
                progress.setVisibility(View.GONE);
                Toast.makeText(AlertasActivity.this, R.string.alertas_erro_salvar, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerDestinatarios != null) {
            listenerDestinatarios.remove();
        }
    }
}
