package com.example.numidiapath;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostDetailFragment extends Fragment {

    private String postId;
    private FirebaseFirestore db;
    private String currentUserId;
    private boolean isAnonymous = true;
    private ListenerRegistration postListener, followListener, authorListener;

    // Vues
    private TextView username, location, caption, likeCount;
    private ImageView imagePost, imageProfile, btnLike, btnSave, btnBack, btnComment, btnOptions, btnMap, btnItinerary;
    private TextView btnFollow; // CORRECTION: Changé de Button à TextView
    private ChipGroup chipGroupTags;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_detail, container, false);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            currentUserId = user.getUid();
            isAnonymous = user.isAnonymous();
        }

        if (getArguments() != null) {
            postId = getArguments().getString("postId");
        }

        initViews(view);
        loadPostDetails();

        return view;
    }

    private void initViews(View view) {
        username = view.findViewById(R.id.textUsername);
        location = view.findViewById(R.id.textLocationDate);
        caption = view.findViewById(R.id.textDescription);
        likeCount = view.findViewById(R.id.textLikeCount);
        imagePost = view.findViewById(R.id.imagePost);
        imageProfile = view.findViewById(R.id.imageProfile);
        btnLike = view.findViewById(R.id.btnLike);
        btnSave = view.findViewById(R.id.btnSave);
        btnComment = view.findViewById(R.id.btnComment);
        btnOptions = view.findViewById(R.id.btnPostOptions);
        btnBack = view.findViewById(R.id.btnBack);
        btnMap = view.findViewById(R.id.btnOpenMap);
        btnItinerary = view.findViewById(R.id.btnItinerary);
        btnFollow = view.findViewById(R.id.btnFollow);
        chipGroupTags = view.findViewById(R.id.chipGroupTags);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }
    }

    private void loadPostDetails() {
        if (postId == null || postId.isEmpty()) return;

        postListener = db.collection("posts").document(postId).addSnapshotListener((doc, error) -> {
            if (!isAdded() || error != null || doc == null || !doc.exists()) return;

            Post post = doc.toObject(Post.class);
            if (post != null) {
                if (post.getPostId() == null) post.setPostId(doc.getId());
                displayPost(post);
                loadAuthorProfile(post.getPublisherId());
                setupFollowButton(post.getPublisherId());
                setupOptionsBtn(post);
            }
        });
    }

    private void displayPost(Post post) {
        if (username != null) username.setText(post.getUsername());
        if (location != null) location.setText(post.getLocation());
        if (caption != null) caption.setText(post.getCaption());

        if (btnItinerary != null) {
            btnItinerary.setOnClickListener(v -> {
                if (post.getLatitude() != 0 && post.getLongitude() != 0) {
                    openGoogleMaps(post.getLatitude(), post.getLongitude());
                } else {
                    Toast.makeText(getContext(), "Coordonnées indisponibles", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnMap != null) {
            btnMap.setOnClickListener(v -> Toast.makeText(getContext(), "Affichage sur la carte...", Toast.LENGTH_SHORT).show());
        }

        if (chipGroupTags != null) {
            chipGroupTags.removeAllViews();
            List<String> tags = post.getTags();
            if (tags != null) {
                for (String tag : tags) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(tag);
                    chip.setChipBackgroundColorResource(R.color.light_gray);
                    chipGroupTags.addView(chip);
                }
            }
        }

        if (imagePost != null) {
            Glide.with(this).load(post.getImageUrl()).into(imagePost);
        }

        setupInteractions(post);
    }

    private void setupOptionsBtn(Post post) {
        if (btnOptions == null) return;
        boolean isOwner = post.getPublisherId() != null && post.getPublisherId().equals(currentUserId);

        btnOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            if (isOwner) {
                popup.getMenu().add("Modifier");
                popup.getMenu().add("Supprimer la publication");
            } else {
                popup.getMenu().add("Signaler la publication");
            }

            popup.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if (title.equals("Supprimer la publication")) confirmDeletion(post);
                else if (title.equals("Modifier")) Toast.makeText(getContext(), "Bientôt disponible", Toast.LENGTH_SHORT).show();
                else if (title.equals("Signaler la publication")) Toast.makeText(getContext(), "Signalement envoyé", Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });
    }

    private void confirmDeletion(Post post) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Supprimer ?")
                .setMessage("Voulez-vous vraiment effacer cette publication ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    db.collection("posts").document(post.getPostId()).delete().addOnSuccessListener(aVoid -> {
                        if (currentUserId != null) {
                            db.collection("users").document(currentUserId).update("postsCount", FieldValue.increment(-1));
                        }
                        Toast.makeText(getContext(), "Supprimé", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void openGoogleMaps(double lat, double lon) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lon);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lon)));
        }
    }

    private void setupInteractions(Post post) {
        int likes = (post.getLikedBy() != null) ? post.getLikedBy().size() : 0;
        if (likeCount != null) likeCount.setText(likes + " likes");

        boolean isLiked = post.getLikedBy() != null && post.getLikedBy().contains(currentUserId);
        if (btnLike != null) {
            btnLike.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
            btnLike.setOnClickListener(v -> {
                if (isAnonymous) Toast.makeText(getContext(), "Connectez-vous pour liker", Toast.LENGTH_SHORT).show();
                else toggleLike(post, isLiked);
            });
        }

        if (btnComment != null) {
            btnComment.setOnClickListener(v -> new CommentBottomSheet(post.getPostId()).show(getParentFragmentManager(), "CommentSheet"));
        }
    }

    private void toggleLike(Post post, boolean liked) {
        db.collection("posts").document(post.getPostId()).update("likedBy",
                liked ? FieldValue.arrayRemove(currentUserId) : FieldValue.arrayUnion(currentUserId));
    }

    private void setupFollowButton(String authorId) {
        if (btnFollow == null || getContext() == null) return;

        if (isAnonymous || currentUserId == null || currentUserId.equals(authorId)) {
            btnFollow.setVisibility(View.GONE);
            return;
        }

        btnFollow.setVisibility(View.VISIBLE);

        if (followListener != null) followListener.remove();

        followListener = db.collection("users").document(currentUserId).collection("following").document(authorId)
                .addSnapshotListener((doc, e) -> {
                    if (getContext() == null || doc == null) return;

                    if (doc.exists()) {
                        btnFollow.setText("Abonné");
                        btnFollow.setBackgroundResource(R.drawable.bg_button_followed);
                    } else {
                        btnFollow.setText("S'abonner");
                        btnFollow.setBackgroundResource(R.drawable.bg_button_follow);
                    }
                });

        btnFollow.setOnClickListener(v -> toggleFollow(authorId));
    }

    private void toggleFollow(String targetUserId) {
        if (isAnonymous) {
            Toast.makeText(getContext(), "Inscrivez-vous pour suivre", Toast.LENGTH_SHORT).show();
            return;
        }
        DocumentReference followingRef = db.collection("users").document(currentUserId).collection("following").document(targetUserId);
        followingRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                followingRef.delete();
                db.collection("users").document(targetUserId).collection("followers").document(currentUserId).delete();
                updateFollowCounts(targetUserId, -1);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("timestamp", FieldValue.serverTimestamp());
                followingRef.set(data);
                db.collection("users").document(targetUserId).collection("followers").document(currentUserId).set(data);
                updateFollowCounts(targetUserId, 1);
            }
        });
    }

    private void updateFollowCounts(String targetId, int inc) {
        db.collection("users").document(currentUserId).update("followingCount", FieldValue.increment(inc));
        db.collection("users").document(targetId).update("followersCount", FieldValue.increment(inc));
    }

    private void loadAuthorProfile(String authorId) {
        if (authorId == null) return;
        if (authorListener != null) authorListener.remove();
        authorListener = db.collection("users").document(authorId).addSnapshotListener((doc, e) -> {
            if (isAdded() && doc != null && doc.exists()) {
                String url = doc.getString("profileImageUrl");
                if (imageProfile != null) Glide.with(this).load(url).circleCrop().placeholder(R.drawable.ic_person).into(imageProfile);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postListener != null) postListener.remove();
        if (followListener != null) followListener.remove();
        if (authorListener != null) authorListener.remove();
    }
}