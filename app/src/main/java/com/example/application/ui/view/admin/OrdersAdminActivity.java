package com.example.application.ui.view.admin;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Timer;
import java.util.TimerTask;

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
    private JSONArray allOrdersArray; // Для хранения всех заказов
    private boolean showFinishedOrders = false;
    private EditText searchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders_admin);

        adminOrderContainer = findViewById(R.id.adminOrderContainer);
        searchEditText = findViewById(R.id.searchEditText);

        Button finishedOrdersButton = findViewById(R.id.finishedOrdersButton);
        finishedOrdersButton.setOnClickListener(v -> {
            showFinishedOrders = !showFinishedOrders;
            finishedOrdersButton.setText(showFinishedOrders ? "Все заказы" : "Завершенные заказы");
            filterOrders(searchEditText.getText().toString());
        });

        setupSearchView();

        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getAccessToken();
            if (token != null) {
                fetchAllOrders(token);
            } else {
                Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        setupNavigation();
    }

    private void setupSearchView() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> filterOrders(s.toString()));
                    }
                }, 300);
            }
        });
    }

    private void filterOrders(String query) {
        adminOrderContainer.removeAllViews();

        if (ordersArray == null) return;

        try {
            for (int i = 0; i < ordersArray.length(); i++) {
                JSONObject orderObject = ordersArray.getJSONObject(i);
                String status = orderObject.getString("status");

                // Проверяем фильтр по статусу
                boolean statusMatch = (showFinishedOrders && "finished".equalsIgnoreCase(status)) ||
                        (!showFinishedOrders && !"finished".equalsIgnoreCase(status));

                if (statusMatch) {
                    // Если поисковый запрос пустой или совпадает с номером заказа или именем пользователя
                    if (query.isEmpty() ||
                            String.valueOf(orderObject.getInt("id_order")).contains(query) ||
                            orderObject.getString("user_name").toLowerCase().contains(query.toLowerCase())) {
                        addOrderCard(orderObject);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("OrdersAdminActivity", "Ошибка фильтрации заказов", e);
        }
    }

    private void fetchAllOrders(String token) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://automser.store/api/orders/admin")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(OrdersAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d("OrdersAdminResponse", "Ответ сервера: " + responseBody);
                        ordersArray = new JSONArray(responseBody);
                        allOrdersArray = new JSONArray(responseBody); // Сохраняем все заказы
                        runOnUiThread(() -> filterOrders(searchEditText.getText().toString()));
                    } catch (Exception e) {
                        Log.e("OrdersAdminActivity", "Ошибка обработки ответа", e);
                    }
                } else {
                    runOnUiThread(() -> {
                        if (response.code() == 403) {
                            Toast.makeText(OrdersAdminActivity.this,
                                    "Нет прав администратора", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(OrdersAdminActivity.this,
                                    "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void setupNavigation() {
        findViewById(R.id.homeButton).setOnClickListener(v ->
                startActivity(new Intent(this, HomeAdminActivity.class)));
        findViewById(R.id.usersButton).setOnClickListener(v ->
                startActivity(new Intent(this, UsersAdminActivity.class)));
        findViewById(R.id.orderButton).setOnClickListener(v ->
                startActivity(new Intent(this, OrdersAdminActivity.class)));
    }

    private void displayOrders() {
        adminOrderContainer.removeAllViews();
        if (ordersArray != null) {
            for (int i = 0; i < ordersArray.length(); i++) {
                try {
                    JSONObject orderObject = ordersArray.getJSONObject(i);
                    String status = orderObject.getString("status");

                    if ((showFinishedOrders && "finished".equalsIgnoreCase(status)) ||
                            (!showFinishedOrders && !"finished".equalsIgnoreCase(status))) {
                        addOrderCard(orderObject);
                    }
                } catch (Exception e) {
                    Log.e("OrdersAdminActivity", "Ошибка обработки заказа", e);
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
            String carInfo = "Не указано";
            if (orderObject.has("car")) {
                JSONObject carObject = orderObject.getJSONObject("car");
                if (carObject.has("model")) {
                    JSONObject modelObject = carObject.getJSONObject("model");
                    String brandName = modelObject.getJSONObject("brand").getString("brand_name");
                    String modelName = modelObject.getString("model_name");
                    carInfo = brandName + " " + modelName;
                }
            }

            // Создаем карточку
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(24, 24, 24, 24);

            GradientDrawable cardShape = new GradientDrawable();
            cardShape.setShape(GradientDrawable.RECTANGLE);
            cardShape.setCornerRadius(55f);
            cardShape.setColor(Color.parseColor("#FFFFFF"));
            cardShape.setStroke(4, Color.parseColor("#E3F2FD"));
            card.setBackground(cardShape);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(8, 8, 8, 16);
            card.setLayoutParams(cardParams);

            // Добавляем информацию о заказе
            card.addView(createOrderInfoTextView("Заказ #" + orderId, 18f, Color.parseColor("#2260FF"), true));
            card.addView(createOrderInfoTextView("Статус: " + status, 18f, Color.DKGRAY, false));
            card.addView(createOrderInfoTextView("Пользователь: " + userName + " (" + userEmail + ")", 16f, Color.DKGRAY, false));
            card.addView(createOrderInfoTextView("Автомобиль: " + carInfo, 16f, Color.DKGRAY, false));
            card.addView(createOrderInfoTextView("Дата: " + formattedDate, 14f, Color.DKGRAY, false));
            card.addView(createOrderInfoTextView("Стоимость: " + orderObject.getDouble("total_price") + " руб.", 14f, Color.DKGRAY, false));
            card.addView(createOrderInfoTextView("Услуги: " + orderObject.optString("services", "Нет услуг"), 14f, Color.DKGRAY, false));
            card.addView(createOrderInfoTextView("Комментарий: " + orderObject.optString("comment", "Нет"), 14f, Color.DKGRAY, false));

            if (!"finished".equalsIgnoreCase(status)) {
                View spacer = new View(this);
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(16)
                );
                spacer.setLayoutParams(spacerParams);
                card.addView(spacer);

                Button statusButton = new Button(this);
                setupRoundButton(statusButton, "Обновить статус");
                statusButton.setOnClickListener(v -> showUpdateStatusDialog(orderId, status));

                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(40)
                );
                statusButton.setLayoutParams(buttonParams);
                card.addView(statusButton);
            }

            adminOrderContainer.addView(card);
        } catch (Exception e) {
            Log.e("OrdersAdminActivity", "Ошибка создания карточки заказа", e);
        }
    }

    private TextView createOrderInfoTextView(String text, float textSize, int color, boolean isBold) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(color);
        textView.setPadding(0, 4, 0, 4);
        if (isBold) {
            textView.setTypeface(null, Typeface.BOLD);
        }
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

    private String formatDate(String rawDate) throws ParseException {
        return new SimpleDateFormat("dd.MM.yyyy")
                .format(new SimpleDateFormat("yyyy-MM-dd").parse(rawDate));
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

        builder.setSingleChoiceItems(statuses, selectedIndex, null);
        builder.setPositiveButton("Обновить", (dialog, which) -> {
            android.app.AlertDialog alertDialog = (android.app.AlertDialog) dialog;
            int selectedPosition = alertDialog.getListView().getCheckedItemPosition();
            if (selectedPosition >= 0) {
                try {
                    String token = encryptedSharedPrefs.getAccessToken();
                    if (token != null) {
                        updateOrderStatus(token, orderId, statuses[selectedPosition]);
                    }
                } catch (Exception e) {
                    Log.e("OrdersAdminActivity", "Ошибка обновления статуса", e);
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
            Log.e("OrdersAdminActivity", "Ошибка создания JSON", e);
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                statusUpdate.toString()
        );

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/orders/admin/" + orderId)
                .put(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(OrdersAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(OrdersAdminActivity.this, "Статус обновлен", Toast.LENGTH_SHORT).show();
                        try {
                            fetchAllOrders(encryptedSharedPrefs.getAccessToken());
                        } catch (Exception e) {
                            Log.e("OrdersAdminActivity", "Ошибка обновления списка", e);
                        }
                    } else {
                        Toast.makeText(OrdersAdminActivity.this,
                                "Ошибка обновления: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}