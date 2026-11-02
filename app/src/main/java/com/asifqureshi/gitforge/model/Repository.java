package com.asifqureshi.gitforge.model;

import com.google.gson.annotations.SerializedName;

public class Repository {
    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("private")
    private boolean isPrivate;

    @SerializedName("default_branch")
    private String defaultBranch;

    @SerializedName("description")
    private String description;

    @SerializedName("html_url")
    private String htmlUrl;

    @SerializedName("stargazers_count")
    private int stargazersCount;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("fork")
    private boolean fork;

    @SerializedName("owner")
    private User owner;

    public Repository() {}

    public Repository(long id, String name, String fullName, boolean isPrivate, String defaultBranch) {
        this.id = id;
        this.name = name;
        this.fullName = fullName;
        this.isPrivate = isPrivate;
        this.defaultBranch = defaultBranch;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public String getFullName() { return fullName != null ? fullName : getName(); }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    public String getDefaultBranch() {
        return (defaultBranch != null && !defaultBranch.isEmpty()) ? defaultBranch : "main";
    }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public String getDescription() { return description != null ? description : "No description provided"; }
    public void setDescription(String description) { this.description = description; }

    public String getHtmlUrl() { return htmlUrl != null ? htmlUrl : ""; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

    public int getStargazersCount() { return stargazersCount; }
    public void setStargazersCount(int stargazersCount) { this.stargazersCount = stargazersCount; }

    public String getUpdatedAt() { return updatedAt != null ? updatedAt : ""; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public boolean isFork() { return fork; }
    public void setFork(boolean fork) { this.fork = fork; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
}
