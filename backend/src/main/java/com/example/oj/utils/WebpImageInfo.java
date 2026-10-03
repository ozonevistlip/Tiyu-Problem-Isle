package com.example.oj.utils;

import java.nio.charset.StandardCharsets;

/** Reads the canvas size of a still WebP without relying on an optional ImageIO plugin. */
public record WebpImageInfo(int width, int height) {
    public static WebpImageInfo read(byte[] data) {
        if (data.length < 20 || !fourcc(data, 0, "RIFF") || !fourcc(data, 8, "WEBP")
                || uint32(data, 4) != data.length - 8L) return null;

        int width = 0, height = 0, images = 0;
        boolean extended = false;
        for (int offset = 12; offset < data.length; ) {
            if (data.length - offset < 8) return null;
            long size = uint32(data, offset + 4);
            long end = offset + 8L + size;
            if (end > data.length || end + (size & 1) > data.length) return null;
            int start = offset + 8;
            if (fourcc(data, offset, "VP8X")) {
                if (offset != 12 || size < 10 || extended) return null;
                extended = true;
                if ((data[start] & 0x02) != 0) return null;
                width = 1 + uint24(data, start + 4);
                height = 1 + uint24(data, start + 7);
            } else if (fourcc(data, offset, "ANIM") || fourcc(data, offset, "ANMF")) {
                return null;
            } else if (fourcc(data, offset, "VP8 ")) {
                if (++images > 1 || size < 10 || (data[start] & 1) != 0
                        || (data[start + 3] & 0xff) != 0x9d
                        || (data[start + 4] & 0xff) != 0x01
                        || (data[start + 5] & 0xff) != 0x2a) return null;
                int imageWidth = uint16(data, start + 6) & 0x3fff;
                int imageHeight = uint16(data, start + 8) & 0x3fff;
                if (extended && (width != imageWidth || height != imageHeight)) return null;
                width = imageWidth;
                height = imageHeight;
            } else if (fourcc(data, offset, "VP8L")) {
                if (++images > 1 || size < 5 || (data[start] & 0xff) != 0x2f
                        || (data[start + 4] & 0xe0) != 0) return null;
                int imageWidth = 1 + ((data[start + 1] & 0xff) | ((data[start + 2] & 0x3f) << 8));
                int imageHeight = 1 + (((data[start + 2] & 0xff) >> 6)
                        | ((data[start + 3] & 0xff) << 2) | ((data[start + 4] & 0x0f) << 10));
                if (extended && (width != imageWidth || height != imageHeight)) return null;
                width = imageWidth;
                height = imageHeight;
            }
            offset = (int) (end + (size & 1));
        }
        return images == 1 && width > 0 && height > 0 ? new WebpImageInfo(width, height) : null;
    }

    private static boolean fourcc(byte[] data, int offset, String expected) {
        return new String(data, offset, 4, StandardCharsets.US_ASCII).equals(expected);
    }

    private static int uint16(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    private static int uint24(byte[] data, int offset) {
        return uint16(data, offset) | ((data[offset + 2] & 0xff) << 16);
    }

    private static long uint32(byte[] data, int offset) {
        return (uint16(data, offset) & 0xffffL) | ((long) uint16(data, offset + 2) << 16);
    }
}
