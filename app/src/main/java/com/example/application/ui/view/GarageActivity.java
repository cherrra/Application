package com.example.application.ui.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
    private TextView pustoTextView;
    private CarViewModel carViewModel;
    private static final String TAG = "GarageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_garage);

        carContainer = findViewById(R.id.carContainer);
        pustoTextView = findViewById(R.id.pustoTextView);
        carViewModel = new ViewModelProvider(this).get(CarViewModel.class);

        Button addCarButton = findViewById(R.id.addCarButton);
        setupButtonAnimation(addCarButton);

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
                pustoTextView.setVisibility(View.GONE);
                updateUI(cars);
            } else {
                Log.d(TAG, "Нет автомобилей в гараже");
                pustoTextView.setVisibility(View.VISIBLE);
                carContainer.removeAllViews();
            }
        });
    }

    private void updateUI(List<Car> cars) {
        carContainer.removeAllViews();
        for (Car car : cars) {
            addCarCard(car);
        }
    }

    private void addCarCard(Car car) {
        RelativeLayout card = new RelativeLayout(this);
        card.setPadding(24, 24, 24, 24);

        GradientDrawable cardShape = new GradientDrawable();
        cardShape.setShape(GradientDrawable.RECTANGLE);
        cardShape.setCornerRadius(55f);
        cardShape.setColor(Color.parseColor("#80CAD6FF"));
        cardShape.setStroke(2, Color.parseColor("#CAD6FF"));
        card.setBackground(cardShape);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(8, 8, 8, 16);
        card.setLayoutParams(cardParams);

        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        infoLayout.setId(View.generateViewId());

        RelativeLayout.LayoutParams infoParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        infoParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        infoParams.addRule(RelativeLayout.CENTER_VERTICAL);
        infoLayout.setLayoutParams(infoParams);

        TextView carView = createCarInfoTextView(car.getBrand() + " " + car.getModel());
        infoLayout.addView(carView);

        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(16)
        );
        spacer.setLayoutParams(spacerParams);
        infoLayout.addView(spacer);

        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setGravity(Gravity.BOTTOM);

        Button detailsButton = new Button(this);
        setupRoundButton(detailsButton, "подробнее");
        detailsButton.setOnClickListener(v -> {
            String carJson = new Gson().toJson(car);
            Intent intent = new Intent(GarageActivity.this, DetailsCarActivity.class);
            intent.putExtra("carDetails", carJson);
            startActivity(intent);
        });

        setupButtonAnimation(detailsButton);
        buttonsLayout.addView(detailsButton);

        Button deleteButton = new Button(this);
        setupRoundButton(deleteButton, "удалить");
        deleteButton.setOnClickListener(v -> deleteCar(car.getIdCar(), card));

        setupButtonAnimation(deleteButton);
        buttonsLayout.addView(deleteButton);

        infoLayout.addView(buttonsLayout);
        card.addView(infoLayout);

        ImageView carImagePlaceholder = new ImageView(this);
        GradientDrawable placeholderShape = new GradientDrawable();
        placeholderShape.setShape(GradientDrawable.RECTANGLE);
        placeholderShape.setCornerRadius(16f);
        placeholderShape.setColor(Color.parseColor("#B0C6FF"));
        carImagePlaceholder.setBackground(placeholderShape);

        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
                dpToPx(100),
                dpToPx(150)
        );
        imageParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        imageParams.addRule(RelativeLayout.CENTER_VERTICAL);
        imageParams.setMargins(16, 0, 0, 0);
        carImagePlaceholder.setLayoutParams(imageParams);
        carImagePlaceholder.setScaleType(ImageView.ScaleType.CENTER_CROP);

        card.addView(carImagePlaceholder);

        carContainer.addView(card);
    }


    private void setupButtonAnimation(Button button) {
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

    private TextView createCarInfoTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(18f);
        textView.setTextColor(Color.parseColor("#2260FF"));
        textView.setPadding(0, 4, 0, 4);
        textView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        return textView;
    }

    private void setupRoundButton(Button button, String text) {
        GradientDrawable buttonShape = new GradientDrawable();
        buttonShape.setShape(GradientDrawable.RECTANGLE);
        buttonShape.setCornerRadius(30f);
        buttonShape.setColor(Color.parseColor("#2260FF"));

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackground(buttonShape);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(100),
                dpToPx(25)
        );
        params.setMargins(0, 8, 8, 0);
        button.setLayoutParams(params);
        button.setPadding(0, 0, 0, 0);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void deleteCar(int carId, RelativeLayout card) {
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