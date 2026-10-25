package org.schabi.newpipe.streams;

import org.schabi.newpipe.streams.io.SharpStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Adds iTunes-style tags (title, artist, cover) to a plain (not fragmented) MP4/M4A file
 * by rewriting its moov box.
 */
public final class Mp4Tagger {
    private static final int FTYP = 0x66747970;
    private static final int MOOV = 0x6D6F6F76;
    private static final int UDTA = 0x75647461;
    private static final int TRAK = 0x7472616B;
    private static final int MDIA = 0x6D646961;
    private static final int MINF = 0x6D696E66;
    private static final int STBL = 0x7374626C;
    private static final int STCO = 0x7374636F;
    private static final int CO64 = 0x636F3634;
    private static final long MAX_MOOV = 32L * 1024 * 1024;

    private Mp4Tagger() {
    }

    public static byte[] buildUdta(final String title, final String artist, final byte[] cover) {
        final ArrayList<byte[]> items = new ArrayList<>();
        if (title != null && !title.isEmpty()) {
            items.add(item(0xA96E616D, 1, title.getBytes(StandardCharsets.UTF_8)));
        }
        if (artist != null && !artist.isEmpty()) {
            items.add(item(0xA9415254, 1, artist.getBytes(StandardCharsets.UTF_8)));
        }
        if (cover != null && cover.length > 8) {
            final boolean jpeg = (cover[0] & 0xFF) == 0xFF && (cover[1] & 0xFF) == 0xD8;
            final boolean png = (cover[0] & 0xFF) == 0x89 && cover[1] == 'P'
                    && cover[2] == 'N' && cover[3] == 'G';
            if (jpeg || png) {
                items.add(item(0x636F7672, jpeg ? 13 : 14, cover));
            }
        }
        if (items.isEmpty()) {
            return null;
        }
        final byte[] hdlr = {0, 0, 0, 0x21, 'h', 'd', 'l', 'r', 0, 0, 0, 0, 0, 0, 0, 0,
                'm', 'd', 'i', 'r', 'a', 'p', 'p', 'l', 0, 0, 0, 0, 0, 0, 0, 0, 0};
        final byte[] ilst = box(0x696C7374, items.toArray(new byte[0][]));
        final byte[] meta = box(0x6D657461, new byte[4], hdlr, ilst);
        return box(UDTA, meta);
    }

    /** Checks (without writing anything) that the file layout is understood. */
    public static boolean canTag(final SharpStream in) {
        try {
            final byte[] head = new byte[8];
            boolean first = true;
            boolean moov = false;
            while (readFully(in, head, 8)) {
                final long size = u32(head, 0);
                final int type = ByteBuffer.wrap(head, 4, 4).getInt();
                if (first && type != FTYP) {
                    return false;
                }
                first = false;
                if (size == 0) {
                    return moov;
                }
                if (size < 8) {
                    return false;
                }
                if (type == MOOV) {
                    if (size > MAX_MOOV) {
                        return false;
                    }
                    moov = true;
                }
                if (!skipFully(in, size - 8)) {
                    return false;
                }
            }
            return moov;
        } catch (final Exception e) {
            return false;
        }
    }

    /** Copies in to out, replacing the moov box with a tagged one. */
    public static void tag(final SharpStream in, final SharpStream out, final byte[] udta)
            throws IOException {
        final byte[] head = new byte[8];
        final byte[] buf = new byte[64 * 1024];
        long pos = 0;
        boolean done = false;

        while (readFully(in, head, 8)) {
            final long size = u32(head, 0);
            final int type = ByteBuffer.wrap(head, 4, 4).getInt();

            if (size == 0) {
                out.write(head, 0, 8);
                copy(in, out, Long.MAX_VALUE, buf);
                break;
            }
            if (type == MOOV && !done) {
                final byte[] payload = new byte[(int) (size - 8)];
                if (!readFully(in, payload, payload.length)) {
                    throw new IOException("truncated moov");
                }
                writeMoov(out, payload, pos + size, udta);
                done = true;
            } else {
                out.write(head, 0, 8);
                copy(in, out, size - 8, buf);
            }
            pos += size;
        }
    }

