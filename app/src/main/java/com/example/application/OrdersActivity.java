package com.example.application;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OrdersActivity extends AppCompatActivity {

    private LinearLayout orderContainer;
    private JSONArray ordersArray;
    private boolean isHistoryDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        orderContainer = findViewById(R.id.orderContainer);
        fetchOrders();

        Button historyButton = findViewById(R.id.historyButton);
        historyButton.setOnClickListener(v -> {
            if (isHistoryDisplayed) {
                displayAllOrders();
                historyButton.setText("История");
                isHistoryDisplayed = false;
            } else {
                displayFinishedOrders();
                historyButton.setText("Все заказы");
                isHistoryDisplayed = true;
            }
        });

        TextView homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrdersActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        TextView accountButton = findViewById(R.id.accountButton);
        accountButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrdersActivity.this, AccountActivity.class);
            startActivity(intent);
        });

        TextView garageButton = findViewById(R.id.garageButton);
        garageButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrdersActivity.this, GarageActivity.class);
            startActivity(intent);
        });

        TextView orderButton = findViewById(R.id.orderButton);
        orderButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrdersActivity.this, OrdersActivity.class);
            startActivity(intent);
        });

        TextView addOrderButton = findViewById(R.id.addOrderButton);
        addOrderButton.setOnClickListener(v -> {
            Intent intent = new Intent(OrdersActivity.this, AddOrderActivity.class);
            startActivity(intent);
        });
    }

    private void fetchOrders() {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (GeneralSecurityException | IOException e) {
            Log.e("OrdersActivity", "Ошибка инициализации EncryptedSharedPrefs: " + e.getMessage());
            return;
        }

        if (token == null) {
            Log.e("OrdersActivity", "Токен отсутствует");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/orders")
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("OrdersActivity", "Ошибка получения данных: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(OrdersActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        ordersArray = new JSONArray(response.body().string());
                        runOnUiThread(() -> displayAllOrders());
                    } catch (Exception e) {
                        Log.e("OrdersActivity", "Ошибка обработки ответа: " + e.getMessage());
                    }
                } else {
                    Log.e("OrdersActivity", "Ответ сервера: " + response.code());
                    runOnUiThread(() -> Toast.makeText(OrdersActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void displayAllOrders() {
        orderContainer.removeAllViews();
        if (ordersArray != null) {
            for (int i = 0; i < ordersArray.length(); i++) {
                try {
                    JSONObject orderObject = ordersArray.getJSONObject(i);
                    String status = orderObject.getString("status");
                    if (!"finished".equalsIgnoreCase(status)) { // Исключение finished-заказов
                        addOrderCard(orderObject);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void displayFinishedOrders() {
        orderContainer.removeAllViews();
        if (ordersArray != null) {
            for (int i = 0; i < ordersArray.length(); i++) {
                try {
                    JSONObject orderObject = ordersArray.getJSONObject(i);
                    String status = orderObject.getString("status");
                    if ("finished".equalsIgnoreCase(status)) {
                        addOrderCard(orderObject);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

private void addOrderCard(JSONObject orderObject) {
    try {
        String rawDate = orderObject.getString("order_date");
        String formattedDate = formatDate(rawDate);

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

        TextView idView = new TextView(this);
        int orderId = orderObject.getInt("id_order");
        idView.setText("Номер заказа: " + orderId);
        idView.setTextSize(18f);
        idView.setTextColor(Color.BLACK);
        card.addView(idView);

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

        TextView timeView = new TextView(this);
        timeView.setText("Время: " + orderObject.getString("order_time"));
        timeView.setTextSize(14f);
        timeView.setTextColor(Color.DKGRAY);
        card.addView(timeView);

        TextView priceView = new TextView(this);
        priceView.setText("Стоимость: " + orderObject.getDouble("total_price") + " руб.");
        priceView.setTextSize(14f);
        priceView.setTextColor(Color.DKGRAY);
        card.addView(priceView);

        TextView statusView = new TextView(this);
        String status = orderObject.getString("status");
        statusView.setText("Статус: " + status);
        statusView.setTextSize(14f);
        statusView.setTextColor(Color.DKGRAY);
        card.addView(statusView);

        if (!"finished".equalsIgnoreCase(status)) {
            Button cancelButton = new Button(this);
            cancelButton.setText("Отменить");
            cancelButton.setOnClickListener(v -> deleteOrder(orderId, card));
            card.addView(cancelButton);
        }

        orderContainer.addView(card);
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    private void deleteOrder(int orderId, View cardView) {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (GeneralSecurityException | IOException e) {
            Log.e("OrdersActivity", "Ошибка токена: " + e.getMessage());
            return;
        }

        if (token == null) {
            Log.e("OrdersActivity", "Токен отсутствует");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/orders/" + orderId)
                .delete()
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("OrdersActivity", "Ошибка удаления: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(OrdersActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        orderContainer.removeView(cardView); // Удаление карточки с клиента
                        Toast.makeText(OrdersActivity.this, "Заказ удалён", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.e("OrdersActivity", "Ошибка сервера: " + response.code());
                    runOnUiThread(() -> Toast.makeText(OrdersActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }


    private String formatDate(String rawDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy");
            return outputFormat.format(inputFormat.parse(rawDate));
        } catch (Exception e) {
            e.printStackTrace();
            return rawDate;
        }
    }
}

