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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.application.R;
import com.example.application.ui.viewmodel.UserViewModel;
import com.example.application.utils.EncryptedSharedPrefs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class UsersAdminActivity extends AppCompatActivity {

    private LinearLayout userContainer;
    private EncryptedSharedPrefs encryptedSharedPrefs;
    private final UserViewModel usersViewModel = new UserViewModel();
    private EditText searchEditText;
    private JSONArray allUsers = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        userContainer = findViewById(R.id.userContainer);
        searchEditText = findViewById(R.id.searchEditText);

        setupSearchView();
        setupNavigationButtons();

        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();
            if (token != null) {
                fetchUsers(token);
            } else {
                Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                        runOnUiThread(() -> filterUsers(s.toString()));
                    }
                }, 300);
            }
        });
    }

    private void filterUsers(String query) {
        try {
            userContainer.removeAllViews();

            if (query.isEmpty()) {
                for (int i = 0; i < allUsers.length(); i++) {
                    addUserCard(allUsers.getJSONObject(i));
                }
                return;
            }

            for (int i = 0; i < allUsers.length(); i++) {
                JSONObject user = allUsers.getJSONObject(i);
                String username = user.getString("username").toLowerCase();
                if (username.contains(query.toLowerCase())) {
                    addUserCard(user);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void fetchUsers(String token) {
        usersViewModel.fetchUsers(token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(UsersAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d("API_RESPONSE", "Полный ответ сервера: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        allUsers = new JSONArray(responseBody);
                        runOnUiThread(() -> {
                            userContainer.removeAllViews();
                            for (int i = 0; i < allUsers.length(); i++) {
                                try {
                                    addUserCard(allUsers.getJSONObject(i));
                                } catch (JSONException e) {
                                    Log.e("PARSE_ERROR", "Ошибка парсинга: " + e.getMessage());
                                }
                            }
                        });
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Ошибка JSON: " + e.getMessage());
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(UsersAdminActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
    private void addUserCard(JSONObject userObject) throws JSONException {
        int userId = userObject.getInt("id");
        String username = userObject.getString("username");
        String email = userObject.getString("email");
        String cars = userObject.optString("cars", "нет");
        String imageUrl = userObject.optString("link_img", userObject.optString("linkImg", null));

        // Создаем карточку
        RelativeLayout card = new RelativeLayout(this);
        card.setPadding(16, 16, 16, 16);

        // Стиль карточки
        GradientDrawable cardShape = new GradientDrawable();
        cardShape.setShape(GradientDrawable.RECTANGLE);
        cardShape.setCornerRadius(24f);
        cardShape.setColor(Color.parseColor("#FFFFFF"));
        cardShape.setStroke(4, Color.parseColor("#E3F2FD"));
        card.setBackground(cardShape);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(8, 8, 8, 12);
        card.setLayoutParams(cardParams);

        // Аватар (слева)
        ImageView avatarView = new ImageView(this);
        avatarView.setId(View.generateViewId());
        RelativeLayout.LayoutParams avatarParams = new RelativeLayout.LayoutParams(
                dpToPx(48),
                dpToPx(48)
        );
        avatarParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        avatarParams.addRule(RelativeLayout.CENTER_VERTICAL);
        avatarView.setLayoutParams(avatarParams);

        // Загрузка аватара
        if (imageUrl != null && !imageUrl.isEmpty()) {
            loadUserAvatar(avatarView, imageUrl);
        } else {
            avatarView.setImageResource(R.drawable.ic_placeholder);
        }
        card.addView(avatarView);

        // Контейнер информации (справа от аватара)
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setId(View.generateViewId());
        RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.addRule(RelativeLayout.END_OF, avatarView.getId());
        infoParams.addRule(RelativeLayout.CENTER_VERTICAL);
        infoParams.setMargins(16, 0, 0, 0);
        infoLayout.setLayoutParams(infoParams);

        // Имя пользователя
        TextView usernameView = new TextView(this);
        usernameView.setText(username);
        usernameView.setTextSize(16f);
        usernameView.setTextColor(Color.BLACK);
        usernameView.setTypeface(Typeface.DEFAULT_BOLD);
        infoLayout.addView(usernameView);

        // Email
        TextView emailView = new TextView(this);
        emailView.setText(email);
        emailView.setTextSize(14f);
        emailView.setTextColor(Color.parseColor("#555555"));
        infoLayout.addView(emailView);

        // Машины
        TextView carsView = new TextView(this);
        carsView.setText("Машины: " + cars);
        carsView.setTextSize(12f);
        carsView.setTextColor(Color.parseColor("#777777"));
        infoLayout.addView(carsView);

        // Кнопка удаления
        Button deleteButton = new Button(this);
        setupRoundButton(deleteButton, "Удалить");
        deleteButton.setOnClickListener(v -> deleteUser(userId, card));
        setupButtonAnimation(deleteButton);

        // Контейнер для кнопки (выравнивание по правому краю)
        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        buttonParams.addRule(RelativeLayout.CENTER_VERTICAL);
        deleteButton.setLayoutParams(buttonParams);

        card.addView(deleteButton);
        card.addView(infoLayout);
        userContainer.addView(card);
    }

    private void setupRoundButton(Button button, String text) {
        GradientDrawable buttonShape = new GradientDrawable();
        buttonShape.setShape(GradientDrawable.RECTANGLE);
        buttonShape.setCornerRadius(40f);
        buttonShape.setColor(Color.parseColor("#2260FF"));

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackground(buttonShape);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(100),
                dpToPx(30)
        );
        params.setMargins(0, 0, 8, 0);
        button.setLayoutParams(params);
        button.setPadding(0, 0, 0, 0);
    }

    private void loadUserAvatar(ImageView imageView, String imageUrl) {
        try {
            if (!imageUrl.startsWith("http")) {
                String baseUrl = "http://10.0.2.2:5000/";
                String cleanPath = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
                imageUrl = baseUrl + cleanPath;
            }

            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .circleCrop()
                    .into(imageView);
        } catch (Exception e) {
            Log.e("AvatarLoad", "Error loading avatar: " + e.getMessage());
            imageView.setImageResource(R.drawable.ic_placeholder);
        }
    }

    private void deleteUser(int userId, RelativeLayout card) {
        try {
            String token = encryptedSharedPrefs.getToken();
            if (token == null) {
                Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
                return;
            }

            usersViewModel.deleteUser(userId, token, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(UsersAdminActivity.this, "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            userContainer.removeView(card);
                            Toast.makeText(UsersAdminActivity.this, "Пользователь удален", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UsersAdminActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupNavigationButtons() {
        findViewById(R.id.homeButton).setOnClickListener(v -> {
            Intent intent = new Intent(UsersAdminActivity.this, HomeAdminActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.usersButton).setOnClickListener(v -> {
            Intent intent = new Intent(UsersAdminActivity.this, UsersAdminActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.orderButton).setOnClickListener(v -> {
            Intent intent = new Intent(UsersAdminActivity.this, OrdersAdminActivity.class);
            startActivity(intent);
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void setupButtonAnimation(View button) {
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