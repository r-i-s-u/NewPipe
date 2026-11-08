package us.shandian.giga.get;

import android.content.Context;
import android.content.SharedPreferences;

import org.schabi.newpipe.extractor.stream.StreamInfo;

/** Remembers uploader and thumbnail of a download, for formats that cannot carry tags (WebM, 3GP). */
public final class MissionMeta {
    private static final String PREFS = "mission_meta";

    private MissionMeta() {
    }

    public static void save(final Context ctx, final StreamInfo info, final String source) {
        if (ctx == null || info == null || source == null) {
            return;
        }
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString("u:" + source, info.getUploaderName())
                    .putString("t:" + source, thumbnailUrl(info))
                    .apply();
        } catch (final Exception ignored) {
            // metadata is optional
        }
    }

    /** Returns {uploader, thumbnailUrl} (each may be null) or null when nothing is stored. */
    public static String[] get(final Context ctx, final String source) {
        if (ctx == null || source == null) {
            return null;
        }
        final SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final String u = p.getString("u:" + source, null);
        final String t = p.getString("t:" + source, null);
        return u == null && t == null ? null : new String[]{u, t};
    }

    private static String thumbnailUrl(final StreamInfo info) {
        try {
            if (info.getServiceId() == 0 && info.getId() != null) {
                return "https://i.ytimg.com/vi/" + info.getId() + "/hqdefault.jpg";
            }
            String best = null;
            int bestHeight = -2;
            for (final var image : info.getThumbnails()) {
                if (image.getUrl() != null && image.getHeight() > bestHeight) {
                    bestHeight = image.getHeight();
                    best = image.getUrl();
                }
            }
            return best;
        } catch (final Exception e) {
            return null;
        }
    }
}
