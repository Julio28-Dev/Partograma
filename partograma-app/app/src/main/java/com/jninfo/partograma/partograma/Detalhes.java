package com.jninfo.partograma.partograma;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.jninfo.partograma.partograma.data.PatientRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tela "Detalhes": formulario de registro horario do partograma de um paciente
 * (BCF, dilatacao, integridade da bolsa, liquido amniotico, plano de Lee, frequencia das
 * contracoes, ocitocina, misoprostol, outros medicamentos, examinador).
 *
 * Navegacao entre horas (1..16) e feita pelos botoes "-"/"+", que apenas trocam o NUMERO
 * exibido -- os campos do formulario so sao recarregados quando a tela e aberta ou quando
 * "Novo Registro" e pressionado (que tambem limpa os campos e usa a data/hora atual).
 * Esse e o comportamento exato do APK original: alterar a hora com "-"/"+" nao recarrega
 * automaticamente os dados daquela hora.
 */
public class Detalhes extends AppCompatActivity {

    private static final String[] ARR_INTEGRIDADE = {"ROTA", "ÍNTEGRA"};
    private static final String[] ARR_LIQUIDO = {
            "LC: Líquido Claro", "LCCG: Líquido Claro com Grumos", "LCSG: Líquido Claro sem Grumos",
            "LM: Líquido Meconial", "LS: Liquido Sanguinolento"
    };
    private static final String[] ARR_LEE = {"- AM", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4"};
    private static final String[] ARR_FREQ_CONTRACAO = {"LEVE", "MODERADA", "ALTA"};

    private static final String COR_ALERTA = "#FFFF4081";
    private static final String COR_OK = "#FFBAF39B";
    private static final int ULTIMA_POSICAO_LEE = 8;

    private PatientRepository repositorio;
    private int slot;
    private String nomeClicado;

    private ImageView imgViewFundo;
    private TextView txtAvisoLee;
    private TextView txtAvisoBCF;
    private TextView txtAvisoDilatacao;
    private TextView txtDataRegistrada;
    private TextView txtHoraRegistro;
    private EditText txtBCF;
    private Spinner spnIntegridade;
    private Spinner spnLiquido;
    private Spinner spnLee;
    private Spinner spnFreqContracao;
    private EditText txtDilatacao;
    private EditText txtOcitosina;
    private EditText txtMesoprostol;
    private EditText txtRemedioa;
    private EditText txtExaminador;
    private Button btnGirarLee;
    private Button btnNovo;
    private Button btnSalvar;
    private Button btnGerar;
    private ScrollView scroll;
    private ImageView imgViewLee;

    private int posicaoLeeAtual = 1;
    private String diaAtual;
    private String horaDoDiaAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhes);

        repositorio = new PatientRepository(this);

        imgViewFundo = findViewById(R.id.imageView2);
        txtAvisoLee = findViewById(R.id.textoAlertaLee);
        txtAvisoBCF = findViewById(R.id.textoAlertaBCF);
        txtAvisoDilatacao = findViewById(R.id.textoAlertaDilatacao);
        TextView txtNome = findViewById(R.id.nomePessoa);
        txtDataRegistrada = findViewById(R.id.dataPessoa);
        txtHoraRegistro = findViewById(R.id.textoHoraRegistro);
        txtBCF = findViewById(R.id.textoBCF);
        spnIntegridade = findViewById(R.id.spinnerIntegridade);
        spnLiquido = findViewById(R.id.spinnerLiquido);
        spnLee = findViewById(R.id.spinnerLee);
        spnFreqContracao = findViewById(R.id.spinnerFreqContracao);
        txtDilatacao = findViewById(R.id.textoDilatacao);
        txtOcitosina = findViewById(R.id.textoOcitonina);
        txtMesoprostol = findViewById(R.id.textoMesoprostol);
        txtRemedioa = findViewById(R.id.textoRemedios);
        txtExaminador = findViewById(R.id.textoExaminador);
        Button btnDiminuir = findViewById(R.id.btnDiminuir);
        Button btnAumentar = findViewById(R.id.btnAumentar);
        btnGerar = findViewById(R.id.botaoGerar);
        btnGirarLee = findViewById(R.id.botaoGirarLee);
        btnNovo = findViewById(R.id.botaoNovo);
        btnSalvar = findViewById(R.id.botaoSalvar);
        scroll = findViewById(R.id.mainScrollView);
        imgViewLee = findViewById(R.id.imageViewLee);

        // Os spinners exibem as opcoes definidas em res/values/arrays.xml (android:entries
        // no layout); os arrays acima sao usados apenas para localizar o indice selecionado
        // ao recarregar um registro salvo (equivalente ao ArrayAdapter.getPosition do original).

