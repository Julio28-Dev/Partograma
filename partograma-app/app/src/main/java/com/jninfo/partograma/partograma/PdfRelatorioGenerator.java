package com.jninfo.partograma.partograma;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import com.jninfo.partograma.partograma.data.Paciente;
import com.jninfo.partograma.partograma.data.RegistroPartograma;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Gera um PDF real (android.graphics.pdf.PdfDocument, nativo do Android -- sem
 * biblioteca externa) com os dados REAIS da paciente selecionada: identificacao, dados
 * clinicos, historico de avaliacoes do partograma, medicacoes, intercorrencias e
 * observacoes. Nada aqui e estatico/mockado -- tudo vem do que foi passado pelo chamador
 * (que por sua vez busca no Firestore antes de chamar este gerador).
 */
public class PdfRelatorioGenerator {

    private static final int LARGURA_A4 = 595;
    private static final int ALTURA_A4 = 842;
    private static final int MARGEM = 40;

    private final Context context;
    private final Paint paintTitulo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintSecao = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLinha = new Paint(Paint.ANTI_ALIAS_FLAG);

    private PdfDocument documento;
    private PdfDocument.Page pagina;
    private Canvas canvas;
    private int y;
    private int numeroPagina;

    public PdfRelatorioGenerator(Context context) {
        this.context = context.getApplicationContext();
        paintTitulo.setTextSize(18f);
        paintTitulo.setColor(Color.parseColor("#E8608F"));
        paintTitulo.setFakeBoldText(true);

        paintSecao.setTextSize(13f);
        paintSecao.setColor(Color.parseColor("#3A3149"));
        paintSecao.setFakeBoldText(true);

        paintTexto.setTextSize(11f);
        paintTexto.setColor(Color.parseColor("#3A3149"));

        paintLinha.setColor(Color.parseColor("#EDE6F0"));
        paintLinha.setStrokeWidth(1f);
    }

