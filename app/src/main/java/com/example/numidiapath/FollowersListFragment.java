package com.example.numidiapath;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class FollowersListFragment extends Fragment {

    private String userId, title;
    private boolean showFollowers;
    private List<User> userList = new ArrayList<>();
    private UserAdapter adapter;

    public static FollowersListFragment newInstance(String userId, String title, boolean showFollowers) {
        FollowersListFragment fragment = new FollowersListFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putString("title", title);
        args.putBoolean("showFollowers", showFollowers);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_followers_list, container, false);

        if (getArguments() != null) {
            userId = getArguments().getString("userId");
            title = getArguments().getString("title");
            showFollowers = getArguments().getBoolean("showFollowers");
        }

        ((TextView) view.findViewById(R.id.textTitle)).setText(title);
        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewUsers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserAdapter(userList, getContext());
        recyclerView.setAdapter(adapter);

        loadUsers();
        return view;
    }

    private void loadUsers() {
        String collectionPath = showFollowers ? "followers" : "following";

        FirebaseFirestore.getInstance().collection("users").document(userId)
                .collection(collectionPath).get().addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        fetchUserProfile(doc.getId());
                    }
                });
    }

    private void fetchUserProfile(String uid) {
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        user.setUid(documentSnapshot.getId());
                        userList.add(user);
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}