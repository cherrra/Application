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

        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();

            if (token != null) {
                // Извлечение данных из токена
                String email = JwtUtils.decodeToken(token, "email");
                String username = JwtUtils.decodeToken(token, "username");

                // Вывод имени пользователя и email
                usernameTextView.setText("Ваше имя: " + username);
                emailTextView.setText("Ваш email: " + email);
            } else {
                usernameTextView.setText("Пользователь не найден");
                emailTextView.setText("");
            }
        } catch (Exception e) {
            e.printStackTrace();
            usernameTextView.setText("Ошибка загрузки данных");
            emailTextView.setText("");
        }

        // Обработчик для кнопки выхода
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
    }

    private void logout() {
        try {
            // Удаляем токен
            encryptedSharedPrefs.saveToken(null);

            // Проверяем, что токен очищен
            String clearedToken = encryptedSharedPrefs.getToken();
            if (clearedToken == null) {
                Log.d("AccountActivity", "Токен успешно очищен");
            } else {
                Log.d("AccountActivity", "Токен не очищен: " + clearedToken);
            }

            // Переходим на MainActivity
            Intent intent = new Intent(AccountActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // Закрываем текущую активность
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}