package com.example.application;


import android.util.Base64;
import org.json.JSONObject;

public class JwtUtils {
    public static String decodeToken(String token, String key) {
        try {
            String[] split = token.split("\\.");
            String body = new String(Base64.decode(split[1], Base64.URL_SAFE));
            JSONObject jsonObject = new JSONObject(body);
            return jsonObject.getString(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
