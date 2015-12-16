package de.struckmeierfliesen.ds.wochenbericht;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

public class DayAdapter extends FragmentStatePagerAdapter {
    public static final int DAY_FRAGMENTS = 365;

    SparseArray<EntryListFragment> registeredFragments = new SparseArray<>();

    public DayAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int i) {
        return new EntryListFragment();
    }

    @Override
    public int getCount() {
        return DAY_FRAGMENTS;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        EntryListFragment fragment = (EntryListFragment) super.instantiateItem(container, position);
        try {
            Bundle args = new Bundle();
            args.putInt("position", position);
            fragment.setArguments(args);
            registeredFragments.put(position, fragment);
        } catch (IllegalStateException e) {
            String msg = "SCREENSHOT! ERROR!\n" +
                    "IllegalStateException: " +
                    "fragment.isAdded() => " + fragment.isAdded() + "\n" +
                    "registeredFragments contains => " + registeredFragments.get(position).equals(fragment) + "\n" +
                    "Bitte mit Screenshot melden!";
            Util.logToFile(msg, e);
            Dialog.alert(fragment.getContext(),
                    msg,
                    true);
        }
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public EntryListFragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }
}
