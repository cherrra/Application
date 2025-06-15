package com.example.application.ui.view;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
    private static final long INTERVAL = 30000;

    private LinearLayout notificationsContainer;
    private LinearLayout emptyStateContainer;
    private EncryptedSharedPrefs prefs;
    private static final String NOTIFICATIONS_KEY = "notifications";
    private static final String SENT_NOTIFICATIONS_KEY = "sent_notifications";
    private static final String CHANNEL_ID = "order_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        ImageView backArrow = findViewById(R.id.backArrow);
        notificationsContainer = findViewById(R.id.notificationsContainer);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);

        backArrow.setOnClickListener(v -> finish());

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
                requestNotificationPermission();
                createNotificationChannel();
                loadSavedNotifications();
                startOrderStatusChecker();
                updateEmptyState();
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

    private void updateEmptyState() {
        if (notificationsContainer.getChildCount() == 0) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            notificationsContainer.setVisibility(View.GONE);
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            notificationsContainer.setVisibility(View.VISIBLE);
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
        String notificationText = "Статус заказа #" + orderId + " изменен на: " + status;

        String lastStatusKey = "last_status_" + orderId;
        String lastKnownStatus = prefs.getString(lastStatusKey, "");

        if (!status.equals(lastKnownStatus)) {
            runOnUiThread(() -> {
                addNotificationToScreen(notificationText);
                sendSystemNotification(notificationText);
                updateEmptyState();
            });

            prefs.putString(lastStatusKey, status);
            saveNotificationText(notificationText);
        }
    }

    private void addNotificationToScreen(String text) {
        if (isNotificationAlreadyDisplayed(text)) return;

        LinearLayout notificationCard = new LinearLayout(this);
        notificationCard.setOrientation(LinearLayout.HORIZONTAL);
        notificationCard.setGravity(Gravity.CENTER_VERTICAL);
        notificationCard.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        GradientDrawable cardShape = new GradientDrawable();
        cardShape.setShape(GradientDrawable.RECTANGLE);
        cardShape.setCornerRadius(dpToPx(24));
        cardShape.setColor(Color.parseColor("#FFFFFF"));
        cardShape.setStroke(dpToPx(2), Color.parseColor("#E3F2FD"));
        notificationCard.setBackground(cardShape);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
        notificationCard.setLayoutParams(cardParams);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_notification);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dpToPx(32), dpToPx(32));
        iconParams.gravity = Gravity.CENTER_VERTICAL;
        iconParams.setMargins(0, 0, dpToPx(12), 0);
        icon.setLayoutParams(iconParams);

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textContainerParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        textContainer.setLayoutParams(textContainerParams);

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(Color.parseColor("#2260FF"));
        try {
            Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/nunitosans_regular.ttf");
            textView.setTypeface(typeface);
        } catch (Exception e) {
            textView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            Log.e("NotificationActivity", "Error loading NunitoSans font", e);
        }
        textView.setLineSpacing(dpToPx(4), 1f);

        textContainer.addView(textView);
        notificationCard.addView(icon);
        notificationCard.addView(textContainer);

        notificationsContainer.addView(notificationCard, 0);

        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_top);
        notificationCard.startAnimation(slideIn);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private boolean isNotificationAlreadyDisplayed(String text) {
        for (int i = 0; i < notificationsContainer.getChildCount(); i++) {
            View view = notificationsContainer.getChildAt(i);
            if (view instanceof LinearLayout) {
                LinearLayout card = (LinearLayout) view;
                if (card.getChildCount() > 1) {
                    View textContainer = card.getChildAt(1);
                    if (textContainer instanceof LinearLayout) {
                        LinearLayout container = (LinearLayout) textContainer;
                        if (container.getChildCount() > 0) {
                            View textView = container.getChildAt(0);
                            if (textView instanceof TextView) {
                                TextView tv = (TextView) textView;
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
            addNotificationToScreen(notification);
        }
        updateEmptyState();
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