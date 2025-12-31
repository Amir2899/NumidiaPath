package com.example.numidiapath;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private Context context;
    private String currentUserId;
    private FirebaseFirestore db;
    private OnUserClickListener listener; // AJOUT: Interface pour le clic

    // --- INTERFACE POUR LE CLIC ---
    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public UserAdapter(List<User> userList, Context context) {
        this.userList = userList;
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getUid();
    }

    // --- MÉTHODE POUR DÉFINIR LE LISTENER DEPUIS L'EXTÉRIEUR ---
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.username.setText("@" + user.getUsername());
        holder.fullName.setText(user.getFullName());

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(context).load(user.getProfileImageUrl()).circleCrop().into(holder.imageProfile);
        } else {
            holder.imageProfile.setImageResource(R.drawable.ic_person);
        }

        if (currentUserId != null && user.getUid().equals(currentUserId)) {
            holder.btnFollow.setVisibility(View.GONE);
        } else {
            holder.btnFollow.setVisibility(View.VISIBLE);
            checkFollowStatus(user.getUid(), holder.btnFollow);
        }

        // --- GESTION DU CLIC ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // Si un listener est défini (comme dans InviteMembersFragment), on l'utilise
                listener.onUserClick(user);
            } else {
                // Comportement par défaut: ouvrir le profil
                openUserProfile(user.getUid());
            }
        });

        holder.btnFollow.setOnClickListener(v -> toggleFollow(user, holder.btnFollow));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    private void checkFollowStatus(String targetId, Button btnFollow) {
        if(currentUserId == null) return;
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

    private void toggleFollow(User targetUser, Button btnFollow) {
        if(currentUserId == null) return;
        String targetId = targetUser.getUid();
        db.collection("users").document(currentUserId).collection("following").document(targetId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                db.collection("users").document(currentUserId).collection("following").document(targetId).delete();
                db.collection("users").document(targetId).collection("followers").document(currentUserId).delete();
                updateCounts(targetId, -1);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("timestamp", FieldValue.serverTimestamp());
                db.collection("users").document(currentUserId).collection("following").document(targetId).set(data);
                db.collection("users").document(targetId).collection("followers").document(currentUserId).set(data);
                updateCounts(targetId, 1);
                sendFollowNotification(targetUser);
            }
        });
    }

    private void sendFollowNotification(User targetUser) {
        if(currentUserId == null) return;
        db.collection("users").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String senderName = documentSnapshot.getString("username");
                if (senderName == null) senderName = "Un explorateur";
                NotificationHelper.sendNotificationToUser(
                        targetUser.getUid(),
                        senderName,
                        NotificationHelper.TYPE_FOLLOW
                );
            }
        });
    }

    private void updateCounts(String targetId, int increment) {
        if(currentUserId == null) return;
        db.collection("users").document(currentUserId).update("followingCount", FieldValue.increment(increment));
        db.collection("users").document(targetId).update("followersCount", FieldValue.increment(increment));
    }

    private void openUserProfile(String userId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        fragment.setArguments(args);

        if(context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void updateUserList(List<User> newList) {
        userList.clear();
        userList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imageProfile;
        TextView username, fullName;
        Button btnFollow;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProfile = itemView.findViewById(R.id.userItemImage);
            username = itemView.findViewById(R.id.userItemUsername);
            fullName = itemView.findViewById(R.id.userItemFullName);
            btnFollow = itemView.findViewById(R.id.userItemBtnFollow);
        }
    }
}