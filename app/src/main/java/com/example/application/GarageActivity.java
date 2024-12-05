package com.example.application;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GarageActivity extends AppCompatActivity {

    private LinearLayout carContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garage);

        carContainer = findViewById(R.id.carContainer);
        fetchGarageDetails();

        TextView homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(GarageActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        TextView accountButton = findViewById(R.id.accountButton);
        accountButton.setOnClickListener(v -> {
            Intent intent = new Intent(GarageActivity.this, AccountActivity.class);
            startActivity(intent);
        });

        TextView garageButton = findViewById(R.id.garageButton);
        garageButton.setOnClickListener(v -> {
            Intent intent = new Intent(GarageActivity.this, GarageActivity.class);
            startActivity(intent);
        });

        TextView orderButton = findViewById(R.id.orderButton);
        orderButton.setOnClickListener(v -> {
            Intent intent = new Intent(GarageActivity.this, OrdersActivity.class);
            startActivity(intent);
        });

        Button addCarButton = findViewById(R.id.addCarButton);
        addCarButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, AddCarActivity.class);
            startActivity(intent);
        });
    }

    private void fetchGarageDetails() {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (GeneralSecurityException | IOException e) {
            Log.e("GarageActivity", "Ошибка инициализации EncryptedSharedPrefs: " + e.getMessage());
            return;
        }

        if (token == null) {
            Log.e("GarageActivity", "Токен отсутствует");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/cars")
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("GarageActivity", "Ошибка получения данных: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(GarageActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray carsArray = new JSONArray(response.body().string());
                        runOnUiThread(() -> {
                            carContainer.removeAllViews();
                            for (int i = 0; i < carsArray.length(); i++) {
                                try {
                                    JSONObject carObject = carsArray.getJSONObject(i);
                                    addCarCard(carObject);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (Exception e) {
                        Log.e("GarageActivity", "Ошибка обработки ответа: " + e.getMessage());
                    }
                } else {
                    Log.e("GarageActivity", "Ответ сервера: " + response.code());
                    runOnUiThread(() -> Toast.makeText(GarageActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void addCarCard(JSONObject carObject) {
        try {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16, 16, 16, 16);
            card.setBackgroundColor(Color.parseColor("#E0E0E0"));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            card.setLayoutParams(params);

            TextView modelView = new TextView(this);
            modelView.setText("Модель: " + carObject.getString("model"));
            modelView.setTextSize(18f);
            modelView.setTextColor(Color.BLACK);
            card.addView(modelView);

            TextView brandView = new TextView(this);
            brandView.setText("Марка: " + carObject.getString("brand"));
            brandView.setTextSize(16f);
            brandView.setTextColor(Color.DKGRAY);
            card.addView(brandView);

            TextView yearView = new TextView(this);
            yearView.setText("Год: " + carObject.getInt("year"));
            yearView.setTextSize(14f);
            yearView.setTextColor(Color.DKGRAY);
            card.addView(yearView);

            TextView mileageView = new TextView(this);
            mileageView.setText("Пробег: " + carObject.getInt("mileage") + " км");
            mileageView.setTextSize(14f);
            mileageView.setTextColor(Color.DKGRAY);
            card.addView(mileageView);

            Button detailsButton = new Button(this);
            detailsButton.setText("Подробнее");
            detailsButton.setOnClickListener(v -> {
                Intent intent = new Intent(GarageActivity.this, DetailsCarActivity.class);
                intent.putExtra("carDetails", carObject.toString());
                startActivity(intent);
            });
            card.addView(detailsButton);

            TextView deleteButton = new TextView(this);
            deleteButton.setText("Удалить");
            deleteButton.setTextColor(Color.BLACK);
            deleteButton.setPadding(8, 8, 8, 8);
            deleteButton.setOnClickListener(v -> {
                int carId = carObject.optInt("id_car", -1);
                if (carId > 0) {
                    deleteCar(carId, card);
                } else {
                    Toast.makeText(GarageActivity.this, "Некорректный идентификатор машины", Toast.LENGTH_SHORT).show();
                    Log.e("GarageActivity", "Некорректный id_car: " + carId);
                }
            });

            card.addView(deleteButton);

            carContainer.addView(card);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteCar(int carId, LinearLayout card) {
        OkHttpClient client = new OkHttpClient();

        try {
            String token = new EncryptedSharedPrefs(this).getToken();
            if (token == null) {
                Toast.makeText(this, "Необходим токен авторизации", Toast.LENGTH_SHORT).show();
                return;
            }

            Request request = new Request.Builder()
                    .url("http://10.0.2.2:5000/api/cars/" + carId)
                    .delete()
                    .addHeader("Authorization", token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Log.e("GarageActivity", "Ошибка удаления: " + e.getMessage());
                        Toast.makeText(GarageActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            carContainer.removeView(card);
                            Toast.makeText(GarageActivity.this, "Машина успешно удалена", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Log.e("GarageActivity", "Ошибка удаления, код: " + response.code());
                        runOnUiThread(() -> Toast.makeText(GarageActivity.this, "Ошибка удаления машины", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (Exception e) {
            Log.e("GarageActivity", "Ошибка удаления машины: " + e.getMessage());
        }
    }
}
