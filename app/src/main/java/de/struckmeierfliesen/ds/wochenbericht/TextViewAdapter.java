package de.struckmeierfliesen.ds.wochenbericht;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class TextViewAdapter extends RecyclerView.Adapter<TextViewAdapter.StringHolder> {
    private String[] items;
    private OnItemClickListener listener;

    public TextViewAdapter(String... items) {
        this.items = items;
    }

    public TextViewAdapter(List<Client> items) {
        this.items = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            this.items[i] = items.get(i).toString();
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        listener = onItemClickListener;
    }

    @Override
    public StringHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView view = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.client_list, parent, false);
        return new StringHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(StringHolder holder, int position) {
        holder.mTextView.setText(items[position]);
    }

    @Override
    public int getItemCount() {
        return items.length;
    }

    public static class StringHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;

        public StringHolder(TextView view, final OnItemClickListener listener) {
            super(view);
            mTextView = view;
            // TODO maybe change the color indicating frequency
            mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        TextView textView = (TextView) v;
                        listener.onItemClick(textView, textView.getText().toString(), getAdapterPosition());
                    }
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(TextView view, String text, int item);
    }
}