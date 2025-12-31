package com.example.numidiapath;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private String groupId;
    private String groupName;
    private String description;
    private String adminId;
    private List<String> members;
    private long timestamp;

    public Group() {
        this.members = new ArrayList<>();
    }

    public Group(String groupName, String description, String adminId) {
        this.groupName = groupName;
        this.description = description;
        this.adminId = adminId;
        this.members = new ArrayList<>();
        this.members.add(adminId); // Le créateur est le premier membre
        this.timestamp = System.currentTimeMillis();
    }

    // Getters et Setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public String getDescription() { return description; }
    public String getAdminId() { return adminId; }
    public List<String> getMembers() { return members; }
}