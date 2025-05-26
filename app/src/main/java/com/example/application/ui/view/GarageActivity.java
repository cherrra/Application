package com.example.application.ui.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
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
    private LinearLayout emptyStateContainer;
    private TextView pustoTextView;
    private CarViewModel carViewModel;
    private static final String TAG = "GarageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garage);

        carContainer = findViewById(R.id.carContainer);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        carViewModel = new ViewModelProvider(this).get(CarViewModel.class);

        Button addCarButton = findViewById(R.id.addCarButton);
//        setupButtonAnimation(addCarButton);

        setupNavigation();
        fetchGarageDetails();
    }

    private void fetchGarageDetails() {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getAccessToken();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения токена: " + e.getMessage());
            return;
        }

        carViewModel.getCars(token).observe(this, cars -> {
            if (cars != null && !cars.isEmpty()) {
                Log.d(TAG, "Загружено " + cars.size() + " автомобилей");
                emptyStateContainer.setVisibility(View.GONE);
                updateUI(cars);
            } else {
                Log.d(TAG, "Нет автомобилей в гараже");
                emptyStateContainer.setVisibility(View.VISIBLE);
                carContainer.removeAllViews();
            }
        });
    }

    private void updateUI(List<Car> cars) {
        carContainer.removeAllViews();
        for (Car car : cars) {
            if (car != null) {
                addCarCard(car);
            }
        }
    }

    private void addCarCard(Car car) {
        // Основной контейнер карточки
        RelativeLayout card = new RelativeLayout(this);
        card.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Стиль карточки - белый фон с голубой обводкой (как в addUserCard)
        GradientDrawable cardShape = new GradientDrawable();
        cardShape.setShape(GradientDrawable.RECTANGLE);
        cardShape.setCornerRadius(dpToPx(24)); // Скругление углов 24dp
        cardShape.setColor(Color.WHITE);
        cardShape.setStroke(dpToPx(2), Color.parseColor("#E3F2FD")); // Голубая обводка
        card.setBackground(cardShape);
        card.setElevation(dpToPx(2)); // Легкая тень

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(16));
        card.setLayoutParams(cardParams);

        // Изображение автомобиля с небольшим скруглением
        ImageView carImageView = new ImageView(this);
        GradientDrawable imageShape = new GradientDrawable();
        imageShape.setShape(GradientDrawable.RECTANGLE);
        imageShape.setCornerRadius(dpToPx(12)); // Скругление углов изображения 12dp
        imageShape.setColor(Color.parseColor("#F5F5F5")); // Цвет placeholder'а

        // Для API >= 21 можно использовать ViewOutlineProvider для лучшего скругления
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            carImageView.setClipToOutline(true);
            carImageView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dpToPx(12));
                }
            });
        }
        carImageView.setBackground(imageShape);

        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                dpToPx(180) // Фиксированная высота изображения
        );
        imageParams.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8)); // Отступы вокруг изображения
        carImageView.setLayoutParams(imageParams);
        carImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        carImageView.setId(View.generateViewId());

        // Загрузка изображения
        if (car.getLinkImg() != null && !car.getLinkImg().isEmpty()) {
            loadCarImage(car.getLinkImg(), carImageView);
        } else {
            carImageView.setImageResource(R.drawable.ic_placeholder);
            carImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        }

        card.addView(carImageView);

        // Контейнер для текстовой информации
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(16));

        RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.addRule(RelativeLayout.BELOW, carImageView.getId());
        infoLayout.setLayoutParams(infoParams);

        // Название автомобиля
        String brandName = "Не указано";
        String modelName = "Не указано";

        if (car.getModel() != null) {
            modelName = car.getModel().getModelName();
            if (car.getModel().getBrand() != null) {
                brandName = car.getModel().getBrand().getBrandName();
            }
        }

        TextView carTitleView = new TextView(this);
        carTitleView.setText(brandName + " " + modelName);
        carTitleView.setTextSize(20);
        carTitleView.setTextColor(Color.parseColor("#2260FF")); // Голубой цвет текста
        try {
            Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/nunitosans_bold.ttf");
            carTitleView.setTypeface(typeface);
        } catch (Exception e) {
            // В случае ошибки загрузки шрифта используем стандартный
            carTitleView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            Log.e("Garage", "Error loading NunitoSans font", e);
        }
        carTitleView.setEllipsize(TextUtils.TruncateAt.END);
        carTitleView.setMaxLines(1);
        infoLayout.addView(carTitleView);

        // Контейнер для кнопок
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.END);

        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonsParams.setMargins(0, dpToPx(12), 0, 0);
        buttonsLayout.setLayoutParams(buttonsParams);

        // Кнопка "Подробнее"
        Button detailsButton = new Button(this);
        setupModernButton(detailsButton, "Подробнее", "#2260FF");
        detailsButton.setOnClickListener(v -> {
            String carJson = new Gson().toJson(car);
            Intent intent = new Intent(GarageActivity.this, DetailsCarActivity.class);
            intent.putExtra("carDetails", carJson);
            startActivity(intent);
        });
        buttonsLayout.addView(detailsButton);

        // Кнопка "Удалить"
        Button deleteButton = new Button(this);
        setupModernButton(deleteButton, "Удалить", "#FF5252");
        deleteButton.setOnClickListener(v -> deleteCar(car.getIdCar(), card));
        buttonsLayout.addView(deleteButton);

        infoLayout.addView(buttonsLayout);
        card.addView(infoLayout);

        // Добавляем карточку в контейнер
        carContainer.addView(card);
    }

    // Метод для создания стилизованных кнопок
    private void setupModernButton(Button button, String text, String color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(dpToPx(20));
        shape.setColor(Color.parseColor(color));

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackground(shape);
        button.setAllCaps(false);
        button.setTextSize(14);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(120),
                dpToPx(40)
        );
        params.setMargins(dpToPx(8), 0, 0, 0);
        button.setLayoutParams(params);

        // Анимация при нажатии
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    // Остальные методы остаются без изменений
//    private void setupButtonAnimation(Button button) {
//        button.setOnTouchListener((v, event) -> {
//            switch (event.getAction()) {
//                case MotionEvent.ACTION_DOWN:
//                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
//                    break;
//                case MotionEvent.ACTION_UP:
//                case MotionEvent.ACTION_CANCEL:
//                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
//                    break;
//            }
//            return false;
//        });
//    }

    private TextView createCarInfoTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(18f);
        textView.setTextColor(Color.parseColor("#2260FF"));
        textView.setPadding(0, 4, 0, 4);
        textView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        return textView;
    }

