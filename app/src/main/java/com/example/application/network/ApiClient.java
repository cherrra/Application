package com.example.application.network;

import com.example.application.network.request.LoginRequest;
import com.example.application.network.request.RegisterRequest;
import com.google.gson.Gson;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ApiClient {
    private static final String BASE_URL = "http://10.0.2.2:5000/api/";
    private static ApiClient instance;
    private final OkHttpClient client;
    private final Gson gson = new Gson();

    public ApiClient() {
        client = new OkHttpClient();
    }

    public static ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }


    public void register(String username, String email, String password, Callback callback) {
        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        String json = gson.toJson(registerRequest);

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(BASE_URL + "auth/register")
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public void login(String email, String password, Callback callback) {
        LoginRequest loginRequest = new LoginRequest(email, password);
        String json = gson.toJson(loginRequest);

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(BASE_URL + "auth/login")
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public void getUserDetails(String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "auth/user")
                .addHeader("Authorization", token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void deleteAccount(String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "auth/delete")
                .delete()
                .addHeader("Authorization", token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void updateUser(RequestBody body, String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "auth/user")
                .addHeader("Authorization", token)
                .put(body)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public void uploadImage(RequestBody body, String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/upload")
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }


    public void getCars(String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "cars")
                .addHeader("Authorization", token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    // Удалить машину
    public void deleteCar(int carId, String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "cars/" + carId)
                .delete()
                .addHeader("Authorization", token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    // Добавить машину
    public void addCar(RequestBody body, String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "cars")
                .addHeader("Authorization", token)
                .post(body)
                .build();
        client.newCall(request).enqueue(callback);
    }


    // Обновить данные машины
    public void updateCar(int carId, RequestBody body, String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "cars/" + carId)
                .addHeader("Authorization", token)
                .put(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void getCategories(String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "categories")
                .addHeader("Authorization", token)
                .build();
        client.newCall(request).enqueue(callback);
    }
    public void getServices(int categoryId, String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "services?id_category=" + categoryId)
                .addHeader("Authorization", token)
                .build();
        client.newCall(request).enqueue(callback);
    }


    public void addService(RequestBody body, String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "services")
                .post(body)
                .addHeader("Authorization", token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void updateService(int serviceId, RequestBody body, String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "services/" + serviceId)
                .put(body)
                .addHeader("Authorization", token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void deleteService(int serviceId, String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "services/" + serviceId)
                .delete()
                .addHeader("Authorization", token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void getOrders(String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "orders")
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void getAdminOrders(String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "orders/admin")
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void deleteOrder(String token, int orderId, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "orders/" + orderId)
                .delete()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void updateOrderStatus(int orderId, String newStatus, String token, Callback callback) {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                "{\"status\": \"" + newStatus + "\"}"
        );

        Request request = new Request.Builder()
                .url(BASE_URL + "orders/admin/" + orderId)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public void getUserById(String token, int userId, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "users/" + userId)
                .addHeader("Authorization", "Bearer " + token)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void updateCategory(int categoryId, RequestBody body, String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "categories/" + categoryId)
                .addHeader("Authorization", token)
                .put(body)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void deleteCategory(int categoryId, String token, Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "categories/" + categoryId)
                .delete()
                .addHeader("Authorization", token)
                .build();
        client.newCall(request).enqueue(callback);
    }
    public void getUsers(String token, Callback callback) {
         Request request = new Request.Builder()
                 .url(BASE_URL + "users")
                 .addHeader("Authorization", token)
                 .build();
         client.newCall(request).enqueue(callback);
        }

    public void deleteUser(int userId, String token, Callback callback) {
        Request request = new Request.Builder()
               .url(BASE_URL + "users/" + userId)
               .addHeader("Authorization", token)
               .delete()
               .build();
            client.newCall(request).enqueue(callback);
        }

}

