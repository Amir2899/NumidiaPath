package com.example.numidiapath;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView profileName, profileBio, textPostCount, textFollowersCount, textFollowingCount;
    private ShapeableImageView profileImage;
    private RecyclerView recyclerView;
    private ImageButton btnMenu;
    private Button btnAction;
    private LinearLayout layoutFollowers, layoutFollowing;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProfileGridAdapter adapter;
    private List<Post> userPosts = new ArrayList<>();

    private String targetUserId;
    private String currentUserId;

    // Listeners actifs pour mise à jour en temps réel
    private ListenerRegistration profileListener;
    private ListenerRegistration gridListener;
    private ListenerRegistration followStatusListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) return;
        currentUserId = currentUser.getUid();

        // Déterminer si on affiche son propre profil ou celui d'un tiers
        if (getArguments() != null && getArguments().containsKey("userId")) {
            targetUserId = getArguments().getString("userId");
        } else {
            targetUserId = currentUserId;
        }

        initViews(view);

        if (targetUserId != null) {
            setupUIBasedOnUser();
            loadUserProfile();
            setupGrid();
            setupClickListeners();

            // Synchronisation forcée du compteur pour son propre profil
            if (targetUserId.equals(currentUserId)) {
                FirestoreHelper.syncPostCount(targetUserId);
            }
        }
    }

    private void initViews(View view) {
        profileName = view.findViewById(R.id.profileName);
        profileBio = view.findViewById(R.id.profileBio);
        profileImage = view.findViewById(R.id.profileImageLarge);
        textPostCount = view.findViewById(R.id.textPostCount);
        textFollowersCount = view.findViewById(R.id.textFollowersCount);
        textFollowingCount = view.findViewById(R.id.textFollowingCount);
        recyclerView = view.findViewById(R.id.recyclerViewProfile);
        btnMenu = view.findViewById(R.id.btnProfileMenu);
        btnAction = view.findViewById(R.id.btnEditProfile);

        // Liaison sécurisée des layouts parents pour les clics sur les stats
        if (textFollowersCount != null && textFollowersCount.getParent() instanceof LinearLayout)
            layoutFollowers = (LinearLayout) textFollowersCount.getParent();
        if (textFollowingCount != null && textFollowingCount.getParent() instanceof LinearLayout)
            layoutFollowing = (LinearLayout) textFollowingCount.getParent();
    }

    private void loadUserProfile() {
        if (profileListener != null) profileListener.remove();

        profileListener = db.collection("users").document(targetUserId).addSnapshotListener((document, error) -> {
            if (!isAdded() || document == null || !document.exists()) return;

            User userObj = document.toObject(User.class);
            if (userObj != null) {
                profileName.setText(userObj.getFullName());

                String bioDisplay = "@" + userObj.getUsername();
                if (userObj.getBio() != null && !userObj.getBio().isEmpty()) {
                    bioDisplay += "\n" + userObj.getBio();
                }
                profileBio.setText(bioDisplay);

                // Affichage des compteurs officiels Firestore
                textPostCount.setText(String.valueOf(userObj.getPostsCount()));
                textFollowersCount.setText(String.valueOf(userObj.getFollowersCount()));
                textFollowingCount.setText(String.valueOf(userObj.getFollowingCount()));

                if (isAdded() && userObj.getProfileImageUrl() != null && !userObj.getProfileImageUrl().isEmpty()) {
                    Glide.with(this).load(userObj.getProfileImageUrl()).circleCrop()
                            .placeholder(R.drawable.ic_person).into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.ic_person);
                }
            }
        });
    }

    private void setupGrid() {
        if (recyclerView == null) return;

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new ProfileGridAdapter(userPosts, post -> {
            PostDetailFragment detailFragment = new PostDetailFragment();
            Bundle args = new Bundle();
            args.putString("postId", post.getPostId());
            detailFragment.setArguments(args);
            getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null).commit();
        });
        recyclerView.setAdapter(adapter);

        if (gridListener != null) gridListener.remove();

        gridListener = db.collection("posts")
                .whereEqualTo("publisherId", targetUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded() || value == null) return;

                    userPosts.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Post post = doc.toObject(Post.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            userPosts.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    // Correction auto du compteur si une différence est détectée entre la grille et le chiffre affiché
                    if (targetUserId.equals(currentUserId)) {
                        try {
                            int displayedCount = Integer.parseInt(textPostCount.getText().toString());
                            if (userPosts.size() != displayedCount) {
                                FirestoreHelper.syncPostCount(targetUserId);
                            }
                        } catch (Exception ignored) {}
                    }
                });
    }

    // NOTE : Les anciennes méthodes setupFollowersNotification, setupLikesNotification,
    // et setupCommentsNotification ont été supprimées.
    // Elles sont remplacées par l'écouteur global dans MainActivity.

    private void setupClickListeners() {
        View.OnClickListener followersClick = v -> openFollowersList("Abonnés", true);
        if (textFollowersCount != null) textFollowersCount.setOnClickListener(followersClick);
        if (layoutFollowers != null) layoutFollowers.setOnClickListener(followersClick);

        View.OnClickListener followingClick = v -> openFollowersList("Suivis", false);
        if (textFollowingCount != null) textFollowingCount.setOnClickListener(followingClick);
        if (layoutFollowing != null) layoutFollowing.setOnClickListener(followingClick);
    }

    private void openFollowersList(String title, boolean isFollowers) {
        FollowersListFragment fragment = FollowersListFragment.newInstance(targetUserId, title, isFollowers);
        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment)
                .addToBackStack(null).commit();
    }

    private void setupUIBasedOnUser() {
        if (targetUserId.equals(currentUserId)) {
            if (btnMenu != null) {
                btnMenu.setVisibility(View.VISIBLE);
                btnMenu.setOnClickListener(v -> showPopupMenu(v, mAuth.getCurrentUser()));
            }
            if (btnAction != null) {
                btnAction.setText("Modifier le profil");
                btnAction.setOnClickListener(v -> openEditBottomSheet());
            }
        } else {
            if (btnMenu != null) btnMenu.setVisibility(View.GONE);
            if (btnAction != null) checkFollowStatus(btnAction);
        }
    }

    private void checkFollowStatus(Button btnFollow) {
        if (followStatusListener != null) followStatusListener.remove();
        followStatusListener = db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId)
                .addSnapshotListener((doc, e) -> {
                    if (!isAdded() || doc == null) return;
                    if (doc.exists()) {
                        btnFollow.setText("Abonné");
                        btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray)));
                    } else {
                        btnFollow.setText("S'abonner");
                        btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.purple_500)));
                    }
                });
        btnFollow.setOnClickListener(v -> toggleFollow());
    }

    private void toggleFollow() {
        db.collection("users").document(currentUserId)
                .collection("following").document(targetUserId).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc.exists()) {
                        FirestoreHelper.unfollowUser(currentUserId, targetUserId, null);
                    } else {
                        FirestoreHelper.followUser(currentUserId, targetUserId, null);
                    }
                });
    }

    private void openEditBottomSheet() {
        EditProfileBottomSheet editSheet = new EditProfileBottomSheet((newName, newBio, newPhotoUri) -> {});
        editSheet.show(getChildFragmentManager(), "EditProfile");
    }

    private void showPopupMenu(View v, FirebaseUser user) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_logout) showLogoutDialog();
            else if (id == R.id.action_settings) {
                getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment())
                        .addToBackStack(null).commit();
            }
            return true;
        });
        popup.show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext()).setTitle("Déconnexion").setMessage("Voulez-vous quitter ?")
                .setPositiveButton("Oui", (dialog, id) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                }).setNegativeButton("Non", null).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nettoyage impératif pour éviter les fuites de mémoire
        if (profileListener != null) profileListener.remove();
        if (gridListener != null) gridListener.remove();
        if (followStatusListener != null) followStatusListener.remove();
    }
}