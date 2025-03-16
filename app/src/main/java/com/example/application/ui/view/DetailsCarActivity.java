package com.example.application.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.R;
import com.example.application.data.model.Car;
import com.example.application.ui.viewmodel.CarViewModel;
import com.google.gson.Gson;

public class DetailsCarActivity extends AppCompatActivity {

    private CarViewModel carViewModel;
    private String carDetailsJson;
    private static final String TAG = "DetailsCarActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_car);

        TextView detailsTextView = findViewById(R.id.detailsTextView);
        Button editButton = findViewById(R.id.editButton);

        // Инициализация ViewModel
        carViewModel = new ViewModelProvider(this).get(CarViewModel.class);

        // Получаем JSON-строку из Intent
        carDetailsJson = getIntent().getStringExtra("carDetails");

        if (carDetailsJson != null) {
            Log.d(TAG, "Получен JSON: " + carDetailsJson);
            try {
                // Используем Gson для парсинга
                Car car = new Gson().fromJson(carDetailsJson, Car.class);

                if (car == null) {
                    Log.e(TAG, "Ошибка: car == null после парсинга");
                    detailsTextView.setText("Ошибка загрузки данных автомобиля");
                    return;
                }

                // Устанавливаем машину в ViewModel
                carViewModel.setSelectedCar(car);

                // Отображаем данные
                detailsTextView.setText(getCarDetailsString(car));

                // Кнопка редактирования
//                editButton.setOnClickListener(v -> {
//                    Intent intent = new Intent(DetailsCarActivity.this, EditCarActivity.class);
//                    intent.putExtra("carDetails", carDetailsJson);
//                    startActivity(intent);
//                });
                editButton.setOnClickListener(v -> {
                    Intent intent = new Intent(DetailsCarActivity.this, EditCarActivity.class);
                    intent.putExtra("carDetails", carDetailsJson);

                    // Передаем объект в ViewModel (на случай, если он сбросится)
                    carViewModel.setSelectedCar(car);

                    startActivity(intent);
                });



            } catch (Exception e) {
                Log.e(TAG, "Ошибка парсинга JSON: " + e.getMessage());
                detailsTextView.setText("Ошибка загрузки данных автомобиля");
            }
        } else {
            Log.e(TAG, "carDetailsJson == null");
            detailsTextView.setText("Нет данных для отображения");
        }
    }

    private String getCarDetailsString(Car car) {
        return "Модель: " + car.getModel() + "\n" +
                "Марка: " + car.getBrand() + "\n" +
                "Год: " + car.getYear() + "\n" +
                "Пробег: " + car.getMileage() + " км\n" +
                "VIN: " + car.getVinCode() + "\n" +
                "Номерной знак: " + car.getLicensePlate() + "\n" +
                "Тип кузова: " + car.getBodyType() + "\n" +
                "Тип двигателя: " + car.getEngineType();
    }
}
//package com.example.application.ui.view;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.lifecycle.ViewModelProvider;
//
//import com.example.application.R;
//import com.example.application.data.model.Car;
//import com.example.application.ui.viewmodel.CarViewModel;
//
//public class DetailsCarActivity extends AppCompatActivity {
//
//    private CarViewModel carViewModel;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_details_car);
//
//        TextView detailsTextView = findViewById(R.id.detailsTextView);
//        Button editButton = findViewById(R.id.editButton);
//
//        carViewModel = new ViewModelProvider(this).get(CarViewModel.class);
//        Car car = carViewModel.getSelectedCar().getValue();
//
//        if (car != null) {
//            detailsTextView.setText(getCarDetailsString(car));
//
//            editButton.setOnClickListener(v -> {
//                Intent intent = new Intent(DetailsCarActivity.this, EditCarActivity.class);
//                startActivity(intent);
//            });
//        } else {
//            detailsTextView.setText("Ошибка загрузки данных автомобиля");
//        }
//    }
//
//    private String getCarDetailsString(Car car) {
//        return "Модель: " + car.getModel() + "\n" +
//                "Марка: " + car.getBrand() + "\n" +
//                "Год: " + car.getYear() + "\n" +
//                "Пробег: " + car.getMileage() + " км\n" +
//                "VIN: " + car.getVinCode() + "\n" +
//                "Номерной знак: " + car.getLicensePlate() + "\n" +
//                "Тип кузова: " + car.getBodyType() + "\n" +
//                "Тип двигателя: " + car.getEngineType();
//    }
//}
