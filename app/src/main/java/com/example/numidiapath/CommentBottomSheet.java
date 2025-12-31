package com.example.numidiapath;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentBottomSheet extends BottomSheetDialogFragment {

    private String postId;
    private RecyclerView recyclerView;
    private CommentAdapter adapter;
    private List<Comment> commentList;
    private EditText editComment;
    private ImageView btnPublish;
    private FirebaseFirestore db;

    public CommentBottomSheet(String postId) {
        this.postId = postId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_comment, container, false);

        db = FirebaseFirestore.getInstance();
        editComment = view.findViewById(R.id.editComment);
        btnPublish = view.findViewById(R.id.btnPublishComment);
        recyclerView = view.findViewById(R.id.recyclerViewComments);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        commentList = new ArrayList<>();

        adapter = new CommentAdapter(commentList, postId);
        recyclerView.setAdapter(adapter);

        loadComments();

        btnPublish.setOnClickListener(v -> {
            String text = editComment.getText().toString().trim();
            if (!text.isEmpty()) {
                addComment(text);
            } else {
                Toast.makeText(getContext(), "Le commentaire ne peut pas être vide", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void addComment(String text) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        // Récupération sécurisée du nom d'utilisateur
        String userName = (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty())
                ? currentUser.getDisplayName()
                : (currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "Explorateur");

        String userProfilePic = (currentUser.getPhotoUrl() != null)
                ? currentUser.getPhotoUrl().toString()
                : "";

        Map<String, Object> commentData = new HashMap<>();
        commentData.put("commentText", text);
        commentData.put("publisherId", userId);
        commentData.put("username", userName);
        commentData.put("profileImageUrl", userProfilePic);
        commentData.put("timestamp", System.currentTimeMillis());

        db.collection("posts").document(postId)
                .collection("comments")
                .add(commentData)
                .addOnSuccessListener(documentReference -> {
                    // --- CORRECTION : APPEL DE LA NOTIFICATION AVEC LES 3 ARGUMENTS ---
                    // On passe postId, userId et le contenu du texte (text)
                    CommentAdapter.sendCommentNotification(postId, userId, text);

                    editComment.setText("");
                    Toast.makeText(getContext(), "Commentaire ajouté", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadComments() {
        db.collection("posts").document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && isAdded()) { // isAdded() évite les crashs si le fragment est fermé
                        commentList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Comment comment = doc.toObject(Comment.class);
                            if (comment != null) {
                                comment.setCommentId(doc.getId());
                                commentList.add(comment);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}