package de.struckmeierfliesen.ds.wochenbericht;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Spinner;

public class SelectAgainSpinner extends Spinner {
    OnItemSelectedListener mOnItemSelectedListener;

    public SelectAgainSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);

        if (position == getSelectedItemPosition() && mOnItemSelectedListener != null) {
            View v = getSelectedView();
            int selection = getSelectedItemPosition();
            mOnItemSelectedListener.onItemSelected(this, v, position, getAdapter().getItemId(selection));
        }
    }

    @Override
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        //super.setOnItemSelectedListener(listener);
        this.mOnItemSelectedListener = listener;
    }
}
