package com.example.numidiapath;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> postList;
    private FirebaseFirestore db;
    private SwipeRefreshLayout swipeRefresh;
    private ListenerRegistration firestoreListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Initialisation Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 2. Configuration de la Toolbar (Si elle est définie dans ton fragment_home.xml)
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        }

        // 3. Toolbar : Gestion des boutons existants
        ImageView btnOpenMap = view.findViewById(R.id.btnOpenMap);
        ImageView btnSearch = view.findViewById(R.id.btnSearch);
        ImageView btnLoginRedirect = view.findViewById(R.id.btnLoginRedirect);

        if (btnLoginRedirect != null) {
            if (currentUser != null && currentUser.isAnonymous()) {
                btnLoginRedirect.setVisibility(View.VISIBLE);
                btnLoginRedirect.setOnClickListener(v -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) getActivity().finish();
                });
            } else {
                btnLoginRedirect.setVisibility(View.GONE);
            }
        }

        if (btnOpenMap != null) {
            btnOpenMap.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new MapFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ExploreFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // 4. Initialisation RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewHome);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        postList = new ArrayList<>();
        adapter = new PostAdapter(postList);
        recyclerView.setAdapter(adapter);

        // 5. Configuration du SwipeRefreshLayout
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.purple_500);
            swipeRefresh.setOnRefreshListener(this::loadPostsFromFirebase);
        }

        loadPostsFromFirebase();

        return view;
    }

    // --- CHARGEMENT DES DONNÉES ---

    private void loadPostsFromFirebase() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);

        Query query = db.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING);

        if (firestoreListener != null) firestoreListener.remove();

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

            if (error != null) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Erreur : " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return;
            }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }
}
