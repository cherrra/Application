package com.example.application.ui.view;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.R;
import com.example.application.data.model.Service;
import com.example.application.ui.viewmodel.ServiceViewModel;
import com.example.application.utils.EncryptedSharedPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ServicesActivity extends AppCompatActivity {
    private GridLayout servicesContainer;
    private ServiceViewModel serviceViewModel;
    private ArrayList<Integer> selectedServiceIds = new ArrayList<>();
    private double totalPrice = 0;
    private SharedPreferences sharedPreferences;
    private String token;
    private int categoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);

        categoryId = getIntent().getIntExtra("id_category", -1);
        servicesContainer = findViewById(R.id.servicesContainer);
        sharedPreferences = getSharedPreferences("OrderPrefs", MODE_PRIVATE);
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

        try {
            token = new EncryptedSharedPrefs(this).getAccessToken();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка получения токена", Toast.LENGTH_SHORT).show();
            return;
        }

        serviceViewModel = new ViewModelProvider(this).get(ServiceViewModel.class);
        observeServices();
    }

    private void observeServices() {
        serviceViewModel.getServices(categoryId, token).observe(this, services -> {
            if (services != null) {
                updateServiceUI(services);
            } else {
                Toast.makeText(this, "Ошибка загрузки услуг", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateServiceUI(List<Service> services) {
        servicesContainer.removeAllViews();
        for (Service service : services) {
            addServiceCard(service);
        }
    }

    private void addServiceCard(Service service) {
        // Создаем контейнер для карточки
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 32, 32, 32);
        card.setGravity(Gravity.CENTER); // Центрируем содержимое

        // Фон с закругленными углами
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(55f);
        shape.setColor(Color.parseColor("#80CAD6FF"));
        card.setBackground(shape);

        // Параметры карточки
        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.width = 0;
        cardParams.height = dpToPx(285);
        cardParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardParams.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        card.setLayoutParams(cardParams);

        // Название услуги
        TextView nameView = new TextView(this);
        nameView.setText(service.getServiceName());
        nameView.setTextSize(18f);
        nameView.setTextColor(Color.parseColor("#2260FF"));
        nameView.setGravity(Gravity.CENTER);
        nameView.setTypeface(null, Typeface.BOLD);
        card.addView(nameView);

        // Описание услуги (если есть)
        if (service.getDescription() != null && !service.getDescription().isEmpty()) {
            TextView descView = new TextView(this);
            descView.setText(service.getDescription());
            descView.setTextSize(14f);
            descView.setTextColor(Color.BLACK);
            descView.setGravity(Gravity.CENTER);
            descView.setPadding(0, dpToPx(8), 0, dpToPx(8));
            card.addView(descView);
        }

        // Цена
        TextView priceView = new TextView(this);
        priceView.setText(service.getPrice() + " ₽");
        priceView.setTextSize(16f);
        priceView.setTextColor(Color.parseColor("#2260FF"));
        priceView.setGravity(Gravity.CENTER);
        priceView.setTypeface(null, Typeface.BOLD);
        card.addView(priceView);

        // Кнопка в виде кружка с плюсом
        ImageView addButton = new ImageView(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
        );
        btnParams.gravity = Gravity.CENTER;
        btnParams.setMargins(0, dpToPx(16), 0, 0);
        addButton.setLayoutParams(btnParams);

        // Круглая кнопка с плюсом
        GradientDrawable btnShape = new GradientDrawable();
        btnShape.setShape(GradientDrawable.OVAL);
        btnShape.setColor(Color.parseColor("#902260FF"));
        addButton.setBackground(btnShape);

        addButton.setImageResource(android.R.drawable.ic_input_add);
        addButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addButton.setColorFilter(Color.WHITE);

        addButton.setOnClickListener(v -> {
            saveSelectedService(service);
            updateTotalPrice();

            v.animate()
                    .scaleX(0.8f).scaleY(0.8f)
                    .setDuration(100)
                    .withEndAction(() -> v.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(100)
                            .start())
                    .start();

            Toast.makeText(this, service.getServiceName() + " добавлена", Toast.LENGTH_SHORT).show();
        });

        card.addView(addButton);
        servicesContainer.addView(card);
    }


    // Вспомогательный метод для преобразования dp в пиксели
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    private void saveSelectedService(Service service) {
        try {
            String servicesJson = sharedPreferences.getString("selectedServices", "[]");
            JSONArray servicesArray = new JSONArray(servicesJson);

            JSONObject newService = new JSONObject();
            newService.put("id_service", service.getIdService());
            newService.put("service_name", service.getServiceName());
            newService.put("price", service.getPrice());

            servicesArray.put(newService);
            sharedPreferences.edit().putString("selectedServices", servicesArray.toString()).apply();
            totalPrice += service.getPrice();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при добавлении услуги", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotalPrice() {
        TextView totalPriceView = findViewById(R.id.totalPriceView);
        totalPriceView.setText("Добавлена услуга на сумму: " + totalPrice + " ₽");
    }
}
