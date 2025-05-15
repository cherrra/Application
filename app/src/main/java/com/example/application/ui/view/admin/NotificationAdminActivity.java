package com.example.application.ui.view.admin;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.application.R;
import com.example.application.utils.EncryptedSharedPrefs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationAdminActivity extends AppCompatActivity {
    private Handler handler = new Handler();
    private Runnable runnable;
    private static final long INTERVAL = 30000; // 30 секунд

    private LinearLayout notificationsContainer;
    private EncryptedSharedPrefs prefs;
    private static final String NOTIFICATIONS_KEY = "admin_notifications";
    private static final String LAST_NOTIFIED_TIMESTAMP_KEY = "last_notified_timestamp";
    private static final String CHANNEL_ID = "admin_order_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_admin);

        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(v -> finish());
        backArrow.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                v.performClick();
            }
            return true;
        });

        try {
            prefs = new EncryptedSharedPrefs(this);
            String token = prefs.getAccessToken();
            if (token != null) {
                notificationsContainer = findViewById(R.id.notificationsContainer);

                requestNotificationPermission();
                createNotificationChannel();
                loadSavedNotifications();
                startOrderChecker();
            } else {
                showToast("Необходима авторизация");
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Ошибка инициализации");
            finish();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void startOrderChecker() {
        runnable = new Runnable() {
            @Override
            public void run() {
                checkNewOrders();
                handler.postDelayed(this, INTERVAL);
            }
        };
        handler.post(runnable);
    }

    private void checkNewOrders() {
        String token = prefs.getAccessToken();
        if (token == null) return;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://automser.store/api/orders/admin")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showToast("Ошибка соединения: " + e.getMessage()));
                Log.e("NotificationAdmin", "Connection error", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> showToast("Ошибка сервера: " + response.code()));
                    Log.e("NotificationAdmin", "Server error: " + response.code());
                    return;
                }

                try {
                    String responseBody = response.body().string();
                    Log.d("NotificationAdmin", "Response: " + responseBody);
                    JSONArray ordersArray = new JSONArray(responseBody);

                    if (ordersArray.length() > 0) {
                        // Обрабатываем от новых к старым
                        for (int i = ordersArray.length() - 1; i >= 0; i--) {
                            JSONObject order = ordersArray.getJSONObject(i);
                            processNewOrder(order);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> showToast("Ошибка обработки данных"));
                    Log.e("NotificationAdmin", "JSON parsing error", e);
                }
            }
        });
    }

    private void processNewOrder(JSONObject order) throws JSONException {
        try {
            String orderId = order.getString("id_order");
            String customerName = order.optString("user_name", "Новый клиент");
            String orderDate = order.optString("order_date", "");
            String orderTime = order.optString("order_time", "");
            String totalPrice = order.optString("total_price", "0.00");
            String status = order.optString("status", "");
            String services = order.optString("services", "");

            // Формируем полную дату и время
            String fullDateTime = "";
            if (!orderDate.isEmpty() && !orderTime.isEmpty()) {
                // Удаляем 'T' и миллисекунды из ISO формата
                String normalizedDate = orderDate.replace("T", " ").split("\\.")[0];
                fullDateTime = normalizedDate + " " + orderTime;
            }

            long orderTimestamp = parseDateToTimestamp(fullDateTime);
            String lastNotifiedTimestampStr = prefs.getString(LAST_NOTIFIED_TIMESTAMP_KEY, "0");
            long lastNotifiedTimestamp = Long.parseLong(lastNotifiedTimestampStr);

            if (orderTimestamp > lastNotifiedTimestamp) {
                String notificationText = "Новый заказ #" + orderId +
                        "\nКлиент: " + customerName +
                        "\nАвто: " + getCarInfo(order) +
                        "\nСумма: " + totalPrice + " руб." +
                        "\nУслуги: " + (services.isEmpty() ? "не указаны" : services) +
                        "\nСтатус: " + getStatusText(status) +
                        "\nДата: " + formatDateTime(fullDateTime);

                runOnUiThread(() -> {
                    addNotificationToScreen(notificationText);
                    sendSystemNotification(notificationText);
                });

                prefs.putString(LAST_NOTIFIED_TIMESTAMP_KEY, String.valueOf(orderTimestamp));
                saveNotificationText(notificationText);

                Log.d("NotificationAdmin", "New order detected: " + orderId);
            }
        } catch (Exception e) {
            Log.e("NotificationAdmin", "Error processing order", e);
        }
    }

    private String getCarInfo(JSONObject order) throws JSONException {
        if (order.has("car")) {
            JSONObject car = order.getJSONObject("car");
            JSONObject model = car.getJSONObject("model");
            JSONObject brand = model.getJSONObject("brand");
            return brand.getString("brand_name") + " " +
                    model.getString("model_name") + " (" +
                    car.getString("license_plate") + ")";
        }
        return "не указано";
    }

    private String getStatusText(String status) {
        switch (status) {
            case "in_progress": return "В работе";
            case "accepted": return "Принят";
            case "completed": return "Завершен";
            case "finished": return "Выполнен";
            default: return status;
        }
    }

    private long parseDateToTimestamp(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return 0;
        }

        try {
            // Формат: "yyyy-MM-dd HH:mm:ss HH:mm:ss"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(dateTimeStr);
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            Log.e("NotificationAdmin", "Date parsing error for: " + dateTimeStr, e);
            return 0;
        }
    }

    private String formatDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return "дата не указана";
        }

        try {
            SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss HH:mm:ss", Locale.getDefault());
            SimpleDateFormat sdfOutput = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            Date date = sdfInput.parse(dateTimeStr);
            return date != null ? sdfOutput.format(date) : dateTimeStr;
        } catch (ParseException e) {
            Log.e("NotificationAdmin", "Date formatting error", e);
            return dateTimeStr;
        }
    }
    private void addNotificationToScreen(String text) {
        if (isNotificationAlreadyDisplayed(text)) return;

        LinearLayout notificationLayout = new LinearLayout(this);
        notificationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, dpToPx(16));
        notificationLayout.setLayoutParams(layoutParams);

        // Карточка уведомления
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.HORIZONTAL);
        cardLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        cardLayout.setBackgroundResource(R.drawable.card_background);
        cardLayout.setElevation(dpToPx(2));
        cardLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Иконка
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_notification);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dpToPx(24), dpToPx(24));
        iconParams.gravity = Gravity.CENTER_VERTICAL;
        iconParams.setMargins(0, 0, dpToPx(12), 0);
        icon.setLayoutParams(iconParams);

        // Текст уведомления
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(ContextCompat.getColor(this, android.R.color.tertiary_text_dark));
        textView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textView.setLineSpacing(dpToPx(4), 1f);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textView.setLayoutParams(textParams);

        cardLayout.addView(icon);
        cardLayout.addView(textView);
        notificationLayout.addView(cardLayout);
        notificationsContainer.addView(notificationLayout, 0);

        // Анимация
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        notificationLayout.startAnimation(fadeIn);

        // Добавляем разделитель если нужно
        if (notificationsContainer.getChildCount() > 1) {
            addDivider();
        }
    }

    private void addDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1));
        params.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        divider.setLayoutParams(params);
        notificationsContainer.addView(divider, 1);
    }

    private boolean isNotificationAlreadyDisplayed(String text) {
        for (int i = 0; i < notificationsContainer.getChildCount(); i++) {
            View view = notificationsContainer.getChildAt(i);
            if (view instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) view;
                if (layout.getChildCount() > 0) {
                    View child = layout.getChildAt(0);
                    if (child instanceof LinearLayout) {
                        LinearLayout card = (LinearLayout) child;
                        if (card.getChildCount() > 1) {
                            View textChild = card.getChildAt(1);
                            if (textChild instanceof TextView) {
                                TextView tv = (TextView) textChild;
                                if (tv.getText().toString().equals(text)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void sendSystemNotification(String text) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w("NotificationAdmin", "Notification permission not granted");
            return;
        }

        // Intent для открытия активности при нажатии на уведомление
        Intent intent = new Intent(this, NotificationAdminActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Новый заказ в системе")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 300, 200, 300});

        NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), builder.build());

        Log.d("NotificationAdmin", "System notification sent: " + text);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Новые заказы",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Уведомления о новых заказах");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 200, 300});

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d("NotificationAdmin", "Notification channel created");
            }
        }
    }


    private void loadSavedNotifications() {
        Set<String> savedNotifications = prefs.getStringSet(NOTIFICATIONS_KEY, new HashSet<>());
        if (savedNotifications != null) {
            for (String notification : savedNotifications) {
                addNotificationToScreen(notification);
            }
            Log.d("NotificationAdmin", "Loaded " + savedNotifications.size() + " saved notifications");
        }
    }

    private void saveNotificationText(String text) {
        Set<String> notifications = prefs.getStringSet(NOTIFICATIONS_KEY, new HashSet<>());
        Set<String> newNotifications = new HashSet<>();
        if (notifications != null) {
            newNotifications.addAll(notifications);
        }
        newNotifications.add(text);
        prefs.putStringSet(NOTIFICATIONS_KEY, newNotifications);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        Log.d("NotificationAdmin", "Activity destroyed");
    }
}