        nomeClicado = getIntent().getExtras().getString("nomeClicado");
        slot = repositorio.localizarSlotPorNome(nomeClicado);
        txtNome.setText(nomeClicado);

        btnDiminuir.setOnClickListener(v -> {
            int hora = horaAtualExibida();
            if (hora > 1) {
                txtHoraRegistro.setText(String.valueOf(hora - 1));
            }
        });

        btnAumentar.setOnClickListener(v -> {
            int hora = horaAtualExibida();
            if (hora < PatientRepository.MAX_HORAS) {
                txtHoraRegistro.setText(String.valueOf(hora + 1));
            }
        });

        btnGerar.setOnClickListener(v -> {
            Intent relatorio = new Intent(Detalhes.this, relatorio.class);
            relatorio.putExtra("nomeClicado", nomeClicado);
            startActivity(relatorio);
        });

        btnNovo.setOnClickListener(v -> {
            novoRegistro();
            liberarBotoes();
            if (horaAtualExibida() == 0) {
                txtHoraRegistro.setText("1");
            }
        });

        btnSalvar.setOnClickListener(v -> {
            scroll.fullScroll(View.FOCUS_UP);
            btnNovo.setEnabled(true);
            salvarDados();
            travarBotoes();
            btnGerar.setEnabled(true);
        });

        btnGirarLee.setOnClickListener(v -> {
            posicaoLeeAtual = proximaPosicaoLee(posicaoLeeAtual);
            atualizarImagemLee();
        });

