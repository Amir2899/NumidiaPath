package com.example.numidiapath;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private String postId;

    public CommentAdapter(List<Comment> commentList, String postId) {
        this.commentList = commentList;
        this.postId = postId;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        Context context = holder.itemView.getContext();

        holder.commentText.setText(comment.getCommentText());
        holder.authorText.setText(comment.getUsername() != null ? comment.getUsername() : "Utilisateur");

        // --- NOUVEAU : Affichage de l'heure réelle ---
        if (comment.getTimestamp() > 0) {
            holder.timeText.setText(getTimeAgo(comment.getTimestamp()));
            holder.timeText.setVisibility(View.VISIBLE);
        } else {
            holder.timeText.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(comment.getProfileImageUrl())
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_report_image)
                .into(holder.profileImage);

        holder.itemView.setOnLongClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && comment.getPublisherId() != null &&
                    comment.getPublisherId().equals(currentUser.getUid())) {
                showOptionsPicker(context, v, comment);
            }
            return true;
        });
    }

    // --- Méthode utilitaire pour le calcul du temps écoulé ---
    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) return "À l'instant";
        if (diff < 3600000) return "Il y a " + (diff / 60000) + " min";
        if (diff < 86400000) return "Il y a " + (diff / 3600000) + " h";
        return "Il y a " + (diff / 86400000) + " j";
    }

    private void showOptionsPicker(Context context, View anchorView, Comment comment) {
        PopupMenu popupMenu = new PopupMenu(context, anchorView);
        popupMenu.getMenu().add("Modifier");
        popupMenu.getMenu().add("Supprimer");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (comment.getCommentId() == null || comment.getCommentId().isEmpty()) {
                Toast.makeText(context, "ID introuvable", Toast.LENGTH_SHORT).show();
                return true;
            }

            if (item.getTitle().equals("Supprimer")) {
                showDeleteConfirmation(context, comment);
            } else if (item.getTitle().equals("Modifier")) {
                showEditDialog(context, comment);
            }
            return true;
        });
        popupMenu.show();
    }

    private void showEditDialog(Context context, Comment comment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Modifier votre commentaire");

        final EditText input = new EditText(context);
        input.setText(comment.getCommentText());
        builder.setView(input);

        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            String newText = input.getText().toString().trim();
            if (!newText.isEmpty()) {
                FirebaseFirestore.getInstance()
                        .collection("posts").document(postId)
                        .collection("comments").document(comment.getCommentId())
                        .update("commentText", newText)
                        .addOnSuccessListener(aVoid -> Toast.makeText(context, "Mis à jour !", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(context, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void showDeleteConfirmation(Context context, Comment comment) {
        new AlertDialog.Builder(context)
                .setTitle("Supprimer")
                .setMessage("Voulez-vous supprimer ce commentaire ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    FirebaseFirestore.getInstance()
                            .collection("posts").document(postId)
                            .collection("comments").document(comment.getCommentId())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(context, "Supprimé", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(context, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Non", null)
                .show();
    }

    public static void sendCommentNotification(String postId, String commenterId, String commentText) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("posts").document(postId).get().addOnSuccessListener(postDoc -> {
            if (postDoc.exists()) {
                String publisherId = postDoc.getString("publisherId");

                if (publisherId != null && !publisherId.equals(commenterId)) {
                    db.collection("users").document(commenterId).get().addOnSuccessListener(userDoc -> {
                        String senderName = userDoc.getString("username");
                        if (senderName == null) senderName = "Un explorateur";

                        NotificationHelper.sendNotificationToUser(
                                publisherId,
                                senderName,
                                "a commenté : " + commentText
                        );
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorText, commentText, timeText; // Ajout de timeText
        ShapeableImageView profileImage;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorText = itemView.findViewById(R.id.commentUsername);
            commentText = itemView.findViewById(R.id.commentText);
            profileImage = itemView.findViewById(R.id.commentProfileImage);
            // Assurez-vous d'ajouter ce TextView dans item_comment.xml
            timeText = itemView.findViewById(R.id.commentTime);
        }
    }
}