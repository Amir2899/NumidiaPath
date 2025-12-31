package com.example.numidiapath;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> favoritePosts;
    private FirebaseFirestore db;

    private ChipGroup chipGroupTags, chipGroupLocations;
    private FloatingActionButton btnAddFavorite;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Initialisation UI existante (Posts enregistrés)
        recyclerView = view.findViewById(R.id.recyclerViewFavorites);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        favoritePosts = new ArrayList<>();
        adapter = new PostAdapter(favoritePosts);
        recyclerView.setAdapter(adapter);

        // Initialisation nouvelle UI (Tags & Lieux)
        chipGroupTags = view.findViewById(R.id.chipGroupTags);
        chipGroupLocations = view.findViewById(R.id.chipGroupLocations);
        btnAddFavorite = view.findViewById(R.id.btnAddFavorite);

        // Listener pour l'ajout manuel
        btnAddFavorite.setOnClickListener(v -> showAddFavoriteDialog());

        if (currentUserId != null) {
            loadFavoritePosts();
            loadFollowedInterests();
        }

        return view;
    }

    // --- DIALOGUE D'AJOUT MANUEL ---
    private void showAddFavoriteDialog() {
        String[] options = {"Suivre un Tag (ex: Nature)", "Suivre un Lieu (ex: Montpellier)"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Ajouter un favori")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showInputDialog("Nom du Tag", "followedTags");
                    } else {
                        showInputDialog("Nom du Lieu", "followedLocations");
                    }
                }).show();
    }

    private void showInputDialog(String title, String fieldName) {
        EditText input = new EditText(getContext());
        input.setHint("Ex: Nature, Sport, Paris...");

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Ajouter", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) {
                        db.collection("users").document(currentUserId)
                                .update(fieldName, FieldValue.arrayUnion(value))
                                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Ajouté aux favoris", Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    // --- CHARGEMENT DES POSTS ENREGISTRÉS ---
    private void loadFavoritePosts() {
        db.collection("posts")
                .whereArrayContains("savedBy", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        favoritePosts.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(doc.getId());
                                favoritePosts.add(post);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // --- CHARGEMENT DES TAGS ET LIEUX SUIVIS ---
    private void loadFollowedInterests() {
        db.collection("users").document(currentUserId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Tags
                        List<String> tags = (List<String>) documentSnapshot.get("followedTags");
                        displayChips(tags, chipGroupTags, "followedTags");

                        // Lieux
                        List<String> locations = (List<String>) documentSnapshot.get("followedLocations");
                        displayChips(locations, chipGroupLocations, "followedLocations");
                    }
                });
    }

    private void displayChips(List<String> items, ChipGroup group, String fieldName) {
        if (!isAdded()) return;
        group.removeAllViews();
        if (items == null) return;

        for (String item : items) {
            Chip chip = new Chip(requireContext());
            chip.setText(item);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                db.collection("users").document(currentUserId)
                        .update(fieldName, FieldValue.arrayRemove(item));
            });
            group.addView(chip);
        }
    }
}