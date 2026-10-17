package com.goreecloud.clips;

import java.net.URI;
import java.util.Locale;

public final class MediaUrls {
    private MediaUrls() {}

    public static boolean isTikTok(String value) {
        URI uri = parse(value);
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
        String host = hostname(uri);
        return host != null && (host.equals("tiktok.com") || host.endsWith(".tiktok.com"));
    }

    public static boolean isVideoLink(String value) {
        if (!isTikTok(value)) return false;
        URI uri = parse(value);
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        String host = hostname(uri);
        return path.contains("/video/") || path.startsWith("/t/") ||
            host.equals("vm.tiktok.com") || host.equals("vt.tiktok.com");
    }

    public static boolean isProviderUrl(String value) {
        URI uri = parse(value);
        if (!isPublicHttps(uri)) return false;
        String path = uri.getPath();
        // Cobalt v10 exposes the resolver at POST /. Do not send links to arbitrary paths.
        return (path == null || path.isEmpty() || path.equals("/")) &&
            uri.getQuery() == null && uri.getFragment() == null;
    }

    public static boolean isDownloadUrl(String value) {
        return isPublicHttps(parse(value));
    }

    private static boolean isPublicHttps(URI uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) ||
            uri.getRawUserInfo() != null) return false;
        String host = hostname(uri);
        if (host == null || !host.contains(".") || host.equals("localhost") ||
            host.endsWith(".local") || host.endsWith(".internal")) return false;
        // Avoid loopback/link-local/private IPv4 destinations.
        if (host.matches("[0-9.]+")) {
            String[] p = host.split("\\.");
            if (p.length != 4) return false;
            try {
                int a = Integer.parseInt(p[0]), c = Integer.parseInt(p[1]);
                if (a == 0 || a == 10 || a == 127 || a >= 224 ||
                    (a == 169 && c == 254) || (a == 172 && c >= 16 && c <= 31) ||
                    (a == 192 && c == 168)) return false;
            } catch (NumberFormatException e) { return false; }
        }
        return true;
    }

    public static String safeName(String raw) {
        String base = raw == null ? "" : raw.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[^a-zA-Z0-9._ -]", "_").trim();
        if (base.startsWith(".")) base = "clip" + base;
        if (base.length() > 80) base = base.substring(0, 80);
        if (base.isEmpty() || base.equals(".") || base.equals("..")) base = "goreecloud-clip";
        if (!base.toLowerCase(Locale.ROOT).matches(".*\\.(mp4|webm|mov|m4v)$"))
            base += ".mp4";
        return base;
    }

    private static String hostname(URI uri) {
        return uri == null || uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT);
    }

    private static URI parse(String url) {
        try { return url == null ? null : URI.create(url.trim()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
