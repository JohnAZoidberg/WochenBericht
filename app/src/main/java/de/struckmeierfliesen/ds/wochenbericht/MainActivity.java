package de.struckmeierfliesen.ds.wochenbericht;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private Date date = new Date();
    private EditText dateView;
    private EditText clientEdit;
    private EditText workEdit;
    private int duration = 0;
    private int installerId = 0;
    private ArrayAdapter<String> installerAdapter;
    private EntryListAdapter entryListAdapter;
    private BiMap<String, Integer> installers = HashBiMap.create();
    private ArrayList<String> installerStrings = new ArrayList<String>(); // TODO use BiMap instead

    private DataBaseConnection dbConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        dbConn = new DataBaseConnection(this);

        // findViews
        Button submitButton = (Button) findViewById(R.id.button);
        dateView = (EditText) findViewById(R.id.editDate);
        clientEdit = (EditText) findViewById(R.id.editClient);
        workEdit = (EditText) findViewById(R.id.editWork);

        // set up number picker
        Spinner durationSpinner = (Spinner) findViewById(R.id.durationSpinner);
        ArrayList<String> durationStrings = new ArrayList<String>();
        durationStrings.add("0:15");
        for(int i = 1; i <= 16; i++) {
            durationStrings.add(Util.convertDuration(i));
        }
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, durationStrings);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(durationAdapter);
        durationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                duration = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // set up DateView
        updateDateView();
        dateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dateFragment = new DatePickerFragment();
                dateFragment.show(getSupportFragmentManager(), "datePicker");
            }
        });

        // set up DurationView
        //updateDurationView();
        // TODO DurationPicker

        // set up Button
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Entry entry = extractDataFromInputs();
                addEntry(entry);
            }
        });

        // set up FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String displayString = "";
                ArrayList<Entry> entries = getEntries();
                for(Entry entry : entries) {
                    displayString += entry.toString() + "\n";
                }
                Util.alert(view.getContext(), displayString);
            }
        });

        // set up Installer Spinner
        SelectAgainSpinner installerSpinner = (SelectAgainSpinner) findViewById(R.id.spinner);

        getInstallers();

        installerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, installerStrings);
        installerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        installerSpinner.setAdapter(installerAdapter);
        installerSpinner.setSelection(0);
        installerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getCount() == position + 1) {// last item
                    displayAddInstallerDialog();
                    installerId = position + 1;
                } else {
                    installerId = installers.get(installerAdapter.getItem(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //set up RecyclerView
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.listView);
        entryListAdapter = new EntryListAdapter(getEntries());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(entryListAdapter);
    }

    private ArrayList<Entry> getEntries() {
        dbConn.open();
        ArrayList<Entry> entries = dbConn.getEntriesWithInstaller();
        dbConn.close();
        return entries;
    }

    private void getInstallers() {
        dbConn.open();
        installers = dbConn.getInstallers();
        dbConn.close();
        for(String s : installers.keySet()) installerStrings.add(s);
        installerStrings.add(Util.ADD_INSTALLER); // add dummy installer which acts as a button
    }

    private void addEntry(Entry entry) {
        dbConn.open();
        if(entry != null) dbConn.saveEntry(entry);
        entryListAdapter.addEntry(entry, 0);
        dbConn.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // own methods

    private Entry extractDataFromInputs() {
        // extract inputs
        String client = clientEdit.getText().toString();
        String work = workEdit.getText().toString();

        // check if inputs are valid
        if(client.isEmpty() || work.isEmpty() || installers.size() == 0) {
            Util.alert(this, getString(R.string.please_enter_input));
            return null;
        }

        // clear inputs
        workEdit.setText("");
        clientEdit.setText("");

        Entry entry = new Entry(client, this.date, duration, installerId, work);
        entry.installer = installers.inverse().get(installerId);
        return entry;
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day);
            MainActivity activity = (MainActivity) getActivity();
            activity.date = calendar.getTime();
            activity.updateDateView();
            // TODO Util.alert(getContext(), day + "." + month + "." + (year - 2000));
        }
    }

    public void displayAddInstallerDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_installer))
                .setView(input)
                .setPositiveButton(getString(R.string.add), null)
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface d) {

                Button b = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        String installer = input.getText().toString();
                        MainActivity.this.addInstaller(installer);
                        dialog.dismiss();
                    }
                });
            }
        });
        dialog.show();
    }

    private void addInstaller(String installer) {
        dbConn.open();
        dbConn.addInstaller(installer);
        dbConn.close();
        // add installer to all lists and maps
        installers.put(installer, installerId);
        installerStrings.add(installer);
        // Remove last item and add it again
        installerAdapter.remove(Util.ADD_INSTALLER);
        installerAdapter.add(Util.ADD_INSTALLER);
        // Update installerAdapter
        installerAdapter.notifyDataSetChanged();
    }

    private void updateDateView() {
        dateView.setText(DateFormat.format("dd.MM.yy", this.date));
    }
}
