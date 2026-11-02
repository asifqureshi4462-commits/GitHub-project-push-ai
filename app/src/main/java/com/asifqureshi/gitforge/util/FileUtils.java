package com.asifqureshi.gitforge.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.documentfile.provider.DocumentFile;

import com.asifqureshi.gitforge.model.FileItem;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileUtils {

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return (result != null && !result.isEmpty()) ? result : "file";
    }

    public static byte[] readBytes(Context context, Uri uri) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        try (InputStream inputStream = resolver.openInputStream(uri);
             ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {
            if (inputStream == null) return new byte[0];
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        }
    }

    /**
     * Traverses a folder selected via Storage Access Framework (SAF) DocumentFile tree.
     */
    public static List<FileItem> traverseDocumentFolder(Context context, DocumentFile dir, String currentPath) {
        List<FileItem> items = new ArrayList<>();
        DocumentFile[] files = dir.listFiles();
        if (files == null) return items;

        for (DocumentFile file : files) {
            String name = file.getName();
            if (name == null || ZipUtils.shouldIgnore(name)) continue;

            String relative = currentPath.isEmpty() ? name : currentPath + "/" + name;

            if (file.isDirectory()) {
                items.addAll(traverseDocumentFolder(context, file, relative));
            } else if (file.isFile()) {
                try {
                    byte[] data = readBytes(context, file.getUri());
                    items.add(new FileItem(relative, name, data.length, false, data));
                } catch (Exception ignored) {}
            }
        }
        return items;
    }

    public static boolean isTextFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".json")
                || lower.endsWith(".java") || lower.endsWith(".kt") || lower.endsWith(".js")
                || lower.endsWith(".ts") || lower.endsWith(".html") || lower.endsWith(".css")
                || lower.endsWith(".xml") || lower.endsWith(".gradle") || lower.endsWith(".py")
                || lower.endsWith(".sh") || lower.endsWith(".c") || lower.endsWith(".cpp")
                || lower.endsWith(".h") || lower.endsWith(".yml") || lower.endsWith(".yaml")
                || lower.endsWith(".properties") || lower.endsWith(".sql") || lower.endsWith(".go");
    }

    public static String getPreviewSnippet(byte[] data, int maxChars) {
        if (data == null || data.length == 0) return "(Empty file)";
        try {
            int length = Math.min(data.length, maxChars * 2);
            String text = new String(data, 0, length, StandardCharsets.UTF_8);
            if (text.length() > maxChars) {
                return text.substring(0, maxChars) + "\n... [truncated preview]";
            }
            return text;
        } catch (Exception e) {
            return "(Binary data / non-UTF-8 preview)";
        }
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
