package de.struckmeierfliesen.ds.wochenbericht;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import java.util.List;

public class ClientActivity extends ClientListFragment.ClientLoaderActivity {

    private DataBaseConnection dbConn;
    private static String FRAGMENT_TAG = "fragment_tag";
    private Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        // initialize DataBaseConnection
        dbConn = new DataBaseConnection(this);

        // set up Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if(supportActionBar != null) supportActionBar.setDisplayHomeAsUpEnabled(true);

        // prevent overlapping fragments
        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
            Log.d("Just a hint", "savedInstanceState != null");
            return;
        }

        Intent intent = getIntent();
        if (intent.hasExtra(ClientDetailsFragment.ARG_CLIENT)) {
            fragment = new ClientDetailsFragment();
            fragment.setArguments(getIntent().getExtras());
        } else {
            fragment = new ClientListFragment();
        }
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment, FRAGMENT_TAG).commit();
    }

    public String[] loadClients() {
        dbConn.open();
        List<String> allClients = dbConn.getAllClients();
        dbConn.close();
        return allClients.toArray(new String[allClients.size()]);
    }

    @Override
    List<Entry> loadEntries(String client) {
        dbConn.open();
        List<Entry> entries = dbConn.getEntriesForClient(client);
        dbConn.close();
        return entries;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Util.handlePictureResult(requestCode, resultCode, data, this)){
            if (fragment instanceof Updatable) {
                ((Updatable) fragment).update();
                Dialog.alert(this, "yayyyyyy");
            } else {
                Dialog.alert(this, "nahhhhhh");
            }
        }
    }

    interface Updatable {
        void update();
    }
}
