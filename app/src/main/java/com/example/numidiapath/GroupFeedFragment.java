package com.example.numidiapath;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class GroupFeedFragment extends Fragment {

    private String groupId, groupName;
    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> postList = new ArrayList<>();
    private ImageView btnOptions;
    private TextView textTitle;
    private String groupAdminId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_feed, container, false);

        if (getArguments() != null) {
            groupId = getArguments().getString("groupId");
            groupName = getArguments().getString("groupName");
        }

        textTitle = view.findViewById(R.id.textGroupTitle);
        if (textTitle != null) {
            textTitle.setText(groupName != null ? groupName : "Groupe");
        }

        btnOptions = view.findViewById(R.id.btnGroupOptions);
        checkIfUserIsAdmin();

        recyclerView = view.findViewById(R.id.recyclerViewGroupPosts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new PostAdapter(postList);
        recyclerView.setAdapter(adapter);

        loadGroupPosts();

        return view;
    }

    private void checkIfUserIsAdmin() {
        if (groupId == null) return;
        String currentUserId = FirebaseAuth.getInstance().getUid();

        FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    groupAdminId = documentSnapshot.getString("adminId");
                    if (currentUserId != null && currentUserId.equals(groupAdminId)) {
                        btnOptions.setVisibility(View.VISIBLE);
                        btnOptions.setOnClickListener(this::showGroupMenu);
                    }
                }
            });
    }

    private void showGroupMenu(View view) {
        if (getContext() == null) return;
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenuInflater().inflate(R.menu.group_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_invite_member) {
                // --- OUVRIR L'ÉCRAN D'INVITATION ---
                openInviteFragment();
                return true;
            } else if (itemId == R.id.menu_edit_group) {
                showEditGroupNameDialog();
                return true;
            } else if (itemId == R.id.menu_delete_group) {
                confirmGroupDeletion();
                return true;
            }
            return false;
        });
        popup.show();
    }

    // --- NOUVELLE MÉTHODE POUR OUVRIR L'ÉCRAN D'INVITATION ---
    private void openInviteFragment() {
        InviteMembersFragment fragment = new InviteMembersFragment();
        Bundle args = new Bundle();
        args.putString("groupId", groupId);
        fragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }

    private void showEditGroupNameDialog() {
        if (getContext() == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Modifier le nom du groupe");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(groupName);
        builder.setView(input);

        builder.setPositiveButton("Valider", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(groupName)) {
                updateGroupName(newName);
            }
        });
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateGroupName(String newName) {
        if (groupId == null) return;
        FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .update("groupName", newName)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Nom du groupe mis à jour", Toast.LENGTH_SHORT).show();
                groupName = newName;
                textTitle.setText(newName);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Erreur de mise à jour", Toast.LENGTH_SHORT).show();
            });
    }

    private void confirmGroupDeletion() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
            .setTitle("Supprimer le groupe")
            .setMessage("Êtes-vous sûr de vouloir supprimer ce groupe ? Cette action est irréversible.")
            .setPositiveButton("Supprimer", (dialog, which) -> {
                deleteGroup();
            })
            .setNegativeButton("Annuler", null)
            .show();
    }

    private void deleteGroup() {
        if (groupId == null) return;
        FirebaseFirestore.getInstance().collection("groups").document(groupId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Groupe supprimé avec succès", Toast.LENGTH_SHORT).show();
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().popBackStack();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadGroupPosts() {
        if (groupId == null) return;

        FirebaseFirestore.getInstance().collection("posts")
                .whereEqualTo("groupId", groupId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        postList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(doc.getId());
                                postList.add(post);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}