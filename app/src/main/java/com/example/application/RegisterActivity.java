package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

        privacyPolicyTextView.setOnClickListener(v -> showPrivacyPolicyDialog());

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
                Toast.makeText(RegisterActivity.this, errorMessages.toString(), Toast.LENGTH_LONG).show();
                return;
            }

            apiService.register(username, email, password, new Callback() {
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

    private void showPrivacyPolicyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Согласие на обработку пероснальных данных");
        builder.setMessage("Я (далее - Заявитель) даю согласие ИП Таций Я.Э, ИНН 772331755151, адрес г. Москва, Новоясеневский проспект, 6, корп. 1 (далее — Оператор) на автоматизированную, а также без использования средств автоматизации, обработку моих персональных данных, предоставленных мною Оператору посредством сети Интернет через сайт http://mymotor.ru, а именно фамилия, имя и отчество, год, месяц и дату рождения, данные СТС, номер телефона, электронная почта,\n" +
                "включая сбор, запись, систематизацию, накопление, хранение, уточнение (обновление, изменение), извлечение, использование, передачу (предоставление, доступ), обезличивание, блокирование, удаление и уничтожение данных, а также на вышеуказанную обработку иных моих персональных данных, полученных в результате их обработки, включая обработку третьими лицами.\n" +
                "\n" +
                "Целями обработки персональных данных является: оказание услуг автосервиса, выполнение условий договоров на предоставление услуг, проведение обратной связи и консультаций, выполнение требований законодательства РФ.\n" +
                "\n" +
                "Указанное согласие дано на срок 3 года.\n" +
                "\n" +
                "В случае, если в течение указанного срока с Заявителем будет заключен договор на оказание услуг автосервиса (например, договор на проведение технического обслуживания, ремонта автомобиля или предоставления дополнительных услуг), настоящее Согласие Заявителя сохраняет силу в течение всего срока действия каждого такого договора, а также в течение 3 лет с даты прекращения обязательств сторон по указанным договорам.\n" +
                "\n" +
                "Я уведомлен, что предоставление указанного выше согласия является моим правом, а не обязанностью, данные согласия могут быть отозваны в порядке, предусмотренном законодательством Российской Федерации. Согласие может быть отозвано субъектом персональных данных путем обращения к Оператору с заявлением, оформленным в письменной форме по адресу г. Москва, Новоясеневский проспект, 6, корп. 1 или в электронном обращении на электронную почту mymotor@mail.ru\n");
        builder.setCancelable(true);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
