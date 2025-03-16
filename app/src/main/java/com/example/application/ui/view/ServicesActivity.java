package com.example.application.ui.view;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
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

        try {
            token = new EncryptedSharedPrefs(this).getToken();
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
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);
        card.setBackgroundColor(Color.parseColor("#E0E0E0"));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        params.setMargins(16, 16, 16, 16);
        card.setLayoutParams(params);

        TextView nameView = new TextView(this);
        nameView.setText(service.getServiceName());
        nameView.setTextSize(16f);
        nameView.setTextColor(Color.BLACK);
        card.addView(nameView);

        TextView priceView = new TextView(this);
        priceView.setText("Цена: " + service.getPrice() + " ₽");
        priceView.setTextSize(14f);
        priceView.setTextColor(Color.GRAY);
        card.addView(priceView);

        Button buyButton = new Button(this);
        buyButton.setText("Выбрать");
        buyButton.setOnClickListener(v -> {
            saveSelectedService(service);
            updateTotalPrice();
            Toast.makeText(this, "Услуга добавлена в заказ", Toast.LENGTH_SHORT).show();
        });

        card.addView(buyButton);
        servicesContainer.addView(card);
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
