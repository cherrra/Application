package com.example.application.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;

//public class EncryptedSharedPrefs {
//    private static final String FILE_NAME = "encrypted_prefs";
//    private final SharedPreferences sharedPreferences;
//
//    public EncryptedSharedPrefs(Context context) throws GeneralSecurityException, IOException {
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
//    public void saveToken(String token) {
//        sharedPreferences.edit().putString("token", token).apply();
//        Log.d("EncryptedSharedPrefs", "Токен: " + (token != null ? token : "очищен"));
//    }
//
//    public String getToken() {
//        return sharedPreferences.getString("token", null);
//    }
//}
public class EncryptedSharedPrefs {
    private static final String FILE_NAME = "encrypted_prefs";
    private final SharedPreferences sharedPreferences;

    public EncryptedSharedPrefs(Context context) throws GeneralSecurityException, IOException {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        sharedPreferences = EncryptedSharedPreferences.create(
                FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    public void saveTokens(String accessToken, String refreshToken) {
        sharedPreferences.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .apply();
        Log.d("EncryptedSharedPrefs", "Токены сохранены");
    }

    public String getAccessToken() {
        return sharedPreferences.getString("access_token", null);
    }

    public String getRefreshToken() {
        return sharedPreferences.getString("refresh_token", null);
    }

    public void clearTokens() {
        sharedPreferences.edit()
                .remove("access_token")
                .remove("refresh_token")
                .apply();
        Log.d("EncryptedSharedPrefs", "Токены очищены");
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public void putString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    public void putStringSet(String key, Set<String> values) {
        sharedPreferences.edit().putStringSet(key, values).apply();
    }

    public Set<String> getStringSet(String key, Set<String> defaultValues) {
        return sharedPreferences.getStringSet(key, defaultValues);
    }


}