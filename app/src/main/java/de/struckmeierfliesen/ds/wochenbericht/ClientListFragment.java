package de.struckmeierfliesen.ds.wochenbericht;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class ClientListFragment extends Fragment {

    private TextViewAdapter clientAdapter;
    private ClientLoaderActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_client, container, false);
        activity = (ClientLoaderActivity) getActivity();

        EmptyRecyclerView recyclerView = (EmptyRecyclerView) rootView.findViewById(R.id.clientList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));
        clientAdapter = new TextViewAdapter(activity.loadClients());
        clientAdapter.setOnItemClickListener(new TextViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(TextView view, String text, int item) {
                startDetailsFragment(text);
            }
        });
        recyclerView.setAdapter(clientAdapter);
        recyclerView.setEmptyView(rootView.findViewById(R.id.empty));
        return rootView;
    }

    private void startDetailsFragment(String client) {
        // Create fragment and give it an argument specifying the article it should show
        ClientDetailsFragment newFragment = new ClientDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ClientDetailsFragment.ARG_CLIENT, client);
        newFragment.setArguments(args);

        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();

        // Replace whatever is in the fragment_container view with this fragment,
        // and add the transaction to the back stack so the user can navigate back
        transaction.replace(R.id.fragment_container, newFragment);
        transaction.addToBackStack(null);

        // Commit the transaction
        transaction.commit();
    }

    abstract public static class ClientLoaderActivity extends AppCompatActivity {
        abstract String[] loadClients();
        abstract List<Entry> loadEntries(String client);
    }
}