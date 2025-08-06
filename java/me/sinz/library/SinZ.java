package me.sinz.library;

import android.content.Context;

public class SinZ {

    public static int dip2px(Context ctx, int dips) {
        return (int) Math.ceil(dips * ctx.getResources().getDisplayMetrics().density);
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytes2hex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int n = 0; n < bytes.length; n++) {
            int v = bytes[n] & 0xFF;
            hexChars[n * 2] = HEX_ARRAY[v >>> 4];
            hexChars[n * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

}
