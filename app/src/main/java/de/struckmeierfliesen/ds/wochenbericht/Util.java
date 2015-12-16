package de.struckmeierfliesen.ds.wochenbericht;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.BindingAdapter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;

import com.google.common.io.Files;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Util {
    public static final String APP_VERSION_NAME = "Beta 0.5.2";
    public static final String TEMP_IMAGE = "azubiLogTemp.jpg";
    public static final int SELECT_FILE = 0;
    public static final int REQUEST_CAMERA = 1;

    public static int lastEntryPictureClicked = -1;
    private static float oneDipPixels = -1;
    private static Random randomGenerator = new Random();

    public static String convertDuration(int duration, String divider) {
        String hours = String.valueOf((int) Math.floor(duration / 4d));
        String minutes = "00";
        if (duration % 4 == 0) {
            minutes = "00";
        } else if ((duration + 1) % 4 == 0) {
            minutes = "45";
        } else if ((duration + 2) % 4 == 0) {
            minutes = "30";
        } else if ((duration + 3) % 4 == 0) {
            minutes = "15";
        }
        return hours + divider + minutes;
    }

    public static String convertDuration(int duration) {
        return convertDuration(duration, ":");
    }

    public static int convertDuration(String durationString) {
        String[] split = durationString.split(":");
        if (split[1].equals("15")) return 1;
        int hrs = Integer.parseInt(split[0]);
        int mnts = 0;
        switch (split[1]) {
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
        return hrs * 4 + mnts;
        // TODO has not been tested
    }

    // TODO maybe profile
    // returns true if either date is null
    public static boolean isSameDay(Date date1, Date date2) {
        return date1 == null || date2 == null
                || DateFormat.format("dd.MM.yy", date1).equals(DateFormat.format("dd.MM.yy", date2));
    }

    public static int[] extractIntFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        return new int[]{day, month, year};
    }

    public static Date addDays(Date date, int value) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, value);
        return c.getTime();
    }

    public static int getDayDifference(Date date1, Date date2) {
        if (isSameDay(date1, date2)) return 0;
        else if (isSameDay(addDays(date1, 1), date2)) return -1;
        else if (isSameDay(addDays(date1, -1), date2)) return 1;
        else {
            date1 = dateAtMidnight(date1);
            date2 = dateAtMidnight(date2);
            long diffInMillies = date1.getTime() - date2.getTime();
            return (int) TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        }
    }

    public static Date dateAtMidnight(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
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
        if (date == null) return "null";
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
        if (Locale.getDefault().getCountry().equals(Locale.ENGLISH.getCountry())) {
            days = new String[]{"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
        } else {
            days = new String[]{"So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"};
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

    public static void deletePictureFromEntry(DataBaseConnection dbConn, int entryId) {
        dbConn.open();
        dbConn.deletePictureFromEntry(entryId);
        dbConn.close();
    }

    public static void deletePictureFromEntry(Activity activity, int entryId) {
        DataBaseConnection dbConn = new DataBaseConnection(activity);
        deletePictureFromEntry(dbConn, entryId);
    }

    // returns success
    public static boolean handlePictureResult(int requestCode, int resultCode, Intent data, Activity activity) {
        boolean success = false;
        if (resultCode == Activity.RESULT_OK && Util.lastEntryPictureClicked != -1) {
            if (requestCode == REQUEST_CAMERA) {
                File f = new File(Environment.getExternalStorageDirectory().toString());
                for (File temp : f.listFiles()) {
                    if (temp.getName().equals(Util.TEMP_IMAGE)) {
                        f = temp;
                        break;
                    }
                }
                try {
                    BitmapFactory.Options btmapOptions = new BitmapFactory.Options();
                    Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath(), btmapOptions);
                    f.delete();

                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String fileName = "AzubiLog_" + Util.lastEntryPictureClicked + "_" + timeStamp + ".png";
                    File file = Util.newPictureFile(fileName);

                    OutputStream fOut = null;
                    try {
                        fOut = new FileOutputStream(file);
                        bm.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
                        fOut.flush();
                        fOut.close();
                        String picturePath = file.getAbsolutePath();
                        Util.addPictureToEntry(activity, Util.lastEntryPictureClicked, picturePath);
                        success = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == SELECT_FILE) {
                Uri selectedImageUri = data.getData();
                String picturePath = getPath(selectedImageUri, activity);
                Util.addPictureToEntry(activity, Util.lastEntryPictureClicked, picturePath);
                success = true;
            }
        }
        Util.lastEntryPictureClicked = -1;
        return success;
    }

    public static String getPath(Uri uri, Context context) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    interface OnInputSubmitListener<T> {
        void onSubmit(View v, T input);
    }

    @BindingAdapter("app:imagePath")
    public static void loadImage(ImageView view, String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            Picasso.with(view.getContext())
                    .load(new File(imagePath))
                    .resize(200, 200)
                    .centerCrop()
                    .error(R.drawable.ic_add_a_photo_black_24dp)
                    .into(view);
        } else {
            Picasso.with(view.getContext())
                    .load(R.drawable.ic_add_a_photo_black_24dp)
                    .into(view);
        }
    }

    public static void selectImageIntent(boolean fromCamera, Entry entry, Activity activity) {
        lastEntryPictureClicked = entry.id;
        if (fromCamera) {
            File f = new File(android.os.Environment.getExternalStorageDirectory(), Util.TEMP_IMAGE);
            Uri uriSavedImage = Uri.fromFile(f);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
            activity.startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            activity.startActivityForResult(Intent.createChooser(intent, activity.getString(R.string.select_file)), SELECT_FILE);
        }
    }

    public static float dipToPixels(float dip) {
        return getOneDipPixels() * dip;
    }

    public static float pixelToDip(float px) {
        return getOneDipPixels() / px;
    }

    public static float getOneDipPixels(Context context) {
        if (oneDipPixels == -1) {
            oneDipPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());
        }
        return oneDipPixels;
    }

    public static float getOneDipPixels() {
        if (oneDipPixels == -1) {
            throw new RuntimeException(
                    "You need to call Util.getOneDipPixels(Context context) once before calling it without the context."
            );
        } else {
            return oneDipPixels;
        }
    }

    public static int[] resize(int[] size, int[] max) {
        int newX;
        int newY;

        int oldX = size[0];
        int oldY = size[1];

        int maxX = max[0];
        int maxY = max[1];
        if (oldX < oldY) {
            newX = maxX;
            newY = oldY * (oldX / newX);
        } else {
            newY = maxY;
            newX = oldX * (oldY / newY);
        }

        return new int[]{newX, newY};
    }

    public static boolean randomBoolean() {
        return randomGenerator.nextBoolean();
    }

    public static void logToFile(String message, Throwable exception) {
        message += "Stacktrace:\n" + Log.getStackTraceString(exception);
        logToFile(message);
    }
    public static void logToFile(String message) {
        String logString = new Date().toString() +
                "App-Version: " + APP_VERSION_NAME + ": \n";
        logString += message;
        logString += "\n------------------------------------------------------\n";
        try {
            Files.append(logString, Util.newFile("AzubiLogErrors.txt"), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("hello my friend");
        }
        Log.d("LogToFile", logString);
    }
}
