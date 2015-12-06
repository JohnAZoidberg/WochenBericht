package de.struckmeierfliesen.ds.wochenbericht;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TimePicker;

import com.google.common.io.Files;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Calendar;

public class SettingsActivity extends AppCompatActivity {

    private EditText editName;
    private EditText editFirstName;
    private Button setTimeButton;
    private Switch reminderSwitch;
    private SharedPreferences sharedPrefs;
    private int hourOfDay = 12;
    private int minute = 0;

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPrefs = getSharedPreferences(
                "de.struckmeierfliesen.ds.wochenbericht.SETTINGS", Context.MODE_PRIVATE);

        // set up name inputs
        editFirstName = (EditText) findViewById(R.id.editFirstName);
        editName = (EditText) findViewById(R.id.editName);
        View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    saveNames();
                }
            }
        };
        editFirstName.setOnFocusChangeListener(onFocusChangeListener);
        editName.setOnFocusChangeListener(onFocusChangeListener);

        // set up reminder stuff
        setTimeButton = (Button) findViewById(R.id.setTimeButton);
        setTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new TimePickerFragment();
                newFragment.show(getSupportFragmentManager(), "timePicker");
            }
        });

        reminderSwitch = (Switch) findViewById(R.id.reminderSwitch);
        reminderSwitch.setChecked(true);
        reminderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                switchAlarmOnOff(isChecked);
            }
        });
        loadNotifSettings();

        final DataBaseConnection dbConn = new DataBaseConnection(this);
        // set up Database controls
        Button exportButton = (Button) findViewById(R.id.exportButton);
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbConn.open();
                String databaseJsonString = dbConn.exportDatabase();
                dbConn.close();
                try {
                    Files.write(databaseJsonString, Util.newFile("azubilogDB.json"), Charset.forName("UTF-8"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Button importButton = (Button) findViewById(R.id.importButton);
        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                de.struckmeierfliesen.ds.wochenbericht.Dialog.askForConfirmation(getApplicationContext(),
                        getString(R.string.really_import), getString(R.string.import_explanation), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String jsonString = null;
                                try {
                                    jsonString = Files.toString(Util.newFile("azubilogDB.json"), Charset.forName("UTF-8"));
                                    DataBaseConnection.dropDatabase(SettingsActivity.this);
                                    dbConn.open();
                                    dbConn.importDatabase(jsonString);
                                    dbConn.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
            }
        });
        Button dropDbButton = (Button) findViewById(R.id.dropDbButton);
        dropDbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataBaseConnection.dropDatabase(SettingsActivity.this);
            }
        });

        // TODO to be deleted in future versions
        Button updateButton = (Button) findViewById(R.id.updateButton);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                de.struckmeierfliesen.ds.wochenbericht.Dialog.askForConfirmation(SettingsActivity.this, "Datenbank wirklich updaten?",
                        "Du darfst die Datenbank nur einmal updaten und das auch nur," +
                                "wenn nach dem App Update deine Zeiten" +
                                "der Dauer für die Einträge nicht mehr stimmen!",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dbConn.open();
                                dbConn.upgradeDurations();
                                dbConn.close();
                                de.struckmeierfliesen.ds.wochenbericht.Dialog.alert(getApplicationContext(), "Update durchgeführt!");
                            }
                        });
            }
        });
    }

    private void switchAlarmOnOff(boolean switchOn) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean("remind", switchOn);
        editor.apply();
        enableAlarm(switchOn);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNames();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNames();
    }

    private void loadNotifSettings() {
        boolean remind = sharedPrefs.getBoolean("remind", false);
        reminderSwitch.setChecked(remind);
        hourOfDay = sharedPrefs.getInt("hourOfDay", 12);
        minute = sharedPrefs.getInt("minute", 0);
        setTimeButton.setText(getString(R.string.timeDivider, hourOfDay, ((minute < 10) ? "0" : "") + minute));
        setTimeButton.setVisibility(remind ? View.VISIBLE : View.GONE);
    }

    private void enableAlarm(boolean enable) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        Intent notificationIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        if (enable) {
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
            de.struckmeierfliesen.ds.wochenbericht.Dialog.alert(this, "Alarm set at " + Util.formatDate(cal.getTime()));
            setTimeButton.setVisibility(View.VISIBLE);
        } else {
            de.struckmeierfliesen.ds.wochenbericht.Dialog.alert(this, "Alarm disabled!");
            setTimeButton.setVisibility(View.GONE);
        }
    }

    public static void disableAlarm(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                "de.struckmeierfliesen.ds.wochenbericht.SETTINGS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean("remind", true);
        editor.apply();

        Intent notificationIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        de.struckmeierfliesen.ds.wochenbericht.Dialog.alert(context, "Alarm disabled!");
    }

    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    true);//DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            ((SettingsActivity) getActivity()).setTime(hourOfDay, minute);
        }
    }

    public void setTime(int hourOfDay, int minute) {
        this.hourOfDay = hourOfDay;
        this.minute = minute;
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt("hourOfDay", hourOfDay);
        editor.putInt("minute", minute);
        editor.putBoolean("remind", true);
        editor.apply();
        setTimeButton.setText(getString(R.string.timeDivider, hourOfDay, (minute == 0) ? "00" : minute));
        reminderSwitch.setChecked(true);
        enableAlarm(true);
    }

    private void saveNames() {
        String firstName = editFirstName.getText().toString().trim();
        String name = editName.getText().toString().trim();

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString("firstName", firstName);
        editor.putString("name", name);
        editor.apply();
    }

    private void loadNames() {
        String firstName = sharedPrefs.getString("firstName", "");
        String name = sharedPrefs.getString("name", "");
        editFirstName.setText(firstName);
        editName.setText(name);
    }

}
