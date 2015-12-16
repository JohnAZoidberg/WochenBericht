package de.struckmeierfliesen.ds.wochenbericht;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Dialog {
    public static void alert(Context context, String msg) {
        alert(context, msg, false);
    }

    public static void alert(Context context, String msg, boolean longLength) {
        Toast.makeText(context, msg,
                longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT
        ).show();
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
        if (message != null) builder.setMessage(message);
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

    public static void askForInput(@NonNull Context context, @StringRes int titleId, @StringRes int positiveId, @NonNull final Util.OnInputSubmitListener<String> onPositiveListener) {
        askForInput(context, context.getString(titleId), context.getString(positiveId), onPositiveListener);
    }

    public static void askForInput(@NonNull Context context, String title, String positive, @NonNull final Util.OnInputSubmitListener<String> onPositiveListener) {
        askForInput(context, title, positive, InputType.TYPE_CLASS_TEXT, onPositiveListener);
    }

    public static void askForInput(@NonNull Context context, String title, String positive, int inputType, @NonNull final Util.OnInputSubmitListener<String> onPositiveListener) {
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

    public static void selectImage(final Activity activity, final Entry entry, final Runnable runner) {
        final CharSequence[] items;
        if (runner != null && entry.getPicturePath() != null) {
            items = new String[]{
                    activity.getString(R.string.take_photo),
                    activity.getString(R.string.choose_from_library),
                    activity.getString(R.string.delete)
            };
        } else {
            items = new String[]{
                    activity.getString(R.string.take_photo),
                    activity.getString(R.string.choose_from_library)
            };
        }

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.add_photo));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals(activity.getString(R.string.take_photo))) {
                    Util.selectImageIntent(true, entry, activity);
                } else if (items[item].equals(activity.getString(R.string.choose_from_library))) {
                    Util.selectImageIntent(false, entry, activity);
                } else if (items[item].equals(activity.getString(R.string.delete))) {
                    runner.run();
                } else if (items[item].equals(activity.getString(R.string.cancel))) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    public static void selectImage(final Activity activity, final Entry entry) {
        selectImage(activity, entry, null);
    }
}