    private static void writeMoov(final SharpStream out, final byte[] payload,
                                  final long oldMoovEnd, final byte[] udta) throws IOException {
        final ByteArrayOutputStream kept =
                new ByteArrayOutputStream(payload.length + (udta == null ? 0 : udta.length));
        int p = 0;
        while (p + 8 <= payload.length) {
            final long size = u32(payload, p);
            final int type = ByteBuffer.wrap(payload, p + 4, 4).getInt();
            if (size < 8 || size > payload.length - p) {
                break; // unexpected layout: keep the rest untouched
            }
            if (type != UDTA) {
                kept.write(payload, p, (int) size);
            }
            p += (int) size;
        }
        if (p < payload.length) {
            kept.write(payload, p, payload.length - p);
        }
        if (udta != null) {
            kept.write(udta, 0, udta.length);
        }

        final byte[] body = kept.toByteArray();
        final long newSize = 8L + body.length;
        final long oldSize = 8L + payload.length;
        patchOffsets(body, 0, body.length, newSize - oldSize, oldMoovEnd);

        out.write(ByteBuffer.allocate(8).putInt((int) newSize).putInt(MOOV).array(), 0, 8);
        out.write(body, 0, body.length);
    }

    private static void patchOffsets(final byte[] b, final int start, final int end,
                                     final long delta, final long threshold) {
        if (delta == 0) {
            return;
        }
        final ByteBuffer bb = ByteBuffer.wrap(b);
        int p = start;
        while (p + 8 <= end) {
            final long size = u32(b, p);
            final int type = bb.getInt(p + 4);
            if (size < 8 || p + size > end) {
                return;
            }
            final int bodyStart = p + 8;
            final int next = (int) (p + size);
            if (type == TRAK || type == MDIA || type == MINF || type == STBL) {
                patchOffsets(b, bodyStart, next, delta, threshold);
            } else if (type == STCO || type == CO64) {
                final boolean wide = type == CO64;
                final int count = bb.getInt(bodyStart + 4);
                int q = bodyStart + 8;
                for (int i = 0; i < count && q + (wide ? 8 : 4) <= next; i++) {
                    if (wide) {
                        final long v = bb.getLong(q);
                        if (v >= threshold) {
                            bb.putLong(q, v + delta);
                        }
                        q += 8;
                    } else {
                        final long v = bb.getInt(q) & 0xFFFFFFFFL;
                        if (v >= threshold) {
                            bb.putInt(q, (int) (v + delta));
                        }
                        q += 4;
                    }
                }
            }
            p = next;
        }
    }

    private static byte[] item(final int type, final int dataType, final byte[] value) {
        final ByteBuffer data = ByteBuffer.allocate(8 + value.length);
        data.putInt(dataType).putInt(0).put(value);
        return box(type, box(0x64617461, data.array()));
    }

    private static byte[] box(final int type, final byte[]... parts) {
        int size = 8;
        for (final byte[] part : parts) {
            size += part.length;
        }
        final ByteBuffer bb = ByteBuffer.allocate(size);
        bb.putInt(size).putInt(type);
        for (final byte[] part : parts) {
            bb.put(part);
        }
        return bb.array();
    }

    private static long u32(final byte[] b, final int off) {
        return ByteBuffer.wrap(b, off, 4).getInt() & 0xFFFFFFFFL;
    }

    private static boolean readFully(final SharpStream in, final byte[] buf, final int len)
            throws IOException {
        int pos = 0;
        while (pos < len) {
            final int n = in.read(buf, pos, len - pos);
            if (n <= 0) {
                return false;
            }
            pos += n;
        }
        return true;
    }

    private static boolean skipFully(final SharpStream in, final long amount) throws IOException {
        long left = amount;
        while (left > 0) {
            final long n = in.skip(left);
            if (n <= 0) {
                return false;
            }
            left -= n;
        }
        return true;
    }

    private static void copy(final SharpStream in, final SharpStream out, final long count,
                             final byte[] buf) throws IOException {
        long left = count;
        while (left > 0) {
            final int n = in.read(buf, 0, (int) Math.min(buf.length, left));
            if (n <= 0) {
                if (count == Long.MAX_VALUE) {
                    return;
                }
                throw new IOException("unexpected end of file");
            }
            out.write(buf, 0, n);
            left -= n;
        }
    }
}
