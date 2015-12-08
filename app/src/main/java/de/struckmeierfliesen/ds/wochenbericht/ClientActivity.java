package de.struckmeierfliesen.ds.wochenbericht;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import java.util.List;

public class ClientActivity extends AppCompatActivity {

    private TextViewAdapter clientAdapter;
    private DataBaseConnection dbConn;

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

        EmptyRecyclerView recyclerView = (EmptyRecyclerView) findViewById(R.id.clientList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        clientAdapter = new TextViewAdapter(loadClients());
        clientAdapter.setOnItemClickListener(new TextViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(TextView view, String text, int item) {
                Dialog.alert(ClientActivity.this, "Kunde " + text + " wurde gedrückt.");
                /*Intent intent = new Intent(this, ClientActivity.class);
                startActivity(intent);*/
            }
        });
        recyclerView.setAdapter(clientAdapter);
        recyclerView.setEmptyView(findViewById(R.id.empty));
    }

    private String[] loadClients() {
        dbConn.open();
        List<String> allClients = dbConn.getAllClients();
        dbConn.close();
        return allClients.toArray(new String[allClients.size()]);
    }
}
