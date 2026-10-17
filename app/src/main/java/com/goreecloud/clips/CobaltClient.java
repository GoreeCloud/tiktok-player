package com.goreecloud.clips;

import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class CobaltClient {
    public static final class Resolved {
        public final String url;
        public final String filename;
        Resolved(String url, String filename) {
            this.url = url;
            this.filename = MediaUrls.safeName(filename);
        }
    }
    private CobaltClient() {}

    public static Resolved resolve(String endpoint, String sourceUrl) throws Exception {
        if (!MediaUrls.isProviderUrl(endpoint) || !MediaUrls.isVideoLink(sourceUrl)) {
            throw new IllegalArgumentException("Invalid provider or video URL.");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(false);
            connection.setDoOutput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            JSONObject request = new JSONObject().put("url", sourceUrl).put("filenameStyle", "basic");
            byte[] data = request.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(data.length);
            try (java.io.OutputStream output = connection.getOutputStream()) { output.write(data); }
            if (connection.getResponseCode() != 200)
                throw new IllegalStateException("Processing server returned HTTP " + connection.getResponseCode());
            byte[] payload = new byte[65537];
            int count = 0, read;
            try (InputStream input = connection.getInputStream()) {
                while ((read = input.read(payload, count, payload.length - count)) > 0) {
                    count += read;
                    if (count >= 65536) throw new IllegalStateException("Processing response too large.");
                }
            }
            JSONObject response = new JSONObject(new String(payload, 0, count, StandardCharsets.UTF_8));
            String status = response.optString("status");
            if ("tunnel".equals(status) || "redirect".equals(status)) {
                String downloadUrl = response.optString("url");
                if (!MediaUrls.isDownloadUrl(downloadUrl))
                    throw new IllegalStateException("Unsafe destination returned by provider.");
                return new Resolved(downloadUrl, response.optString("filename"));
            }
            if ("error".equals(status)) throw new IllegalStateException("Provider cannot download this link.");
            throw new IllegalStateException("Provider requires additional media selection or processing.");
        } finally {
            connection.disconnect();
        }
    }
}
