package us.kbase.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TextUtils {
    private static final String HEXES = "0123456789abcdef";

    public static String stringToHex(String text) {
        return byteToHex(text.getBytes(Charset.forName("utf-8")));
    }

    public static String byteToHex(byte[] bytes) {
        final StringBuilder hex = new StringBuilder(2 * bytes.length);
        for (final byte b : bytes)
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        return hex.toString();
    }

    public static String streamToHex(InputStream is) throws IOException {
        byte[] buffer = new byte[10000];
        final StringBuilder hex = new StringBuilder();
        while (true) {
            int len = is.read(buffer);
            if (len < 0)
                break;
            for (int i = 0; i < len; i++) {
                byte b = buffer[i];
                hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
            }
        }
        return hex.toString();
    }

    public static String hexToString(String hex) {
        return new String(hexToBytes(hex), Charset.forName("utf-8"));
    }
    
    public static byte[] hexToBytes(String hex) {
        hex = hex.toLowerCase();
        byte[] ret = new byte[hex.length() / 2];
        for (int i = 0; i < ret.length; i++)
            ret[i] = (byte)Integer.parseInt(hex.substring(i * 2, (i + 1) * 2), 16);
        return ret;
    }
    
    public static String getMD5(InputStream is) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[10000];
        while (true) {
            int len = is.read(buffer);
            if (len < 0)
                break;
            if (len == 0)
                continue;
            digest.update(buffer, 0, len);
        }
        return byteToHex(digest.digest());
    }
}
