package com.example.application.ui.view.admin;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
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
            String token = encryptedSharedPrefs.getAccessToken();
            if (token != null) {
                fetchUsers(token);
            } else {
                Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("UsersAdminActivity", "Ошибка инициализации", e);
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
            Log.e("UsersAdminActivity", "Ошибка фильтрации", e);
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
                Log.d("API_RESPONSE", "Ответ сервера: " + responseBody);

                if (response.isSuccessful()) {
                    try {
                        allUsers = new JSONArray(responseBody);
                        runOnUiThread(() -> {
                            userContainer.removeAllViews();
                            for (int i = 0; i < allUsers.length(); i++) {
                                try {
                                    addUserCard(allUsers.getJSONObject(i));
                                } catch (JSONException e) {
                                    Log.e("PARSE_ERROR", "Ошибка парсинга пользователя", e);
                                }
                            }
                        });
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Ошибка JSON", e);
                        runOnUiThread(() ->
                                Toast.makeText(UsersAdminActivity.this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(UsersAdminActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

//    private void addUserCard(JSONObject userObject) throws JSONException {
//        int userId = userObject.getInt("id");
//        String username = userObject.getString("username");
//        String email = userObject.getString("email");
//        String imageUrl = userObject.optString("link_img", null);
//
//        // Получаем список автомобилей
//        JSONArray carsArray = userObject.optJSONArray("cars");
//        String carsInfo = "Нет автомобилей";
//
//        if (carsArray != null && carsArray.length() > 0) {
//            StringBuilder carsBuilder = new StringBuilder();
//            for (int i = 0; i < carsArray.length(); i++) {
//                JSONObject car = carsArray.getJSONObject(i);
//                JSONObject model = car.getJSONObject("model");
//                JSONObject brand = model.getJSONObject("brand");
//
//                if (i > 0) carsBuilder.append(", ");
//                carsBuilder.append(brand.getString("brand_name"))
//                        .append(" ")
//                        .append(model.getString("model_name"));
//            }
//            carsInfo = carsBuilder.toString();
//        }
//
//        // Создаем карточку
//        RelativeLayout card = new RelativeLayout(this);
//        card.setPadding(16, 16, 16, 16);
//
//        // Стиль карточки
//        GradientDrawable cardShape = new GradientDrawable();
//        cardShape.setShape(GradientDrawable.RECTANGLE);
//        cardShape.setCornerRadius(24f);
//        cardShape.setColor(Color.parseColor("#FFFFFF"));
//        cardShape.setStroke(4, Color.parseColor("#E3F2FD"));
//        card.setBackground(cardShape);
//
//        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//        );
//        cardParams.setMargins(8, 8, 8, 12);
//        card.setLayoutParams(cardParams);
//
//        // Аватар (слева)
//        ImageView avatarView = new ImageView(this);
//        avatarView.setId(View.generateViewId());
//        RelativeLayout.LayoutParams avatarParams = new RelativeLayout.LayoutParams(
//                dpToPx(48),
//                dpToPx(48)
//        );
//        avatarParams.addRule(RelativeLayout.ALIGN_PARENT_START);
//        avatarParams.addRule(RelativeLayout.CENTER_VERTICAL);
//        avatarView.setLayoutParams(avatarParams);
//
//        loadUserAvatar(avatarView, imageUrl);
//        card.addView(avatarView);
//
//        // Контейнер информации (справа от аватара)
//        LinearLayout infoLayout = new LinearLayout(this);
//        infoLayout.setOrientation(LinearLayout.VERTICAL);
//        infoLayout.setId(View.generateViewId());
//        RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams(
//                RelativeLayout.LayoutParams.MATCH_PARENT,
//                RelativeLayout.LayoutParams.WRAP_CONTENT
//        );
//        infoParams.addRule(RelativeLayout.END_OF, avatarView.getId());
//        infoParams.addRule(RelativeLayout.CENTER_VERTICAL);
//        infoParams.setMargins(16, 0, 0, 0);
//        infoLayout.setLayoutParams(infoParams);
//
//        // Имя пользователя
//        TextView usernameView = new TextView(this);
//        usernameView.setText(username);
//        usernameView.setTextSize(16f);
//        usernameView.setTextColor(Color.parseColor("#2260FF"));
//        usernameView.setTypeface(Typeface.DEFAULT_BOLD);
//        infoLayout.addView(usernameView);
//
//        // Email
//        TextView emailView = new TextView(this);
//        emailView.setText(email);
//        emailView.setTextSize(14f);
//        emailView.setTextColor(Color.parseColor("#555555"));
//        infoLayout.addView(emailView);
//
//        // Машины
//        TextView carsView = new TextView(this);
//        carsView.setText("Автомобили: " + carsInfo);
//        carsView.setTextSize(12f);
//        carsView.setTextColor(Color.parseColor("#777777"));
//        infoLayout.addView(carsView);
//
//        // Кнопка удаления
//        Button deleteButton = new Button(this);
//        setupRoundButton(deleteButton, "Удалить");
//        deleteButton.setOnClickListener(v -> deleteUser(userId, card));
//        setupButtonAnimation(deleteButton);
//
//        // Контейнер для кнопки (выравнивание по правому краю)
//        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
//                RelativeLayout.LayoutParams.WRAP_CONTENT,
//                RelativeLayout.LayoutParams.WRAP_CONTENT
//        );
//        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_END);
//        buttonParams.addRule(RelativeLayout.CENTER_VERTICAL);
//        deleteButton.setLayoutParams(buttonParams);
//
//        card.addView(deleteButton);
//        card.addView(infoLayout);
//        userContainer.addView(card);
//    }


    private void addUserCard(JSONObject userObject) throws JSONException {
        int userId = userObject.getInt("id");
        String username = userObject.getString("username");
        String email = userObject.getString("email");
        String imageUrl = userObject.optString("link_img", null);

        // Получаем список автомобилей
        JSONArray carsArray = userObject.optJSONArray("cars");
        String carsInfo = "Нет автомобилей";

        if (carsArray != null && carsArray.length() > 0) {
            StringBuilder carsBuilder = new StringBuilder();
            for (int i = 0; i < carsArray.length(); i++) {
                JSONObject car = carsArray.getJSONObject(i);
                JSONObject model = car.getJSONObject("model");
                JSONObject brand = model.getJSONObject("brand");

                if (i > 0) carsBuilder.append(", ");
                carsBuilder.append(brand.getString("brand_name"))
                        .append(" ")
                        .append(model.getString("model_name"));
            }
            carsInfo = carsBuilder.toString();
        }

        // Создаем карточку
        RelativeLayout card = new RelativeLayout(this);
        card.setPadding(24, 24, 24, 24); // Увеличен паддинг

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
        cardParams.setMargins(8, 8, 8, 16); // Чуть больше нижний отступ
        card.setLayoutParams(cardParams);

        // Аватар
        ImageView avatarView = new ImageView(this);
        avatarView.setId(View.generateViewId());
        RelativeLayout.LayoutParams avatarParams = new RelativeLayout.LayoutParams(
                dpToPx(48),
                dpToPx(48)
        );
        avatarParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        avatarParams.addRule(RelativeLayout.CENTER_VERTICAL);
        avatarView.setLayoutParams(avatarParams);
        loadUserAvatar(avatarView, imageUrl);
        card.addView(avatarView);

        // Контейнер информации
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

        // Имя
        TextView usernameView = new TextView(this);
        usernameView.setText(username);
        usernameView.setTextSize(16f);
        usernameView.setTextColor(Color.parseColor("#2260FF"));
        usernameView.setTypeface(Typeface.DEFAULT_BOLD);
        infoLayout.addView(usernameView);

        // Email
        TextView emailView = new TextView(this);
        emailView.setText(email);
        emailView.setTextSize(14f);
        emailView.setTextColor(Color.parseColor("#555555"));
        emailView.setPadding(0, 4, 0, 4); // Добавим вертикальный отступ
        infoLayout.addView(emailView);

        // Автомобили
        TextView carsView = new TextView(this);
        carsView.setText("Автомобили: " + carsInfo);
        carsView.setTextSize(12f);
        carsView.setTextColor(Color.parseColor("#777777"));
        carsView.setMaxLines(2); // максимум 2 строки
        carsView.setEllipsize(TextUtils.TruncateAt.END); // троеточие в конце
        carsView.setPadding(0, 4, 0, 4);
        infoLayout.addView(carsView);

        // Кнопка удаления
        Button deleteButton = new Button(this);
        setupRoundButton(deleteButton, "Удалить");
        deleteButton.setOnClickListener(v -> deleteUser(userId, card));
        setupButtonAnimation(deleteButton);

        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(
                deleteButton.getLayoutParams().width,
                deleteButton.getLayoutParams().height
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
        buttonShape.setCornerRadius(32f);
        buttonShape.setColor(Color.parseColor("#2260FF"));

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackground(buttonShape);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(80),  // ширина
                dpToPx(26)   // высота
        );
        params.setMargins(0, 0, 8, 0);
        button.setLayoutParams(params);

        button.setTextSize(12f);
        button.setPadding(0, 0, 0, 0);
    }


    private void loadUserAvatar(ImageView imageView, String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageView.setImageResource(R.drawable.ic_placeholder);
                return;
            }

            if (!imageUrl.startsWith("https")) {
                String baseUrl = "https://automser.store/";
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
            Log.e("AvatarLoad", "Ошибка загрузки аватара", e);
            imageView.setImageResource(R.drawable.ic_placeholder);
        }
    }

    private void deleteUser(int userId, RelativeLayout card) {
        try {
            String token = encryptedSharedPrefs.getAccessToken();
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
            Log.e("UsersAdminActivity", "Ошибка удаления пользователя", e);
        }
    }

    private void setupNavigationButtons() {
        findViewById(R.id.homeButton).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeAdminActivity.class));
            finish();
        });

        findViewById(R.id.usersButton).setOnClickListener(v -> {
            startActivity(new Intent(this, UsersAdminActivity.class));
            finish();
        });

        findViewById(R.id.orderButton).setOnClickListener(v -> {
            startActivity(new Intent(this, OrdersAdminActivity.class));
            finish();
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