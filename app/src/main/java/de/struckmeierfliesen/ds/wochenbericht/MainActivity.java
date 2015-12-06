package de.struckmeierfliesen.ds.wochenbericht;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.apache.pdfbox.exceptions.COSVisitorException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static android.provider.MediaStore.Images.Media;

public class MainActivity extends AppCompatActivity {

    public static final int SELECT_FILE = 0;
    public static final int REQUEST_CAMERA = 1;

    private static final int SET_ENTRY_DATE = 0;
    private static final int SET_SHOWN_DATE = 1;
    private static final int NOT_EDITING = -1;
    private AutoCompleteTextView clientEdit;
    private EditText workEdit;
    private int duration = -1;
    private int installerId = -1;
    private DeletableArrayAdapter<String> installerAdapter;
    private BiMap<String, Integer> installers = HashBiMap.create();
    private List<String> installerStrings = new ArrayList<>(); // TODO use BiMap instead
    private SelectAgainSpinner installerSpinner;
    private Spinner durationSpinner;
    private TextView changeDateButton;
    private Date date = new Date();
    private DataBaseConnection dbConn;
    private Button saveButton;
    private TextView totalDurationView;

    private ViewPager dayViewPager;
    private DayAdapter dayAdapter;
    ViewPager.OnPageChangeListener onPageChangeListener;

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
        setDate(date);
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
        clientEdit = (AutoCompleteTextView) findViewById(R.id.editClient);
        workEdit = (EditText) findViewById(R.id.editWork);
        totalDurationView = (TextView) findViewById(R.id.totalHours);

        // preset installer and duration
        avgEntry = loadAverageEntry();

        // set up number picker
        durationSpinner = (Spinner) findViewById(R.id.durationSpinner);
        List<String> durationStrings = new ArrayList<String>();
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

        dayAdapter = new DayAdapter(getSupportFragmentManager());
        dayViewPager = (ViewPager) findViewById(R.id.listView);
        dayViewPager.setAdapter(dayAdapter);
        dayViewPager.setCurrentItem(DayAdapter.DAY_FRAGMENTS / 2);
        onPageChangeListener = new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                stopEditing(false);

                EntryListFragment registeredFragment = dayAdapter.getRegisteredFragment(position);
                if (registeredFragment == null) {
                    Log.d("Testing", "registeredFragment == null in onPageSelected (MainActivity)");
                    return;
                }

                int hours = registeredFragment.getTotalHours();
                setTotalDuration(hours);

                Date selectedDate = registeredFragment.getDate();
                setDate(selectedDate);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };
        dayViewPager.addOnPageChangeListener(onPageChangeListener);
        //dayViewPager.setCurrentItem(50);

        // set up clientEdit

        List<String> clients = getClients();
        ArrayAdapter<String> clientEditAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, clients);
        clientEdit.setAdapter(clientEditAdapter);

        // set up Fab which is only to be used while debugging

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*//createReport();
                dbConn.open();
                dbConn.renameInstaller("Holger lange", "Holger Lange");
                //dbConn.upgradeDurations();
                dbConn.close();*/
                /*Util.askForInput(MainActivity.this, "Which page?", "Change to", InputType.TYPE_CLASS_NUMBER, new Util.OnInputSubmitListener<String>() {
                    @Override
                    public void onSubmit(View v, String input) {
                        dayViewPager.setCurrentItem(Integer.parseInt(input));
                    }
                });*/

                //SettingsActivity.disableAlarm(MainActivity.this);
                //Util.alert(MainActivity.this, "Alarm cancelled!");


                Intent showPicIntent = new Intent(MainActivity.this, PictureViewerActivity.class);
                showPicIntent.putExtra("fileName", Util.newPictureFile("statistics.png").getAbsolutePath());
                showPicIntent.putExtra("title", "My Statistics");
                startActivity(showPicIntent);
            }
        });

        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dayViewPager.setCurrentItem(dayViewPager.getCurrentItem() + 1);
                dayViewPager.setCurrentItem(dayViewPager.getCurrentItem());
                return true;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && Util.lastEntryPictureClicked != -1) {
            if (requestCode == REQUEST_CAMERA) {
                String picturePath = null;

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
                        picturePath = file.getAbsolutePath();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Util.addPictureToEntry(dbConn, Util.lastEntryPictureClicked, picturePath);
            } else if (requestCode == SELECT_FILE) {
                Uri selectedImageUri = data.getData();
                String picturePath = getPath(selectedImageUri);
                Util.addPictureToEntry(dbConn, Util.lastEntryPictureClicked, picturePath);
            }
            reloadDayFragments();
        }
        Util.lastEntryPictureClicked = -1;
    }

    // own methods

    private void reloadDayFragments() {
        // TODO use different method to reload all pages
        dayViewPager.setAdapter(dayAdapter);
        dayViewPager.addOnPageChangeListener(null);
        showDate(date);
        dayViewPager.addOnPageChangeListener(onPageChangeListener);
    }

    public String getPath(Uri uri) {
        String res = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    private String getLastImagePath() {
        String photoPath = null;
        Cursor cursor = getContentResolver().query(Media.EXTERNAL_CONTENT_URI, new String[]{Media.DATA, Media.DATE_ADDED, MediaStore.Images.ImageColumns.ORIENTATION}, Media.DATE_ADDED, null, "date_added ASC");
        if( cursor != null && cursor.moveToFirst()) {
            do {
                Uri uri = Uri.parse(cursor.getString(cursor.getColumnIndex(Media.DATA)));
                photoPath = uri.toString();
            } while (cursor.moveToNext());
            cursor.close();
        }
        return photoPath;
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }

    private List<String> getClients() {
        dbConn.open();
        List<String> allClients = dbConn.getAllClients();
        dbConn.close();
        return allClients;
    }

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
                        runner.execute(editComment.getText().toString().trim(),
                                editPageNumber.getText().toString().trim(),
                                editYearNumber.getText().toString().trim());
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
        return dayAdapter.getRegisteredFragment(dayViewPager.getCurrentItem());
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
            if (dateArray != null) {
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
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.showDate(date);
        }
    }

    private void showDate(Date date) {
        int dayDifference = Util.getDayDifference(new Date(), date);
        if (Math.abs(dayDifference) < 50) {
            dayViewPager.setCurrentItem(DayAdapter.DAY_FRAGMENTS / 2 - dayDifference);
        } else {
            Util.alert(this, getString(R.string.fifty_day_limit));
        }
    }

    public void displayAddInstallerDialog() {
        Util.askForInput(this, R.string.add_installer, R.string.add, new Util.OnInputSubmitListener<String>() {
            @Override
            public void onSubmit(View v, String input) {
                if (input.isEmpty()) {
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
                    installerAdapter.remove(installer);
                    installerAdapter.notifyDataSetChanged();
                    reloadDayFragments();
                }
                Util.alert(MainActivity.this, "Installer " + installer + (deleted ? " " : " un") + "sucessfully deleted!");
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
        changeDateButton.setText(new StringBuilder().append(Util.getDayAbbrev(date)).append(" ").append(DateFormat.format("dd.MM.yy", date)).append("  ").toString());
        if (! Util.isSameDay(new Date(), date)) {
            changeDateButton.setTextColor(0xFFFFFFFF);
        } else {
            changeDateButton.setTextColor(ContextCompat.getColor(this, R.color.colorPrimaryFont));
        }
    }

    public void setInstaller(String name) {
        installerSpinner.setSelection(installerStrings.indexOf(name));
    }

    public void setInstallerById(int installerId) {
        installerSpinner.setSelection(installerStrings.indexOf(installers.inverse().get(installerId)));
    }

}
