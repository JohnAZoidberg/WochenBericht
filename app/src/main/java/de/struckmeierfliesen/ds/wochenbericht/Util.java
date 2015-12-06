package de.struckmeierfliesen.ds.wochenbericht;

import android.app.Activity;
import android.content.Intent;
import android.databinding.BindingAdapter;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Util {
    public static int lastEntryPictureClicked = -1;
    public static final String TEMP_IMAGE = "azubiLogTemp.jpg";

    public static String convertDuration(int duration, String divider) {
        String hours =  String.valueOf((int) Math.floor(duration / 4d));
        String minutes = "00";
        if(duration % 4 == 0) {
            minutes = "00";
        } else if((duration + 1) % 4 == 0) {
            minutes = "45";
        } else if((duration + 2) % 4 == 0) {
            minutes = "30";
        } else if((duration + 3) % 4 == 0) {
            minutes = "15";
        }
        return hours + divider + minutes;
    }

    public static String convertDuration(int duration) {
        return convertDuration(duration, ":");
    }

    public static int convertDuration(String durationString) {
        String[] split = durationString.split(":");
        if(split[1].equals("15")) return 1;
        int hrs = Integer.parseInt(split[0]);
        int mnts = 0;
        switch(split[1]) {
            case "00":
                mnts = 0;
                break;
            case "15":
                mnts = 1;
                break;
            case "30":
                mnts = 2;
                break;
            case "45":
                mnts = 3;
                break;
        }
        return  hrs * 4 + mnts;
        // TODO has not been tested
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

    public static File newFile(String fileName) {
        File path = Environment.getExternalStorageDirectory();
        return new File(path, fileName);
    }

    public static File newPictureFile(String fileName) {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(path, fileName);
    }

    public static String formatDate(Date date) {
        return DateFormat.format("dd.MM.yy", date).toString();
    }

    public static Date[] getDatesOfLastWeek(Date date) {
        Date[] week = new Date[5];

        Calendar now = Calendar.getInstance();
        now.setTime(date);
        int weekday = now.get(Calendar.DAY_OF_WEEK);
        if (weekday != Calendar.MONDAY) {
            if (weekday == Calendar.SUNDAY) {
                now.add(Calendar.DAY_OF_YEAR, -6);
            } else {
                int days = weekday - 2;
                now.add(Calendar.DAY_OF_YEAR, -days);
            }
        }
        week[0] = now.getTime();
        for (int i = 1; i <= 4; i++) {
            now.add(Calendar.DAY_OF_YEAR, 1);
            week[i] = now.getTime();
        }
        return week;
    }

    public static String getDayAbbrev(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        String[] days;
        if(Locale.getDefault().getCountry().equals(Locale.ENGLISH.getCountry())) {
            days = new String[] {"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        } else {
            days = new String[] {"So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"};
        }
        return days[day - 1];
    }

    public static Date getEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    public static Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static int addPictureToEntry(DataBaseConnection dbConn, int entryId, String pictureFile) {
        dbConn.open();
        int rowsAffected = dbConn.addPictureToEntry(entryId, pictureFile);
        dbConn.close();
        return rowsAffected;
    }

    public static int addPictureToEntry(Activity activity, int entryId, String pictureFile) {
        DataBaseConnection dbConn = new DataBaseConnection(activity);
        return addPictureToEntry(dbConn, entryId, pictureFile);

    }

    interface OnInputSubmitListener<T> {
        void onSubmit(View v, T input);
    }

    @BindingAdapter("app:imagePath")
    public static void loadImage(ImageView view, String imagePath) {
        if (imagePath != null  && !imagePath.isEmpty()) {
            String absolutePath = new File(imagePath).getAbsolutePath();
            Picasso.with(view.getContext()).load(new File(absolutePath)).error(R.drawable.no_pic).into(view);
        }
    }

    public static void selectImageIntent(boolean fromCamera, Entry entry, Activity activity) {
        lastEntryPictureClicked = entry.id;
        if (fromCamera) {
            File f = new File(android.os.Environment.getExternalStorageDirectory(), Util.TEMP_IMAGE);
            Uri uriSavedImage = Uri.fromFile(f);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
            activity.startActivityForResult(intent, MainActivity.REQUEST_CAMERA);
        }else {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            activity.startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.select_file)), MainActivity.SELECT_FILE);
        }
    }
}
