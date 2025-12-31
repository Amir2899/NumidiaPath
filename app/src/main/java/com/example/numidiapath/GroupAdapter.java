package com.example.numidiapath;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private List<Group> groupList;
    private String currentUserId;

    public GroupAdapter(List<Group> groupList) {
        this.groupList = groupList;
        // Vérification de sécurité pour éviter un crash si l'utilisateur se déconnecte
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groupList.get(position);

        holder.groupName.setText(group.getGroupName());
        holder.groupDesc.setText(group.getDescription());

        int memberCount = group.getMembers() != null ? group.getMembers().size() : 0;
        holder.memberCount.setText(memberCount + " membres");

        // Vérifier si l'utilisateur est déjà membre
        boolean isMember = group.getMembers() != null && group.getMembers().contains(currentUserId);

        if (isMember) {
            holder.btnAction.setText("Voir");
            holder.btnAction.setBackgroundTintList(null);
        } else {
            holder.btnAction.setText("Rejoindre");
        }

        holder.btnAction.setOnClickListener(v -> {
            if (isMember) {
                // CORRECTION : On passe la vue pour récupérer le contexte sans crasher
                openGroupDetails(group, v);
            } else {
                joinGroup(group, holder);
            }
        });
    }

    private void joinGroup(Group group, GroupViewHolder holder) {
        FirebaseFirestore.getInstance().collection("groups")
                .document(group.getGroupId())
                .update("members", FieldValue.arrayUnion(currentUserId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(holder.itemView.getContext(), "Bienvenue dans " + group.getGroupName(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(holder.itemView.getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // MISE À JOUR : Navigation sécurisée pour éviter le crash
    private void openGroupDetails(Group group, View view) {
        Context context = view.getContext();

        // Navigation vers le fragment qui affiche les publications du groupe
        GroupFeedFragment fragment = new GroupFeedFragment();
        Bundle args = new Bundle();
        args.putString("groupId", group.getGroupId());
        args.putString("groupName", group.getGroupName());
        fragment.setArguments(args);

        if (context instanceof AppCompatActivity) {
            ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            // Secours au cas où le contexte n'est pas l'activité attendue
            Toast.makeText(context, "Ouverture de " + group.getGroupName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return groupList != null ? groupList.size() : 0;
    }

    public static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupName, groupDesc, memberCount;
        Button btnAction;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.textGroupName);
            groupDesc = itemView.findViewById(R.id.textGroupDescription);
            memberCount = itemView.findViewById(R.id.textMemberCount);
            btnAction = itemView.findViewById(R.id.btnGroupAction);
        }
    }
}