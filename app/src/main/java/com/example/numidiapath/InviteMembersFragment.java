package com.example.numidiapath;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class InviteMembersFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList = new ArrayList<>();
    private String groupId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_invite_members, container, false);

        if (getArguments() != null) {
            groupId = getArguments().getString("groupId");
        }

        recyclerView = view.findViewById(R.id.recyclerViewInvite);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        userAdapter = new UserAdapter(userList, getContext());
        recyclerView.setAdapter(userAdapter);

        // --- CORRECTION : Définir le listener de clic ---
        userAdapter.setOnUserClickListener(user -> {
            if (groupId != null) {
                inviteUserToGroup(user);
            }
        });

        loadAllUsers();

        return view;
    }

    private void loadAllUsers() {
        FirebaseFirestore.getInstance().collection("users")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                userList.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    User user = document.toObject(User.class);
                    user.setUid(document.getId());
                    userList.add(user);
                }
                userAdapter.notifyDataSetChanged();
            });
    }

    private void inviteUserToGroup(User user) {
        if (groupId == null) return;
        FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .update("members", FieldValue.arrayUnion(user.getUid()))
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), user.getUsername() + " a été ajouté au groupe !", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Erreur lors de l'invitation", Toast.LENGTH_SHORT).show();
            });
    }
}