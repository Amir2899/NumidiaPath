package com.example.numidiapath;

import com.google.firebase.firestore.PropertyName;

/**
 * Modèle de données pour l'utilisateur de NumidiaPath.
 * Synchronisé avec les fonctionnalités de statistiques sociales et de profil.
 */
public class User {
    private String uid;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String birthDate;
    private String profileImageUrl;
    private String bio;

    // --- STATISTIQUES SOCIALES ---
    private int followersCount;
    private int followingCount;
    private int postsCount;

    /**
     * Constructeur vide : INDISPENSABLE pour Firebase Firestore.
     */
    public User() {
        // Initialisation par défaut pour éviter les valeurs nulles lors de la désérialisation
        this.followersCount = 0;
        this.followingCount = 0;
        this.postsCount = 0;
    }

    /**
     * Constructeur complet utilisé lors de l'inscription.
     */
    public User(String uid, String firstName, String lastName, String username,
                String email, String birthDate, String profileImageUrl, String bio) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
        this.birthDate = birthDate;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.followersCount = 0;
        this.followingCount = 0;
        this.postsCount = 0;
    }

    // --- GETTERS ET SETTERS DE BASE ---

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFirstName() { return (firstName != null) ? firstName : ""; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return (lastName != null) ? lastName : ""; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getUsername() {
        return (username != null && !username.isEmpty()) ? username : "Explorateur";
    }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getBirthDate() { return (birthDate != null) ? birthDate : ""; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getProfileImageUrl() { return (profileImageUrl != null) ? profileImageUrl : ""; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getBio() {
        return (bio != null && !bio.isEmpty()) ? bio : "Passionné de voyages.";
    }
    public void setBio(String bio) { this.bio = bio; }

    // --- ACCESSEURS POUR LES STATISTIQUES (Mapping explicite) ---

    @PropertyName("followersCount")
    public int getFollowersCount() { return followersCount; }

    @PropertyName("followersCount")
    public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }

    @PropertyName("followingCount")
    public int getFollowingCount() { return followingCount; }

    @PropertyName("followingCount")
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    @PropertyName("postsCount")
    public int getPostsCount() { return postsCount; }

    @PropertyName("postsCount")
    public void setPostsCount(int postsCount) { this.postsCount = postsCount; }

    // --- LOGIQUE UI ---

    public String getFullName() {
        String first = getFirstName().trim();
        String last = getLastName().trim();
        if (first.isEmpty() && last.isEmpty()) {
            return getUsername();
        }
        return (first + " " + last).trim();
    }
}