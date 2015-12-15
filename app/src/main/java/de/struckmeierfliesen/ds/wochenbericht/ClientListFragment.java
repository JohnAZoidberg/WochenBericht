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
    private List<Client> clients;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_client, container, false);
        activity = (ClientLoaderActivity) getActivity();

        EmptyRecyclerView recyclerView = (EmptyRecyclerView) rootView.findViewById(R.id.clientList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false));

        clients = activity.loadClientObjects();
        clientAdapter = new TextViewAdapter(clients);
        clientAdapter.setOnItemClickListener(new TextViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(TextView view, String text, int item) {
                startDetailsFragment(clients.get(item));
            }
        });
        recyclerView.setAdapter(clientAdapter);
        recyclerView.setEmptyView(rootView.findViewById(R.id.empty));
        return rootView;
    }

    private void startDetailsFragment(Client client) {
        // Create fragment and give it an argument specifying the article it should show
        ClientDetailsFragment newFragment = new ClientDetailsFragment();
        Bundle args = new Bundle();
        args.putInt(ClientDetailsFragment.ARG_CLIENT_ID, client.id);
        /*args.putString(ClientDetailsFragment.ARG_CLIENT_NAME, client.name);
        args.putString(ClientDetailsFragment.ARG_CLIENT_ADRESS, client.adress);
        args.putInt(ClientDetailsFragment.ARG_CLIENT_TEL, client.tel);*/
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
        public abstract Client loadClient(int clientId);
        public abstract Client loadClient(String client);
        public abstract void deleteClient(int clientId);
        public abstract int renameClient(int clientId, String newName);
        public abstract void mergeClients(int merge, int with);
        public abstract List<Client> loadClientObjects();
        public abstract List<Entry> loadEntries(int clientId);
        public abstract void saveDetails(int clientId, String tel, String adress);
    }
}