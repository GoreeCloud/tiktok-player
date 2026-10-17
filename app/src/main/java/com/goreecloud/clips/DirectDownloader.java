package com.goreecloud.clips;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import dev.ffmpegkit_maintained.ytdlp.YtDlp;
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest;
import dev.ffmpegkit_maintained.ytdlp.YtDlpResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public final class DirectDownloader {
    public interface Progress {
        void update(float percentage, long etaSeconds);
    }

    public static final class SavedFile {
        public final Uri uri;
        public final String name;
        SavedFile(Uri uri, String name) { this.uri = uri; this.name = name; }
    }

    private DirectDownloader() {}

    public static SavedFile download(Context context, String videoUrl, Progress progress) throws Exception {
        if (!MediaUrls.isVideoLink(videoUrl)) {
            throw new IllegalArgumentException("Enter a valid TikTok video URL.");
        }
        File root = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (root == null) throw new IOException("App video storage is unavailable.");
        File temp = new File(new File(root, "temporary"), UUID.randomUUID().toString());
        if (!temp.mkdirs()) throw new IOException("Could not create a temporary video directory.");
        try {
            YtDlp.init(context.getApplicationContext());
            YtDlpRequest request = new YtDlpRequest(videoUrl)
                    .setOutputTemplate(new File(temp, "%(id).80s.%(ext)s").getAbsolutePath())
                    .addOption("--no-playlist")
                    .addOption("--no-overwrites")
                    .addOption("-f", "best[ext=mp4]/best");
            YtDlpResponse result = YtDlp.execute(request, (percent, eta, line) -> {
                if (progress != null) progress.update(percent, eta);
            });
            if (!result.isSuccess()) {
                throw new IOException("The video could not be downloaded. It may be unavailable or the site may block requests.");
            }
            File output = mediaFile(temp);
            if (output == null || output.length() <= 0) {
                throw new IOException("No complete video file was produced.");
            }
            return saveToLibrary(context, output);
        } finally {
            clear(temp);
        }
    }

    private static File mediaFile(File folder) {
        File[] found = folder.listFiles();
        if (found == null) return null;
        for (File file : found) {
            String name = file.getName().toLowerCase(java.util.Locale.ROOT);
            if (file.isFile() && (name.endsWith(".mp4") || name.endsWith(".webm")
                    || name.endsWith(".mov") || name.endsWith(".m4v"))) {
                return file;
            }
        }
        return null;
    }

    private static SavedFile saveToLibrary(Context context, File source) throws Exception {
        String name = "goreecloud-" + MediaUrls.safeName(source.getName());
        if (Build.VERSION.SDK_INT < 29) {
            // On older Android versions, retain files in app-specific external storage.
            File destinationRoot = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (destinationRoot == null) throw new IOException("No video storage.");
            File destination = new File(destinationRoot, name);
            try (FileInputStream input = new FileInputStream(source);
                 java.io.FileOutputStream output = new java.io.FileOutputStream(destination)) {
                copy(input, output);
            }
            return new SavedFile(Uri.fromFile(destination), name);
        }

        String mime = name.toLowerCase(java.util.Locale.ROOT).endsWith(".webm")
                ? "video/webm" : "video/mp4";
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Video.Media.MIME_TYPE, mime);
        values.put(MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/GoreeCloud Clips");
        values.put(MediaStore.Video.Media.IS_PENDING, 1);
        Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Could not create media entry.");
        boolean completed = false;
        try {
            try (FileInputStream input = new FileInputStream(source);
                 OutputStream output = resolver.openOutputStream(uri, "w")) {
                if (output == null) throw new IOException("Cannot write media entry.");
                copy(input, output);
            }
            ContentValues done = new ContentValues();
            done.put(MediaStore.Video.Media.IS_PENDING, 0);
            if (resolver.update(uri, done, null, null) <= 0) {
                throw new IOException("Could not publish the video.");
            }
            completed = true;
            return new SavedFile(uri, name);
        } finally {
            if (!completed) resolver.delete(uri, null, null);
        }
    }

    private static void copy(java.io.InputStream input, OutputStream output) throws IOException {
        byte[] bytes = new byte[32768];
        int read;
        while ((read = input.read(bytes)) != -1) {
            output.write(bytes, 0, read);
        }
    }

    private static void clear(File folder) {
        File[] children = folder.listFiles();
        if (children != null) for (File file : children) if (file.isFile()) file.delete();
        folder.delete();
    }
}
