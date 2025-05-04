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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private LinearLayout categoryContainer;
    private CategoryViewModel categoryViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        WebView webView = findViewById(R.id.webview);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        String url = "https://yandex.ru/maps/?ll=37.383833%2C55.725686&z=15&pt=37.383833,55.725686,pm2rdl&l=map";
        webView.loadUrl(url);

        ImageButton notificationButton = findViewById(R.id.notificationButton);
        notificationButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, NotificationActivity.class);
            startActivity(intent);
        });


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
            String token = encryptedSharedPrefs.getAccessToken();
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
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(24, 24, 24, 24);
        card.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(55f);
        shape.setColor(Color.parseColor("#80CAD6FF"));
        shape.setStroke(2, Color.parseColor("#CAD6FF"));
        card.setBackground(shape);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(8, 8, 8, 8);
        card.setLayoutParams(cardParams);

        ImageView iconView = new ImageView(this);
        iconView.setImageResource(R.drawable.ic_category);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(160, 160);
        iconParams.setMargins(0, 0, 16, 0);
        iconView.setLayoutParams(iconParams);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        card.addView(iconView);

        TextView nameView = new TextView(this);
        nameView.setText(category.getCategoryName());
        nameView.setTextSize(16f);
        nameView.setTextColor(Color.parseColor("#2260FF"));
        nameView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        nameView.setMaxLines(3);
        nameView.setEllipsize(null);
        nameView.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
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
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
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
