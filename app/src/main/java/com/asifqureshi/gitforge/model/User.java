package com.asifqureshi.gitforge.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("login")
    private String login;

    @SerializedName("id")
    private long id;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("name")
    private String name;

    @SerializedName("company")
    private String company;

    @SerializedName("bio")
    private String bio;

    @SerializedName("public_repos")
    private int publicRepos;

    @SerializedName("total_private_repos")
    private int totalPrivateRepos;

    @SerializedName("email")
    private String email;

    public User() {}

    public User(String login, String name, String avatarUrl, int publicRepos, int totalPrivateRepos) {
        this.login = login;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.publicRepos = publicRepos;
        this.totalPrivateRepos = totalPrivateRepos;
    }

    public String getLogin() { return login != null ? login : ""; }
    public void setLogin(String login) { this.login = login; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getAvatarUrl() { return avatarUrl != null ? avatarUrl : ""; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getName() { return (name != null && !name.isEmpty()) ? name : getLogin(); }
    public void setName(String name) { this.name = name; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getBio() { return bio != null ? bio : ""; }
    public void setBio(String bio) { this.bio = bio; }

    public int getPublicRepos() { return publicRepos; }
    public void setPublicRepos(int publicRepos) { this.publicRepos = publicRepos; }

    public int getTotalPrivateRepos() { return totalPrivateRepos; }
    public void setTotalPrivateRepos(int totalPrivateRepos) { this.totalPrivateRepos = totalPrivateRepos; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
