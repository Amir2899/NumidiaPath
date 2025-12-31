package com.example.numidiapath;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProfileGridAdapter extends RecyclerView.Adapter<ProfileGridAdapter.GridViewHolder> {

    private List<Post> posts;
    private OnPostClickListener listener; // Écouteur pour la communication avec le Fragment

    // --- INTERFACE DE CLIC ---
    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    // --- CONSTRUCTEUR MIS À JOUR (Correction de l'erreur de compilation) ---
    public ProfileGridAdapter(List<Post> posts, OnPostClickListener listener) {
        this.posts = posts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // CORRECTION : Utilisation du layout spécifique au profil, sans le bouton "Suivre"
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_grid_profile, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        Post post = posts.get(position);

        // 1. Chargement de l'image
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(post.getImageUrl())
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.imageView);
        }

        // 2. Clic simple : Utilise l'interface passée par le Fragment
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            }
        });

        // 3. Clic long : Menu contextuel pour gérer les options
        holder.itemView.setOnLongClickListener(v -> {
            showPostOptions(v, post, holder.getAdapterPosition());
            return true;
        });
    }

    private void showPostOptions(View view, Post post, int position) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        popupMenu.getMenu().add("Modifier");
        popupMenu.getMenu().add("Supprimer");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Supprimer")) {
                confirmDeletion(view.getContext(), post, position);
            } else if (item.getTitle().equals("Modifier")) {
                Toast.makeText(view.getContext(), "Modification bientôt disponible", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        popupMenu.show();
    }

    private void confirmDeletion(Context context, Post post, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Supprimer l'aventure")
                .setMessage("Êtes-vous sûr de vouloir supprimer cette publication ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    deletePostFromFirestore(context, post, position);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deletePostFromFirestore(Context context, Post post, int position) {
        if (position == RecyclerView.NO_POSITION) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("posts").document(post.getPostId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // MISE À JOUR ATOMIQUE : Décrémente le compteur global du profil
                    FirestoreHelper.decrementPostCount(post.getPublisherId());

                    // Mise à jour de la liste locale pour un retrait visuel immédiat
                    if (position < posts.size()) {
                        posts.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, posts.size());
                    }

                    Toast.makeText(context, "Publication supprimée", Toast.LENGTH_SHORT).show();

                    // NOUVEAU : Synchronise le compteur pour être sûr du résultat
                    FirestoreHelper.syncPostCount(post.getPublisherId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Échec de la suppression", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return (posts != null) ? posts.size() : 0;
    }

    public static class GridViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imagePostGrid);
        }
    }
}