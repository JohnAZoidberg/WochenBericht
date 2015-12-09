package de.struckmeierfliesen.ds.wochenbericht;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import static de.struckmeierfliesen.ds.wochenbericht.ClientListFragment.ClientLoaderActivity;

public class ClientDetailsFragment extends Fragment implements ClientActivity.Updatable{

    public static final String ARG_CLIENT = "client";
    private ClientLoaderActivity activity;
    private EntryDetailListAdapter entryListAdapter;
    private String client = null;
    private ActionBar actionbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_client_details, container, false);
        activity = (ClientLoaderActivity) getActivity();

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARG_CLIENT)) {
            client = args.getString(ARG_CLIENT, null);
            actionbar = activity.getSupportActionBar();
        } else {
            throw new RuntimeException("No Client specified!");
        }

        // set up RecyclerView
        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.entriesList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Entry> entries = activity.loadEntries(client);
        entryListAdapter = new EntryDetailListAdapter(entries);
        recyclerView.setAdapter(entryListAdapter);
        return rootView;
    }

    @Override
    public void update() {
        entryListAdapter.setData(activity.loadEntries(client));
    }

    @Override
    public void onStart() {
        super.onStart();
        if (actionbar != null) actionbar.setTitle(client);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (actionbar != null) actionbar.setTitle(R.string.clients);
    }
}
