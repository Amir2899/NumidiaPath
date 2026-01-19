package com.example.numidiapath;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private final Map<Integer, ListenerRegistration> userListeners = new HashMap<>();

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        // On vérifie si le parent est bien un RecyclerView avant de caster
        if (parent instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) parent;
            if (recyclerView.getLayoutManager() instanceof GridLayoutManager) {
                view = inflater.inflate(R.layout.item_explore, parent, false);
            } else {
                view = inflater.inflate(R.layout.item_post, parent, false);
            }
        } else {
            // Cas par défaut au cas où
            view = inflater.inflate(R.layout.item_post, parent, false);
        }

        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        boolean isAnonymous = (currentUser == null || currentUser.isAnonymous());
        String currentUserId = (currentUser != null) ? currentUser.getUid() : null;

        // --- 1. GESTION DE L'AUTEUR (Sécurisée) ---
        cleanupListener(holder);
        if (post.getPublisherId() != null) {
            ListenerRegistration registration = FirebaseFirestore.getInstance().collection("users")
                    .document(post.getPublisherId())
                    .addSnapshotListener((document, error) -> {
                        // FIX: Vérifier que le contexte est toujours valide pour Glide
                        if (document != null && document.exists() && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                            String realUsername = document.getString("username");
                            String realProfilePic = document.getString("profileImageUrl");
                            if (holder.username != null) holder.username.setText(realUsername != null ? realUsername : "Anonyme");
                            if (holder.imageProfile != null && realProfilePic != null) {
                                Glide.with(holder.itemView.getContext()).load(realProfilePic).circleCrop().placeholder(R.drawable.ic_person).into(holder.imageProfile);
                            }
                        }
                    });
            userListeners.put(holder.hashCode(), registration);
        }

        // --- 2. CONTENU DU POST & INTEGRATION ---
        if (holder.locationDate != null) {
            holder.locationDate.setText(post.getLocation() != null ? post.getLocation() : "Lieu inconnu");

            holder.locationDate.setOnClickListener(v -> {
                PopupMenu locationMenu = new PopupMenu(v.getContext(), v);
                locationMenu.getMenu().add("Suivre ce lieu");
                locationMenu.getMenu().add("Planifier un voyage ici (TravelPath)");
                locationMenu.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Suivre ce lieu")) {
                        if (isAnonymous) showAnonWarning(v);
                        else showFollowDialog(v, post.getLocation(), "followedLocations");
                    } else {
                        // ENVOI VERS L'AUTRE APP (Destination + Image URL)
                        openTravelPathDeepLink(v.getContext(), post.getLocation(), post.getImageUrl());
                    }
                    return true;
                });
                locationMenu.show();
            });
        }

        if (holder.description != null) holder.description.setText(post.getCaption() != null ? post.getCaption() : "");

        // GESTION DES TAGS
        if (holder.chipGroupTags != null) {
            holder.chipGroupTags.removeAllViews();
            if (post.getTags() != null) {
                for (String tag : post.getTags()) {
                    Chip chip = new Chip(holder.itemView.getContext());
                    chip.setText("#" + tag);
                    chip.setOnClickListener(v -> {
                        if (isAnonymous) showAnonWarning(v);
                        else showFollowDialog(v, tag, "followedTags");
                    });
                    holder.chipGroupTags.addView(chip);
                }
            }
        }

        if (holder.imagePost != null && post.getImageUrl() != null) {
            Glide.with(holder.itemView.getContext()).load(post.getImageUrl()).centerCrop().placeholder(android.R.drawable.ic_menu_gallery).into(holder.imagePost);
        }

        // --- 3. INTERACTIONS ---
        updateLikeUI(holder, post, currentUserId, isAnonymous);

        if (holder.btnLike != null) {
            holder.btnLike.setOnClickListener(v -> {
                if (isAnonymous) showAnonWarning(v);
                else { animateView(v); handleLikeAction(post, currentUserId); }
            });
        }

        if (holder.btnSave != null) {
            boolean isSaved = !isAnonymous && post.getSavedBy() != null && post.getSavedBy().contains(currentUserId);
            holder.btnSave.setImageResource(isSaved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
            holder.btnSave.setOnClickListener(v -> {
                if (isAnonymous) showAnonWarning(v);
                else { animateView(v); toggleSave(post, currentUserId); }
            });
        }

        if (holder.btnComment != null) {
            holder.btnComment.setOnClickListener(v -> {
                if (post.getPostId() != null) {
                    CommentBottomSheet commentSheet = new CommentBottomSheet(post.getPostId());
                    commentSheet.show(((AppCompatActivity) v.getContext()).getSupportFragmentManager(), "CommentSheet");
                }
            });
        }

        if (holder.btnReport != null) {
            holder.btnReport.setOnClickListener(v -> {
                if (isAnonymous) showAnonWarning(v);
                else showPopupMenu(v, post, currentUserId);
            });
        }
    }

    // MÉTHODE DEEP LINK MISE À JOUR (Passage Destination + Image)
    private void openTravelPathDeepLink(Context context, String location, String imageUrl) {
        if (location == null) return;

        Uri uri = Uri.parse("numediapath://planifier")
                .buildUpon()
                .appendQueryParameter("destination", location)
                .appendQueryParameter("image_url", imageUrl != null ? imageUrl : "")
                .build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Application TravelPath non installée.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLikeAction(Post post, String currentUserId) {
        if (post.getPostId() == null || currentUserId == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        boolean isAlreadyLiked = post.getLikedBy() != null && post.getLikedBy().contains(currentUserId);
        db.collection("posts").document(post.getPostId())
                .update("likedBy", isAlreadyLiked ? FieldValue.arrayRemove(currentUserId) : FieldValue.arrayUnion(currentUserId));
    }

    private void updateLikeUI(PostViewHolder holder, Post post, String userId, boolean isAnon) {
        int likesCount = (post.getLikedBy() != null) ? post.getLikedBy().size() : 0;
        if (holder.textLikeCount != null) holder.textLikeCount.setText(likesCount + " likes");
        if (holder.btnLike != null) {
            boolean isLiked = !isAnon && post.getLikedBy() != null && post.getLikedBy().contains(userId);
            holder.btnLike.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart);
            holder.btnLike.setColorFilter(isLiked ? Color.RED : Color.parseColor("#424242"));
        }
    }

    private void toggleSave(Post post, String userId) {
        if (post.getPostId() == null || userId == null) return;
        boolean isSaved = post.getSavedBy() != null && post.getSavedBy().contains(userId);
        FirebaseFirestore.getInstance().collection("posts").document(post.getPostId())
                .update("savedBy", isSaved ? FieldValue.arrayRemove(userId) : FieldValue.arrayUnion(userId));
    }

    private void showFollowDialog(View v, String value, String fieldName) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        new AlertDialog.Builder(v.getContext())
                .setTitle("Suivre")
                .setMessage("Voulez-vous suivre " + value + " ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                            .update(fieldName, FieldValue.arrayUnion(value))
                            .addOnSuccessListener(aVoid -> Toast.makeText(v.getContext(), "Abonnement réussi", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void showPopupMenu(View v, Post post, String currentUserId) {
        PopupMenu popup = new PopupMenu(v.getContext(), v);
        if (post.getPublisherId() == null || currentUserId == null) return;

        boolean isOwner = post.getPublisherId().equals(currentUserId);
        if (isOwner) {
            popup.getMenu().add("Modifier la légende");
            popup.getMenu().add("Supprimer la publication");
        } else {
            popup.getMenu().add("Signaler le contenu");
        }
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.contains("Supprimer")) confirmDeletion(post, v);
            else if (title.contains("Modifier")) showEditDialog(post, v);
            else Toast.makeText(v.getContext(), "Signalement envoyé", Toast.LENGTH_SHORT).show();
            return true;
        });
        popup.show();
    }

    private void confirmDeletion(Post post, View v) {
        new AlertDialog.Builder(v.getContext()).setTitle("Supprimer").setMessage("Voulez-vous supprimer ?")
                .setPositiveButton("Supprimer", (d, i) -> {
                    FirebaseFirestore.getInstance().collection("posts").document(post.getPostId()).delete();
                }).setNegativeButton("Annuler", null).show();
    }

    private void showEditDialog(Post post, View v) {
        EditText input = new EditText(v.getContext()); input.setText(post.getCaption());
        new AlertDialog.Builder(v.getContext()).setTitle("Modifier").setView(input)
                .setPositiveButton("Enregistrer", (d, w) -> {
                    FirebaseFirestore.getInstance().collection("posts").document(post.getPostId()).update("caption", input.getText().toString());
                }).setNegativeButton("Annuler", null).show();
    }

    private void animateView(View v) {
        if (v == null) return;
        v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction(() ->
                v.animate().scaleX(1.0f).scaleY(1.0f).start()).start();
    }

    private void showAnonWarning(View v) {
        Toast.makeText(v.getContext(), "Connexion requise", Toast.LENGTH_SHORT).show();
    }

    private void cleanupListener(PostViewHolder holder) {
        if (userListeners.containsKey(holder.hashCode())) {
            userListeners.get(holder.hashCode()).remove();
            userListeners.remove(holder.hashCode());
        }
    }

    @Override
    public void onViewRecycled(@NonNull PostViewHolder holder) {
        super.onViewRecycled(holder);
        cleanupListener(holder);
    }

    @Override
    public int getItemCount() { return (postList != null) ? postList.size() : 0; }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView username, locationDate, description, textLikeCount;
        ImageView imagePost, btnLike, btnComment, btnReport, btnSave, imageProfile;
        ChipGroup chipGroupTags;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.textUsername);
            locationDate = itemView.findViewById(R.id.textLocationDate);
            description = itemView.findViewById(R.id.textDescription);
            textLikeCount = itemView.findViewById(R.id.textLikeCount);
            imagePost = itemView.findViewById(R.id.imagePost);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnReport = itemView.findViewById(R.id.btnReport);
            btnSave = itemView.findViewById(R.id.btnSave);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            chipGroupTags = itemView.findViewById(R.id.chipGroupPostTags);
        }
    }
}