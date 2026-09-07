package com.jninfo.partograma.partograma.notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;

import com.jninfo.partograma.partograma.R;
import com.jninfo.partograma.partograma.data.PatientRepository;

/**
 * Agenda e cancela os lembretes de exame de cada paciente.
 *
 * Reproduz o comportamento do APK original: ao cadastrar um paciente (slot 1..10) sao
 * agendados 15 lembretes ("Examinar hora 2 de: NOME" ate "Examinar hora 16 de: NOME"),
 * um por hora, via AlarmManager.set(ELAPSED_REALTIME, ...). Ao excluir o paciente, os
 * 15 alarmes desse slot sao cancelados.
 *
 * O app original implementava isso com 10 x 15 = 150 metodos praticamente identicos
 * (scheduleNotification1_1 .. scheduleNotification10_15), um por combinacao fixa de
 * slot+indice. Aqui o mesmo resultado e obtido com um unico loop parametrizado.
 *
 * DESVIOS CONSCIENTES EM RELACAO AO APK ORIGINAL (documentados em detalhe no
 * REVERSE_ENGINEERING.md, secao "Pontos que nao puderam ser recuperados fielmente"):
 *
 * 1) BUG PRESERVADO DE PROPOSITO: o 10o lembrete (index 10, "Examinar hora 11") usa o
 *    mesmo atraso do 1o lembrete (1 hora) em vez de 10 horas, exatamente como no
 *    original ({@link #DELAYS_MS}[9]). Isso e um erro determinado e observavel do
 *    codigo original, reproduzido de proposito.
 *
 * 2) BUG *NAO* PRESERVADO: no codigo original, o "request code" do PendingIntent (usado
 *    pelo Android para diferenciar alarmes) foi digitado manualmente para cada um dos
 *    150 metodos e, a partir do paciente de slot 7 em diante, contem erros de digitacao
 *    -- inclusive reaproveitando constantes do Android (ex.: PointerIconCompat.TYPE_ALIAS)
 *    como se fossem numeros arbitrarios. Isso faz com que, no app original, alarmes de
 *    pacientes diferentes ocasionalmente colidam (um cancela/substitui o outro). Esse
 *    padrao nao tem nenhum significado de regra de negocio -- e apenas um erro de
 *    transcricao manual - por isso aqui usamos uma formula unica e sem colisoes
 *    (slot, indice) -> codigo. O texto, o destinatario, a quantidade e o horario de
 *    cada lembrete permanecem identicos ao original.
 *
 * 3) Assim como no APK original, a notificacao e criada sem NotificationChannel
 *    (targetSdkVersion 26). Em dispositivos com Android 8.0+ isso significa que a
 *    notificacao e agendada mas NAO chega a ser exibida ao usuario -- limitacao real
 *    do aplicativo original, preservada aqui de propósito.
 */
public class ExamReminderScheduler {

    private static final int NUM_LEMBRETES = 15;

    /** Atraso (ms) de cada um dos 15 lembretes, a partir do cadastro do paciente. */
    private static final long[] DELAYS_MS = {
            3_600_000L,   // 1: "hora 2"
            7_200_000L,   // 2: "hora 3"
            10_800_000L,  // 3: "hora 4"
            14_400_000L,  // 4: "hora 5"
            18_000_000L,  // 5: "hora 6"
            21_600_000L,  // 6: "hora 7"
            25_200_000L,  // 7: "hora 8"
            28_800_000L,  // 8: "hora 9"
            32_400_000L,  // 9: "hora 10"
            3_600_000L,   // 10: "hora 11" -- BUG ORIGINAL: deveria ser 36_000_000L (10h)
            39_600_000L,  // 11: "hora 12"
            43_200_000L,  // 12: "hora 13"
            46_800_000L,  // 13: "hora 14"
            50_400_000L,  // 14: "hora 15"
            54_000_000L,  // 15: "hora 16"
    };

    private final Context context;

    public ExamReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Agenda os 15 lembretes horarios do paciente recem-cadastrado no slot informado. */
    public void agendarParaPaciente(int slot, String nomePaciente) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (int indice = 1; indice <= NUM_LEMBRETES; indice++) {
            int horaRotulo = indice + 1;
            String texto = "Examinar hora " + horaRotulo + " de: " + nomePaciente;
            PendingIntent pendingIntent = criarPendingIntent(slot, indice, texto);
            long disparoEm = SystemClock.elapsedRealtime() + DELAYS_MS[indice - 1];
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, disparoEm, pendingIntent);
        }
    }

    /** Cancela os 15 lembretes agendados para o slot (usado ao excluir o paciente). */
    public void cancelarParaPaciente(int slot) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (int indice = 1; indice <= NUM_LEMBRETES; indice++) {
            PendingIntent pendingIntent = criarPendingIntent(slot, indice, null);
            alarmManager.cancel(pendingIntent);
        }
    }

    private PendingIntent criarPendingIntent(int slot, int indice, String texto) {
        int codigo = codigoUnico(slot, indice);
        Intent intent = new Intent(context, MyNotificationPublisher.class);
        intent.putExtra(MyNotificationPublisher.NOTIFICATION_ID, codigo);
        if (texto != null) {
            intent.putExtra(MyNotificationPublisher.NOTIFICATION, criarNotificacao(texto));
        }
        return PendingIntent.getBroadcast(context, codigo, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** slot 1..10, indice 1..15 -> codigo 0..149, sem colisoes entre pacientes. */
    private static int codigoUnico(int slot, int indice) {
        return (slot - 1) * NUM_LEMBRETES + (indice - 1);
    }

    private Notification criarNotificacao(String texto) {
        return new NotificationCompat.Builder(context, "partograma_lembretes")
                .setContentTitle("Scheduled Notification")
                .setContentText(texto)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();
    }
}
