package com.example.numidiapath;

import java.util.ArrayList;
import java.util.List;

public class Post {
    private String postId;
    private String publisherId;
    private String username;
    private String location;
    private String caption;
    private String imageUrl;
    private long timestamp;

    // NOUVEAU : Champ pour lier le post à un groupe (ou vide si public)
    private String groupId;

    // Listes pour les interactions
    private List<String> likedBy = new ArrayList<>();
    private List<String> savedBy = new ArrayList<>();

    // Tags IA / Catégories
    private List<String> tags = new ArrayList<>();

    // Coordonnées pour la carte et l'itinéraire
    private double latitude;
    private double longitude;

    // --- CONSTRUCTEURS ---

    // Constructeur vide REQUIS par Firebase
    public Post() {
        this.likedBy = new ArrayList<>();
        this.savedBy = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.groupId = ""; // Initialisation par défaut
    }

    public Post(String username, String location, String caption, String imageUrl) {
        this.username = username;
        this.location = location;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.timestamp = System.currentTimeMillis();
        this.likedBy = new ArrayList<>();
        this.savedBy = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.groupId = ""; // Par défaut public
    }

    // --- NOUVEAU : GETTER ET SETTER POUR GROUPID ---

    public String getGroupId() {
        return (groupId != null) ? groupId : "";
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    // --- GETTERS ET SETTERS EXISTANTS ---

    public String getPostId() {
        return (postId != null) ? postId : "";
    }
    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPublisherId() {
        return (publisherId != null) ? publisherId : "";
    }
    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
    }

    public String getUsername() {
        return (username != null && !username.isEmpty()) ? username : "Anonyme";
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getLocation() {
        return (location != null) ? location : "";
    }
    public void setLocation(String location) {
        this.location = location;
    }

    public String getCaption() {
        return (caption != null) ? caption : "";
    }
    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getImageUrl() {
        return (imageUrl != null) ? imageUrl : "";
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // --- GESTION SÉCURISÉE DES LISTES ---

    public List<String> getLikedBy() {
        if (likedBy == null) likedBy = new ArrayList<>();
        return likedBy;
    }

    public void setLikedBy(List<String> likedBy) {
        this.likedBy = (likedBy != null) ? likedBy : new ArrayList<>();
    }

    public List<String> getSavedBy() {
        if (savedBy == null) savedBy = new ArrayList<>();
        return savedBy;
    }

    public void setSavedBy(List<String> savedBy) {
        this.savedBy = (savedBy != null) ? savedBy : new ArrayList<>();
    }

    public List<String> getTags() {
        if (tags == null) tags = new ArrayList<>();
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = (tags != null) ? tags : new ArrayList<>();
    }
}