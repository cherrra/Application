package com.example.application;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UsersActivity extends AppCompatActivity {

    private LinearLayout userContainer;
    private EncryptedSharedPrefs encryptedSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        userContainer = findViewById(R.id.userContainer);

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

        TextView homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(UsersActivity.this, HomeAdminActivity.class);
            startActivity(intent);
        });

        TextView usersButton = findViewById(R.id.usersButton);
        usersButton.setOnClickListener(v -> {
            Intent intent = new Intent(UsersActivity.this, UsersActivity.class);
            startActivity(intent);
        });

        TextView orderButton = findViewById(R.id.orderButton);
        orderButton.setOnClickListener(v -> {
            Intent intent = new Intent(UsersActivity.this, OrdersAdminActivity.class);
            startActivity(intent);
        });
    }

    private void fetchUsers(String token) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/users")
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(UsersActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray usersArray = new JSONArray(response.body().string());
                        runOnUiThread(() -> {
                            userContainer.removeAllViews();
                            for (int i = 0; i < usersArray.length(); i++) {
                                try {
                                    JSONObject userObject = usersArray.getJSONObject(i);
                                    addUserCard(userObject);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() ->
                                Toast.makeText(UsersActivity.this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(UsersActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void addUserCard(JSONObject userObject) throws JSONException {
        int userId = userObject.getInt("id");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);
        card.setBackgroundColor(Color.parseColor("#E0E0E0"));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        TextView usernameView = new TextView(this);
        usernameView.setText("Имя: " + userObject.getString("username"));
        usernameView.setTextSize(18f);
        usernameView.setTextColor(Color.BLACK);
        card.addView(usernameView);

        TextView emailView = new TextView(this);
        emailView.setText("Email: " + userObject.getString("email"));
        emailView.setTextSize(16f);
        emailView.setTextColor(Color.DKGRAY);
        card.addView(emailView);

        TextView carsView = new TextView(this);
        String cars = userObject.optString("cars", "нет");
        carsView.setText("Машины: " + cars);
        carsView.setTextSize(14f);
        carsView.setTextColor(Color.DKGRAY);
        card.addView(carsView);

        TextView deleteButton = new TextView(this);
        deleteButton.setText("Удалить");
        deleteButton.setTextColor(Color.RED);
        deleteButton.setPadding(8, 8, 8, 8);
        deleteButton.setOnClickListener(v -> deleteUser(userId, card));
        card.addView(deleteButton);

        userContainer.addView(card);
    }

    private void deleteUser(int userId, LinearLayout card) {
        OkHttpClient client = new OkHttpClient();

        try {
            String token = encryptedSharedPrefs.getToken();
            if (token == null) {
                Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
                return;
            }

            Request request = new Request.Builder()
                    .url("http://10.0.2.2:5000/api/users/" + userId)
                    .addHeader("Authorization", token)
                    .delete()
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(UsersActivity.this, "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            userContainer.removeView(card);
                            Toast.makeText(UsersActivity.this, "Пользователь удален", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(UsersActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

