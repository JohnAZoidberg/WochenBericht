package de.struckmeierfliesen.ds.wochenbericht;

import android.content.Context;
import android.widget.Toast;

public class Util {
    public static final String ADD_INSTALLER = "Monteur hinzufügen";

    public static void alert(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static String convertDuration(int duration) {
        if(duration == 0) return "0:15";
        double i = (double) duration;
        double hours = Math.floor(i / 2d);
        String minutes = (i % 2 == 0) ? "00" : "30";
        return (int) hours + ":" + minutes;
    }

    public static int convertDuration(String durationString) {
        String[] split = durationString.split(":");
        if(split[1].equals("15")) return 0;
        int hrs = Integer.parseInt(split[0]);
        int mnts = split[1].equals("30") ? 1 : 0;
        return  hrs * 2 + mnts;
    }
}
