package com.jninfo.partograma.partograma.notifications;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Recebe o alarme disparado pelo {@link ExamReminderScheduler} e publica a notificacao.
 * Identico em comportamento ao BroadcastReceiver do APK original.
 */
public class MyNotificationPublisher extends BroadcastReceiver {

    public static final String NOTIFICATION_ID = "notification-id";
    public static final String NOTIFICATION = "notification";

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = intent.getParcelableExtra(NOTIFICATION);
        int id = intent.getIntExtra(NOTIFICATION_ID, 0);
        notificationManager.notify(id, notification);
    }
}
