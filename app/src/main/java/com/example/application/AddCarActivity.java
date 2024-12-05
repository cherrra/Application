package com.example.application;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddCarActivity extends AppCompatActivity {
    private EditText modelInput, brandInput, yearInput, mileageInput, vinCodeInput, licensePlateInput, bodyTypeInput, engineTypeInput;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);

        modelInput = findViewById(R.id.modelInput);
        brandInput = findViewById(R.id.brandInput);
        yearInput = findViewById(R.id.yearInput);
        mileageInput = findViewById(R.id.mileageInput);
        vinCodeInput = findViewById(R.id.vinCodeInput);
        licensePlateInput = findViewById(R.id.licensePlateInput);
        bodyTypeInput = findViewById(R.id.bodyTypeInput);
        engineTypeInput = findViewById(R.id.engineTypeInput);
        saveButton = findViewById(R.id.saveButton);

        saveButton.setOnClickListener(view -> saveCar());
    }

    private void saveCar() {
        String model = modelInput.getText().toString();
        String brand = brandInput.getText().toString();
        String year = yearInput.getText().toString();
        String mileage = mileageInput.getText().toString();
        String vinCode = vinCodeInput.getText().toString();
        String licensePlate = licensePlateInput.getText().toString();
        String bodyType = bodyTypeInput.getText().toString();
        String engineType = engineTypeInput.getText().toString();

        if (model.isEmpty() || brand.isEmpty() || year.isEmpty() || mileage.isEmpty() ||
                vinCode.isEmpty() || licensePlate.isEmpty() || bodyType.isEmpty() || engineType.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (GeneralSecurityException | IOException e) {
            Log.e("AddCarActivity", "Ошибка получения токена: " + e.getMessage());
            return;
        }

        if (token == null) {
            Toast.makeText(this, "Не авторизован", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        JSONObject carJson = new JSONObject();
        try {
            carJson.put("model", model);
            carJson.put("brand", brand);
            carJson.put("year", Integer.parseInt(year));
            carJson.put("mileage", Integer.parseInt(mileage));
            carJson.put("vin_code", vinCode);
            carJson.put("license_plate", licensePlate);
            carJson.put("body_type", bodyType);
            carJson.put("engine_type", engineType);
        } catch (Exception e) {
            Log.e("AddCarActivity", "Ошибка создания JSON: " + e.getMessage());
            return;
        }

        RequestBody body = RequestBody.create(carJson.toString(), JSON);
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/cars")
                .addHeader("Authorization", token)
                .post(body)
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Машина добавлена", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    Log.e("AddCarActivity", "Ошибка ответа сервера: " + response.code());
                }
            } catch (IOException e) {
                Log.e("AddCarActivity", "Ошибка запроса: " + e.getMessage());
            }
        }).start();
    }
}
