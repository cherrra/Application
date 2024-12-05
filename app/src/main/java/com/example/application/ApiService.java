package com.example.application;

import com.google.gson.Gson;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ApiService {
    private static final String BASE_URL = "http://10.0.2.2:5000/api/auth";
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public void register(String username, String email, String password, Callback callback) {
        User user = new User(username, email, password);
        String json = gson.toJson(user);

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(BASE_URL + "/register").post(body).build();
        client.newCall(request).enqueue(callback);
    }

    public void login(String email, String password, Callback callback) {
        User user = new User(null, email, password);
        String json = gson.toJson(user);

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(BASE_URL + "/login").post(body).build();
        client.newCall(request).enqueue(callback);
    }


}

class User {
    private final String username;
    private final String email;
    private final String password;

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}


