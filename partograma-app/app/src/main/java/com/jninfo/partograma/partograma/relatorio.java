package com.jninfo.partograma.partograma;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jninfo.partograma.partograma.data.PatientRepository;
import com.jninfo.partograma.partograma.report.ReportGridBinder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Tela "Relatorio": mostra a tabela de registros e o grafico do partograma (dilatacao,
 * BCF, Lee, frequencia de contracoes) do paciente, e permite salvar/compartilhar uma
 * imagem da tela (botoes Zoom e Compartilhar).
 */
public class relatorio extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    private PatientRepository repositorio;
    private int slot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relatorio);

        repositorio = new PatientRepository(this);

        String nomeClicado = getIntent().getExtras().getString("nomeClicado");
        slot = repositorio.localizarSlotPorNome(nomeClicado);

        TextView txtNome = findViewById(R.id.Nome);
        txtNome.setText(nomeClicado);

        preencherTabela();
        new ReportGridBinder(this, repositorio, slot).aplicarGraficoCompleto();

        Button btnZoom = findViewById(R.id.btnZoom);
        Button btnCompartilhar = findViewById(R.id.buttonCompartilhar);

        // Igual ao original: em versoes anteriores ao Android 6.0 (API 23), quando o
        // modelo de permissoes em tempo de execucao ainda nao existia, estes dois botoes
        // simplesmente nao faziam nada. Preservado aqui de proposito.
        btnZoom.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23) {
                if (!checkPermission()) {
                    requestPermission();
                    return;
                }
                View screenView = v.getRootView();
                screenView.setDrawingCacheEnabled(true);
                Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
                screenView.setDrawingCacheEnabled(false);
                salvarECompartilharViaVisualizacao(bitmap);
            }
        });

        btnCompartilhar.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23) {
                if (!checkPermission()) {
                    requestPermission();
                    return;
                }
                ConstraintLayout conteudo = findViewById(R.id.contraintlayouti);
                conteudo.setDrawingCacheEnabled(true);
                conteudo.buildDrawingCache();
                Bitmap bitmap = Bitmap.createBitmap(conteudo.getWidth(), conteudo.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                conteudo.draw(canvas);
                salvarECompartilharViaEnvio(bitmap);
            }
        });
    }

    /** Preenche a tabela de registros (uma coluna por hora ja registrada). */
    private void preencherTabela() {
        for (int hora = 1; hora <= PatientRepository.MAX_HORAS; hora++) {
            if (!repositorio.horaFoiRegistrada(slot, hora)) {
                continue;
            }
            String dia = repositorio.getCampo(slot, "dia", hora, "ainda sem");
            String horaDoDia = repositorio.getCampo(slot, "horadodia", hora, "-");
            setTextoPorNomeId("textoDia" + hora, dia);
            setTextoPorNomeId("textoHoradodia" + hora, horaDoDia);
            setTextoPorNomeId("textoBolsa" + hora, repositorio.getCampo(slot, "integridade", hora, "-"));
            setTextoPorNomeId("textoLa" + hora, repositorio.getCampo(slot, "liquido", hora, "-"));
            setTextoPorNomeId("textoOcitocina" + hora, repositorio.getCampo(slot, "ocitosina", hora, "-"));
            setTextoPorNomeId("textoMedicamentos" + hora, repositorio.getCampo(slot, "remedios", hora, "-"));
            setTextoPorNomeId("textoExaminador" + hora, repositorio.getCampo(slot, "examinador", hora, "-"));
        }
    }

    private void setTextoPorNomeId(String nomeId, String texto) {
        int id = getResources().getIdentifier(nomeId, "id", getPackageName());
        if (id != 0) {
            TextView view = findViewById(id);
            if (view != null) {
                view.setText(texto);
            }
        }
    }

    // ---- Exportacao / compartilhamento da imagem do relatorio -----------------------------

    private static File caminhoDaImagem() {
        return new File(Environment.getExternalStorageDirectory() + File.separator + "Pictures/screenshot.png");
    }

    private void salvarECompartilharViaVisualizacao(Bitmap bitmap) {
        File caminho = caminhoDaImagem();
        if (salvarBitmap(bitmap, caminho)) {
            enviarImagem("android.intent.action.VIEW", "Enviar Partograma...", caminho);
        }
    }

    private void salvarECompartilharViaEnvio(Bitmap bitmap) {
        File caminho = caminhoDaImagem();
        if (salvarBitmap(bitmap, caminho)) {
            enviarImagem("android.intent.action.SEND", "Send mail...", caminho);
        }
    }

    private boolean salvarBitmap(Bitmap bitmap, File destino) {
        try (FileOutputStream fos = new FileOutputStream(destino)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            return true;
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void enviarImagem(String acao, String tituloEscolha, File caminho) {
        android.content.Intent intent = new android.content.Intent(acao);
        intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{""});
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Partograma");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "Partograma");
        intent.setType("image/png");
        intent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.parse("file://" + caminho));
        startActivity(android.content.Intent.createChooser(intent, tituloEscolha));
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE")
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.WRITE_EXTERNAL_STORAGE")) {
            Toast.makeText(this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.e("value", "Permission Granted, Now you can use local drive .");
            } else {
                android.util.Log.e("value", "Permission Denied, You cannot use local drive .");
            }
        }
    }
}
