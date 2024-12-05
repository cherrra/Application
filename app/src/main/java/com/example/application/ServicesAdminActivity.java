package com.example.application;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ServicesAdminActivity extends AppCompatActivity {
    private int categoryId;
    private GridLayout servicesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services_admin);

        categoryId = getIntent().getIntExtra("id_category", -1);

        servicesContainer = findViewById(R.id.servicesContainer);

        Button addButton = findViewById(R.id.addServiceButton);
        addButton.setOnClickListener(v -> showAddServiceDialog());

        if (categoryId != -1) {
            fetchServices(categoryId);
        } else {
            Toast.makeText(this, "Ошибка: категория не выбрана", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchServices(int categoryId) {
        OkHttpClient client = new OkHttpClient();

        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            Log.e("ServicesAdminActivity", "Ошибка получения токена: " + e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/services?id_category=" + categoryId)
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ServicesAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
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
                        runOnUiThread(() -> Toast.makeText(ServicesAdminActivity.this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ServicesAdminActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show());
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

    TextView nameView = new TextView(this);
    nameView.setText(serviceObject.getString("service_name"));
    nameView.setTextSize(16f);
    nameView.setTextColor(Color.BLACK);
    card.addView(nameView);

    TextView priceView = new TextView(this);
    priceView.setText("Цена: " + serviceObject.getString("price") + " ₽");
    priceView.setTextSize(14f);
    priceView.setTextColor(Color.GRAY);
    card.addView(priceView);

    TextView descriptionView = new TextView(this);
    descriptionView.setText(serviceObject.getString("description"));
    descriptionView.setTextSize(14f);
    descriptionView.setTextColor(Color.DKGRAY);
    descriptionView.setVisibility(View.GONE);
    card.addView(descriptionView);

    Button detailsButton = new Button(this);
    detailsButton.setText("Подробнее");
    detailsButton.setOnClickListener(v -> {
        if (descriptionView.getVisibility() == View.GONE) {
            descriptionView.setVisibility(View.VISIBLE);
            detailsButton.setText("Скрыть");
        } else {
            descriptionView.setVisibility(View.GONE);
            detailsButton.setText("Подробнее");
        }
    });
    card.addView(detailsButton);

    Button deleteButton = new Button(this);
    deleteButton.setText("Удалить");
    deleteButton.setOnClickListener(v -> deleteService(serviceObject, card));
    card.addView(deleteButton);

    Button editButton = new Button(this);
    editButton.setText("Редактировать");
    editButton.setOnClickListener(v -> showEditDialog(serviceObject, nameView, priceView, descriptionView));
    card.addView(editButton);

    servicesContainer.addView(card);
}

    private void showAddServiceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить услугу");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Название услуги");
        layout.addView(nameInput);

        EditText priceInput = new EditText(this);
        priceInput.setHint("Цена");
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(priceInput);

        EditText descriptionInput = new EditText(this);
        descriptionInput.setHint("Описание");
        layout.addView(descriptionInput);

        builder.setView(layout);

        builder.setPositiveButton("Добавить", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String price = priceInput.getText().toString();
            String description = descriptionInput.getText().toString();

            if (!name.isEmpty() && !price.isEmpty() && !description.isEmpty()) {
                addService(name, price, description);
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void addService(String name, String price, String description) {
        OkHttpClient client = new OkHttpClient();

        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            Log.e("ServicesAdminActivity", "Ошибка получения токена: " + e.getMessage());
            return;
        }

        JSONObject newServiceData = new JSONObject();
        try {
            newServiceData.put("service_name", name);
            newServiceData.put("price", price);
            newServiceData.put("description", description);
            newServiceData.put("id_category", categoryId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(newServiceData.toString(), MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/services")
                .post(body)
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ServicesAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(ServicesAdminActivity.this, "Услуга добавлена", Toast.LENGTH_SHORT).show();
                        fetchServices(categoryId); // Обновляем список услуг
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(ServicesAdminActivity.this, "Ошибка добавления услуги", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void deleteService(JSONObject serviceObject, View card) {
        OkHttpClient client = new OkHttpClient();

        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            Log.e("ServicesAdminActivity", "Ошибка получения токена: " + e.getMessage());
            return;
        }

        try {
            int serviceId = serviceObject.getInt("id_service");

            Request request = new Request.Builder()
                    .url("http://10.0.2.2:5000/api/services/" + serviceId)
                    .delete()
                    .addHeader("Authorization", token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> Toast.makeText(ServicesAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            servicesContainer.removeView(card);
                            Toast.makeText(ServicesAdminActivity.this, "Услуга успешно удалена", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(ServicesAdminActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showEditDialog(JSONObject serviceObject, TextView nameView, TextView priceView, TextView descriptionView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Редактировать услугу");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Название услуги");
        nameInput.setText(nameView.getText().toString());
        layout.addView(nameInput);

        EditText priceInput = new EditText(this);
        priceInput.setHint("Цена");
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        priceInput.setText(priceView.getText().toString().replace("Цена: ", "").replace(" ₽", ""));
        layout.addView(priceInput);

        EditText descriptionInput = new EditText(this);
        descriptionInput.setHint("Описание");
        descriptionInput.setText(descriptionView.getText().toString());
        layout.addView(descriptionInput);

        builder.setView(layout);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            try {
                String updatedName = nameInput.getText().toString();
                String updatedPrice = priceInput.getText().toString();
                String updatedDescription = descriptionInput.getText().toString();

                updateService(serviceObject.getInt("id_service"), updatedName, updatedPrice, updatedDescription);

                nameView.setText(updatedName);
                priceView.setText("Цена: " + updatedPrice + " ₽");
                descriptionView.setText(updatedDescription);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void updateService(int serviceId, String name, String price, String description) {
        OkHttpClient client = new OkHttpClient();

        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            Log.e("ServicesAdminActivity", "Ошибка получения токена: " + e.getMessage());
            return;
        }

        JSONObject updateData = new JSONObject();
        try {
            updateData.put("service_name", name);
            updateData.put("price", price);
            updateData.put("description", description);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(updateData.toString(), MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/services/" + serviceId)
                .put(body)
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ServicesAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(ServicesAdminActivity.this, "Услуга обновлена", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(ServicesAdminActivity.this, "Ошибка обновления", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
