package com.example.application.ui.view;


import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.application.ApiService;
import com.example.application.R;

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
    private TextView privacyPolicyTextView;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = new ApiService();
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        privacyPolicyCheckBox = findViewById(R.id.privacyPolicyCheckBox);
        privacyPolicyTextView = findViewById(R.id.privacyPolicyTextView);

        registerButton.setEnabled(false);

        privacyPolicyCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            registerButton.setEnabled(isChecked);
        });

        privacyPolicyTextView.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, PrivacyPolicyActivity.class);
            startActivity(intent);
        });

        registerButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            boolean isValid = true;
            StringBuilder errorMessages = new StringBuilder();

            if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.(ru|com)$")) {
                errorMessages.append("Email должен содержать '@' и заканчиваться на .ru или .com\n");
                isValid = false;
            }

            if (password.length() < 8) {
                errorMessages.append("Пароль должен содержать не менее 8 символов\n");
                isValid = false;
            }

            if (!isValid) {
                Toast.makeText(com.example.application.ui.view.RegisterActivity.this, errorMessages.toString(), Toast.LENGTH_LONG).show();
                return;
            }

            apiService.register(username, email, password, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(com.example.application.ui.view.RegisterActivity.this, "Ошибка подключения", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(com.example.application.ui.view.RegisterActivity.this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(com.example.application.ui.view.RegisterActivity.this, LoginActivity.class));
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
                                        Toast.makeText(com.example.application.ui.view.RegisterActivity.this, serverErrors.toString(), Toast.LENGTH_LONG).show()
                                );
                            } else {
                                runOnUiThread(() ->
                                        Toast.makeText(com.example.application.ui.view.RegisterActivity.this, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
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
}