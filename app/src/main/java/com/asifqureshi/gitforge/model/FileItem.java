package com.asifqureshi.gitforge.model;

public class FileItem {
    private String relativePath;
    private String fileName;
    private long sizeBytes;
    private boolean isDirectory;
    private byte[] data;
    private boolean isSelected;

    public FileItem(String relativePath, String fileName, long sizeBytes, boolean isDirectory, byte[] data) {
        this.relativePath = relativePath;
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.isDirectory = isDirectory;
        this.data = data;
        this.isSelected = true;
    }

    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    public String getFormattedSize() {
        if (sizeBytes < 1024) return sizeBytes + " B";
        if (sizeBytes < 1024 * 1024) return String.format(java.util.Locale.US, "%.1f KB", sizeBytes / 1024.0);
        return String.format(java.util.Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }
}