//    private void setupRoundButton(Button button, String text) {
//        GradientDrawable buttonShape = new GradientDrawable();
//        buttonShape.setShape(GradientDrawable.RECTANGLE);
//        buttonShape.setCornerRadius(30f);
//        buttonShape.setColor(Color.parseColor("#2260FF"));
//
//        button.setText(text);
//        button.setTextColor(Color.WHITE);
//        button.setBackground(buttonShape);
//        button.setAllCaps(false);
//        button.setGravity(Gravity.CENTER);
//
//        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                dpToPx(100),
//                dpToPx(25)
//        );
//        params.setMargins(0, 8, 8, 0);
//        button.setLayoutParams(params);
//        button.setPadding(0, 0, 0, 0);
//    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void deleteCar(int carId, RelativeLayout card) {
        // Создаем кастомное диалоговое окно
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_car, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Делаем прозрачный фон для скругленных углов
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        confirmButton.setOnClickListener(v -> {
            String token;
            try {
                token = new EncryptedSharedPrefs(GarageActivity.this).getAccessToken();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка получения токена: " + e.getMessage());
                dialog.dismiss();
                return;
            }

            ApiClient.getInstance().deleteCar(carId, token, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(GarageActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            carContainer.removeView(card);
                            if (carContainer.getChildCount() == 0) {
                                emptyStateContainer.setVisibility(View.VISIBLE);
                            }
                            Toast.makeText(GarageActivity.this, "Автомобиль удален", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(GarageActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                        }
                        dialog.dismiss();
                    });
                }
            });
        });

        dialog.show();
    }

    private void loadCarImage(String imageUrl, ImageView imageView) {
        try {
            String baseUrl = "https://automser.store/";
            String cleanPath = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
            String fullImageUrl = baseUrl + cleanPath;

            Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.rounded_backgroun)
                    .centerCrop()
                    .into(imageView);
        } catch (Exception e) {
            Log.e("GarageActivity", "Error loading car image: " + e.getMessage());
            imageView.setImageResource(R.drawable.rounded_backgroun);
        }
    }


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