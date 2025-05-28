package com.example.application.ui.view;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.application.R;
import com.example.application.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {
    private EditText usernameEditText, emailEditText, passwordEditText;
    private Button registerButton;
    private CheckBox privacyPolicyCheckBox;

    private TextView privacyPolicyText;
    private ApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiClient = ApiClient.getInstance();
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordInputEditText);
        registerButton = findViewById(R.id.registerButton);
        privacyPolicyCheckBox = findViewById(R.id.privacyPolicyCheckBox);
        privacyPolicyText = findViewById(R.id.privacyPolicyText);

        // Начальное состояние кнопки (заблокирована)
        registerButton.setEnabled(false);
        updateRegisterButtonState(false);
        setupButtonAnimation(registerButton);

        privacyPolicyCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            registerButton.setEnabled(isChecked);
            updateRegisterButtonState(isChecked);
        });

        privacyPolicyText.setOnClickListener(v -> {
            // Переход на политику конфиденциальности
            startActivity(new Intent(RegisterActivity.this, PrivacyPolicyActivity.class));
        });

        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            boolean isValid = true;
            StringBuilder errorMessages = new StringBuilder();

            // Проверка email
            if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.(ru|com)$")) {
                errorMessages.append("Email должен содержать '@' и заканчиваться на .ru или .com\n");
                isValid = false;
            } else if (email.toLowerCase().contains("@admin")) {
                errorMessages.append("Использование '@admin' в email запрещено\n");
                isValid = false;
            }

            // Проверка пароля
            if (password.length() < 8) {
                errorMessages.append("Пароль должен содержать не менее 8 символов\n");
                isValid = false;
            }

            if (!isValid) {
                Toast.makeText(RegisterActivity.this, errorMessages.toString(), Toast.LENGTH_LONG).show();
                return;
            }

            apiClient.register(username, email, password, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(RegisterActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(RegisterActivity.this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        });
                    } else {
                        try {
                            String responseBody = response.body().string();
                            JSONObject jsonObject = new JSONObject(responseBody);
                            if (jsonObject.has("errors")) {
                                JSONArray errors = jsonObject.getJSONArray("errors");
                                StringBuilder serverErrors = new StringBuilder();
                                for (int i = 0; i < errors.length(); i++) {
                                    serverErrors.append(errors.getString(i)).append("\n");
                                }
                                runOnUiThread(() ->
                                        Toast.makeText(RegisterActivity.this, serverErrors.toString(), Toast.LENGTH_LONG).show()
                                );
                            } else {
                                runOnUiThread(() ->
                                        Toast.makeText(RegisterActivity.this, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
                                );
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });
    }

    private void updateRegisterButtonState(boolean enabled) {
        if (enabled) {
            // Включенное состояние
            registerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.blue)));
            registerButton.setTextColor(getResources().getColor(R.color.white));
        } else {
            // Выключенное состояние
            registerButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.light_blue)));
            registerButton.setTextColor(getResources().getColor(R.color.blue));
        }
    }

    private void setupButtonAnimation(Button button) {
        button.setOnTouchListener((v, event) -> {
            if (button.isEnabled()) { // Анимация только для активной кнопки
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        break;
                }
            }
            return false;
        });
    }
}