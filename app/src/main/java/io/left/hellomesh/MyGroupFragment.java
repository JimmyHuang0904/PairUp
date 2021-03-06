package io.left.hellomesh;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Class to display list of users and group the user is currently in.
 */
public class MyGroupFragment extends Fragment {

/*    private ListAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
*//*        mAdapter = new ListAdapter(getContext());

        // Populate lists with groups and names here
        for (int i = 1; i < 30; i++) {
            mAdapter.addItem("Row Item #" + i);
            if (i % 4 == 0) {
                mAdapter.addSectionHeaderItem("Section #" + i);
            }
        }
        setListAdapter(mAdapter);*//*
    }*/

    public MyGroupFragment(){};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_group, container, false);

        String[] users = {"John", "Amy", "Mark"};

        ListView listView = (ListView) view.findViewById(R.id.userList);

        ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                users
        );

        listView.setAdapter(listViewAdapter);
        // Inflate the layout for this fragment
        return view;
    }

}
