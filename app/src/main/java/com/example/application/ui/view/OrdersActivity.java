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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
    private LinearLayout emptyStateContainer;
    private JSONArray ordersArray;
    private boolean isHistoryDisplayed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        orderContainer = findViewById(R.id.orderContainer);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
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

        ImageView infoIcon = findViewById(R.id.infoIcon);
        infoIcon.setOnClickListener(v -> showStatusInfoDialog());

        infoIcon.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    private void showStatusInfoDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_status_info, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button okButton = dialogView.findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void fetchOrders() {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getAccessToken();
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
                .url("https://automser.store/api/orders")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("OrdersActivity", "Ошибка сети: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(OrdersActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d("OrdersResponse", "Ответ сервера: " + responseBody);
                        ordersArray = new JSONArray(responseBody);
                        runOnUiThread(() -> {
                            if (ordersArray.length() > 0) {
                                emptyStateContainer.setVisibility(View.GONE);
                                displayAllOrders();
                            } else {
                                showEmptyState();
                            }
                        });
                    } catch (Exception e) {
                        Log.e("OrdersActivity", "Ошибка обработки ответа: " + e.getMessage());
//                        runOnUiThread(this::showEmptyState);
                    }
                } else {
                    Log.e("OrdersActivity", "Ошибка сервера: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(OrdersActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                        showEmptyState();
                    });
                }
            }
        });
    }

    private void showEmptyState() {
        emptyStateContainer.setVisibility(View.VISIBLE);
        orderContainer.removeAllViews();
    }

    private void displayAllOrders() {
        orderContainer.removeAllViews();
        if (ordersArray != null && ordersArray.length() > 0) {
            emptyStateContainer.setVisibility(View.GONE);
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
        } else {
            showEmptyState();
        }
    }

    private void displayFinishedOrders() {
        orderContainer.removeAllViews();
        if (ordersArray != null && ordersArray.length() > 0) {
            emptyStateContainer.setVisibility(View.GONE);
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
        } else {
            showEmptyState();
        }
    }

    // Остальные методы остаются без изменений
    private void addOrderCard(JSONObject orderObject) {
        try {
            String formattedDate = formatDate(orderObject.getString("order_date"));
            int orderId = orderObject.getInt("id_order");
            String status = orderObject.getString("status");

            // Получаем данные об автомобиле из вложенного объекта car или напрямую
            String carInfo = "Не указано";
            if (orderObject.has("car")) {
                JSONObject carObject = orderObject.getJSONObject("car");
                if (carObject.has("model")) {
                    JSONObject modelObject = carObject.getJSONObject("model");
                    String brandName = modelObject.getJSONObject("brand").getString("brand_name");
                    String modelName = modelObject.getString("model_name");
                    carInfo = brandName + " " + modelName;
                }
            } else if (orderObject.has("car_model") && orderObject.has("car_brand")) {
                // Для обратной совместимости, если данные приходят как плоские поля
                carInfo = orderObject.getString("car_brand") + " " + orderObject.getString("car_model");
            }

            RelativeLayout card = new RelativeLayout(this);
            card.setPadding(24, 24, 24, 24);

            GradientDrawable cardShape = new GradientDrawable();
            cardShape.setShape(GradientDrawable.RECTANGLE);
            cardShape.setCornerRadius(55f);
            cardShape.setColor(Color.parseColor("#80CAD6FF"));
            cardShape.setStroke(2, Color.parseColor("#CAD6FF"));
            card.setBackground(cardShape);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(8, 8, 8, 16);
            card.setLayoutParams(cardParams);

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

            infoLayout.addView(createOrderInfoTextView("Заказ #" + orderId, 18f, Color.BLACK, true));
            infoLayout.addView(createOrderInfoTextView("Автомобиль: " + carInfo, 16f, Color.DKGRAY, false));
            infoLayout.addView(createOrderInfoTextView("Дата: " + formattedDate, 14f, Color.DKGRAY, false));
            infoLayout.addView(createOrderInfoTextView("Время: " + orderObject.getString("order_time"), 14f, Color.DKGRAY, false));
            infoLayout.addView(createOrderInfoTextView("Услуги: " + orderObject.getString("services"), 14f, Color.DKGRAY, false));
            infoLayout.addView(createOrderInfoTextView("Стоимость: " + orderObject.getDouble("total_price") + " руб.", 14f, Color.DKGRAY, false));

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

            TextView statusTextView = new TextView(this);
            statusTextView.setText(translateStatus(status));
            statusTextView.setTextSize(14f);
            statusTextView.setTypeface(null, Typeface.BOLD);
            statusTextView.setTextColor(getStatusColor(status));

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

    // Остальные вспомогательные методы остаются без изменений
    private String translateStatus(String status) {
        switch (status.toLowerCase()) {
            case "created":
                return "Создан";
            case "accepted":
                return "Принят";
            case "in_progress":
                return "В процессе";
            case "completed":
                return "Выполнен";
            case "finished":
                return "Завершен";
            case "canceled":
                return "Отменён";
            default:
                return status;
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

    private int getStatusColor(String status) {
        switch (status.toLowerCase()) {
            case "created": // Создан
                return Color.parseColor("#2196F3"); // Синий
            case "accepted": // Принят
                return Color.parseColor("#4CAF50"); // Зеленый
            case "in_progress": // В процессе
                return Color.parseColor("#FF9800"); // Оранжевый
            case "completed": // Завершён
            case "finished": // Выполнен
                return Color.parseColor("#673AB7"); // Фиолетовый
            case "canceled": // Отменён
                return Color.parseColor("#F44336"); // Красный
            default:
                return Color.parseColor("#9E9E9E"); // Серый по умолчанию
        }
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

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(100),
                dpToPx(25)
        );
        params.setMargins(0, 8, 8, 0);
        button.setLayoutParams(params);
        button.setPadding(0, 0, 0, 0);
    }

    private void deleteOrder(int orderId, View cardView) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_cancel_order, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        confirmButton.setOnClickListener(v -> {
            String token;
            try {
                token = new EncryptedSharedPrefs(OrdersActivity.this).getAccessToken();
            } catch (GeneralSecurityException | IOException e) {
                Log.e("OrdersActivity", "Ошибка получения токена: " + e.getMessage());
                dialog.dismiss();
                return;
            }

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://automser.store/api/orders/" + orderId)
                    .delete()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(OrdersActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            orderContainer.removeView(cardView);
                            if (orderContainer.getChildCount() == 0) {
                                showEmptyState();
                            }
                            Toast.makeText(OrdersActivity.this, "Заказ отменён", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(OrdersActivity.this, "Ошибка отмены заказа", Toast.LENGTH_SHORT).show();
                        }
                        dialog.dismiss();
                    });
                }
            });
        });

        dialog.show();
    }

    private String formatDate(String rawDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy");
            return outputFormat.format(inputFormat.parse(rawDate));
        } catch (Exception e) {
            return rawDate;
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
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
}