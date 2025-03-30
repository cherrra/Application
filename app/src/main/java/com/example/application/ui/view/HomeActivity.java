package com.example.application.ui.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.R;
import com.example.application.data.model.Category;
import com.example.application.ui.viewmodel.CategoryViewModel;
import com.example.application.utils.EncryptedSharedPrefs;

import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private GridLayout categoryContainer;
    private CategoryViewModel categoryViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        WebView webView = findViewById(R.id.webview);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        String url = "https://yandex.ru/maps/?ll=37.593471%2C55.721609&z=10&pt=37.593471,55.721609,pm2rdl";
        webView.loadUrl(url);

        TextView homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        TextView accountButton = findViewById(R.id.accountButton);
        accountButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AccountActivity.class);
            startActivity(intent);
        });

        TextView garageButton = findViewById(R.id.garageButton);
        garageButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, GarageActivity.class);
            startActivity(intent);
        });

        TextView orderButton = findViewById(R.id.orderButton);
        orderButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, OrdersActivity.class);
            startActivity(intent);
        });
        categoryContainer = findViewById(R.id.categoryContainer);

        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        try {
            EncryptedSharedPrefs encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();
            if (token != null) {
                observeCategories(token);
            } else {
                Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка инициализации", Toast.LENGTH_SHORT).show();
        }
    }

    private void observeCategories(String token) {
        categoryViewModel.getCategories(token).observe(this, categories -> {
            if (categories != null) {
                updateCategoryUI(categories);
            } else {
                Toast.makeText(this, "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCategoryUI(List<Category> categories) {
        categoryContainer.removeAllViews();
        for (Category category : categories) {
            addCategoryCard(category);
        }
    }

    private void addCategoryCard(Category category) {
        // Создаем контейнер для карточки
        GridLayout card = new GridLayout(this);
        card.setColumnCount(1); // Одна колонка для вертикального расположения
        card.setPadding(24, 24, 24, 24); // Уменьшаем отступы внутри карточки

        // Фон с закругленными углами и прозрачностью 50%
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(55f);
        shape.setColor(Color.parseColor("#80CAD6FF"));
        shape.setStroke(2, Color.parseColor("#CAD6FF"));

        card.setBackground(shape);

        // Параметры карточки - уменьшаем ширину и отступы
        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.width = 0; // Используем 0 для автоматического растягивания
        cardParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        cardParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Равномерное распределение
        cardParams.setMargins(8, 8, 8, 8); // Уменьшаем отступы между карточками
        card.setLayoutParams(cardParams);

        // Добавляем иконку
        ImageView iconView = new ImageView(this);
        iconView.setImageResource(R.drawable.ic_category);

        GridLayout.LayoutParams iconParams = new GridLayout.LayoutParams();
        iconParams.width = 160; // Уменьшаем размер иконки
        iconParams.height = 160;
        iconParams.setGravity(Gravity.CENTER);
        iconParams.setMargins(0, 0, 0, 8); // Уменьшаем отступ снизу

        iconView.setLayoutParams(iconParams);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(iconView);

        // Текст категории
        TextView nameView = new TextView(this);
        nameView.setText(category.getCategoryName());
        nameView.setTextSize(16f); // Уменьшаем размер текста
        nameView.setTextColor(Color.parseColor("#2260FF"));;
        nameView.setGravity(Gravity.CENTER);
        nameView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));


        GridLayout.LayoutParams textParams = new GridLayout.LayoutParams();
        textParams.width = GridLayout.LayoutParams.WRAP_CONTENT;
        textParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        textParams.setGravity(Gravity.CENTER);

        nameView.setLayoutParams(textParams);
        card.addView(nameView);

        // Анимация при нажатии
        card.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    v.performClick();
                    break;
            }
            return true;
        });

        card.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ServicesActivity.class);
            intent.putExtra("id_category", category.getIdCategory());
            serviceLauncher.launch(intent);
        });

        categoryContainer.addView(card);
    }

    private final ActivityResultLauncher<Intent> serviceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    setResult(RESULT_OK, result.getData());
                    finish();
                }
            }
    );
}
