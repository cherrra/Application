package com.example.application.ui.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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

        // Кнопка переключения между активными и завершенными заказами
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

    // Настройка нижней панели навигации
    private void setupNavigation() {
        findViewById(R.id.homeButton).setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.accountButton).setOnClickListener(v -> startActivity(new Intent(this, AccountActivity.class)));
        findViewById(R.id.garageButton).setOnClickListener(v -> startActivity(new Intent(this, GarageActivity.class)));
        findViewById(R.id.orderButton).setOnClickListener(v -> startActivity(new Intent(this, OrdersActivity.class)));
        findViewById(R.id.addOrderButton).setOnClickListener(v -> startActivity(new Intent(this, AddOrderActivity.class)));
    }

    // Запрос списка заказов с сервера
    private void fetchOrders() {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (GeneralSecurityException | IOException e) {
            Log.e("OrdersActivity", "Ошибка получения токена: " + e.getMessage());
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
                Log.e("OrdersActivity", "Ошибка сети: " + e.getMessage());
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
                    Log.e("OrdersActivity", "Ошибка сервера: " + response.code());
                    runOnUiThread(() -> Toast.makeText(OrdersActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // Отображение всех заказов (кроме завершенных)
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
                    Log.e("OrdersActivity", "Ошибка обработки заказа: " + e.getMessage());
                }
            }
        }
    }

    // Отображение только завершенных заказов
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
                    Log.e("OrdersActivity", "Ошибка обработки заказа: " + e.getMessage());
                }
            }
        }
    }

    // Создание карточки заказа
    private void addOrderCard(JSONObject orderObject) {
        try {
            String formattedDate = formatDate(orderObject.getString("order_date"));
            int orderId = orderObject.getInt("id_order");
            String status = orderObject.getString("status");

            // Получаем данные об автомобиле
            String carModel = orderObject.getString("car_model");
            String carBrand = orderObject.optString("car_brand", ""); // Безопасное получение марки

            // Формируем строку с информацией об автомобиле
            String carInfo;
            if (!carBrand.isEmpty()) {
                carInfo = carBrand + " " + carModel; // Если марка есть - "Марка Модель"
            } else {
                carInfo = carModel; // Если нет марки - только модель
            }

            // Создаем основную карточку
            RelativeLayout card = new RelativeLayout(this);
            card.setPadding(24, 24, 24, 24);

            // Настраиваем фон карточки
            GradientDrawable cardShape = new GradientDrawable();
            cardShape.setShape(GradientDrawable.RECTANGLE);
            cardShape.setCornerRadius(55f);
            cardShape.setColor(Color.parseColor("#80CAD6FF"));
            cardShape.setStroke(2, Color.parseColor("#CAD6FF"));
            card.setBackground(cardShape);

            // Параметры карточки
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(8, 8, 8, 16);
            card.setLayoutParams(cardParams);

            // Контейнер для основной информации
            LinearLayout infoLayout = new LinearLayout(this);
            infoLayout.setOrientation(LinearLayout.VERTICAL);
            infoLayout.setId(View.generateViewId());

            RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            infoParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            infoParams.addRule(RelativeLayout.CENTER_VERTICAL);
            infoLayout.setLayoutParams(infoParams);

            // Добавляем информацию о заказе
            infoLayout.addView(createOrderInfoTextView("Заказ #" + orderId, 18f, Color.BLACK));
            infoLayout.addView(createOrderInfoTextView("Автомобиль: " + carInfo, 16f, Color.DKGRAY)); // Отображаем марку и модель
            infoLayout.addView(createOrderInfoTextView("Дата: " + formattedDate, 14f, Color.DKGRAY));
            infoLayout.addView(createOrderInfoTextView("Время: " + orderObject.getString("order_time"), 14f, Color.DKGRAY));
            infoLayout.addView(createOrderInfoTextView("Услуги: " + orderObject.getString("services"), 14f, Color.DKGRAY));
            infoLayout.addView(createOrderInfoTextView("Стоимость: " + orderObject.getDouble("total_price") + " руб.", 14f, Color.DKGRAY));

            // Добавляем кнопки, если заказ не завершен
            if (!"finished".equalsIgnoreCase(status)) {
                View spacer = new View(this);
                LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(16)
                );
                spacer.setLayoutParams(spacerParams);
                infoLayout.addView(spacer);

                Button cancelButton = new Button(this);
                setupRoundButton(cancelButton, "Отменить");
                cancelButton.setOnClickListener(v -> deleteOrder(orderId, card));
                setupButtonAnimation(cancelButton);
                infoLayout.addView(cancelButton);
            }

            card.addView(infoLayout);

            // Добавляем статус в правую часть
            TextView statusTextView = new TextView(this);
            statusTextView.setText(status.toUpperCase());
            statusTextView.setTextSize(14f);
            statusTextView.setTypeface(null, Typeface.BOLD);

            // Устанавливаем цвет текста в зависимости от статуса
            switch (status.toLowerCase()) {
                case "completed":
                case "finished":
                    statusTextView.setTextColor(Color.BLUE);
                    break;
                case "cancelled":
                    statusTextView.setTextColor(Color.BLUE);
                    break;
                case "in_progress":
                    statusTextView.setTextColor(Color.BLUE);
                    break;
                default:
                    statusTextView.setTextColor(Color.BLUE);
                    break;
            }

            RelativeLayout.LayoutParams statusParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            statusParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            statusParams.addRule(RelativeLayout.CENTER_VERTICAL);
            statusParams.setMargins(16, 0, 16, 0);
            statusTextView.setLayoutParams(statusParams);

            card.addView(statusTextView);

            orderContainer.addView(card);
        } catch (Exception e) {
            Log.e("OrdersActivity", "Ошибка создания карточки заказа: " + e.getMessage());
        }
    }

    // Создание текстового поля для информации о заказе
    private TextView createOrderInfoTextView(String text, float textSize, int color) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(color);
        textView.setPadding(0, 4, 0, 4);
        return textView;
    }

    // Настройка круглых кнопок
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

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(100),
                dpToPx(25)
        );
        params.setMargins(0, 8, 8, 0);
        button.setLayoutParams(params);
        button.setPadding(0, 0, 0, 0);
    }

    // Удаление заказа
    private void deleteOrder(int orderId, View cardView) {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (GeneralSecurityException | IOException e) {
            Log.e("OrdersActivity", "Ошибка получения токена: " + e.getMessage());
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

    // Преобразование даты в формат dd.MM.yyyy
    private String formatDate(String rawDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy");
            return outputFormat.format(inputFormat.parse(rawDate));
        } catch (Exception e) {
            Log.e("OrdersActivity", "Ошибка форматирования даты: " + e.getMessage());
            return rawDate;
        }
    }

    // Преобразование dp в пиксели
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // Анимация кнопки при нажатии
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
}