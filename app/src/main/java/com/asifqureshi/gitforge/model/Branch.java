package com.asifqureshi.gitforge.model;

import com.google.gson.annotations.SerializedName;

public class Branch {
    @SerializedName("name")
    private String name;

    @SerializedName("commit")
    private CommitRef commit;

    @SerializedName("protected")
    private boolean isProtected;

    public Branch() {}

    public Branch(String name, String commitSha) {
        this.name = name;
        this.commit = new CommitRef(commitSha);
    }

    public String getName() { return name != null ? name : ""; }
    public void setName(String name) { this.name = name; }

    public CommitRef getCommit() { return commit; }
    public void setCommit(CommitRef commit) { this.commit = commit; }

    public boolean isProtected() { return isProtected; }
    public void setProtected(boolean aProtected) { isProtected = aProtected; }

    public String getSha() {
        return commit != null ? commit.getSha() : "";
    }

    public static class CommitRef {
        @SerializedName("sha")
        private String sha;

        @SerializedName("url")
        private String url;

        public CommitRef() {}
        public CommitRef(String sha) { this.sha = sha; }

        public String getSha() { return sha != null ? sha : ""; }
        public void setSha(String sha) { this.sha = sha; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
