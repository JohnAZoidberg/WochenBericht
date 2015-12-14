package de.struckmeierfliesen.ds.wochenbericht;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.PhoneNumberUtils;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.List;

import static de.struckmeierfliesen.ds.wochenbericht.ClientListFragment.ClientLoaderActivity;

public class ClientDetailsFragment extends Fragment implements ClientActivity.Updatable{

    public static final String ARG_CLIENT_NAME = "client.name";
    public static final String ARG_CLIENT_ID = "client.id";
    public static final String ARG_CLIENT_TEL = "client.tel";
    public static final String ARG_CLIENT_ADRESS = "client.tel";
    private ClientLoaderActivity activity;
    private EntryDetailListAdapter entryListAdapter;
    private Client client = null;
    private ActionBar actionbar;
    private EditText telText;
    private EditText adressText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_client_details, container, false);
        activity = (ClientLoaderActivity) getActivity();

        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ARG_CLIENT_ID)) {
                int id = args.getInt(ARG_CLIENT_ID, -1);
                client = activity.loadClient(id);
                actionbar = activity.getSupportActionBar();
            } else if (args.containsKey(ARG_CLIENT_NAME)) {
                String clientName = args.getString(ARG_CLIENT_NAME, null);
                client = activity.loadClient(clientName);
                actionbar = activity.getSupportActionBar();
            }
        } else {
            throw new RuntimeException("No Client specified!");
        }

        // set up inputs
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++)
                    if (!PhoneNumberUtils.isDialable(source.charAt(i)))
                        return "";
                return null;
            }
        };
        telText = (EditText) rootView.findViewById(R.id.telText);
        adressText = (EditText) rootView.findViewById(R.id.adressText);
        telText.setFilters(new InputFilter[] { filter });
        if (client.tel != -1) telText.setText(String.valueOf(client.tel));
        if (client.adress != null) adressText.setText(client.adress);

        // set up RecyclerView
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.entriesList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Entry> entries = activity.loadEntries(client.id);
        entryListAdapter = new EntryDetailListAdapter(entries);
        recyclerView.setAdapter(entryListAdapter);
        return rootView;
    }

    @Override
    public void update() {
        entryListAdapter.setData(activity.loadEntries(client.id));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (actionbar != null) actionbar.setTitle(client.name);
    }

    @Override
    public void onStop() {
        super.onStop();
        saveDetails();
        if (actionbar != null) actionbar.setTitle(R.string.clients);
    }

    private void saveDetails() {
        if (client.id != 1) {
            String telString = telText.getText().toString().trim();
            int tel = telString.isEmpty() ? -1 : Integer.parseInt(telString);
            String adress = adressText.getText().toString().trim();
            activity.saveDetails(client.id, tel, adress);
        }
    }
}
