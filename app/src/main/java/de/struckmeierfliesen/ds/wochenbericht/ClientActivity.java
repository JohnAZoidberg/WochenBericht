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
        if (intent.hasExtra(ClientDetailsFragment.ARG_CLIENT_NAME)) {
            fragment = new ClientDetailsFragment();
            fragment.setArguments(getIntent().getExtras());
        } else {
            fragment = new ClientListFragment();
        }
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment, FRAGMENT_TAG).commit();
    }

    @Override
    public Client loadClient(int clientId) {
        dbConn.open();
        Client client = dbConn.getClient(clientId);
        dbConn.close();
        return client;
    }

/*@Override
    public String[] loadClients() {
        dbConn.open();
        List<String> allClients = dbConn.getAllClients(false);
        dbConn.close();
        return allClients.toArray(new String[allClients.size()]);
    }*/

    @Override
    public List<Client> loadClientObjects() {
        dbConn.open();
        List<Client> allClientObjects = dbConn.getAllClientObjects(false);
        dbConn.close();
        return allClientObjects;
    }

    @Override
    public List<Entry> loadEntries(int clientId) {
        dbConn.open();
        List<Entry> entries = dbConn.getEntriesForClient(clientId);
        dbConn.close();
        return entries;
    }

    @Override
    public void saveDetails(int clientId, int tel, String adress) {
        dbConn.open();
        dbConn.saveClientDetails(clientId, tel, adress);
        dbConn.close();
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
