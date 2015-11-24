package de.struckmeierfliesen.ds.wochenbericht;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class DeletableArrayAdapter<T> extends ArrayAdapter<T>  {

    private View.OnLongClickListener onLongClickListener;

    public interface DeleteListener {
        void onDelete(int position, View view);
    }

    DeleteListener deleteListener;

    public void setDeleteListener(DeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public DeletableArrayAdapter(Context context, @LayoutRes int resource, @NonNull List<T> objects) {
        super(context, resource, objects);
    }

    public DeletableArrayAdapter(Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull List<T> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getDropDownView(final int position, View convertView, ViewGroup parent) {
        final View view =  super.getDropDownView(position, convertView, parent);
        ImageView spinnerDeleteImage = (ImageView) view.findViewById(R.id.spinnerDelete);
        TextView label = (TextView) view.findViewById(R.id.spinnerText);
        // The last item(which acts as a button) and the dummy item should not have the delete button
        String addInstaller = view.getContext().getString(R.string.add_installer);
        String installer = view.getContext().getString(R.string.installer);
        if(!label.getText().equals(installer) && !label.getText().equals(addInstaller)) spinnerDeleteImage.setVisibility(View.VISIBLE);

        spinnerDeleteImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deleteListener != null) deleteListener.onDelete(position, v);
                    else Util.alert(view.getContext(),
                        "Position " + position + " has been clicked, but no DeleteListener attached");
            }
        });
        return view;
    }
}
