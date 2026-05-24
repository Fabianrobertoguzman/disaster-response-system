package com.coit20258.drs.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Security {

    public static String hashPassword(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainText.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) {
                    hex.append('0');
                }
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    public static boolean verifyPassword(String passwordPlainText, String passwordHash) {
        return passwordHash.equals(hashPassword(passwordPlainText));
    }
}
