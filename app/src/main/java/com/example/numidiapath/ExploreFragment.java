package com.example.numidiapath;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ExploreFragment extends Fragment {

    private RecyclerView recyclerView;
    private ExploreAdapter adapter;
    private List<Post> allPosts = new ArrayList<>();
    private FirebaseFirestore db;
    private SearchView searchView;
    private Button btnGoToGroups; // Bouton pour accéder aux groupes

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        db = FirebaseFirestore.getInstance();

        // 1. Gestion de la navigation vers les Groupes
        btnGoToGroups = view.findViewById(R.id.btnGoToGroups);
        if (btnGoToGroups != null) {
            btnGoToGroups.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new GroupsListFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // 2. Barre de recherche
        searchView = view.findViewById(R.id.searchViewExplore);
        setupSearchLogic();

        // 3. Initialisation des Filtres (Chips IA)
        ChipGroup filterGroup = view.findViewById(R.id.filterChipGroup);
        setupFilterLogic(filterGroup);

        // 4. Initialisation du RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewExplore);
        setupDynamicGrid();

        adapter = new ExploreAdapter(new ArrayList<>(), post -> {
            PostDetailFragment detailFragment = new PostDetailFragment();
            Bundle args = new Bundle();
            args.putString("postId", post.getPostId());
            detailFragment.setArguments(args);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });

        recyclerView.setAdapter(adapter);

        // 5. Chargement des données
        loadDataFromFirestore();

        return view;
    }

    private void setupDynamicGrid() {
        int columnWidthInDp = 140;
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, columnWidthInDp, getResources().getDisplayMetrics());
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int spanCount = Math.max(2, screenWidth / px);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
    }

    private void loadDataFromFirestore() {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if (isAdded()) Toast.makeText(getContext(), "Erreur de chargement", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null && isAdded()) {
                        allPosts.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(doc.getId());
                                allPosts.add(post);
                            }
                        }
                        adapter.updateList(new ArrayList<>(allPosts));
                    }
                });
    }

    private void setupSearchLogic() {
        if (searchView == null) return;
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterData(newText);
                return true;
            }
        });
    }

    private void setupFilterLogic(ChipGroup group) {
        if (group == null) return;
        group.setOnCheckedStateChangeListener((chipGroup, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = chipGroup.findViewById(checkedIds.get(0));
                if (chip != null) {
                    filterData(chip.getText().toString());
                }
            } else {
                adapter.updateList(new ArrayList<>(allPosts));
            }
        });
    }

    private void filterData(String text) {
        if (text == null || text.isEmpty() || text.equalsIgnoreCase("Tout")) {
            adapter.updateList(new ArrayList<>(allPosts));
            return;
        }

        String query = text.toLowerCase().trim();
        List<Post> filteredList = new ArrayList<>();

        for (Post post : allPosts) {
            boolean matchFound = false;

            // Recherche dans les tags IA
            if (post.getTags() != null) {
                for (String tag : post.getTags()) {
                    if (tag.toLowerCase().contains(query)) {
                        matchFound = true;
                        break;
                    }
                }
            }

            // Recherche classique (Lieu, Caption, Username)
            if (!matchFound) {
                if (post.getLocation().toLowerCase().contains(query) ||
                        post.getCaption().toLowerCase().contains(query) ||
                        post.getUsername().toLowerCase().contains(query)) {
                    matchFound = true;
                }
            }

            if (matchFound) {
                filteredList.add(post);
            }
        }
        adapter.updateList(filteredList);
    }
}