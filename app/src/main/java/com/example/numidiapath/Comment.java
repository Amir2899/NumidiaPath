package com.example.numidiapath;

public class Comment {
    // Identifiant unique du document (utile pour Firestore ou Realtime Database)
    private String commentId;
    private String commentText;
    private String publisherId; // Assure-toi que c'est la clé utilisée dans ta base
    private String username;
    private String profileImageUrl;
    private long timestamp; // Utilise long pour le calcul du temps réel (TimeAgo)

    // Constructeur vide requis par Firebase
    public Comment() {
    }

    // Constructeur complet
    public Comment(String commentId, String commentText, String publisherId, String username, String profileImageUrl, long timestamp) {
        this.commentId = commentId;
        this.commentText = commentText;
        this.publisherId = publisherId;
        this.username = username;
        this.profileImageUrl = profileImageUrl;
        this.timestamp = timestamp;
    }

    // --- GETTERS ET SETTERS ---

    public String getCommentId() {
        return commentId != null ? commentId : "";
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getCommentText() {
        return commentText != null ? commentText : "";
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public String getPublisherId() {
        return publisherId != null ? publisherId : "";
    }

    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
    }

    public String getUsername() {
        return username != null ? username : "Anonyme";
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileImageUrl() {
        return profileImageUrl != null ? profileImageUrl : "";
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}