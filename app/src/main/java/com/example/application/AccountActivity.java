package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AccountActivity extends AppCompatActivity {
    private EncryptedSharedPrefs encryptedSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        TextView usernameTextView = findViewById(R.id.usernameTextView);
        TextView emailTextView = findViewById(R.id.emailTextView);
        Button logoutButton = findViewById(R.id.logoutButton);

        TextView homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        TextView accountButton = findViewById(R.id.accountButton);
        accountButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, AccountActivity.class);
            startActivity(intent);
        });

        TextView garageButton = findViewById(R.id.garageButton);
        garageButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, GarageActivity.class);
            startActivity(intent);
        });

        TextView orderButton = findViewById(R.id.orderButton);
        orderButton.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, OrdersActivity.class);
            startActivity(intent);
        });


        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();

            if (token != null) {
                String email = JwtUtils.decodeToken(token, "email");
                String username = JwtUtils.decodeToken(token, "username");

                usernameTextView.setText(username);
                emailTextView.setText(email);
            } else {
                usernameTextView.setText("Имя не найдено");
                emailTextView.setText("Email не найден");
            }
        } catch (Exception e) {
            e.printStackTrace();
            usernameTextView.setText("Ошибка");
            emailTextView.setText("Ошибка");
        }

        TextView infoButton = findViewById(R.id.infoButton);

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String token = encryptedSharedPrefs.getToken();
                    if (token != null) {
                        String email = JwtUtils.decodeToken(token, "email");
                        String username = JwtUtils.decodeToken(token, "username");

                        Intent intent = new Intent(AccountActivity.this, DetailsActivity.class);
                        intent.putExtra("username", username);
                        intent.putExtra("email", email);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }

    private void logout() {
        try {
            encryptedSharedPrefs.saveToken(null);
            String clearedToken = encryptedSharedPrefs.getToken();
            Log.d("AccountActivity", "Токен после выхода: " + (clearedToken == null ? "успешно очищен" : clearedToken));

            Intent intent = new Intent(AccountActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}