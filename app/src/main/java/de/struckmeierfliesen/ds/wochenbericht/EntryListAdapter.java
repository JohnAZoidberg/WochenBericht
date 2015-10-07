package de.struckmeierfliesen.ds.wochenbericht;


import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;

import de.struckmeierfliesen.ds.wochenbericht.databinding.EntriesListBinding;

public class EntryListAdapter extends RecyclerView.Adapter<EntryHolder>{

    private ArrayList<Entry> items;

    public EntryListAdapter(ArrayList<Entry> data) {
        super();
        items = data;
    }

    @Override
    public EntryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        EntriesListBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.entries_list, parent, false);
        return new EntryHolder(binding);
    }

    @Override
    public void onBindViewHolder(EntryHolder holder, int position) {
        holder.bindConnection(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setData(ArrayList<Entry> items) {
        this.items = items;
    }

    public void addEntry(Entry entry, int position) {
        items.add(position, entry);
        notifyDataSetChanged();
    }
}
