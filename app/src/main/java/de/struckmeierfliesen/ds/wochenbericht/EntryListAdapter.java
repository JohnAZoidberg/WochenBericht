package de.struckmeierfliesen.ds.wochenbericht;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import de.struckmeierfliesen.ds.wochenbericht.databinding.EntriesListBinding;

public class EntryListAdapter extends RecyclerView.Adapter<EntryListAdapter.EntryHolder>{

    private List<Entry> items;
    private OnEntryClickListener clickListener;

    public EntryListAdapter(List<Entry> data) {
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

    public void setData(List<Entry> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
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

    public void deleteEntry(Entry entry) {
        items.remove(entry);
        notifyDataSetChanged();
    }

    public List<Entry> getData() {
        return items;
    }

    public interface OnEntryClickListener {
        public void entryClicked(View view, Entry entry);
        public void entryLongClicked(View view, Entry entry);
    }

    public class EntryHolder extends RecyclerView.ViewHolder {

        private EntriesListBinding binding;

        public EntryHolder(EntriesListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.entriesList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(clickListener != null) clickListener.entryClicked(v, items.get(getAdapterPosition()));
                }
            });
            binding.entriesList.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (clickListener != null)
                        clickListener.entryLongClicked(v, items.get(getAdapterPosition()));
                    return true;
                }
            });

            binding.clientView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, ClientActivity.class);
                    intent.putExtra(ClientDetailsFragment.ARG_CLIENT_NAME, ((TextView) v).getText().toString());
                    context.startActivity(intent);
                }
            });

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
