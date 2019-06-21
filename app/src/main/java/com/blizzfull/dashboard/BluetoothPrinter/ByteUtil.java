package com.blizzfull.dashboard.BluetoothPrinter;

import android.support.v4.view.MotionEventCompat;

public class ByteUtil {
    private static final String TAG = "ByteUtil";

    public static String byteArray2HexStr(byte[] byteArray) {
        StringBuffer hexString = new StringBuffer();
        for (byte b : byteArray) {
            String hex = Integer.toHexString(b & MotionEventCompat.ACTION_MASK);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            hexString.append("0x" + hex.toUpperCase() + ",");
        }
        hexString.deleteCharAt(hexString.length() - 1);
        return hexString.toString();
    }

    public static String intArray2HexStr(int[] intArray) {
        StringBuffer hexString = new StringBuffer();
        for (int toHexString : intArray) {
            String hex = Integer.toHexString(toHexString);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            hexString.append("0x" + hex.toUpperCase() + ",");
        }
        hexString.deleteCharAt(hexString.length() - 1);
        return hexString.toString();
    }

    public static byte hexStr2Byte(String hexStr) {
        char[] chars = hexStr.toUpperCase().toCharArray();
        int b = 0;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            b = (int) (((double) b) + (((double) (c - (c >= 'A' ? 55 : 48))) * Math.pow(16.0d, (double) ((chars.length - 1) - i))));
        }
        return (byte) b;
    }
}
