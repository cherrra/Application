package com.example.application.utils;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TokenInterceptor implements Interceptor {
    private final EncryptedSharedPrefs prefs;
    private final OkHttpClient client;

    public TokenInterceptor(Context context) throws GeneralSecurityException, IOException {
        this.prefs = new EncryptedSharedPrefs(context);
        this.client = new OkHttpClient();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        String accessToken = prefs.getAccessToken();
        if (accessToken != null) {
            originalRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .build();
        }

        Response response = chain.proceed(originalRequest);

        if (response.code() == 401) {
            response.close();
            String refreshToken = prefs.getRefreshToken();
            if (refreshToken == null) {
                throw new IOException("No refresh token available");
            }

            String refreshUrl = originalRequest.url().newBuilder()
                    .encodedPath("/api/auth/refresh")
                    .build()
                    .toString();

            RequestBody body = new FormBody.Builder()
                    .add("refreshToken", refreshToken)
                    .build();

            Request refreshRequest = new Request.Builder()
                    .url(refreshUrl)
                    .post(body)
                    .build();

            Response refreshResponse = client.newCall(refreshRequest).execute();

            if (refreshResponse.isSuccessful()) {
                try {
                    String responseBody = refreshResponse.body().string();
                    JSONObject json = new JSONObject(responseBody);
                    String newAccessToken = json.getString("accessToken");

                    prefs.saveTokens(newAccessToken, refreshToken);

                    Request newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer " + newAccessToken)
                            .build();

                    return chain.proceed(newRequest);
                } catch (JSONException e) {
                    throw new IOException("Failed to parse refresh response", e);
                }
            } else {
                prefs.clearTokens();
                throw new IOException("Failed to refresh token");
            }
        }

        return response;
    }
}