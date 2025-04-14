package com.example.application.ui.view.admin;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
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
import java.text.ParseException;
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
    private JSONArray ordersArray;
    private boolean showFinishedOrders = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders_admin);

        adminOrderContainer = findViewById(R.id.adminOrderContainer);

        // Добавляем кнопку для переключения между всеми и завершенными заказами
        Button finishedOrdersButton = findViewById(R.id.finishedOrdersButton);
        finishedOrdersButton.setOnClickListener(v -> {
            showFinishedOrders = !showFinishedOrders;
            finishedOrdersButton.setText(showFinishedOrders ? "Все заказы" : "Завершенные заказы");
            displayOrders();
        });

        // Получение токена из зашифрованных SharedPrefs
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

        // Навигационные кнопки
        setupNavigation();
    }

    private void setupNavigation() {
        findViewById(R.id.homeButton).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeAdminActivity.class));
        });

        findViewById(R.id.usersButton).setOnClickListener(v -> {
            startActivity(new Intent(this, UsersAdminActivity.class));
        });

        findViewById(R.id.orderButton).setOnClickListener(v -> {
            startActivity(new Intent(this, OrdersAdminActivity.class));
        });
    }

    private void fetchAllOrders(String token) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/orders/admin")
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(OrdersAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        ordersArray = new JSONArray(response.body().string());
                        runOnUiThread(() -> displayOrders());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
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

    private void displayOrders() {
        adminOrderContainer.removeAllViews();
        if (ordersArray != null) {
            for (int i = 0; i < ordersArray.length(); i++) {
                try {
                    JSONObject orderObject = ordersArray.getJSONObject(i);
                    String status = orderObject.getString("status");

                    // Показываем заказы в зависимости от выбранного режима
                    if ((showFinishedOrders && "finished".equalsIgnoreCase(status)) ||
                            (!showFinishedOrders && !"finished".equalsIgnoreCase(status))) {
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
            int orderId = orderObject.getInt("id_order");
            String status = orderObject.getString("status");

            // Получаем данные о пользователе
            String userName = orderObject.getString("user_name");
            String userEmail = orderObject.getString("user_email");

            // Получаем данные об автомобиле
            String carModel = orderObject.getString("car_model");
            String carBrand = orderObject.optString("car_brand", "");

            // Формируем строку с информацией об автомобиле
            String carInfo = !carBrand.isEmpty() ? carBrand + " " + carModel : carModel;

            // Создаем основную карточку
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(24, 24, 24, 24);

            // Настраиваем фон карточки
            GradientDrawable cardShape = new GradientDrawable();
            cardShape.setShape(GradientDrawable.RECTANGLE);
            cardShape.setCornerRadius(55f);
            cardShape.setColor(Color.parseColor("#FFFFFF"));
            cardShape.setStroke(4, Color.parseColor("#E3F2FD"));
            card.setBackground(cardShape);

            // Параметры карточки
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(8, 8, 8, 16);
            card.setLayoutParams(cardParams);

            // Добавляем информацию о заказе
            card.addView(createOrderInfoTextView("Заказ #" + orderId, 18f, Color.parseColor("#2260FF")));
            card.addView(createOrderInfoTextView("Статус: " + status, 18f, Color.DKGRAY));
            card.addView(createOrderInfoTextView("Пользователь: " + userName + " (" + userEmail + ")", 16f, Color.DKGRAY));
            card.addView(createOrderInfoTextView("Автомобиль: " + carInfo, 16f, Color.DKGRAY));
            card.addView(createOrderInfoTextView("Дата: " + formattedDate, 14f, Color.DKGRAY));
            card.addView(createOrderInfoTextView("Стоимость: " + orderObject.getDouble("total_price") + " руб.", 14f, Color.DKGRAY));
            card.addView(createOrderInfoTextView("Услуги: " + orderObject.optString("services", "Нет услуг"), 14f, Color.DKGRAY));
            card.addView(createOrderInfoTextView("Комментарий: " + orderObject.optString("comment", "Нет"), 14f, Color.DKGRAY));

            // Добавляем кнопку изменения статуса только для незавершенных заказов
            if (!"finished".equalsIgnoreCase(status)) {
                // Добавляем отступ перед кнопкой
                View spacer = new View(this);
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(16)
                );
                spacer.setLayoutParams(spacerParams);
                card.addView(spacer);

                // Добавляем кнопку изменения статуса
                Button statusButton = new Button(this);
                setupRoundButton(statusButton, "Обновить статус");
                setupButtonAnimation(statusButton);

                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(40)
                );
                statusButton.setLayoutParams(buttonParams);

                statusButton.setOnClickListener(v -> {
                    try {
                        showUpdateStatusDialog(orderId, status);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                card.addView(statusButton);
            }

            // Добавляем карточку в контейнер
            adminOrderContainer.addView(card);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Остальные методы остаются без изменений
    private String formatDate(String rawDate) throws ParseException {
        return new SimpleDateFormat("dd.MM.yyyy")
                .format(new SimpleDateFormat("yyyy-MM-dd").parse(rawDate));
    }

    private TextView createOrderInfoTextView(String text, float textSize, int color) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(color);
        textView.setPadding(0, 4, 0, 4);
        return textView;
    }

    private void setupRoundButton(Button button, String text) {
        GradientDrawable buttonShape = new GradientDrawable();
        buttonShape.setShape(GradientDrawable.RECTANGLE);
        buttonShape.setCornerRadius(30f);
        buttonShape.setColor(Color.parseColor("#2260FF"));

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackground(buttonShape);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, 0, 0, 0);
    }

    private void setupButtonAnimation(Button button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void showUpdateStatusDialog(int orderId, String currentStatus) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Обновить статус заказа");

        String[] statuses = new String[]{"created", "accepted", "in_progress", "finished", "canceled"};

        int selectedIndex = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(currentStatus)) {
                selectedIndex = i;
                break;
            }
        }

        builder.setSingleChoiceItems(statuses, selectedIndex, (dialog, which) -> {});

        builder.setPositiveButton("Обновить", (dialog, which) -> {
            android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
            int selectedPosition = alertDialog.getListView().getCheckedItemPosition();
            if (selectedPosition >= 0 && selectedPosition < statuses.length) {
                String newStatus = statuses[selectedPosition];
                String token = encryptedSharedPrefs.getToken();
                if (token != null) {
                    updateOrderStatus(token, orderId, newStatus);
                } else {
                    Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
                }
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
                .url("http://10.0.2.2:5000/api/orders/admin/" + orderId)
                .put(body)
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(OrdersAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(OrdersAdminActivity.this, "Статус обновлен", Toast.LENGTH_SHORT).show();
                        // Обновляем список заказов
                        fetchAllOrders(token);
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