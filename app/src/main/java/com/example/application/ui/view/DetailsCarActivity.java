package com.example.application.ui.view;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.application.R;
import com.example.application.data.model.Car;
import com.example.application.ui.viewmodel.CarViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

public class DetailsCarActivity extends AppCompatActivity {

    private CarViewModel carViewModel;
    private String carDetailsJson;
    private static final String TAG = "DetailsCarActivity";
    private static final String BASE_URL = "https://automser.store/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_car);

        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(v -> {
            finish(); // Закрываем текущую активность и возвращаемся назад
        });

        backArrow.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    v.performClick();
                    break;
            }
            return true;
        });

        // Инициализация ViewModel
        carViewModel = new ViewModelProvider(this).get(CarViewModel.class);

        // Получение ссылок на View элементы
        ImageView carImage = findViewById(R.id.carImage);
        TextView carTitle = findViewById(R.id.carTitle);
        TextView yearText = findViewById(R.id.yearText);
        TextView mileageText = findViewById(R.id.mileageText);
        TextView vinText = findViewById(R.id.vinText);
        TextView licenseText = findViewById(R.id.licenseText);
        MaterialButton editButton = findViewById(R.id.editButton);

        // Получение данных автомобиля
        carDetailsJson = getIntent().getStringExtra("carDetails");

        if (carDetailsJson != null) {
            try {
                Car car = new Gson().fromJson(carDetailsJson, Car.class);
                if (car != null) {
                    // Установка данных в View
                    carTitle.setText(String.format("%s %s",
                            car.getModel().getBrand().getBrandName(),
                            car.getModel().getModelName()));
                    yearText.setText(String.valueOf(car.getYear()));
                    mileageText.setText(String.format("%s км", car.getMileage()));
                    vinText.setText(car.getVinCode());
                    licenseText.setText(car.getLicensePlate());

                    // Загрузка изображения автомобиля
                    if (car.getLinkImg() != null && !car.getLinkImg().isEmpty()) {
                        loadCarImage(car.getLinkImg(), carImage);
                    } else {
                        carImage.setImageResource(R.drawable.ic_placeholder);
                    }

                    // Сохранение автомобиля в ViewModel
                    carViewModel.setSelectedCar(car);

                    // Обработчик кнопки редактирования
                    editButton.setOnClickListener(v -> {
                        Intent intent = new Intent(this, EditCarActivity.class);
                        intent.putExtra("carDetails", carDetailsJson);
                        startActivity(intent);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке данных автомобиля", e);
            }
        }
    }

    private void loadCarImage(String imageUrl, ImageView imageView) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_placeholder);
            return;
        }

        try {
            String cleanPath = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
            String fullImageUrl = BASE_URL + cleanPath;
            Log.d(TAG, "Загрузка изображения автомобиля: " + fullImageUrl);

            Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .centerCrop()
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "Ошибка загрузки изображения: " + fullImageUrl);
                            if (e != null) {
                                for (Throwable t : e.getRootCauses()) {
                                    Log.e(TAG, "Причина: " + t.getMessage());
                                }
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target, DataSource dataSource,
                                                       boolean isFirstResource) {
                            Log.d(TAG, "Изображение успешно загружено: " + fullImageUrl);
                            return false;
                        }
                    })
                    .into(imageView);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке изображения", e);
            imageView.setImageResource(R.drawable.ic_placeholder);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}