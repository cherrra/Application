package com.example.application.ui.view.admin;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
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

public class NotificationAdminActivity extends AppCompatActivity {
    private Handler handler = new Handler();
    private Runnable runnable;
    private static final long INTERVAL = 30000; // 30 секунд

    private LinearLayout notificationsContainer;
    private EncryptedSharedPrefs prefs;
    private static final String NOTIFICATIONS_KEY = "admin_notifications";
    private static final String LAST_ORDER_ID_KEY = "last_order_id";
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
                        processNewOrder(order);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void processNewOrder(JSONObject order) throws JSONException {
        String orderId = order.getString("id_order");
        String customerName = order.optString("customer_name", "Новый клиент");
        String orderDate = order.optString("created_at", "");
        String lastOrderId = prefs.getString(LAST_ORDER_ID_KEY, "");

        if (!orderId.equals(lastOrderId)) {
            String notificationText = "Новый заказ #" + orderId + " от " + customerName;

            runOnUiThread(() -> {
                addNotificationToScreen(notificationText);
                sendSystemNotification(notificationText);
            });

            prefs.putString(LAST_ORDER_ID_KEY, orderId);
            saveNotificationText(notificationText);
        }
    }

    private void addNotificationToScreen(String text) {
        if (isNotificationAlreadyDisplayed(text)) return;

        LinearLayout notificationLayout = new LinearLayout(this);
        notificationLayout.setOrientation(LinearLayout.HORIZONTAL);
        notificationLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        notificationLayout.setPadding(16, 16, 16, 16);
        notificationLayout.setBackgroundResource(R.drawable.notification_background);

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(Color.BLACK);
        textView.setPadding(16, 8, 16, 8);

        notificationLayout.addView(textView);
        notificationsContainer.addView(notificationLayout, 0);

        if (notificationsContainer.getChildCount() > 1) {
            addDivider();
        }
    }

    private void addDivider() {
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1));
        divider.setBackgroundColor(Color.parseColor("#EEEEEE"));
        notificationsContainer.addView(divider, 1);
    }

    private boolean isNotificationAlreadyDisplayed(String text) {
        for (int i = 0; i < notificationsContainer.getChildCount(); i++) {
            View view = notificationsContainer.getChildAt(i);
            if (view instanceof LinearLayout) {
                LinearLayout layout = (LinearLayout) view;
                if (layout.getChildCount() > 0) {
                    View child = layout.getChildAt(0);
                    if (child instanceof TextView) {
                        TextView tv = (TextView) child;
                        if (tv.getText().toString().equals(text)) {
                            return true;
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
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Новый заказ")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(this)
                .notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "New Orders",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications about new orders");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void loadSavedNotifications() {
        Set<String> savedNotifications = prefs.getStringSet(NOTIFICATIONS_KEY, new HashSet<>());
        for (String notification : savedNotifications) {
            addNotificationToScreen(notification);
        }
    }

    private void saveNotificationText(String text) {
        Set<String> notifications = new HashSet<>(prefs.getStringSet(NOTIFICATIONS_KEY, new HashSet<>()));
        notifications.add(text);
        prefs.putStringSet(NOTIFICATIONS_KEY, notifications);
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