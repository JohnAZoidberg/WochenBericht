package de.struckmeierfliesen.ds.wochenbericht;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Date;
import java.util.List;

// Instances of this class are fragments representing a single
// object in our collection.
public class EntryListFragment extends Fragment implements EntryListAdapter.OnEntryClickListener {
    private MainActivity mainActivity;

    private EntryListAdapter entryListAdapter;
    private Date date = new Date();
    private DataBaseConnection dbConn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        Bundle args = getArguments();
        int position = args.getInt("position");
        date = Util.addDays(new Date(), position - DayAdapter.DAY_FRAGMENTS / 2);

        // initialize DataBaseConnection
        dbConn = new DataBaseConnection(getContext());

        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(R.layout.entries_recycler_view, container, false);

        // set up RecyclerView
        EmptyRecyclerView recyclerView = (EmptyRecyclerView) rootView.findViewById(R.id.entriesList);
        entryListAdapter = new EntryListAdapter(loadEntries(date));
        entryListAdapter.setEntryClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(entryListAdapter);
        recyclerView.setEmptyView(rootView.findViewById(R.id.empty));

        return rootView;
    }

    private List<Entry> loadEntries(Date date) {
        dbConn.open();
        List<Entry> entries = dbConn.getEntriesWithInstaller(date);
        dbConn.close();
        if(Util.isSameDay(date, mainActivity.getDate())) entryChanged(entries);
        return entries;
    }

    public void updateEntries(Date date) {
        this.date = date;
        updateEntries();
    }

    public void updateEntries() {
        List<Entry> entriesWithInstaller = loadEntries(date);
        entryListAdapter.setData(entriesWithInstaller);
        entryChanged();
    }

    public void editEntry(Entry entry) {
        entryListAdapter.editEntry(entry);
        entryChanged();
    }

    public void addEntry(Entry entry, int position) {
        entryListAdapter.addEntry(entry, position);
        entryChanged();
    }

    public void deleteEntry(Entry entry) {
        dbConn.open();
        dbConn.deleteEntry(entry);
        dbConn.close();
        entryListAdapter.deleteEntry(entry);
        entryChanged();
    }

    // from EntryHolder.OnEntryClickListener interface
    @Override
    public void entryClicked(View view, Entry entry) {
        mainActivity.startEditing(entry);
    }

    @Override
    public void entryLongClicked(View view, Entry entry) {
        mainActivity.deleteEntry(entry);
    }

    private void entryChanged() {
        mainActivity.setTotalDuration(getTotalHours());
    }

    private void entryChanged(List<Entry> entries) {
        mainActivity.setTotalDuration(getTotalHours(entries));
    }

    public int getTotalHours() {
        return getTotalHours(entryListAdapter.getData());
    }

    public int getTotalHours(List<Entry> entries) {
        int hours = 0;
        for(Entry entry : entries) {
            hours += entry.duration;
        }
        return hours;
    }

    public Date getDate() {
        return date;
    }
}