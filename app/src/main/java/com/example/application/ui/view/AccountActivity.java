package com.example.application.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.utils.EncryptedSharedPrefs;
import com.example.application.R;
import com.example.application.ui.viewmodel.UserViewModel;

public class AccountActivity extends AppCompatActivity {
    private UserViewModel userViewModel;
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

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();
            Log.d("AccountActivity", "Полученный токен: " + token);

            if (token != null) {
                observeUserData(token, usernameTextView, emailTextView);
            } else {
                Log.e("AccountActivity", "Токен отсутствует");
            }
        } catch (Exception e) {
            Log.e("AccountActivity", "Ошибка доступа к токену: " + e.getMessage());
        }

        TextView infoButton = findViewById(R.id.infoButton);
        infoButton.setOnClickListener(v -> {
            try {
                String token = encryptedSharedPrefs.getToken();
                if (token != null) {
                    userViewModel.getUser(token).observe(this, user -> {
                        if (user != null) {
                            Intent intent = new Intent(AccountActivity.this, DetailsActivity.class);
                            intent.putExtra("userId", user.getId()); // Передаем ID для загрузки данных
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Ошибка загрузки данных пользователя", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        logoutButton.setOnClickListener(v -> logout());
    }

    private void observeUserData(String token, TextView usernameTextView, TextView emailTextView) {
        userViewModel.getUser(token).observe(this, user -> {
            if (user != null) {
                usernameTextView.setText(user.getUsername());
                emailTextView.setText(user.getEmail());
            } else {
                Toast.makeText(this, "Ошибка загрузки данных пользователя", Toast.LENGTH_SHORT).show();
                Log.e("AccountActivity", "User data is null");
            }
        });
    }

    private void logout() {
        try {
            encryptedSharedPrefs.saveToken(null);
            Intent intent = new Intent(AccountActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}