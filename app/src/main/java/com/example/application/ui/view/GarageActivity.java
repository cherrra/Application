package com.example.application.ui.view;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.R;
import com.example.application.data.model.Car;
import com.example.application.network.ApiClient;
import com.example.application.ui.viewmodel.CarViewModel;
import com.example.application.utils.EncryptedSharedPrefs;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
public class GarageActivity extends AppCompatActivity {

    private LinearLayout carContainer;
    private TextView pustoTextView; // Добавлено
    private CarViewModel carViewModel;
    private static final String TAG = "GarageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garage);

        carContainer = findViewById(R.id.carContainer);
        pustoTextView = findViewById(R.id.pustoTextView); // Инициализация TextView
        carViewModel = new ViewModelProvider(this).get(CarViewModel.class);

        setupNavigation();
        fetchGarageDetails();
    }

    private void fetchGarageDetails() {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения токена: " + e.getMessage());
            return;
        }

        carViewModel.getCars(token).observe(this, cars -> {
            if (cars != null && !cars.isEmpty()) {
                Log.d(TAG, "Загружено " + cars.size() + " автомобилей");
                pustoTextView.setVisibility(View.GONE); // Скрываем надпись "Пока пусто"
                updateUI(cars);
            } else {
                Log.d(TAG, "Нет автомобилей в гараже");
                pustoTextView.setVisibility(View.VISIBLE); // Показываем надпись "Пока пусто"
                carContainer.removeAllViews(); // Очищаем контейнер
            }
        });
    }

    // Остальной код без изменений
    private void updateUI(List<Car> cars) {
        carContainer.removeAllViews();
        for (Car car : cars) {
            addCarCard(car);
        }
    }

    private void addCarCard(Car car) {
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
        modelView.setText("Модель: " + car.getModel());
        card.addView(modelView);

        TextView brandView = new TextView(this);
        brandView.setText("Марка: " + car.getBrand());
        card.addView(brandView);

        TextView yearView = new TextView(this);
        yearView.setText("Год: " + car.getYear());
        card.addView(yearView);

        TextView mileageView = new TextView(this);
        mileageView.setText("Пробег: " + car.getMileage() + " км");
        card.addView(mileageView);

        Button detailsButton = new Button(this);
        detailsButton.setText("Подробнее");
        detailsButton.setOnClickListener(v -> {
            String carJson = new Gson().toJson(car);
            Intent intent = new Intent(GarageActivity.this, DetailsCarActivity.class);
            intent.putExtra("carDetails", carJson);
            startActivity(intent);
        });
        card.addView(detailsButton);

        TextView deleteButton = new TextView(this);
        deleteButton.setText("Удалить");
        deleteButton.setOnClickListener(v -> deleteCar(car.getIdCar(), card));
        card.addView(deleteButton);

        carContainer.addView(card);
    }

    private void deleteCar(int carId, LinearLayout card) {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения токена: " + e.getMessage());
            return;
        }

        ApiClient.getInstance().deleteCar(carId, token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(GarageActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        carContainer.removeView(card);
                        // Проверяем, остались ли еще автомобили
                        if (carContainer.getChildCount() == 0) {
                            pustoTextView.setVisibility(View.VISIBLE);
                        }
                        Toast.makeText(GarageActivity.this, "Машина удалена", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(GarageActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // Остальные методы без изменений
    private void setupNavigation() {
        findViewById(R.id.homeButton).setOnClickListener(v -> navigateTo(HomeActivity.class));
        findViewById(R.id.accountButton).setOnClickListener(v -> navigateTo(AccountActivity.class));
        findViewById(R.id.garageButton).setOnClickListener(v -> navigateTo(GarageActivity.class));
        findViewById(R.id.orderButton).setOnClickListener(v -> navigateTo(OrdersActivity.class));
        findViewById(R.id.addCarButton).setOnClickListener(v -> navigateTo(AddCarActivity.class));
    }

    private void navigateTo(Class<?> targetActivity) {
        startActivity(new Intent(this, targetActivity));
    }
}