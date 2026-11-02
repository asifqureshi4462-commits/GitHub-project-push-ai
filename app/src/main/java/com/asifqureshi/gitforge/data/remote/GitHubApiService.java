package com.asifqureshi.gitforge.data.remote;

import com.asifqureshi.gitforge.model.Branch;
import com.asifqureshi.gitforge.model.GitDatabaseModels;
import com.asifqureshi.gitforge.model.Repository;
import com.asifqureshi.gitforge.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GitHubApiService {

    @GET("user")
    Call<User> getAuthenticatedUser();

    @GET("user/repos")
    Call<List<Repository>> getUserRepositories(
            @Query("sort") String sort,
            @Query("per_page") int perPage,
            @Query("page") int page
    );

    @GET("repos/{owner}/{repo}/branches")
    Call<List<Branch>> getBranches(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Query("per_page") int perPage
    );

    // Git Database API
    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    Call<GitDatabaseModels.GitRef> getBranchRef(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Path("branch") String branch
    );

    @GET("repos/{owner}/{repo}/git/commits/{sha}")
    Call<GitDatabaseModels.CommitObject> getCommit(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Path("sha") String sha
    );

    @POST("repos/{owner}/{repo}/git/blobs")
    Call<GitDatabaseModels.CreateBlobResponse> createBlob(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Body GitDatabaseModels.CreateBlobRequest request
    );

    @POST("repos/{owner}/{repo}/git/trees")
    Call<GitDatabaseModels.CreateTreeResponse> createTree(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Body GitDatabaseModels.CreateTreeRequest request
    );

    @POST("repos/{owner}/{repo}/git/commits")
    Call<GitDatabaseModels.CreateCommitResponse> createCommit(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Body GitDatabaseModels.CreateCommitRequest request
    );

    @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
    Call<GitDatabaseModels.GitRef> updateBranchRef(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Path("branch") String branch,
            @Body GitDatabaseModels.UpdateRefRequest request
    );

    @POST("repos/{owner}/{repo}/git/refs")
    Call<GitDatabaseModels.GitRef> createBranchRef(
            @Path("owner") String owner,
            @Path("repo") String repo,
            @Body GitDatabaseModels.CreateRefRequest request
    );
}
