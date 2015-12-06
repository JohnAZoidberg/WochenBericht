package de.struckmeierfliesen.ds.wochenbericht;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.BindingAdapter;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Util {
    public static int lastEntryPictureClicked = -1;
    public static final String TEMP_IMAGE = "azubiLogTemp.jpg";

    public static void alert(Context context, String msg) {
        alert(context, msg, false);
    }
    public static void alert(Context context, String msg, boolean longLength) {
        Toast.makeText(context, msg,
                longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
        ).show();
    }

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

    public static String oldConversion(int duration) {
        String divider = ":";
        if(duration == -1) return "0" + divider + "00";
        if(duration == 0) return "0" + divider + "15";
        double i = (double) duration;
        double hours = Math.floor(i / 2d);
        String minutes = (i % 2 == 0) ? "00" : "30";
        return (int) hours + divider + minutes;
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

    public static void askForConfirmation(@NonNull Context context, @StringRes int titleId, @NonNull final View.OnClickListener onPositiveListener) {
        askForConfirmation(context, titleId, -1, onPositiveListener);
    }

    public static void askForConfirmation(@NonNull Context context, @StringRes int titleId, @StringRes int messageId, @NonNull final View.OnClickListener onPositiveListener) {
        String messageString = (messageId == -1) ? null : context.getString(messageId);
        askForConfirmation(context, context.getString(titleId), messageString, onPositiveListener);
    }

    public static void askForConfirmation(@NonNull Context context, String title, @Nullable String message, @NonNull final View.OnClickListener onPositiveListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton(context.getString(R.string.yes), null)
                .setNegativeButton(context.getString(R.string.no), null);
        if(message != null) builder.setMessage(message);
        final AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface d) {
                Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onPositiveListener.onClick(v);
                        d.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    public static void askForInput(@NonNull Context context, @StringRes int titleId, @StringRes int positiveId, @NonNull final OnInputSubmitListener onPositiveListener) {
        askForInput(context, context.getString(titleId), context.getString(positiveId), onPositiveListener);
    }

    public static void askForInput(@NonNull Context context, String title, String positive, @NonNull final OnInputSubmitListener<String> onPositiveListener) {
        askForInput(context, title, positive, InputType.TYPE_CLASS_TEXT, onPositiveListener);
    }

    public static void askForInput(@NonNull Context context, String title, String positive, int inputType, @NonNull final OnInputSubmitListener<String> onPositiveListener) {
        final EditText input = new EditText(context);
        input.setInputType(inputType);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(positive, null)
                .setNegativeButton(context.getString(R.string.cancel), null).create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(final DialogInterface d) {
                Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onPositiveListener.onSubmit(v, input.getText().toString().trim());
                        d.dismiss();
                    }
                });
            }
        });
        dialog.show();
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

    public static File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "AzubiApp_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
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

    public static void selectImage(final Activity activity, final Entry entry) {
        final CharSequence[] items = { activity.getString(R.string.take_photo), activity.getString(R.string.choose_from_library), activity.getString(R.string.cancel) };

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.add_photo));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals(activity.getString(R.string.take_photo))) {
                    selectImageIntent(true, entry, activity);
                } else if (items[item].equals(activity.getString(R.string.choose_from_library))) {
                    selectImageIntent(false, entry, activity);
                } else if (items[item].equals(activity.getString(R.string.cancel))) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    public static void selectImageIntent(boolean fromCamera, Entry entry, Activity activity) {
        lastEntryPictureClicked = entry.id;
        if (fromCamera) {
            /*String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "AzubiLog_" + entry.id + "_" + timeStamp + ".png";
            File image = Util.newPictureFile(fileName);
            Uri uriSavedImage = Uri.fromFile(image);*/

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
