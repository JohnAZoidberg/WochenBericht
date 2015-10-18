package de.struckmeierfliesen.ds.wochenbericht;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
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
    private ArrayAdapter<String> installerAdapter;
    private BiMap<String, Integer> installers = HashBiMap.create();
    private ArrayList<String> installerStrings = new ArrayList<String>(); // TODO use BiMap instead
    private SelectAgainSpinner installerSpinner;
    private Spinner durationSpinner;
    private TextView changeDateButton;
    private Date date = new Date();
    private DataBaseConnection dbConn;
    private Button saveButton;

    private ViewPager mViewPager;
    private DemoCollectionPagerAdapter mDemoCollectionPagerAdapter;

    // -1 means editing is off and if editingId is on this variable holds the id of the entry being edited
    private int editingId = NOT_EDITING;

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
        changeDateButton.setText(DateFormat.format("dd.MM.yy", date) + "  ");
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
        //addX(clientEdit);
        //addX(workEdit);

        // set up number picker
        durationSpinner = (Spinner) findViewById(R.id.durationSpinner);
        ArrayList<String> durationStrings = new ArrayList<String>();
        // add dummy duration as description
        durationStrings.add(getResources().getString(R.string.duration));
        durationStrings.add("0:15 h");
        for(int i = 1; i <= 16; i++) {
            durationStrings.add(Util.convertDuration(i) + " h");
        }
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, durationStrings);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durationSpinner.setAdapter(durationAdapter);
        durationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                duration = position - 1; // -1 because of the
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // set up saveButton
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Entry entry = extractDataFromInputs();
                if(entry != null) {
                    if(editingId == NOT_EDITING)
                        addEntry(entry);
                    else
                        editEntry(entry);
                }
                stopEditing(entry != null);
            }
        });

        // set up Installer Spinner
        installerSpinner = (SelectAgainSpinner) findViewById(R.id.spinner);

        getInstallers();

        installerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, installerStrings);
        installerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        installerSpinner.setAdapter(installerAdapter);
        installerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getCount() == position + 1) { // last item
                    displayAddInstallerDialog();
                    installerId = position + 1;
                } else {
                    // -1 because the first element is a dummy elemnt which acts as a hint
                    if(position != 0) installerId = installers.get(installerAdapter.getItem(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        installerSpinner.setSelection(0);

        // ViewPager and its adapters use support library
        // fragments, so use getSupportFragmentManager.
        mDemoCollectionPagerAdapter = new DemoCollectionPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.listView);
        mViewPager.setAdapter(mDemoCollectionPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            int lastPosition = 0;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                stopEditing(false);
                setDate(Util.addToDate(date, lastPosition - position));
                lastPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    private EntryListFragment getCurrentFragment() {
        return mDemoCollectionPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
    }

    public Date getDate() {
        return date;
    }

    /*private void addX(final EditText et) {
        String value = "";
        et.setText(value);
        //final Drawable x = getResources().getDrawable(R.drawable.presence_offline);//your x image, this one from standard android images looks pretty good actually
        final Drawable x = ContextCompat.getDrawable(this, R);
        x.setBounds(0, 0, x.getIntrinsicWidth(), x.getIntrinsicHeight());
        et.setCompoundDrawables(null, null, value.equals("") ? null : x, null);
        et.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (et.getCompoundDrawables()[2] == null) {
                    return false;
                }
                if (event.getAction() != MotionEvent.ACTION_UP) {
                    return false;
                }
                if (event.getX() > et.getWidth() - et.getPaddingRight() - x.getIntrinsicWidth()) {
                    et.setText("");
                    et.setCompoundDrawables(null, null, null, null);
                }
                return false;
            }
        });
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                et.setCompoundDrawables(null, null, et.getText().toString().equals("") ? null : x, null);
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
    }*/

    private void getInstallers() {
        dbConn.open();
        installers = dbConn.getInstallers();
        dbConn.close();
        installerStrings.clear();
        installerStrings.add(getResources().getString(R.string.installer));
        for(String s : installers.keySet()) installerStrings.add(s);
        installerStrings.add(Util.ADD_INSTALLER); // add dummy installer which acts as a button
    }

    private void addEntry(Entry entry) {
        dbConn.open();
        if(entry != null) {
            entry.id = dbConn.saveEntry(entry);
            getCurrentFragment().addEntry(entry, 0);
        }
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
                //Util.alert(this, "So true, amenakoi!!");
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=zCfm-vWuQRk")));
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
        if(client.isEmpty()
                || work.isEmpty()
                //|| installers.size() == 0
                || duration == -1
                || installerId == -1) {
            Util.alert(this, getString(R.string.please_enter_input));
            return null;
        }

        Entry entry = new Entry(client, this.date, duration, installerId, work);
        entry.installer = installers.inverse().get(installerId);
        entry.id = editingId;
        return entry;
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

    public void startEditing(Entry entry) {
        clientEdit.setText(entry.client);
        workEdit.setText(entry.work);
        saveButton.setText(getString(R.string.save));
        durationSpinner.setSelection(entry.duration + 1); // +1 because of dummy duration which acts as placeholder
        int position = installerStrings.indexOf(entry.installer);
        installerSpinner.setSelection(position); // +1 because of dummy duration which acts as placeholder
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
        changeDateButton.setText(DateFormat.format("dd.MM.yy", date) + "  ");
    }



    // Since this is an object collection, use a FragmentStatePagerAdapter,
    // and NOT a FragmentPagerAdapter.
    public class DemoCollectionPagerAdapter extends FragmentStatePagerAdapter {
        SparseArray<EntryListFragment> registeredFragments = new SparseArray<EntryListFragment>();

        public DemoCollectionPagerAdapter(FragmentManager fm) {
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
