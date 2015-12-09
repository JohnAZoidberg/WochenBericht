package de.struckmeierfliesen.ds.wochenbericht;


import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.List;

import de.struckmeierfliesen.ds.wochenbericht.databinding.EntryDetailListBinding;

public class EntryDetailListAdapter extends RecyclerView.Adapter<EntryDetailListAdapter.EntryHolder>{

    private List<Entry> items;

    public EntryDetailListAdapter(List<Entry> data) {
        super();
        items = data;
    }

    @Override
    public EntryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        EntryDetailListBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                R.layout.entry_detail_list, parent, false);
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

    public void setData(List<Entry> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public class EntryHolder extends RecyclerView.ViewHolder {

        private EntryDetailListBinding binding;

        public EntryHolder(EntryDetailListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.entryImageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final Entry entry = items.get(getAdapterPosition());
                    final Activity activity = (Activity) v.getContext();
                    Dialog.selectImage(activity, entry, new Runnable() {
                        @Override
                        public void run() {
                            Util.deletePictureFromEntry(activity, entry.id);
                            entry.setPicturePath(null);
                            notifyDataSetChanged();
                        }
                    });
                    return true;
                }
            });
            binding.entryImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Entry entry = items.get(getAdapterPosition());
                    String picturePath = entry.getPicturePath();
                    Activity activity = (Activity) v.getContext();
                    if (picturePath == null || !new File(picturePath).isFile()) {
                        Dialog.selectImage(activity, entry);
                    } else {
                        Intent showPicIntent = new Intent(activity, PictureViewerActivity.class);
                        showPicIntent.putExtra("fileName", picturePath);
                        showPicIntent.putExtra("title", "Picture Title " + entry);
                        activity.startActivity(showPicIntent);
                    }
                }
            });
        }

        public void bindConnection(Entry entry) {
            binding.setEntry(entry);
        }
    }
}
