package de.struckmeierfliesen.ds.wochenbericht;


import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import de.struckmeierfliesen.ds.wochenbericht.databinding.EntriesListBinding;

public class EntryListAdapter extends RecyclerView.Adapter<EntryListAdapter.EntryHolder>{

    private ArrayList<Entry> items;
    private OnEntryClickListener clickListener;

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

    public void setEntryClickListener(OnEntryClickListener listener) {
        this.clickListener = listener;
    }

    public void editEntry(Entry entry) {
        for(Entry listEntry : items) {
            if(entry.id == listEntry.id) {
                int index = items.indexOf(listEntry);
                items.set(index, entry);
            }
        }
        notifyDataSetChanged();
    }

    public interface OnEntryClickListener {
        public void entryClicked(View view, Entry entry);
    }

    public class EntryHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private EntriesListBinding binding;

        public EntryHolder(EntriesListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.entriesList.setOnClickListener(this);
        }

        public void bindConnection(Entry entry) {
            binding.setEntry(entry);
        }

        @Override
        public void onClick(View v) {
            if(clickListener != null) clickListener.entryClicked(v, items.get(getAdapterPosition()));
        }
    }
}
