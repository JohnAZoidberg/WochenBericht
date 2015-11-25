package de.struckmeierfliesen.ds.wochenbericht;

import android.view.View;

public class MultipleClickListener implements View.OnClickListener {

    View.OnClickListener[] onClickListeners;

    public MultipleClickListener (View.OnClickListener... onClickListeners) {
        this.onClickListeners = onClickListeners;
    }

    @Override
    public void onClick(View v) {
        for (View.OnClickListener onClickListener: onClickListeners){
            onClickListener.onClick(v);
        }
    }

    public class MultipleClickSubmitListener {
        Util.OnInputSubmitListener submitListener;
        View.OnClickListener[] onClickListeners;

        public MultipleClickSubmitListener(Util.OnInputSubmitListener submitListener, View.OnClickListener... onClickListeners) {
            this.submitListener = submitListener;
            this.onClickListeners = onClickListeners;
        }
    }

}
