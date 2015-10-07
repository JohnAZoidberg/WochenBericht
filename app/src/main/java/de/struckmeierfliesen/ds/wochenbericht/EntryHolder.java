package de.struckmeierfliesen.ds.wochenbericht;

import android.support.v7.widget.RecyclerView;

import de.struckmeierfliesen.ds.wochenbericht.databinding.EntriesListBinding;

public class EntryHolder extends RecyclerView.ViewHolder {

    private EntriesListBinding binding;

    public EntryHolder(EntriesListBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bindConnection(Entry entry) {
        binding.setEntry(entry);
    }
}