    /** Gera o arquivo e retorna um content:// Uri (FileProvider) pronto para compartilhar. */
    public Uri gerar(Paciente paciente, List<RegistroPartograma> registros, List<String> observacoes) throws IOException {
        documento = new PdfDocument();
        numeroPagina = 0;
        novaPagina();

        desenharTitulo("Relatório de Partograma");
        desenharLinhaTexto("Gerado em: " + android.text.format.DateFormat.format("dd/MM/yyyy - HH:mm", new java.util.Date()));
        espacar(14);

        desenharSecao("Identificação da paciente");
        desenharCampo("Nome", paciente.getNome());
        desenharCampo("Prontuário", paciente.getProntuario());
        int idade = paciente.getIdadeAnos();
        desenharCampo("Idade", idade >= 0 ? idade + " anos" : null);
        desenharCampo("Idade gestacional", paciente.getIdadeGestacional());
        desenharCampo("DUM", paciente.getDum());
        desenharCampo("Data de nascimento", paciente.getDataNascimento());
        desenharCampo("Tipo sanguíneo", paciente.getTipoSanguineo());
        desenharCampo("Alergias", paciente.getAlergias());
        desenharCampo("Profissional responsável", paciente.getProfissionalResponsavel());
        desenharCampo("Queixa principal", paciente.getQueixaPrincipal());
        desenharCampo("Conduta", paciente.getConduta());
        espacar(10);

        desenharSecao("Evolução do trabalho de parto (" + registros.size() + " avaliações)");
        if (registros.isEmpty()) {
            desenharLinhaTexto("Nenhuma avaliação registrada.");
        }
        for (RegistroPartograma r : registros) {
            desenharLinhaTexto(safe(r.getHorario()) + " — Dilatação: " + (r.getDilatacao() != null ? r.getDilatacao() + " cm" : "-")
                    + " | Rotatividade: " + r.getRotuloPosicaoLee()
                    + (r.getBatimentos() != null ? " | BCF: " + r.getBatimentos() + " bpm" : ""));
        }
        espacar(10);

        desenharSecao("Intercorrências");
        boolean algumaIntercorrencia = false;
        for (RegistroPartograma r : registros) {
            if (!TextUtils.isEmpty(r.getIntercorrencia())) {
                desenharLinhaTexto(safe(r.getHorario()) + " — " + r.getIntercorrencia());
                algumaIntercorrencia = true;
            }
        }
        if (!algumaIntercorrencia) {
            desenharLinhaTexto("Nenhuma intercorrência registrada.");
        }
        espacar(10);

        desenharSecao("Medicações");
        boolean algumaMedicacao = false;
        for (RegistroPartograma r : registros) {
            StringBuilder linha = new StringBuilder();
            if (!TextUtils.isEmpty(r.getOcitocina())) linha.append("Ocitocina: ").append(r.getOcitocina()).append("  ");
            if (!TextUtils.isEmpty(r.getMesoprostol())) linha.append("Misoprostol: ").append(r.getMesoprostol()).append("  ");
            if (!TextUtils.isEmpty(r.getRemedios())) linha.append("Outros: ").append(r.getRemedios());
            if (linha.length() > 0) {
                desenharLinhaTexto(safe(r.getHorario()) + " — " + linha);
                algumaMedicacao = true;
            }
        }
        if (!algumaMedicacao) {
            desenharLinhaTexto("Nenhuma medicação registrada.");
        }
        espacar(10);

        desenharSecao("Observações");
        if (observacoes.isEmpty()) {
            desenharLinhaTexto("Nenhuma observação registrada.");
        }
        for (String observacao : observacoes) {
            desenharLinhaTexto(observacao);
        }

        documento.finishPage(pagina);

        File pasta = new File(context.getCacheDir(), "relatorios");
        if (!pasta.exists()) {
            pasta.mkdirs();
        }
        String nomeArquivo = "relatorio_" + safeArquivo(paciente.getNome()) + "_" + System.currentTimeMillis() + ".pdf";
        File arquivo = new File(pasta, nomeArquivo);
        try (FileOutputStream saida = new FileOutputStream(arquivo)) {
            documento.writeTo(saida);
        } finally {
            documento.close();
        }

        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", arquivo);
    }

    private void novaPagina() {
        numeroPagina++;
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(LARGURA_A4, ALTURA_A4, numeroPagina).create();
        pagina = documento.startPage(info);
        canvas = pagina.getCanvas();
        y = MARGEM;
    }

    private void garantirEspaco(int alturaNecessaria) {
        if (y + alturaNecessaria > ALTURA_A4 - MARGEM) {
            documento.finishPage(pagina);
            novaPagina();
        }
    }

    private void desenharTitulo(String texto) {
        garantirEspaco(30);
        y += 18;
        canvas.drawText(texto, MARGEM, y, paintTitulo);
        y += 6;
        canvas.drawLine(MARGEM, y, LARGURA_A4 - MARGEM, y, paintLinha);
        y += 14;
    }

    private void desenharSecao(String texto) {
        garantirEspaco(24);
        y += 16;
        canvas.drawText(texto, MARGEM, y, paintSecao);
        y += 4;
    }

    private void desenharCampo(String rotulo, String valor) {
        if (TextUtils.isEmpty(valor)) {
            return;
        }
        desenharLinhaTexto(rotulo + ": " + valor);
    }

    private void desenharLinhaTexto(String texto) {
        garantirEspaco(16);
        y += 15;
        canvas.drawText(texto, MARGEM, y, paintTexto);
    }

    private void espacar(int px) {
        y += px;
    }

    private String safe(String valor) {
        return valor != null ? valor : "-";
    }

    private String safeArquivo(String nome) {
        if (nome == null) {
            return "paciente";
        }
        return nome.replaceAll("[^a-zA-Z0-9]", "_");
    }
}
