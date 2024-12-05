package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DetailsActivity extends AppCompatActivity {

    private TextView usernameTextView, emailTextView, birthDateTextView, genderTextView, phoneNumberTextView;
    private ImageView profileImageViewDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        usernameTextView = findViewById(R.id.usernameDetailsTextView);
        emailTextView = findViewById(R.id.emailDetailsTextView);
        birthDateTextView = findViewById(R.id.birthDateDetailsTextView);
        genderTextView = findViewById(R.id.genderDetailsTextView);
        phoneNumberTextView = findViewById(R.id.phoneNumberDetailsTextView);
        profileImageViewDetails = findViewById(R.id.profileImageViewDetails);

        Button backButton = findViewById(R.id.backButton);
        Button editButton = findViewById(R.id.editButton);

        backButton.setOnClickListener(v -> finish());

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(DetailsActivity.this, EditActivity.class);
            intent.putExtra("username", usernameTextView.getText().toString());
            intent.putExtra("email", emailTextView.getText().toString());
            intent.putExtra("birthDate", birthDateTextView.getText().toString());
            intent.putExtra("gender", genderTextView.getText().toString());
            intent.putExtra("phoneNumber", phoneNumberTextView.getText().toString());
            startActivity(intent);
        });

        Button deleteAccountButton = findViewById(R.id.deleteAccountButton);

        deleteAccountButton.setOnClickListener(v -> {
            new AlertDialog.Builder(DetailsActivity.this)
                    .setMessage("Вы уверены, что хотите удалить аккаунт?")
                    .setPositiveButton("Да", (dialog, which) -> deleteAccount())
                    .setNegativeButton("Нет", null)
                    .show();
        });

        fetchUserDetails();
    }
    private void fetchUserDetails() {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (GeneralSecurityException | IOException e) {
            Log.e("DetailsActivity", "Ошибка инициализации EncryptedSharedPrefs: " + e.getMessage());
            return;
        }

        if (token == null) {
            Log.e("DetailsActivity", "Токен отсутствует");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/auth/user")
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DetailsActivity", "Ошибка получения данных: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject user = new JSONObject(response.body().string());
                        runOnUiThread(() -> {
                            usernameTextView.setText(user.optString("username"));
                            emailTextView.setText(user.optString("email"));
                            String rawDate = user.optString("birth_date");
                            String formattedDate = formatDate(rawDate);
                            birthDateTextView.setText(formattedDate);
                            genderTextView.setText(user.optString("gender"));
                            phoneNumberTextView.setText(user.optString("phone_number"));
                        });
                    } catch (Exception e) {
                        Log.e("DetailsActivity", "Ошибка обработки ответа: " + e.getMessage());
                    }
                } else {
                    Log.e("DetailsActivity", "Ответ сервера: " + response.code());
                }
            }

        });
    }

    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) {
            return "Не указана";
        }

        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat desiredFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            return desiredFormat.format(isoFormat.parse(rawDate));
        } catch (Exception e) {
            Log.e("DetailsActivity", "Ошибка форматирования даты: " + e.getMessage());
            return rawDate;
        }
    }

    private void deleteAccount() {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (GeneralSecurityException | IOException e) {
            Log.e("DetailsActivity", "Ошибка инициализации EncryptedSharedPrefs: " + e.getMessage());
            return;
        }

        if (token == null) {
            Log.e("DetailsActivity", "Токен отсутствует");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/auth/delete")
                .delete()
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("DetailsActivity", "Ошибка удаления аккаунта: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response)
                    throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Intent intent = new Intent(DetailsActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                } else {
                    Log.e("DetailsActivity", "Ошибка сервера при удалении аккаунта: " + response.code());
                }
            }
        });
    }

}

