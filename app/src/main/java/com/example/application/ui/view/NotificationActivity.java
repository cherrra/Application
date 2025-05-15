package com.example.application.ui.view;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

import com.example.application.R;
import com.example.application.utils.EncryptedSharedPrefs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationActivity extends AppCompatActivity {
    private Handler handler = new Handler();
    private Runnable runnable;
    private static final long INTERVAL = 30000; // 30 секунд

    private LinearLayout notificationsContainer;
    private EncryptedSharedPrefs prefs;
    private static final String NOTIFICATIONS_KEY = "notifications";
    private static final String SENT_NOTIFICATIONS_KEY = "sent_notifications";
    private static final String CHANNEL_ID = "order_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        ImageView backArrow = findViewById(R.id.backArrow);

        backArrow.setOnClickListener(v -> {
            finish();
        });

        backArrow.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    v.performClick();
                    break;
            }
            return true;
        });

        try {
            prefs = new EncryptedSharedPrefs(this);
            String token = prefs.getAccessToken();
            if (token != null) {
                notificationsContainer = findViewById(R.id.notificationsContainer);

                // Запрашиваем разрешения для Android 13+
                requestNotificationPermission();
                createNotificationChannel();
                loadSavedNotifications();
                startOrderStatusChecker();
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

    private void startOrderStatusChecker() {
        runnable = new Runnable() {
            @Override
            public void run() {
                checkOrderStatus();
                handler.postDelayed(this, INTERVAL);
            }
        };
        handler.post(runnable);
    }

    private void checkOrderStatus() {
        String token = prefs.getAccessToken();
        if (token == null) return;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://automser.store/api/orders")
                .addHeader("Authorization", "Bearer " + token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showToast("Ошибка соединения"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;

                try {
                    JSONArray ordersArray = new JSONArray(response.body().string());
                    for (int i = 0; i < ordersArray.length(); i++) {
                        JSONObject order = ordersArray.getJSONObject(i);
                        processOrderStatus(order);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void processOrderStatus(JSONObject order) throws JSONException {
        String orderId = order.getString("id_order");
        String status = order.getString("status");
        String notificationText = "Статус заказа #" + orderId + "изменен на: " + status;

        // Получаем последний известный статус этого заказа
        String lastStatusKey = "last_status_" + orderId;
        String lastKnownStatus = prefs.getString(lastStatusKey, "");

        // Если статус изменился
        if (!status.equals(lastKnownStatus)) {
            runOnUiThread(() -> {
                addNotificationToScreen(notificationText);
                sendSystemNotification(notificationText);
            });

            // Сохраняем новый статус
            prefs.putString(lastStatusKey, status);
            saveNotificationText(notificationText);
        }
    }

//    private void addNotificationToScreen(String text) {
//        if (isNotificationAlreadyDisplayed(text)) return;
//
//        // Создаем контейнер для уведомления
//        LinearLayout notificationLayout = new LinearLayout(this);
//        notificationLayout.setOrientation(LinearLayout.HORIZONTAL);
//        notificationLayout.setLayoutParams(new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT));
//        notificationLayout.setPadding(8, 8, 8, 8);
//
//        // Создаем TextView для текста уведомления
//        TextView textView = new TextView(this);
//        textView.setText(text);
//        textView.setTextSize(18);
//        textView.setPadding(16, 16, 16, 16);
//        textView.setBackgroundResource(R.drawable.rounded_background);
//
//        // Добавляем TextView в контейнер
//        notificationLayout.addView(textView);
//
//        // Добавляем контейнер в основной layout
//        notificationsContainer.addView(notificationLayout, 0);
//
//        // Добавляем разделитель (если нужно)
//        if (notificationsContainer.getChildCount() > 1) {
//            addDivider();
//        }
//    }
private void addNotificationToScreen(String text) {
    if (isNotificationAlreadyDisplayed(text)) return;

    // Создаем контейнер для уведомления
    LinearLayout notificationLayout = new LinearLayout(this);
    notificationLayout.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
    layoutParams.setMargins(0, 0, 0, 16); // Отступ снизу
    notificationLayout.setLayoutParams(layoutParams);

    // Создаем карточку уведомления
    LinearLayout cardLayout = new LinearLayout(this);
    cardLayout.setOrientation(LinearLayout.HORIZONTAL);
    cardLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));
    cardLayout.setBackgroundResource(R.drawable.card_background);
    cardLayout.setElevation(2f);

    // Иконка уведомления
    ImageView icon = new ImageView(this);
    icon.setImageResource(R.drawable.ic_notification);
    LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            dpToPx(24), dpToPx(24));
    iconParams.gravity = Gravity.CENTER_VERTICAL;
    iconParams.setMargins(dpToPx(16), dpToPx(16), 0, dpToPx(16));
    icon.setLayoutParams(iconParams);

    // Текст уведомления
    TextView textView = new TextView(this);
    textView.setText(text);
    textView.setTextSize(16);
    textView.setTextColor(Color.parseColor("#4c5866"));
    textView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    textView.setLineSpacing(dpToPx(4), 1f);
    LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    textParams.setMargins(dpToPx(12), dpToPx(16), dpToPx(16), dpToPx(16));
    textView.setLayoutParams(textParams);

    // Добавляем элементы в карточку
    cardLayout.addView(icon);
    cardLayout.addView(textView);

    // Добавляем карточку в контейнер
    notificationLayout.addView(cardLayout);

    // Добавляем контейнер в основной layout
    notificationsContainer.addView(notificationLayout, 0);

    // Добавляем анимацию
    Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
    notificationLayout.startAnimation(fadeIn);
}

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void addDivider() {
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1));
        divider.setBackgroundColor(Color.LTGRAY);
        notificationsContainer.addView(divider, 1);
    }

    private boolean isNotificationAlreadyDisplayed(String text) {
        for (int i = 0; i < notificationsContainer.getChildCount(); i++) {
            View view = notificationsContainer.getChildAt(i);

            // Пропускаем разделители (если они есть)
            if (view instanceof LinearLayout) {
                LinearLayout notificationLayout = (LinearLayout) view;
                if (notificationLayout.getChildCount() > 0) {
                    View child = notificationLayout.getChildAt(0);
                    if (child instanceof TextView) {
                        TextView textView = (TextView) child;
                        if (textView.getText().toString().equals(text)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void sendSystemNotification(String text) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Обновление статуса заказа")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Order Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Channel for order status updates");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void loadSavedNotifications() {
        Set<String> savedNotifications = prefs.getStringSet(NOTIFICATIONS_KEY, new HashSet<>());
        for (String notification : savedNotifications) {
            // Создаем временный TextView для проверки дубликатов
            TextView tempView = new TextView(this);
            tempView.setText(notification);

            if (!isNotificationAlreadyDisplayed(notification)) {
                addNotificationToScreen(notification);
            }
        }
    }

    private void saveNotificationText(String notificationText) {
        Set<String> currentNotifications = new HashSet<>(prefs.getStringSet(NOTIFICATIONS_KEY, new HashSet<>()));
        currentNotifications.add(notificationText);
        prefs.putStringSet(NOTIFICATIONS_KEY, currentNotifications);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }
}