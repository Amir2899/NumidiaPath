package com.example.numidiapath;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class GroupsListFragment extends Fragment {

    private RecyclerView recyclerView;
    private GroupAdapter adapter;
    private List<Group> groupList = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_groups_list, container, false);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerViewGroups);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new GroupAdapter(groupList);
        recyclerView.setAdapter(adapter);

        // Bouton pour aller vers la création de groupe
        FloatingActionButton fab = view.findViewById(R.id.fabAddGroup);
        fab.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CreateGroupFragment())
                    .addToBackStack(null)
                    .commit();
        });

        loadGroups();
        return view;
    }

    private void loadGroups() {
        db.collection("groups").addSnapshotListener((value, error) -> {
            if (value != null) {
                groupList.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Group group = doc.toObject(Group.class);
                    if (group != null) {
                        group.setGroupId(doc.getId());
                        groupList.add(group);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}