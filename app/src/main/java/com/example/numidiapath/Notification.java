package com.example.numidiapath;

public class Notification {
    // Les noms des variables doivent être IDENTIQUES aux clés dans Firebase
    private String userid;
    private String text;
    private String postid;
    private boolean ispost;
    private long timestamp; // Changement de Object à long pour le calcul du temps réel
    private boolean read; // Champ pour marquer si la notification a été lue

    public Notification() {
        // Constructeur vide obligatoire pour Firebase
    }

    public Notification(String userid, String text, String postid, boolean ispost, long timestamp) {
        this.userid = userid;
        this.text = text;
        this.postid = postid;
        this.ispost = ispost;
        this.timestamp = timestamp;
        this.read = false; // Par défaut, une nouvelle notification est non lue
    }

    // --- Getters et Setters ---

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPostid() {
        return postid;
    }

    public void setPostid(String postid) {
        this.postid = postid;
    }

    public boolean isIspost() {
        return ispost;
    }

    public void setIspost(boolean ispost) {
        this.ispost = ispost;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}