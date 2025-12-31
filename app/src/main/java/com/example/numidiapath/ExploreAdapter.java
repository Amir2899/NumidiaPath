package com.example.numidiapath;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExploreAdapter extends RecyclerView.Adapter<ExploreAdapter.ExploreViewHolder> {

    private List<Post> posts;
    private OnItemClickListener listener;
    private FirebaseFirestore db;
    private String currentUserId;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(Post post);
    }

    public ExploreAdapter(List<Post> posts, OnItemClickListener listener) {
        this.posts = posts;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
        if(FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    public void updateList(List<Post> newList) {
        this.posts = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExploreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        // CORRECTION : Utilisation du bon layout pour la grille
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_grid, parent, false);
        return new ExploreViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExploreViewHolder holder, int position) {
        Post post = posts.get(position);

        // Chargement de l'image du post
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getImageUrl())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.imagePost);
        }

        // Affichage du nom d'utilisateur
        if (holder.username != null && post.getUsername() != null) {
            holder.username.setText("@" + post.getUsername());
        }

        // --- LOGIQUE DU BOUTON SUIVRE ---

        // Ne rien faire si l'utilisateur n'est pas connecté ou si c'est son propre post
        if (currentUserId == null || post.getPublisherId() == null || post.getPublisherId().equals(currentUserId)) {
            holder.btnFollow.setVisibility(View.GONE);
        } else {
            holder.btnFollow.setVisibility(View.VISIBLE);
            checkFollowStatus(post.getPublisherId(), holder.btnFollow);
            holder.btnFollow.setOnClickListener(v -> toggleFollow(post.getPublisherId(), holder.btnFollow));
        }

        // Clic sur l'image pour voir le détail du post
        holder.imagePost.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(post);
            }
        });
    }

    private void checkFollowStatus(String targetId, Button btnFollow) {
        if (currentUserId == null) return;
        db.collection("users").document(currentUserId)
                .collection("following").document(targetId)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && context != null) {
                        if (doc.exists()) {
                            btnFollow.setText("Abonné");
                            btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray)));
                        } else {
                            btnFollow.setText("Suivre");
                            btnFollow.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.purple_500)));
                        }
                    }
                });
    }

    private void toggleFollow(String targetId, Button btnFollow) {
        if (currentUserId == null) return;
        db.collection("users").document(currentUserId).collection("following").document(targetId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // Unfollow
                db.collection("users").document(currentUserId).collection("following").document(targetId).delete();
                db.collection("users").document(targetId).collection("followers").document(currentUserId).delete();
                updateCounts(targetId, -1);
            } else {
                // Follow
                Map<String, Object> data = new HashMap<>();
                data.put("timestamp", FieldValue.serverTimestamp());
                db.collection("users").document(currentUserId).collection("following").document(targetId).set(data);
                db.collection("users").document(targetId).collection("followers").document(currentUserId).set(data);
                updateCounts(targetId, 1);
                sendFollowNotification(targetId);
            }
        });
    }
    
    private void sendFollowNotification(String targetId) {
        if (currentUserId == null) return;
        db.collection("users").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String senderName = documentSnapshot.getString("username");
                if (senderName == null) senderName = "Un explorateur";

                NotificationHelper.sendNotificationToUser(
                        targetId,
                        senderName,
                        NotificationHelper.TYPE_FOLLOW
                );
            }
        });
    }

    private void updateCounts(String targetId, int increment) {
        if (currentUserId == null) return;
        db.collection("users").document(currentUserId).update("followingCount", FieldValue.increment(increment));
        db.collection("users").document(targetId).update("followersCount", FieldValue.increment(increment));
    }

    @Override
    public int getItemCount() {
        return posts != null ? posts.size() : 0;
    }

    public static class ExploreViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePost;
        TextView username;
        Button btnFollow;

        public ExploreViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePost = itemView.findViewById(R.id.imagePostGrid);
            username = itemView.findViewById(R.id.usernamePostGrid);
            btnFollow = itemView.findViewById(R.id.btnFollowPostGrid);
        }
    }
}