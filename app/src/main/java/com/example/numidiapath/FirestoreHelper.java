package com.example.numidiapath;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FirestoreHelper {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // --- LOGIQUE FOLLOW / UNFOLLOW ---

    public static void followUser(String currentUid, String targetUid, OnTaskCompleteListener callback) {
        if (currentUid == null || targetUid == null || currentUid.equals(targetUid)) return;

        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", FieldValue.serverTimestamp());

        DocumentReference followingRef = db.collection("users").document(currentUid)
                .collection("following").document(targetUid);
        DocumentReference followersRef = db.collection("users").document(targetUid)
                .collection("followers").document(currentUid);
        DocumentReference currentUserRef = db.collection("users").document(currentUid);
        DocumentReference targetUserRef = db.collection("users").document(targetUid);

        db.runTransaction(transaction -> {
                    transaction.set(followingRef, data);
                    transaction.set(followersRef, data);

                    transaction.update(currentUserRef, "followingCount", FieldValue.increment(1));
                    transaction.update(targetUserRef, "followersCount", FieldValue.increment(1));

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public static void unfollowUser(String currentUid, String targetUid, OnTaskCompleteListener callback) {
        DocumentReference followingRef = db.collection("users").document(currentUid)
                .collection("following").document(targetUid);
        DocumentReference followersRef = db.collection("users").document(targetUid)
                .collection("followers").document(currentUid);
        DocumentReference currentUserRef = db.collection("users").document(currentUid);
        DocumentReference targetUserRef = db.collection("users").document(targetUid);

        db.runTransaction(transaction -> {
                    transaction.delete(followingRef);
                    transaction.delete(followersRef);

                    transaction.update(currentUserRef, "followingCount", FieldValue.increment(-1));
                    transaction.update(targetUserRef, "followersCount", FieldValue.increment(-1));

                    return null;
                }).addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // --- LOGIQUE DES POSTS ---

    public static void incrementPostCount(String uid) {
        if (uid == null) return;
        db.collection("users").document(uid)
                .update("postsCount", FieldValue.increment(1))
                .addOnFailureListener(e -> {
                    Map<String, Object> initialData = new HashMap<>();
                    initialData.put("postsCount", 1);
                    db.collection("users").document(uid).set(initialData, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    public static void decrementPostCount(String uid) {
        if (uid == null) return;
        DocumentReference userRef = db.collection("users").document(uid);
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(userRef);
            Long currentCount = snapshot.getLong("postsCount");
            if (currentCount != null && currentCount > 0) {
                transaction.update(userRef, "postsCount", FieldValue.increment(-1));
            }
            return null;
        });
    }

    /**
     * MÉTHODE MANQUANTE : Synchronise le compteur réel en comptant les documents.
     */
    public static void syncPostCount(String uid) {
        if (uid == null) return;
        db.collection("posts")
                .whereEqualTo("publisherId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int actualCount = queryDocumentSnapshots.size();
                    db.collection("users").document(uid)
                            .update("postsCount", actualCount);
                });
    }

    // --- INTERFACE DE CALLBACK ---

    public interface OnTaskCompleteListener {
        void onSuccess();
        void onFailure(String error);
    }
}