package com.example.application;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServicesActivity extends AppCompatActivity {
    private int categoryId;
    private GridLayout servicesContainer;
    private ArrayList<Integer> selectedServiceIds = new ArrayList<>();
    private double totalPrice = 0;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);

        categoryId = getIntent().getIntExtra("id_category", -1);

        servicesContainer = findViewById(R.id.servicesContainer);

        if (categoryId != -1) {
            fetchServices(categoryId);
        } else {
            Toast.makeText(this, "Ошибка: категория не выбрана", Toast.LENGTH_SHORT).show();
        }

        sharedPreferences = getSharedPreferences("OrderPrefs", MODE_PRIVATE);

    }

    private void fetchServices(int categoryId) {
        OkHttpClient client = new OkHttpClient();

        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            Log.e("ServicesActivity", "Ошибка получения токена: " + e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/services?id_category=" + categoryId)
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ServicesActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray servicesArray = new JSONArray(response.body().string());
                        runOnUiThread(() -> {
                            servicesContainer.removeAllViews();
                            for (int i = 0; i < servicesArray.length(); i++) {
                                try {
                                    JSONObject serviceObject = servicesArray.getJSONObject(i);
                                    addServiceCard(serviceObject);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() ->
                                Toast.makeText(ServicesActivity.this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(ServicesActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void addServiceCard(JSONObject serviceObject) throws JSONException {
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

        String serviceName = serviceObject.getString("service_name");
        double price = serviceObject.getDouble("price");
        int serviceId = serviceObject.getInt("id_service");

        TextView nameView = new TextView(this);
        nameView.setText(serviceName);
        nameView.setTextSize(16f);
        nameView.setTextColor(Color.BLACK);
        card.addView(nameView);

        TextView priceView = new TextView(this);
        priceView.setText("Цена: " + price + " ₽");
        priceView.setTextSize(14f);
        priceView.setTextColor(Color.GRAY);
        card.addView(priceView);

        Button buyButton = new Button(this);
        buyButton.setText("Выбрать");

        buyButton.setOnClickListener(v -> {
            saveSelectedService(serviceId, serviceName, price);
            updateTotalPrice();
            Toast.makeText(this, "Услуга добавлена в заказ", Toast.LENGTH_SHORT).show();
        });
        card.addView(buyButton);

        servicesContainer.addView(card);
    }

   private void saveSelectedService(int serviceId, String serviceName, double price) {
        try {
            String servicesJson = sharedPreferences.getString("selectedServices", "[]");
            JSONArray servicesArray = new JSONArray(servicesJson);


            JSONObject newService = new JSONObject();
            newService.put("id_service", serviceId);
            newService.put("service_name", serviceName);
            newService.put("price", price);

            servicesArray.put(newService);

            sharedPreferences.edit().putString("selectedServices", servicesArray.toString()).apply();
            totalPrice += price;
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при добавлении услуги", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotalPrice() {
        TextView totalPriceView = findViewById(R.id.totalPriceView);
        totalPriceView.setText("Добавлена услуга на сумму: " + totalPrice + " ₽");
    }
}