        travarBotoes();
        carregarRegistroExistente();
        atualizarImagemLee();
        btnGerar.setEnabled(horaAtualExibida() != 0);
    }

    private int horaAtualExibida() {
        return Integer.parseInt(txtHoraRegistro.getText().toString());
    }

    private void novoRegistro() {
        SimpleDateFormat sdfDia = new SimpleDateFormat("dd/MM", Locale.getDefault());
        SimpleDateFormat sdfHora = new SimpleDateFormat("h:mm a", Locale.getDefault());
        Date agora = new Date();
        diaAtual = sdfDia.format(agora);
        horaDoDiaAtual = sdfHora.format(agora);
        btnNovo.setEnabled(false);
        txtDataRegistrada.setText(diaAtual + ", " + horaDoDiaAtual);
        apagarTexto();
    }

    private void apagarTexto() {
        txtBCF.setText("");
        txtDilatacao.setText("");
        txtOcitosina.setText("");
        txtMesoprostol.setText("");
        txtRemedioa.setText("");
    }

    private void salvarDados() {
        if (txtBCF.getText().toString().isEmpty() || txtDilatacao.getText().toString().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage("Preencha todos os campos BCF ou Dilatação")
                    .setCancelable(true)
                    .setPositiveButton("Ok", (dialog, id) -> dialog.cancel())
                    .create()
                    .show();
            return;
        }

        int hora = horaAtualExibida();
        if (hora >= 1 && hora <= PatientRepository.MAX_HORAS) {
            PatientRepository.RegistroHorario r = new PatientRepository.RegistroHorario();
            r.dia = diaAtual;
            r.horaDoDia = horaDoDiaAtual;
            r.batimentos = txtBCF.getText().toString();
            r.dilatacao = txtDilatacao.getText().toString();
            r.integridade = spnIntegridade.getSelectedItem().toString();
            r.liquido = spnLiquido.getSelectedItem().toString();
            r.lee = spnLee.getSelectedItem().toString();
            r.freqContracao = spnFreqContracao.getSelectedItem().toString();
            r.ocitocina = txtOcitosina.getText().toString();
            r.mesoprostol = txtMesoprostol.getText().toString();
            r.remedios = txtRemedioa.getText().toString();
            r.examinador = txtExaminador.getText().toString();
            r.posicaoLee = posicaoLeeAtual;
            repositorio.salvarRegistro(slot, hora, r);
        }

        // Igual ao original: toda vez que "Salvar" e concluido, a tela primeiro volta para
        // o estado "tudo OK" (faixa verde + os tres avisos neutros) e SO DEPOIS os metodos
        // de aviso abaixo podem sobrescrever isso com o alerta especifico (faixa rosa +
        // mensagem), caso alguma regra de negocio dispare. Sem este reset, um valor normal
        // deixava a tela com a cor de alerta de uma leitura anterior (ou sem nenhuma cor
        // definida), em vez de confirmar visualmente que o registro esta dentro do esperado.
        imgViewFundo.setBackgroundColor(Color.parseColor(COR_OK));
        txtAvisoBCF.setText("Batimentos: OK");
        txtAvisoLee.setText("Plano de Lee: OK");
        txtAvisoDilatacao.setText("Dilatação: OK");

        avisarBatimentos();
        avisarLee(hora);
        avisarDilatacao(hora);
    }

    private void carregarRegistroExistente() {
        txtAvisoBCF.setText(repositorio.getAviso("batimento", slot, "Batimento:"));
        txtAvisoLee.setText(repositorio.getAviso("lee", slot, "Plano de Lee:"));
        txtAvisoDilatacao.setText(repositorio.getAviso("dilatacao", slot, "Dilatação:"));

        String horaSalva = repositorio.getHoraAtual(slot);
        int hora;
        try {
            hora = Integer.parseInt(horaSalva);
        } catch (NumberFormatException e) {
            return; // "ainda sem": nenhum registro salvo ainda, mantem os defaults do layout.
        }
        if (hora < 1 || hora > PatientRepository.MAX_HORAS) {
            return;
        }

        String dia = repositorio.getCampo(slot, "dia", hora, "");
        String horaDoDia = repositorio.getCampo(slot, "horadodia", hora, "");
        txtDataRegistrada.setText(dia + ", " + horaDoDia);
        txtBCF.setText(repositorio.getCampo(slot, "batimentos", hora, "ainda sem"));
        txtDilatacao.setText(repositorio.getCampo(slot, "dilatacao", hora, "ainda sem"));
        txtOcitosina.setText(repositorio.getCampo(slot, "ocitosina", hora, "ainda sem"));
        txtMesoprostol.setText(repositorio.getCampo(slot, "mesoprostol", hora, "ainda sem"));
        txtRemedioa.setText(repositorio.getCampo(slot, "remedios", hora, "ainda sem"));
        txtExaminador.setText(repositorio.getCampo(slot, "examinador", hora, "ainda sem"));
        txtHoraRegistro.setText(String.valueOf(hora));
        spnIntegridade.setSelection(indiceDe(ARR_INTEGRIDADE, repositorio.getCampo(slot, "integridade", hora, "ainda sem")));
        spnLiquido.setSelection(indiceDe(ARR_LIQUIDO, repositorio.getCampo(slot, "liquido", hora, "ainda sem")));
        spnLee.setSelection(indiceDe(ARR_LEE, repositorio.getCampo(slot, "lee", hora, "ainda sem")));
        spnFreqContracao.setSelection(indiceDe(ARR_FREQ_CONTRACAO, repositorio.getCampo(slot, "freqcontracao", hora, "ainda sem")));

        // Igual ao original: a posicao do Lee salva NAO e restaurada ao abrir a tela -- a
        // imagem sempre comeca em "posicao1" (valor padrao de posicaoLeeAtual) ate o usuario
        // tocar em "Girar" pelo menos uma vez. No app original isso acontece porque a funcao
        // que redesenha a imagem roda por cima do valor recem-carregado logo depois, sempre
        // usando a posicao inicial. Preservado aqui de proposito -- ver auditoria funcional.
    }

    private static int indiceDe(String[] opcoes, String valor) {
        for (int i = 0; i < opcoes.length; i++) {
            if (opcoes[i].equals(valor)) {
                return i;
            }
        }
        return 0;
    }

    private void travarBotoes() {
        setCamposHabilitados(false);
    }

    private void liberarBotoes() {
        setCamposHabilitados(true);
    }

    private void setCamposHabilitados(boolean habilitado) {
        txtBCF.setEnabled(habilitado);
        txtDilatacao.setEnabled(habilitado);
        txtOcitosina.setEnabled(habilitado);
        txtMesoprostol.setEnabled(habilitado);
        txtRemedioa.setEnabled(habilitado);
        txtExaminador.setEnabled(habilitado);
        spnIntegridade.setEnabled(habilitado);
        spnLiquido.setEnabled(habilitado);
        spnLee.setEnabled(habilitado);
        spnFreqContracao.setEnabled(habilitado);
        btnGirarLee.setEnabled(habilitado);
        btnSalvar.setEnabled(habilitado);
    }

    /** Avanca o indice de posicao do Lee (1..8, cíclico) e troca a imagem exibida. */
    private static int proximaPosicaoLee(int atual) {
        int proxima = atual + 1;
        return proxima > ULTIMA_POSICAO_LEE ? 1 : proxima;
    }

    private void atualizarImagemLee() {
        int recurso = getResources().getIdentifier("posicao" + posicaoLeeAtual, "drawable", getPackageName());
        imgViewLee.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), recurso));
    }

    // ---- Regras de negocio / avisos --------------------------------------------------------

    /**
     * BCF (batimentos cardio-fetais) normal: 120 a 160 bpm. Fora dessa faixa, exibe alerta
     * e destaca a faixa superior da tela em rosa. Igual ao original, o aviso NAO e resetado
     * automaticamente quando o valor volta ao normal -- permanece ate a proxima leitura
     * fora da faixa (comportamento observado no APK original, preservado aqui).
     */
    private void avisarBatimentos() {
        int bcf = Integer.parseInt(txtBCF.getText().toString());
        if (bcf < 120) {
            txtAvisoBCF.setText("Batimentos: ABAIXO do Normal");
            imgViewFundo.setBackgroundColor(Color.parseColor(COR_ALERTA));
        } else if (bcf > 160) {
            txtAvisoBCF.setText("Batimentos: Acima do Normal");
            imgViewFundo.setBackgroundColor(Color.parseColor(COR_ALERTA));
        }
        repositorio.salvarAviso("batimento", slot, txtAvisoBCF.getText().toString());
    }

    /**
     * Compara o Plano de Lee da hora atual com o da hora anterior (e, se a anterior nao
     * tiver sido registrada, com a de duas horas atras). Na hora 1 sempre mostra "OK".
     *
     * DESVIO CONSCIENTE do APK original: la, essa comparacao usa "==" (igualdade de
     * referencia) em vez de String.equals(), o que faz o alerta quase nunca disparar
     * corretamente para dados reais (so por coincidencia de cache interno do
     * SharedPreferences dentro da mesma sessao do app). Aqui usamos equals() -- a
     * comparacao de valor claramente pretendida pelo nome e texto do recurso -- ja que
     * reproduzir literalmente esse comportamento exigiria depender de um detalhe de
     * implementacao do Android nao documentado e inconsistente entre sessoes.
     * Ver REVERSE_ENGINEERING.md, secao 8.
     *
     * O destaque em rosa a partir da hora 2, por outro lado, e reproduzido tal como no
     * original: dispara sempre, independente do resultado da comparacao.
     */
    private void avisarLee(int hora) {
        if (hora == 1) {
            txtAvisoLee.setText("Plano de Lee: OK");
            return;
        }
        if (hora < 2 || hora > PatientRepository.MAX_HORAS) {
            return;
        }
        String atual = repositorio.getCampo(slot, "lee", hora, "null");
        String anterior = repositorio.getCampo(slot, "lee", hora - 1, "null");
        if (anterior.equals(atual)) {
            txtAvisoLee.setText("Plano de Lee: Mesma Posição de 1 hora atrás");
        } else if (anterior.equals("null") && hora >= 3) {
            String duasAntes = repositorio.getCampo(slot, "lee", hora - 2, "null");
            if (duasAntes.equals(atual)) {
                txtAvisoLee.setText("Plano de Lee: Mesma Posição de 2 horas atrás");
            }
        }
        imgViewFundo.setBackgroundColor(Color.parseColor(COR_ALERTA));
        repositorio.salvarAviso("lee", slot, txtAvisoLee.getText().toString());
    }

    /**
     * Dilatacao deve aumentar ~1cm/hora (linha de alerta classica do partograma). Na hora 1
     * sempre mostra "OK". Das horas 2 a 16, compara com a hora anterior.
     *
     * NOTA: o APK original tambem possui checagens para "dilatou 10cm em menos de 4 horas"
     * (horas 1-4) e "nao dilatou 10cm em 10 horas" (hora 10), mas essas mensagens sao
     * imediatamente sobrescritas, no mesmo metodo, pela mensagem baseada na comparacao com
     * a hora anterior -- ou seja, elas nunca chegam a ser exibidas ao usuario em nenhum
     * caminho possivel. Por isso nao foram reproduzidas aqui (nenhuma diferenca de
     * comportamento observavel). Ver REVERSE_ENGINEERING.md, secao 8.
     */
    private void avisarDilatacao(int hora) {
        if (hora == 1) {
            txtAvisoDilatacao.setText("Dilatação: OK");
            return;
        }
        if (hora < 2 || hora > PatientRepository.MAX_HORAS) {
            return;
        }
        int atual = Integer.parseInt(repositorio.getCampo(slot, "dilatacao", hora, "0"));
        int anterior = Integer.parseInt(repositorio.getCampo(slot, "dilatacao", hora - 1, "0"));
        int diferenca = atual - anterior;
        if (diferenca == 1) {
            txtAvisoDilatacao.setText("Dilatação: Aumentou 1cm/h");
        } else if (diferenca == 0) {
            txtAvisoDilatacao.setText("Dilatação: Não aumentou 1cm/h,Está igual após de 2 toques!");
            imgViewFundo.setBackgroundColor(Color.parseColor(COR_ALERTA));
        } else {
            txtAvisoDilatacao.setText("Dilatação: Não aumentou 1cm/h");
            imgViewFundo.setBackgroundColor(Color.parseColor(COR_ALERTA));
        }
        repositorio.salvarAviso("dilatacao", slot, txtAvisoDilatacao.getText().toString());
    }
}
