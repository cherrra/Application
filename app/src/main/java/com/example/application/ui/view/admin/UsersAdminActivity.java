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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
                        Toast.makeText(UsersAdminActivity.this, "Ошибка сети: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body() != null ? response.body().string() : "{}";
                    Log.d("API_RESPONSE", "Полный ответ: " + responseBody);

                    if (response.isSuccessful()) {
                        try {
                            JSONArray usersArray = new JSONArray(responseBody);
                            allUsers = usersArray;

                            runOnUiThread(() -> {
                                userContainer.removeAllViews();

                                addUsersCountView(usersArray.length());

                                if (usersArray.length() == 0) {
                                    TextView emptyView = new TextView(UsersAdminActivity.this);
                                    emptyView.setText("Нет пользователей");
                                    emptyView.setGravity(Gravity.CENTER);
                                    userContainer.addView(emptyView);
                                    return;
                                }

                                for (int i = 0; i < usersArray.length(); i++) {
                                    try {
                                        JSONObject userJson = usersArray.getJSONObject(i);
                                        Log.d("USER_JSON", "User data: " + userJson.toString());
                                        addUserCard(userJson);
                                    } catch (JSONException e) {
                                        Log.e("PARSE_ERROR", "Ошибка парсинга пользователя", e);
                                    }
                                }
                            });
                        } catch (JSONException e) {
                            Log.e("JSON_ERROR", "Ошибка формата данных", e);
                            runOnUiThread(() ->
                                    Toast.makeText(UsersAdminActivity.this, "Ошибка формата данных", Toast.LENGTH_LONG).show()
                            );
                        }
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(UsersAdminActivity.this,
                                        "Ошибка сервера: " + response.code(),
                                        Toast.LENGTH_LONG).show()
                        );
                    }
                } catch (Exception e) {
                    Log.e("FETCH_ERROR", "Неожиданная ошибка", e);
                    runOnUiThread(() ->
                            Toast.makeText(UsersAdminActivity.this, "Неожиданная ошибка", Toast.LENGTH_LONG).show()
                    );
                } finally {
                    if (response != null && response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    private void addUsersCountView(int usersCount) {
        LinearLayout countContainer = new LinearLayout(this);
        countContainer.setOrientation(LinearLayout.HORIZONTAL);
        countContainer.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));

        GradientDrawable countShape = new GradientDrawable();
        countShape.setShape(GradientDrawable.RECTANGLE);
        countShape.setCornerRadius(dpToPx(12));
        countShape.setColor(Color.parseColor("#E3F2FD"));
        countContainer.setBackground(countShape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(16));
        countContainer.setLayoutParams(params);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_users);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.dark_blue));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                dpToPx(24), dpToPx(24));
        iconParams.gravity = Gravity.CENTER_VERTICAL;
        iconParams.setMargins(0, 0, dpToPx(8), 0);
        icon.setLayoutParams(iconParams);
        countContainer.addView(icon);

        TextView countText = new TextView(this);
        String countString = usersCount + " " + getCorrectPlural(usersCount, "пользователь", "пользователя", "пользователей");
        countText.setText(countString);
        countText.setTextSize(16);
        countText.setTextColor(ContextCompat.getColor(this, R.color.dark_blue));
        countText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        countContainer.addView(countText);

        userContainer.addView(countContainer, 0);
    }

    private String getCorrectPlural(int number, String one, String few, String many) {
        int n = Math.abs(number) % 100;
        int n1 = n % 10;

        if (n > 10 && n < 20) return many;
        if (n1 > 1 && n1 < 5) return few;
        if (n1 == 1) return one;
        return many;
    }

    private void addUserCard(JSONObject userObject) throws JSONException {
        int userId = userObject.getInt("id");
        String username = userObject.getString("username");
        String email = userObject.getString("email");
        String imageUrl = userObject.optString("link_img", null);

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

        RelativeLayout card = new RelativeLayout(this);
        card.setPadding(24, 24, 24, 24);

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
        cardParams.setMargins(8, 8, 8, 16);
        card.setLayoutParams(cardParams);

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

        TextView usernameView = new TextView(this);
        usernameView.setText(username);
        usernameView.setTextSize(16f);
        usernameView.setTextColor(Color.parseColor("#2260FF"));
        usernameView.setTypeface(Typeface.DEFAULT_BOLD);

        RelativeLayout.LayoutParams usernameParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        usernameParams.addRule(RelativeLayout.END_OF, avatarView.getId());
        usernameParams.addRule(RelativeLayout.CENTER_VERTICAL);
        usernameParams.setMargins(16, 0, 0, 0);
        usernameView.setLayoutParams(usernameParams);
        card.addView(usernameView);

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

        LinearLayout dropdownMenu = new LinearLayout(this);
        dropdownMenu.setOrientation(LinearLayout.VERTICAL);
        dropdownMenu.setVisibility(View.GONE);
        dropdownMenu.setPadding(dpToPx(16), dpToPx(8), 0, dpToPx(8));

        TextView emailView = new TextView(this);
        emailView.setText("Email: " + email);
        emailView.setTextSize(14f);
        emailView.setTextColor(Color.parseColor("#555555"));
        dropdownMenu.addView(emailView);

        TextView carsView = new TextView(this);
        carsView.setText("Автомобили: " + carsInfo);
        carsView.setTextSize(12f);
        carsView.setTextColor(Color.parseColor("#777777"));
        carsView.setMaxLines(2);
        carsView.setEllipsize(TextUtils.TruncateAt.END);
        carsView.setPadding(0, dpToPx(4), 0, 0);
        dropdownMenu.addView(carsView);

        LinearLayout cardContainer = new LinearLayout(this);
        cardContainer.setOrientation(LinearLayout.VERTICAL);
        cardContainer.addView(card);
        cardContainer.addView(dropdownMenu);

        card.setOnClickListener(v -> {
            if (dropdownMenu.getVisibility() == View.VISIBLE) {
                dropdownMenu.setVisibility(View.GONE);
            } else {
                dropdownMenu.setVisibility(View.VISIBLE);
            }
        });

        userContainer.addView(cardContainer);
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
                dpToPx(80),
                dpToPx(26)
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
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_user, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        confirmButton.setOnClickListener(v -> {
            try {
                String token = encryptedSharedPrefs.getAccessToken();
                if (token == null) {
                    Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }

                usersViewModel.deleteUser(userId, token, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(UsersAdminActivity.this, "Ошибка при удалении", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                View parent = (View) card.getParent();
                                if (parent != null && parent.getParent() instanceof ViewGroup) {
                                    ((ViewGroup) parent.getParent()).removeView(parent);
                                }
                                Toast.makeText(UsersAdminActivity.this, "Пользователь удален", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(UsersAdminActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                            }
                            dialog.dismiss();
                        });
                    }
                });
            } catch (Exception e) {
                Log.e("UsersAdminActivity", "Ошибка удаления пользователя", e);
                dialog.dismiss();
            }
        });

        dialog.show();
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