package de.struckmeierfliesen.ds.wochenbericht;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

    EditText editName;
    EditText editFirstName;
    SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPrefs = getSharedPreferences(
                "de.struckmeierfliesen.ds.wochenbericht.SETTINGS", Context.MODE_PRIVATE);

        editFirstName = (EditText) findViewById(R.id.editFirstName);
        editName = (EditText) findViewById(R.id.editName);
        View.OnFocusChangeListener onFocusChangeListener =  new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    saveNames();
                }
            }
        };
        editFirstName.setOnFocusChangeListener(onFocusChangeListener);
        editName.setOnFocusChangeListener(onFocusChangeListener);
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

    private void saveNames() {
        String firstName = editFirstName.getText().toString();
        String name = editName.getText().toString();

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
