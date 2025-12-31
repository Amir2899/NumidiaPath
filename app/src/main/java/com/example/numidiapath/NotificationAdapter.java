package com.example.numidiapath;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final Context mContext;
    private final List<Notification> mNotifications;

    public NotificationAdapter(Context mContext, List<Notification> mNotifications) {
        this.mContext = mContext;
        this.mNotifications = mNotifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.notification_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = mNotifications.get(position);

        if (notification == null) return;

        holder.text.setText(notification.getText());

        // --- NOUVEAU : Affichage de l'heure réelle ---
        if (notification.getTimestamp() > 0) {
            holder.time_text.setText(getTimeAgo(notification.getTimestamp()));
            holder.time_text.setVisibility(View.VISIBLE);
        } else {
            holder.time_text.setVisibility(View.GONE);
        }

        if (notification.getUserid() != null) {
            getUserInfo(holder.image_profile, holder.username, notification.getUserid());
        }

        if (notification.isIspost() && notification.getPostid() != null && !notification.getPostid().isEmpty()) {
            holder.post_image.setVisibility(View.VISIBLE);
            getPostImage(holder.post_image, notification.getPostid());
        } else {
            holder.post_image.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mNotifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image_profile, post_image;
        public TextView username, text, time_text; // Ajout de time_text

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image_profile = itemView.findViewById(R.id.image_profile);
            post_image = itemView.findViewById(R.id.post_image);
            username = itemView.findViewById(R.id.username);
            text = itemView.findViewById(R.id.comment);
            // Assure-toi que cet ID existe dans ton layout notification_item.xml
            time_text = itemView.findViewById(R.id.time_text);
        }
    }

    // --- Méthode de calcul du temps écoulé ---
    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) return "À l'instant";
        if (diff < 3600000) return "Il y a " + (diff / 60000) + " min";
        if (diff < 86400000) return "Il y a " + (diff / 3600000) + " h";
        return "Il y a " + (diff / 86400000) + " j";
    }

    private void getUserInfo(final ImageView imageView, final TextView username, String publisherid) {
        FirebaseFirestore.getInstance().collection("users").document(publisherid)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("username");
                        String image = documentSnapshot.getString("profileImageUrl");

                        if (username != null) username.setText(name);
                        if (imageView != null && mContext != null) {
                            Glide.with(mContext.getApplicationContext())
                                    .load(image)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_person)
                                    .into(imageView);
                        }
                    }
                });
    }

    private void getPostImage(final ImageView imageView, String postid) {
        FirebaseFirestore.getInstance().collection("posts").document(postid)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String postImage = documentSnapshot.getString("imageUrl");
                        if (imageView != null && mContext != null) {
                            Glide.with(mContext.getApplicationContext())
                                    .load(postImage)
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .into(imageView);
                        }
                    }
                });
    }
}