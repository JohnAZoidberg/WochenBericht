package de.struckmeierfliesen.ds.wochenbericht;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;

import java.util.Date;

public class AlarmReceiver extends WakefulBroadcastReceiver {
    public static int MID = 0;
    public static Date date = new Date();

    @Override
    public void onReceive(Context context, Intent intent) {
        long when = System.currentTimeMillis();
        if (Util.getDayDifference(new Date(), date) == 0) return;
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.azubilog_small_white)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.azubilog_small_simpleblue_eckig))
                .setContentTitle(context.getResources().getString(R.string.app_name))
                .setContentText(context.getString(R.string.no_entries_yet))
                .setSound(alarmSound)
                .setAutoCancel(true)
                .setWhen(when)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{
                        1000, 1000,
                        1000, 1000,
                        1000, 1000,
                        1000, 1000,
                        1000});
        notificationManager.notify(MID, mNotifyBuilder.build());
        MID++;
    }

}