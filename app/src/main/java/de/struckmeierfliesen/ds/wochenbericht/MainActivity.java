package de.struckmeierfliesen.ds.wochenbericht;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.apache.pdfbox.exceptions.COSVisitorException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int SET_ENTRY_DATE = 0;
    private static final int SET_SHOWN_DATE = 1;
    private static final int NOT_EDITING = -1;
    private EditText clientEdit;
    private EditText workEdit;
    private int duration = -1;
    private int installerId = -1;
    private DeletableArrayAdapter<String> installerAdapter;
    private BiMap<String, Integer> installers = HashBiMap.create();
    private ArrayList<String> installerStrings = new ArrayList<String>(); // TODO use BiMap instead
    private SelectAgainSpinner installerSpinner;
    private Spinner durationSpinner;
    private TextView changeDateButton;
    private Date date = new Date();
    private DataBaseConnection dbConn;
    private Button saveButton;
    private TextView totalDurationView;

    private ViewPager mViewPager;
    private DayAdapter mDemoCollectionPagerAdapter;

    // -1 means editing is off and if editingId is on this variable holds the id of the entry being edited
    private int editingId = NOT_EDITING;
    private Entry avgEntry = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // initialize DataBaseConnection
        dbConn = new DataBaseConnection(this);

        // set up Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if(supportActionBar != null) supportActionBar.setDisplayShowTitleEnabled(false);
        changeDateButton = (TextView) findViewById(R.id.changeDate);
        changeDateButton.setText(Util.getDayAbbrev(date) + " " + DateFormat.format("dd.MM.yy", date) + "  ");
        changeDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dateFragment = new DatePickerFragment();
                Bundle args = new Bundle();
                args.putIntArray("date", Util.extractIntFromDate(MainActivity.this.date));
                dateFragment.setArguments(args);
                dateFragment.show(getSupportFragmentManager(), "datePicker");
            }
        });

        // findViews
        saveButton = (Button) findViewById(R.id.button);
        clientEdit = (EditText) findViewById(R.id.editClient);
        workEdit = (EditText) findViewById(R.id.editWork);
        totalDurationView = (TextView) findViewById(R.id.totalHours);

        // preset installer and duration
        avgEntry = loadAverageEntry();

        // set up number picker
        durationSpinner = (Spinner) findViewById(R.id.durationSpinner);
        ArrayList<String> durationStrings = new ArrayList<String>();
        // add dummy duration as description
        if(avgEntry == null) durationStrings.add(getResources().getString(R.string.duration));
        durationStrings.add("0:15 h");
        for(int i = 2; i <= 4*9; i+=2) {
            durationStrings.add(Util.convertDuration(i) + " h");
        }
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, durationStrings);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(durationAdapter);
        durationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // -1 because the first element is a dummy element which acts as a hint
                // if there have not been previous entries (avgEntry == null)
                int actualPosition = 1 + position - ((avgEntry == null) ? 1 : 0);
                if (actualPosition == 1 || actualPosition == 2) duration = actualPosition;
                else {
                    duration = (actualPosition  * 2) - 2;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        if(avgEntry != null) setDuration(avgEntry.duration);

        // set up saveButton
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Entry entry = extractDataFromInputs();
                if (entry != null) {
                    if (editingId == NOT_EDITING)
                        addEntry(entry);
                    else
                        editEntry(entry);
                }
                stopEditing(entry != null);
            }
        });

        // set up Installer Spinner
        installerSpinner = (SelectAgainSpinner) findViewById(R.id.spinner);

        getInstallers(avgEntry == null); // TODO if theres no installers and avgEntry == null there wont be any hint
        installerAdapter = new DeletableArrayAdapter<String>(this,
                R.layout.spinner_item_deletable, R.id.spinnerText/*android.R.layout.simple_spinner_item*/, installerStrings);
        installerAdapter.setDeleteListener(new DeletableArrayAdapter.DeleteListener() {
            @Override
            public void onDelete(int position, View view) {
                String installer = installerStrings.get(position);
                askForInstallerDeleteConfirmation(installer);
            }
        });
        //installerAdapter.setDropDownViewResource(R.layout.spinner_item_deletable);//android.R.layout.simple_spinner_dropdown_item);
        installerSpinner.setAdapter(installerAdapter);
        installerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getCount() == position + 1) { // last item
                    displayAddInstallerDialog();
                    // +1 because the first element is a dummy element which acts as a hint
                    // if there have not been previous entries (avgEntry == null)
                    //installerId = position + ((avgEntry == null) ? 1 : 0);
                } else {
                    if (position != 0)
                        installerId = installers.get(installerAdapter.getItem(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        //TODO installerSpinner.setSelection(0);
        if(avgEntry != null) {
            setInstallerById(avgEntry.installerId);
            installerId = avgEntry.installerId;
        }

        mDemoCollectionPagerAdapter = new DayAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.listView);
        mViewPager.setAdapter(mDemoCollectionPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            int lastPosition = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                int hours = getCurrentFragment().getTotalHours();
                setTotalDuration(hours);

                stopEditing(false);
                setDate(Util.addDays(date, lastPosition - position));
                lastPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*//createReport();
                dbConn.open();
                dbConn.renameInstaller("Holger lange", "Holger Lange");
                //dbConn.upgradeDurations();
                dbConn.close();*/
                Util.alert(MainActivity.this, "nein!");
            }
        });
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
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_generate:
                createReport();
        }

        return super.onOptionsItemSelected(item);
    }

    // own methods

    private class AsyncReportCreator extends AsyncTask<String, String, String> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(MainActivity.this, getResources().getString(R.string.generate_summary),
                getString(R.string.loading_please_wait), true, false);
        }

        @Override
        protected void onPostExecute(String fileName) {
            dialog.cancel();

            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(Util.newFile(fileName)), "application/pdf");
            startActivity(intent);
        }

        @Override
        protected String doInBackground(String... params) {
            if(params.length != 3) return null;
            String comment = params[0];
            String pageNumber = params[1];
            String yearNumber = params[2];
            try {
                ReportGenerator generator = new ReportGenerator(MainActivity.this);
                generator.fillIn(date, comment, pageNumber, yearNumber);
            } catch (IOException | COSVisitorException e) {
                e.printStackTrace();
            }
            return "newPDF.pdf";
        }
    }

    private void createReport() {
        View view = getLayoutInflater().inflate(R.layout.dialog_create_report, null);
        final EditText editComment = (EditText) view.findViewById(R.id.editComment);
        final EditText editPageNumber = (EditText) view.findViewById(R.id.editPageNumber);
        final EditText editYearNumber = (EditText) view.findViewById(R.id.editYearNumber);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.generate_summary)) // TODO translate
                .setView(view)
                .setPositiveButton(getResources().getString(R.string.generate_summary), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (editComment.length() > 2 * ReportGenerator.CUTOFF) {
                            Util.alert(MainActivity.this, getString(R.string.comment_too_long));
                            return;
                        }
                        AsyncReportCreator runner = new AsyncReportCreator();
                        runner.execute(editComment.getText().toString(),
                                editPageNumber.getText().toString(),
                                editYearNumber.getText().toString());
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();
        dialog.show();
    }

    private Entry loadAverageEntry() {
        dbConn.open();
        Entry entry = dbConn.loadAverageEntry();
        dbConn.close();
        return entry;
    }

    private EntryListFragment getCurrentFragment() {
        return mDemoCollectionPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
    }

    public Date getDate() {
        return date;
    }

    private void getInstallers(boolean addDummy) {
        dbConn.open();
        installers = dbConn.getInstallers();
        dbConn.close();
        installerStrings.clear();
        if(addDummy) installerStrings.add(getResources().getString(R.string.installer));
        for(String s : installers.keySet()) installerStrings.add(s);
        installerStrings.add(getString(R.string.add_installer)); // add dummy installer which acts as a button
    }

    private void addEntry(Entry entry) {
        dbConn.open();
        if(entry != null) {
            entry.id = dbConn.saveEntry(entry);
            getCurrentFragment().addEntry(entry, 0);
        }
        dbConn.close();
    }

    private Entry extractDataFromInputs() {
        // extract inputs
        String clientName = clientEdit.getText().toString().trim();
        String workString = workEdit.getText().toString().trim();

        if(workString.length() == 0 || clientName.length() == 0 || duration == -1 || installerId == -1) {
            Util.alert(this, getString(R.string.please_enter_input));
            return null;
        }

        Entry entry = new Entry(clientName, this.date, duration, installerId, workString);
        entry.installer = installers.inverse().get(installerId);
        entry.id = editingId;
        return entry;
    }

    public void setDuration(int duration) {
        // +1 because of dummy duration which acts as placeholder
        // if there have not been previous entries (avgEntry == null)
        int SPINNER_LIST_CONTANT = ((avgEntry == null) ? 1 : 0) - 1;
        if (duration == 1 || duration == 2) {
            durationSpinner.setSelection(duration + SPINNER_LIST_CONTANT);
        } else {
            int position = (2 + duration) / 2;
            durationSpinner.setSelection(position + SPINNER_LIST_CONTANT);
        }
    }

    public void deleteEntry(Entry entry) {
        askForEntryDeleteConfirmation(entry);
    }

    public void setTotalDuration(int durationCode) {
        String duration = Util.convertDuration(durationCode);
        if (durationCode >=88 ) Util.alert(this, getString(R.string.over_twentyfour));
        else if (durationCode >=48 ) Util.alert(this, getString(R.string.over_twelve));
        totalDurationView.setText(getResources().getQuantityString(
                R.plurals.xHours, duration.equals("1:00") ? 1 : 2, duration));
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int[] dateArray = getArguments().getIntArray("date");
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year;
            int month;
            int day;
            if(dateArray != null) {
                day = dateArray[0];
                month = dateArray[1];
                year = dateArray[2];
            } else {
                year = c.get(Calendar.YEAR);
                month = c.get(Calendar.MONTH);
                day = c.get(Calendar.DAY_OF_MONTH);
            }

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day);
            Date date = calendar.getTime();
            int dayDifference = Util.getDayDifference(new Date(), date);
            if(dayDifference >= 0) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.mViewPager.setCurrentItem(dayDifference);
            } else {
                Util.alert(getContext(), getResources().getString(R.string.only_present));
            }
        }
    }

    public void displayAddInstallerDialog() {
        Util.askForInput(this, R.string.add_installer, R.string.add, new Util.OnInputSubmitListener<String>() {
            @Override
            public void onSubmit(View v, String input) {
                if(input.isEmpty()) {
                    Util.alert(MainActivity.this, getString(R.string.please_enter_input));
                    return;
                }
                MainActivity.this.addInstaller(input);
            }
        });
    }

    public void askForEntryDeleteConfirmation(final Entry entry) {
        Util.askForConfirmation(this, R.string.really_delete, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopEditing(false);
                getCurrentFragment().deleteEntry(entry);
            }
        });
    }

    public void askForInstallerDeleteConfirmation(final String installer) {
        Util.askForConfirmation(this, R.string.really_delete, R.string.all_installers_will_be_deleted, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int installerId = installers.get(installer);
                dbConn.open();
                boolean deleted = dbConn.deleteInstaller(installerId);
                dbConn.close();
                if (deleted) {
                    installers.remove(installer);
                    installerAdapter.remove(installer);
                    installerAdapter.notifyDataSetChanged();
                    // TODO delete all entries of other fragments
                    getCurrentFragment().updateEntries();
                }
                Util.alert(MainActivity.this, "Installer " + installer + (deleted ? " un" : " ") + "sucessfully deleted!");
            }
        });
    }

    private void addInstaller(String installer) {
        dbConn.open();
        installerId = dbConn.addInstaller(installer);
        dbConn.close();
        // add installer to all lists and maps
        installers.put(installer, installerId);
        installerStrings.add(installer);
        // Remove last item and add it again
        installerAdapter.remove(getString(R.string.add_installer));
        installerAdapter.add(getString(R.string.add_installer));
        // Update installerAdapter
        installerAdapter.notifyDataSetChanged();
    }

    public void startEditing(Entry entry) {
        clientEdit.setText(entry.client);
        workEdit.setText(entry.work);
        saveButton.setText(getString(R.string.save));
        setDuration(entry.duration);
        setInstaller(entry.installer);
        editingId = entry.id;
    }

    private void editEntry(Entry entry) {
        dbConn.open();
        dbConn.editEntry(entry);
        dbConn.close();
        getCurrentFragment().editEntry(entry);
    }

    private void stopEditing(boolean clearEdits) {
        if(clearEdits) {
            clientEdit.setText("");
            workEdit.setText("");
        }
        saveButton.setText(getString(R.string.add));
        editingId = NOT_EDITING;
    }

    private void setDate(Date date) {
        this.date = date;
        changeDateButton.setText(Util.getDayAbbrev(date) + " " + DateFormat.format("dd.MM.yy", date) + "  ");
    }

    public void setInstaller(String name) {
        installerSpinner.setSelection(installerStrings.indexOf(name));
    }

    public void setInstallerById(int installerId) {
        installerSpinner.setSelection(installerStrings.indexOf(installers.inverse().get(installerId)));
    }

    public class DayAdapter extends FragmentStatePagerAdapter {
        SparseArray<EntryListFragment> registeredFragments = new SparseArray<EntryListFragment>();

        public DayAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return new EntryListFragment();
        }

        @Override
        public int getCount() {
            return 100;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            EntryListFragment fragment = (EntryListFragment) super.instantiateItem(container, position);
            Bundle args = new Bundle();
            args.putInt("position", position);
            fragment.setArguments(args);
            registeredFragments.put(position, fragment);
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public EntryListFragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }
    }
}
