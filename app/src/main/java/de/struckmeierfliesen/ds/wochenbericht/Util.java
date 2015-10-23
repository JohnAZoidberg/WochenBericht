package de.struckmeierfliesen.ds.wochenbericht;

import android.content.Context;
import android.text.format.DateFormat;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Util {
    public static void alert(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static String convertDuration(int duration) {
        if(duration == -1) return "0:00";
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

    public static boolean isSameDay(Date date1, Date date2) {
        return (date1 != null || date2 != null)
                && DateFormat.format("dd.MM.yy", date1).equals(DateFormat.format("dd.MM.yy", date2));
    }

    public static int[] extractIntFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        return new int[] {day, month, year};
    }

    public static Date addDays(Date date, int value) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, value);
        return c.getTime();
    }

    public static int getDayDifference(Date date1, Date date2) {
        if(isSameDay(date1, date2)) return 0;
        else if(isSameDay(addDays(date1, 1), date2)) return -1;
        else if(isSameDay(addDays(date1, -1), date2)) return 1;
        else {
            long diffInMillies = date1.getTime() - date2.getTime();
            return (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        }
    }
}
