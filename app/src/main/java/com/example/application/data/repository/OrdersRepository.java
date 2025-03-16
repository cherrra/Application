package com.example.application.data.repository;

import com.example.application.data.model.Order;
import com.example.application.data.model.User;
import com.example.application.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class OrdersRepository {

    private ApiClient apiClient;

    public OrdersRepository(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void fetchUserById(String token, int userId, UserCallback callback) {
        apiClient.getUserById(token, userId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        User user = new User(
                                jsonObject.getInt("id"),
                                jsonObject.getString("username"),
                                jsonObject.getString("email"),
                                jsonObject.getString("birth_date"),
                                User.Gender.fromString(jsonObject.getString("gender")),
                                jsonObject.getString("phone_number"),
                                jsonObject.getString("link_img"),
                                jsonObject.getBoolean("is_admin"),
                                jsonObject.getBoolean("id_admin"),
                                jsonObject.getInt("id_car")
                        );
                        callback.onSuccess(user);
                    } catch (Exception e) {
                        callback.onError("Ошибка обработки данных: " + e.getMessage());
                    }
                } else {
                    callback.onError("Ошибка сервера: " + response.code());
                }
            }
        });
    }



    public void fetchOrders(String token, OrderCallback callback) {
        apiClient.getOrders(token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<Order> orders = Order.fromJsonArray(jsonArray);
                        callback.onSuccess(orders);
                    } catch (Exception e) {
                        callback.onError("Ошибка обработки данных: " + e.getMessage());
                    }
                } else {
                    callback.onError("Ошибка сервера: " + response.code());
                }
            }
        });
    }

    public void fetchAdminOrders(String token, OrderCallback callback) {
        apiClient.getAdminOrders(token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<Order> orders = Order.fromJsonArray(jsonArray);
                        callback.onSuccess(orders);
                    } catch (Exception e) {
                        callback.onError("Ошибка обработки данных: " + e.getMessage());
                    }
                } else {
                    callback.onError("Ошибка сервера: " + response.code());
                }
            }
        });
    }

    public void deleteOrder(String token, int orderId, OrderDeleteCallback callback) {
        apiClient.deleteOrder(token, orderId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Ошибка удаления: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Ошибка сервера: " + response.code());
                }
            }
        });
    }

    public void updateOrderStatus(String token, int orderId, String newStatus, OrderUpdateCallback callback) {
        apiClient.updateOrderStatus(orderId, newStatus, token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Ошибка сети: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Ошибка сервера: " + response.code());
                }
            }
        });
    }

    public interface OrderCallback {
        void onSuccess(List<Order> orders);
        void onError(String error);
    }

    public interface OrderDeleteCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface OrderUpdateCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onError(String error);
    }
}
