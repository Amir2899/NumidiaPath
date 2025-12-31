package com.example.numidiapath;

import java.util.ArrayList;
import java.util.List;

public class DataServer {
    private static DataServer instance;
    private List<Post> allPosts;

    private DataServer() {
        allPosts = new ArrayList<>();
        // Note: On utilise une URL vide ou de test car le constructeur attend un String
        allPosts.add(new Post("Amir IDRIGUENE", "Montpellier", "Bienvenue sur NumidiaPath !", "https://via.placeholder.com/500"));
    }

    public static DataServer getInstance() {
        if (instance == null) instance = new DataServer();
        return instance;
    }

    public List<Post> getAllPosts() {
        return allPosts;
    }

    public void addPost(Post post) {
        allPosts.add(0, post);
    }
}