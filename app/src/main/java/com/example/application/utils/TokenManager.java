//package com.example.application.utils;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.util.Log;
//
//import androidx.security.crypto.EncryptedSharedPreferences;
//import androidx.security.crypto.MasterKeys;
//
//import java.io.IOException;
//import java.security.GeneralSecurityException;
//
//public class TokenManager {
//    private static final String FILE_NAME = "encrypted_prefs";
//    private static final String ACCESS_TOKEN_KEY = "access_token";
//    private static final String REFRESH_TOKEN_KEY = "refresh_token";
//
//    private final SharedPreferences sharedPreferences;
//
//    public TokenManager(Context context) throws GeneralSecurityException, IOException {
//        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
//        sharedPreferences = EncryptedSharedPreferences.create(
//                FILE_NAME,
//                masterKeyAlias,
//                context,
//                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
//                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
//        );
//    }
//
//    public void saveTokens(String accessToken, String refreshToken) {
//        sharedPreferences.edit()
//                .putString(ACCESS_TOKEN_KEY, accessToken)
//                .putString(REFRESH_TOKEN_KEY, refreshToken)
//                .apply();
//
//        Log.d("TokenManager", "Access токен сохранен: " + (accessToken != null ? "***" + accessToken.substring(accessToken.length() - 5) : "null"));
//        Log.d("TokenManager", "Refresh токен сохранен: " + (refreshToken != null ? "***" + refreshToken.substring(refreshToken.length() - 5) : "null"));
//    }
//
//    public String getAccessToken() {
//        return sharedPreferences.getString(ACCESS_TOKEN_KEY, null);
//    }
//
//    public String getRefreshToken() {
//        return sharedPreferences.getString(REFRESH_TOKEN_KEY, null);
//    }
//
//    public void clearTokens() {
//        sharedPreferences.edit()
//                .remove(ACCESS_TOKEN_KEY)
//                .remove(REFRESH_TOKEN_KEY)
//                .apply();
//        Log.d("TokenManager", "Токены очищены");
//    }
//}