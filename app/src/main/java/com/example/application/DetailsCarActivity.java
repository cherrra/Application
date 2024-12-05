package com.example.application;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

/*
public class DetailsCarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_car);

        TextView detailsTextView = findViewById(R.id.detailsTextView);

        // Получаем данные автомобиля из Intent
        String carDetailsJson = getIntent().getStringExtra("carDetails");

        if (carDetailsJson != null) {
            try {
                JSONObject carDetails = new JSONObject(carDetailsJson);

                // Форматируем данные для отображения
                String details = "Модель: " + carDetails.getString("model") + "\n" +
                        "Марка: " + carDetails.getString("brand") + "\n" +
                        "Год: " + carDetails.getInt("year") + "\n" +
                        "Пробег: " + carDetails.getInt("mileage") + " км\n" +
                        "VIN: " + carDetails.getString("vin_code") + "\n" +
                        "Номерной знак: " + carDetails.getString("license_plate") + "\n" +
                        "Тип кузова: " + carDetails.getString("body_type") + "\n" +
                        "Тип двигателя: " + carDetails.getString("engine_type");

                detailsTextView.setText(details);


            } catch (Exception e) {
                detailsTextView.setText("Ошибка загрузки данных автомобиля");
            }
        } else {
            detailsTextView.setText("Нет данных для отображения");
        }
    }
}
*/
public class DetailsCarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_car);

        TextView detailsTextView = findViewById(R.id.detailsTextView);
        Button editButton = findViewById(R.id.editButton);

        String carDetailsJson = getIntent().getStringExtra("carDetails");

        if (carDetailsJson != null) {
            try {
                JSONObject carDetails = new JSONObject(carDetailsJson);

                String details = "Модель: " + carDetails.getString("model") + "\n" +
                        "Марка: " + carDetails.getString("brand") + "\n" +
                        "Год: " + carDetails.getInt("year") + "\n" +
                        "Пробег: " + carDetails.getInt("mileage") + " км\n" +
                        "VIN: " + carDetails.getString("vin_code") + "\n" +
                        "Номерной знак: " + carDetails.getString("license_plate") + "\n" +
                        "Тип кузова: " + carDetails.getString("body_type") + "\n" +
                        "Тип двигателя: " + carDetails.getString("engine_type");

                detailsTextView.setText(details);

                editButton.setOnClickListener(v -> {
                    Intent intent = new Intent(DetailsCarActivity.this, EditCarActivity.class);
                    intent.putExtra("carDetails", carDetailsJson); // Передаем данные автомобиля
                    startActivity(intent);
                });

            } catch (Exception e) {
                detailsTextView.setText("Ошибка загрузки данных автомобиля");
            }
        } else {
            detailsTextView.setText("Нет данных для отображения");
        }
    }
}