package com.mediapicker.gallery.presentation.utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtils {

    public static final String WHATSAPP_SEARCH_TERM = "'%WhatsApp%'";
    private static String INTERNAL_PHOTO_PATH = "/DCIM/";
    private static String IMAGE_EXTENSION = ".jpg";
    private static String IMAGE_BASE_NAME = "image";
    private static String PICKED_PHOTO_CACHE_DIR = "picked_photos";

    public static File getNewPhotoFileOnPicturesDirectory() {
        return getNewPhotoFileIn(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
    }

    /**
     * App-specific cache location for photos copied from a content Uri (native photo picker).
     * Needs no storage permission on any API level, unlike the public Pictures directory which
     * fails with EACCES on Android 10 when WRITE_EXTERNAL_STORAGE is not granted.
     */
    public static File getNewPhotoFileOnCacheDirectory(Context context) {
        return getNewPhotoFileIn(new File(context.getCacheDir(), PICKED_PHOTO_CACHE_DIR));
    }

    /**
     * App-specific external Pictures directory for camera captures. The camera app writes here
     * through the host FileProvider (covered by its external-path "."), and the host process
     * needs no storage permission on any API level to create the file, unlike the public
     * Pictures directory on Android 10. Falls back to the public directory if external
     * storage is unavailable.
     */
    public static File getNewPhotoFileOnAppPicturesDirectory(Context context) {
        File directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (directory == null) {
            return getNewPhotoFileOnPicturesDirectory();
        }
        return getNewPhotoFileIn(directory);
    }

    private static File getNewPhotoFileIn(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File tempFile = null;

        int index = 0;

        while (tempFile == null || tempFile.exists()) {
            String imageName = generateFileNameBaseOnTimeWithoutExt(IMAGE_BASE_NAME, index) + IMAGE_EXTENSION;
            tempFile = new File(directory, imageName);

            index++;
        }

        return tempFile;
    }

    public static File getProfileNewPhotoDir(Context context) {
        File path = new File(context.getFilesDir(), "/profile/picture");
        if (!path.exists()) {
            path.mkdirs();
        }
        return new File(path, "profile" + IMAGE_EXTENSION);
    }

    public static File getNewPhotoFileOnExternalStorage() {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File tempFile = null;

        int index = 0;

        while (tempFile == null || tempFile.exists()) {
            String imageName = generateFileNameBaseOnTimeWithoutExt(IMAGE_BASE_NAME, index) + IMAGE_EXTENSION;
            tempFile = new File(externalStorageDirectory + INTERNAL_PHOTO_PATH, imageName);

            index++;
        }

        return tempFile;
    }

    public static String generateFileNameBaseOnTimeWithoutExt(String base, int index) {
        StringBuilder fileName = new StringBuilder();
        fileName.append(base);
        fileName.append("_");
        fileName.append(getCurrentDateInFileNameStringFormat());

        if (index > 0) {
            fileName.append("_");
            fileName.append(String.valueOf(index));
        }

        return fileName.toString();
    }

    public static String getCurrentDateInFileNameStringFormat() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        return dateFormat.format(date);
    }

    public static boolean existsFile(String path) {
        return new File(path).exists();
    }
}
