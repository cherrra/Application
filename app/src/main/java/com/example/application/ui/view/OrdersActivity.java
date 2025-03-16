package com.example.application.ui.view;

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

import com.example.application.R;
import com.example.application.utils.EncryptedSharedPrefs;

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

        setupNavigation();
    }

    private void setupNavigation() {
        findViewById(R.id.homeButton).setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.accountButton).setOnClickListener(v -> startActivity(new Intent(this, AccountActivity.class)));
        findViewById(R.id.garageButton).setOnClickListener(v -> startActivity(new Intent(this, GarageActivity.class)));
        findViewById(R.id.orderButton).setOnClickListener(v -> startActivity(new Intent(this, OrdersActivity.class)));
        findViewById(R.id.addOrderButton).setOnClickListener(v -> startActivity(new Intent(this, AddOrderActivity.class)));
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
                        runOnUiThread(OrdersActivity.this::displayAllOrders);
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
                    if (!"finished".equalsIgnoreCase(orderObject.getString("status"))) {
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
                    if ("finished".equalsIgnoreCase(orderObject.getString("status"))) {
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
            String formattedDate = formatDate(orderObject.getString("order_date"));

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

            int orderId = orderObject.getInt("id_order");

            card.addView(createTextView("Номер заказа: " + orderId, 18f, Color.BLACK));
            card.addView(createTextView("Модель машины: " + orderObject.getString("car_model"), 16f, Color.DKGRAY));
            card.addView(createTextView("Дата: " + formattedDate, 14f, Color.DKGRAY));
            card.addView(createTextView("Время: " + orderObject.getString("order_time"), 14f, Color.DKGRAY));
            card.addView(createTextView("Услуги: " + orderObject.getString("services"), 14f, Color.DKGRAY));
            card.addView(createTextView("Стоимость: " + orderObject.getDouble("total_price") + " руб.", 14f, Color.DKGRAY));

            String status = orderObject.getString("status");
            card.addView(createTextView("Статус: " + status, 14f, Color.DKGRAY));

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

    private TextView createTextView(String text, float textSize, int color) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(color);
        return textView;
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
                        orderContainer.removeView(cardView);
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
