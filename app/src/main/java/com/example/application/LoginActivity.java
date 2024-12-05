package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private ApiService apiService;
    private EncryptedSharedPrefs encryptedSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        apiService = new ApiService();
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String savedToken = encryptedSharedPrefs.getToken();
            Log.d("LoginActivity", "Токен при запуске: " + (savedToken != null ? savedToken : "отсутствует"));
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                apiService.login(email, password, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            try {
                                String responseBody = response.body().string();
                                JSONObject jsonObject = new JSONObject(responseBody);
                                String token = jsonObject.getString("token");

                                if (encryptedSharedPrefs != null) {
                                    encryptedSharedPrefs.saveToken(token);
                                    Log.d("LoginActivity", "Токен успешно сохранён: " + token);
                                }

                                runOnUiThread(() -> {
                                    Toast.makeText(LoginActivity.this, "Вход успешен", Toast.LENGTH_SHORT).show();

                                    if (email.contains("@admin-mail")) {
                                        startActivity(new Intent(LoginActivity.this, HomeAdminActivity.class));
                                    } else {
                                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            runOnUiThread(() ->
                                    Toast.makeText(LoginActivity.this, "Данные неверные", Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            }
        });
    }
}

