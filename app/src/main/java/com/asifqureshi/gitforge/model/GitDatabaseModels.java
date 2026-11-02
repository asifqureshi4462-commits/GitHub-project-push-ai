package com.asifqureshi.gitforge.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GitDatabaseModels {

    // GET /repos/{owner}/{repo}/git/ref/heads/{branch}
    public static class GitRef {
        @SerializedName("ref")
        private String ref;

        @SerializedName("object")
        private GitObject object;

        public String getRef() { return ref; }
        public GitObject getObject() { return object; }
        public String getSha() { return object != null ? object.getSha() : ""; }
    }

    public static class GitObject {
        @SerializedName("sha")
        private String sha;
        @SerializedName("type")
        private String type;
        @SerializedName("url")
        private String url;

        public String getSha() { return sha != null ? sha : ""; }
        public String getType() { return type; }
    }

    // GET /repos/{owner}/{repo}/git/commits/{sha}
    public static class CommitObject {
        @SerializedName("sha")
        private String sha;

        @SerializedName("message")
        private String message;

        @SerializedName("tree")
        private TreeRef tree;

        public String getSha() { return sha; }
        public TreeRef getTree() { return tree; }
        public String getTreeSha() { return tree != null ? tree.getSha() : ""; }
    }

    public static class TreeRef {
        @SerializedName("sha")
        private String sha;
        @SerializedName("url")
        private String url;

        public String getSha() { return sha != null ? sha : ""; }
    }

    // POST /repos/{owner}/{repo}/git/blobs
    public static class CreateBlobRequest {
        @SerializedName("content")
        private String content;

        @SerializedName("encoding")
        private String encoding; // "base64" or "utf-8"

        public CreateBlobRequest(String content, String encoding) {
            this.content = content;
            this.encoding = encoding;
        }
    }

    public static class CreateBlobResponse {
        @SerializedName("sha")
        private String sha;
        @SerializedName("url")
        private String url;

        public String getSha() { return sha != null ? sha : ""; }
    }

    // POST /repos/{owner}/{repo}/git/trees
    public static class CreateTreeRequest {
        @SerializedName("base_tree")
        private String baseTree;

        @SerializedName("tree")
        private List<TreeEntry> tree;

        public CreateTreeRequest(String baseTree, List<TreeEntry> tree) {
            this.baseTree = baseTree;
            this.tree = tree;
        }
    }

    public static class TreeEntry {
        @SerializedName("path")
        private String path;

        @SerializedName("mode")
        private String mode; // "100644" for normal file, "100755" for executable

        @SerializedName("type")
        private String type; // "blob"

        @SerializedName("sha")
        private String sha;

        public TreeEntry(String path, String mode, String type, String sha) {
            this.path = path;
            this.mode = mode;
            this.type = type;
            this.sha = sha;
        }

        public String getPath() { return path; }
        public String getSha() { return sha; }
    }

    public static class CreateTreeResponse {
        @SerializedName("sha")
        private String sha;
        @SerializedName("url")
        private String url;

        public String getSha() { return sha != null ? sha : ""; }
    }

    // POST /repos/{owner}/{repo}/git/commits
    public static class CreateCommitRequest {
        @SerializedName("message")
        private String message;

        @SerializedName("tree")
        private String tree;

        @SerializedName("parents")
        private List<String> parents;

        public CreateCommitRequest(String message, String tree, List<String> parents) {
            this.message = message;
            this.tree = tree;
            this.parents = parents;
        }
    }

    public static class CreateCommitResponse {
        @SerializedName("sha")
        private String sha;
        @SerializedName("html_url")
        private String htmlUrl;

        public String getSha() { return sha != null ? sha : ""; }
        public String getHtmlUrl() { return htmlUrl != null ? htmlUrl : ""; }
    }

    // PATCH /repos/{owner}/{repo}/git/refs/heads/{branch}
    public static class UpdateRefRequest {
        @SerializedName("sha")
        private String sha;

        @SerializedName("force")
        private boolean force;

        public UpdateRefRequest(String sha, boolean force) {
            this.sha = sha;
            this.force = force;
        }
    }

    // POST /repos/{owner}/{repo}/git/refs to create new branch
    public static class CreateRefRequest {
        @SerializedName("ref")
        private String ref; // "refs/heads/branch-name"

        @SerializedName("sha")
        private String sha;

        public CreateRefRequest(String ref, String sha) {
            this.ref = ref;
            this.sha = sha;
        }
    }
}
