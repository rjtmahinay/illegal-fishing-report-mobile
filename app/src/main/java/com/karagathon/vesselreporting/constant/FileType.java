package com.karagathon.vesselreporting.constant;

public enum FileType {
    PICTURE(".jpg"),
    VIDEO(".mp4");

    private String extension;

    FileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
