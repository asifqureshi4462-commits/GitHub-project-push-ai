package com.asifqureshi.gitforge;

import static org.junit.Assert.*;

import com.asifqureshi.gitforge.data.local.UploadHistoryEntity;
import com.asifqureshi.gitforge.model.AIModels;
import com.asifqureshi.gitforge.model.Branch;
import com.asifqureshi.gitforge.model.FileItem;
import com.asifqureshi.gitforge.model.GitDatabaseModels;
import com.asifqureshi.gitforge.model.Repository;
import com.asifqureshi.gitforge.model.User;
import com.asifqureshi.gitforge.util.FileUtils;
import com.asifqureshi.gitforge.util.ZipUtils;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GitForgeLogicTest {

    @Test
    public void testZipPathTraversalProtection() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        // Good file
        zos.putNextEntry(new ZipEntry("src/main/App.java"));
        zos.write("class App {}".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        // Malicious entry attempting path traversal
        zos.putNextEntry(new ZipEntry("../../../etc/passwd"));
        zos.write("root:x:0:0".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        // Ignored file
        zos.putNextEntry(new ZipEntry(".git/config"));
        zos.write("git config".getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        zos.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        List<FileItem> parsed = ZipUtils.parseZipStream(bais);

        // Only "src/main/App.java" should be accepted!
        assertEquals(1, parsed.size());
        assertEquals("src/main/App.java", parsed.get(0).getRelativePath());
    }

    @Test
    public void testFileItemSizeFormatting() {
        FileItem bytesItem = new FileItem("test.txt", "test.txt", 512, false, new byte[512]);
        assertEquals("512 B", bytesItem.getFormattedSize());

        FileItem kbItem = new FileItem("large.json", "large.json", 2048, false, new byte[2048]);
        assertEquals("2.0 KB", kbItem.getFormattedSize());

        FileItem mbItem = new FileItem("big.zip", "big.zip", 2 * 1024 * 1024, false, new byte[2 * 1024 * 1024]);
        assertEquals("2.0 MB", mbItem.getFormattedSize());
    }

    @Test
    public void testIgnoreFilter() {
        assertTrue(ZipUtils.shouldIgnore(".git/HEAD"));
        assertTrue(ZipUtils.shouldIgnore("node_modules/react/index.js"));
        assertTrue(ZipUtils.shouldIgnore(".gradle/caches/module"));
        assertTrue(ZipUtils.shouldIgnore("build/outputs/apk/app.apk"));
        assertTrue(ZipUtils.shouldIgnore(".DS_Store"));
        assertTrue(ZipUtils.shouldIgnore("__MACOSX/icon.png"));

        assertFalse(ZipUtils.shouldIgnore("src/main/java/MainActivity.java"));
        assertFalse(ZipUtils.shouldIgnore("README.md"));
        assertFalse(ZipUtils.shouldIgnore("package.json"));
    }

    @Test
    public void testGitDatabaseModels() {
        GitDatabaseModels.TreeEntry entry = new GitDatabaseModels.TreeEntry("src/App.java", "100644", "blob", "abc123sha");
        assertEquals("src/App.java", entry.getPath());
        assertEquals("abc123sha", entry.getSha());

        GitDatabaseModels.CreateTreeRequest treeReq = new GitDatabaseModels.CreateTreeRequest("baseTreeSha", Collections.singletonList(entry));
        assertNotNull(treeReq);

        GitDatabaseModels.CreateCommitRequest commitReq = new GitDatabaseModels.CreateCommitRequest(
                "Commit message", "newTreeSha", Collections.singletonList("parentSha")
        );
        assertNotNull(commitReq);
    }

    @Test
    public void testRepositoryAndUserModel() {
        User user = new User("octocat", "The Octocat", "https://avatar.url", 10, 5);
        assertEquals("octocat", user.getLogin());
        assertEquals("The Octocat", user.getName());

        Repository repo = new Repository(101, "my-repo", "octocat/my-repo", false, "main");
        assertEquals("my-repo", repo.getName());
        assertEquals("octocat/my-repo", repo.getFullName());
        assertFalse(repo.isPrivate());
        assertEquals("main", repo.getDefaultBranch());

        Branch branch = new Branch("feature-branch", "789abc");
        assertEquals("feature-branch", branch.getName());
        assertEquals("789abc", branch.getSha());
    }

    @Test
    public void testUploadHistoryEntity() {
        UploadHistoryEntity history = new UploadHistoryEntity(
                "owner/cool-repo", "main", "Initial upload",
                System.currentTimeMillis(), "SUCCESS", "sha123456", 15, null
        );

        assertEquals("owner/cool-repo", history.getRepoName());
        assertEquals("main", history.getBranch());
        assertEquals("SUCCESS", history.getStatus());
        assertEquals("sha123456", history.getCommitSha());
        assertEquals(15, history.getFileCount());
        assertNull(history.getErrorMessage());
    }

    @Test
    public void testAIModelStructures() {
        AIModels.GeminiRequest req = new AIModels.GeminiRequest("Analyze this code");
        assertNotNull(req);

        AIModels.GeminiResponse resp = new AIModels.GeminiResponse();
        assertEquals("", resp.getFirstCandidateText());
    }

    @Test
    public void testTextFileDetectionAndPreview() {
        assertTrue(FileUtils.isTextFile("App.java"));
        assertTrue(FileUtils.isTextFile("README.md"));
        assertTrue(FileUtils.isTextFile("config.json"));
        assertFalse(FileUtils.isTextFile("image.png"));
        assertFalse(FileUtils.isTextFile("archive.zip"));

        byte[] textBytes = "System.out.println(\"Hello GitForge\");".getBytes(StandardCharsets.UTF_8);
        String preview = FileUtils.getPreviewSnippet(textBytes, 50);
        assertTrue(preview.contains("Hello GitForge"));
    }
}
