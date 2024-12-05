package com.example.application;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditCarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_car);

        EditText modelEdit = findViewById(R.id.editModel);
        EditText brandEdit = findViewById(R.id.editBrand);
        EditText yearEdit = findViewById(R.id.editYear);
        EditText mileageEdit = findViewById(R.id.editMileage);
        EditText vinCodeEdit = findViewById(R.id.editVinCode);
        EditText licensePlateEdit = findViewById(R.id.editLicensePlate);
        EditText bodyTypeEdit = findViewById(R.id.editBodyType);
        EditText engineTypeEdit = findViewById(R.id.editEngineType);

        Button saveButton = findViewById(R.id.saveButton);

        String carDetailsJson = getIntent().getStringExtra("carDetails");

        try {
            JSONObject carDetails = new JSONObject(carDetailsJson);

            modelEdit.setText(carDetails.getString("model"));
            brandEdit.setText(carDetails.getString("brand"));
            yearEdit.setText(String.valueOf(carDetails.getInt("year")));
            mileageEdit.setText(String.valueOf(carDetails.getInt("mileage")));
            vinCodeEdit.setText(carDetails.getString("vin_code"));
            licensePlateEdit.setText(carDetails.getString("license_plate"));
            bodyTypeEdit.setText(carDetails.getString("body_type"));
            engineTypeEdit.setText(carDetails.getString("engine_type"));

            saveButton.setOnClickListener(v -> {
                try {
                    String updatedModel = modelEdit.getText().toString();
                    String updatedBrand = brandEdit.getText().toString();
                    int updatedYear = Integer.parseInt(yearEdit.getText().toString());
                    int updatedMileage = Integer.parseInt(mileageEdit.getText().toString());
                    String updatedVinCode = vinCodeEdit.getText().toString();
                    String updatedLicensePlate = licensePlateEdit.getText().toString();
                    String updatedBodyType = bodyTypeEdit.getText().toString();
                    String updatedEngineType = engineTypeEdit.getText().toString();

                    JSONObject updatedCar = new JSONObject();
                    updatedCar.put("id_car", carDetails.getInt("id_car"));
                    updatedCar.put("model", updatedModel);
                    updatedCar.put("brand", updatedBrand);
                    updatedCar.put("year", updatedYear);
                    updatedCar.put("mileage", updatedMileage);
                    updatedCar.put("vin_code", updatedVinCode);
                    updatedCar.put("license_plate", updatedLicensePlate);
                    updatedCar.put("body_type", updatedBodyType);
                    updatedCar.put("engine_type", updatedEngineType);

                    // Отправка данных на сервер
                    updateCar(updatedCar);

                } catch (Exception e) {
                    Toast.makeText(this, "Ошибка при сохранении", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCar(JSONObject carData) {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            Log.e("EditCarActivity", "Ошибка получения токена: " + e.getMessage());
            return;
        }

        String apiUrl = "http://10.0.2.2:5000/api/cars/" + carData.optInt("id_car");

        String json = carData.toString();
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", token)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditCarActivity.this, "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditCarActivity.this, "Данные успешно обновлены", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(EditCarActivity.this, "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}

