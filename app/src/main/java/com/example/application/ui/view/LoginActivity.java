package com.example.application.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.application.R;
import com.example.application.network.ApiClient;
import com.example.application.ui.view.admin.HomeAdminActivity;
import com.example.application.utils.EncryptedSharedPrefs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private EncryptedSharedPrefs encryptedSharedPrefs;
    private TextView registerTextB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordInputEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextB = findViewById(R.id.registerTextB);

        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            // Используем getAccessToken() вместо getToken()
            String savedToken = encryptedSharedPrefs.getAccessToken();
            Log.d("LoginActivity", "Токен при запуске: " + (savedToken != null ? savedToken : "отсутствует"));
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка инициализации хранилища", Toast.LENGTH_SHORT).show();
        }

        setupButtonAnimation(loginButton);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            ApiClient apiClient = ApiClient.getInstance(); // без параметров
            apiClient.login(email, password, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Ошибка подключения: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonObject = new JSONObject(responseBody);
                            String accessToken = jsonObject.getString("accessToken");
                            String refreshToken = jsonObject.getString("refreshToken");

                            // Сохраняем оба токена
                            encryptedSharedPrefs.saveTokens(accessToken, refreshToken);

                            runOnUiThread(() -> {
                                Toast.makeText(LoginActivity.this, "Вход успешен", Toast.LENGTH_SHORT).show();

                                Intent intent;
                                if (email.contains("@admin-mail")) {
                                    intent = new Intent(LoginActivity.this, HomeAdminActivity.class);
                                } else {
                                    intent = new Intent(LoginActivity.this, HomeActivity.class);
                                }
                                startActivity(intent);
                                finish();
                            });
                        } catch (JSONException e) {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "Ошибка обработки ответа", Toast.LENGTH_SHORT).show()
                            );
                        }
                    } else {
                        runOnUiThread(() -> {
                            if (response.code() == 401) {
                                Toast.makeText(LoginActivity.this, "Неверный email или пароль", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        });

        registerTextB.setOnClickListener(v -> {
          startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void setupButtonAnimation(Button button) {
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
