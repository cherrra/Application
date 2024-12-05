package com.example.application;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OrdersAdminActivity extends AppCompatActivity {

    private LinearLayout adminOrderContainer;
    private EncryptedSharedPrefs encryptedSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders_admin);

        adminOrderContainer = findViewById(R.id.adminOrderContainer);

        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();
            if (token != null) {
                fetchAllOrders(token);
            } else {
                Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        TextView homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrdersAdminActivity.this, HomeAdminActivity.class);
            startActivity(intent);
        });

        TextView usersButton = findViewById(R.id.usersButton);
        usersButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrdersAdminActivity.this, UsersActivity.class);
            startActivity(intent);
        });

        TextView orderButton = findViewById(R.id.orderButton);
        orderButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrdersAdminActivity.this, OrdersAdminActivity.class);
            startActivity(intent);
        });

    }

    private void fetchAllOrders(String token) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/admin/orders")
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("OrdersAdminActivity", "Ошибка сети: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(OrdersAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray ordersArray = new JSONArray(response.body().string());
                        runOnUiThread(() -> {
                            adminOrderContainer.removeAllViews();
                            for (int i = 0; i < ordersArray.length(); i++) {
                                try {
                                    JSONObject orderObject = ordersArray.getJSONObject(i);
                                    addOrderCard(orderObject);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e("OrdersAdminActivity", "Ошибка обработки данных: " + e.getMessage());
                    }
                } else {
                    Log.e("OrdersAdminActivity", "Ответ сервера: " + response.code());
                    runOnUiThread(() -> {
                        if (response.code() == 403) {
                            Toast.makeText(OrdersAdminActivity.this, "Нет прав администратора", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(OrdersAdminActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void addOrderCard(JSONObject orderObject) {
        try {
            String rawDate = orderObject.getString("order_date");
            String formattedDate = new SimpleDateFormat("dd.MM.yyyy")
                    .format(new SimpleDateFormat("yyyy-MM-dd").parse(rawDate));

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16, 16, 16, 16);
            card.setBackgroundColor(Color.parseColor("#E0E0E0"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            card.setLayoutParams(params);

            TextView userView = new TextView(this);
            userView.setText("Пользователь: " + orderObject.getString("user_name") +
                    " (" + orderObject.getString("user_email") + ")");
            userView.setTextSize(16f);
            userView.setTextColor(Color.DKGRAY);
            card.addView(userView);

            TextView orderView = new TextView(this);
            orderView.setText("Номер заказа: " + orderObject.getInt("id_order"));
            orderView.setTextSize(18f);
            orderView.setTextColor(Color.BLACK);
            card.addView(orderView);

            TextView statusView = new TextView(this);
            statusView.setText("Статус: " + orderObject.getString("status"));
            statusView.setTextSize(18f);
            statusView.setTextColor(Color.DKGRAY);
            card.addView(statusView);

            TextView carView = new TextView(this);
            carView.setText("Модель машины: " + orderObject.getString("car_model"));
            carView.setTextSize(16f);
            carView.setTextColor(Color.DKGRAY);
            card.addView(carView);

            TextView dateView = new TextView(this);
            dateView.setText("Дата: " + formattedDate);
            dateView.setTextSize(14f);
            dateView.setTextColor(Color.DKGRAY);
            card.addView(dateView);

            TextView priceView = new TextView(this);
            priceView.setText("Стоимость: " + orderObject.getDouble("total_price") + " руб.");
            priceView.setTextSize(14f);
            priceView.setTextColor(Color.DKGRAY);
            card.addView(priceView);

            TextView servicesView = new TextView(this);
            servicesView.setText("Услуги: " + orderObject.optString("services", "Нет услуг"));
            servicesView.setTextSize(14f);
            servicesView.setTextColor(Color.DKGRAY);
            card.addView(servicesView);

            TextView commentView = new TextView(this);
            commentView.setText("Комментарий: " + orderObject.optString("comment", "Нет"));
            commentView.setTextSize(14f);
            commentView.setTextColor(Color.DKGRAY);
            card.addView(commentView);

            card.setOnClickListener(v -> {
                try {
                    int orderId = orderObject.getInt("id_order");
                    String currentStatus = orderObject.getString("status");
                    showUpdateStatusDialog(orderId, currentStatus);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            adminOrderContainer.addView(card);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showUpdateStatusDialog(int orderId, String currentStatus) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Обновить статус заказа");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Новый статус");
        input.setText(currentStatus);
        builder.setView(input);

        builder.setPositiveButton("Обновить", (dialog, which) -> {
            String newStatus = input.getText().toString().trim();
            if (!newStatus.isEmpty()) {
                String token = encryptedSharedPrefs.getToken();
                if (token != null) {
                    updateOrderStatus(token, orderId, newStatus);
                } else {
                    Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Статус не может быть пустым", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void updateOrderStatus(String token, int orderId, String newStatus) {
        OkHttpClient client = new OkHttpClient();

        JSONObject statusUpdate = new JSONObject();
        try {
            statusUpdate.put("status", newStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                statusUpdate.toString()
        );

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/admin/orders/" + orderId)
                .put(body)
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("OrdersAdminActivity", "Ошибка сети: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(OrdersAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(OrdersAdminActivity.this, "Статус обновлен", Toast.LENGTH_SHORT).show();
                    } else if (response.code() == 403) {
                        Toast.makeText(OrdersAdminActivity.this, "Нет прав администратора", Toast.LENGTH_SHORT).show();
                    } else if (response.code() == 404) {
                        Toast.makeText(OrdersAdminActivity.this, "Заказ не найден", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(OrdersAdminActivity.this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }


}
