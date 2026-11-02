package com.asifqureshi.gitforge.util;

import com.asifqureshi.gitforge.model.FileItem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {

    /**
     * Reads a ZIP input stream and parses all valid files.
     * Implements strict Zip Slip protection by validating path canonicalization and rejecting path traversal attempts ('..').
     */
    public static List<FileItem> parseZipStream(InputStream inputStream) throws IOException {
        List<FileItem> fileList = new ArrayList<>();
        ZipInputStream zis = new ZipInputStream(inputStream);
        ZipEntry entry;
        byte[] buffer = new byte[8192];

        File dummyRoot = new File("/safe_sandbox");
        String canonicalRoot = dummyRoot.getCanonicalPath();

        while ((entry = zis.getNextEntry()) != null) {
            String entryName = entry.getName();

            // Zip Slip protection
            if (entryName == null || entryName.contains("..")) {
                zis.closeEntry();
                continue;
            }

            File testDest = new File(dummyRoot, entryName);
            if (!testDest.getCanonicalPath().startsWith(canonicalRoot)) {
                // Potential path traversal attack: skip
                zis.closeEntry();
                continue;
            }

            // Skip directories and metadata
            if (entry.isDirectory() || shouldIgnore(entryName)) {
                zis.closeEntry();
                continue;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len;
            while ((len = zis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            byte[] data = baos.toByteArray();

            String normalizedPath = entryName.replace('\\', '/');
            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }

            String fileName = new File(normalizedPath).getName();
            fileList.add(new FileItem(normalizedPath, fileName, data.length, false, data));

            zis.closeEntry();
        }

        zis.close();
        return fileList;
    }

    /**
     * Filters common unwanted files, VCS internal folders, and build artifacts.
     */
    public static boolean shouldIgnore(String path) {
        if (path == null) return true;
        String lower = path.toLowerCase();
        return lower.startsWith("__macosx/")
                || lower.contains("/.ds_store")
                || lower.endsWith(".ds_store")
                || lower.startsWith(".git/")
                || lower.contains("/.git/")
                || lower.startsWith(".gradle/")
                || lower.contains("/.gradle/")
                || lower.startsWith("node_modules/")
                || lower.contains("/node_modules/")
                || lower.startsWith("build/")
                || lower.contains("/build/")
                || lower.contains("/.idea/")
                || lower.endsWith(".class");
    }
}
