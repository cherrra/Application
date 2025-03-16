package com.example.application.ui.view;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.application.R;
import com.example.application.utils.EncryptedSharedPrefs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddOrderActivity extends AppCompatActivity {

    private Spinner carSpinner;
    private LinearLayout selectedServicesContainer;
    private EditText dateInput, timeInput, commentInput;
    private Button bookButton;
    private EncryptedSharedPrefs encryptedSharedPrefs;
    private SharedPreferences sharedPreferences;
    private List<Integer> carIds = new ArrayList<>();
    private List<String> selectedServices = new ArrayList<>();
    private double totalPrice = 0;
    private TextView totalPriceView;
    private Calendar calendar = Calendar.getInstance();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_order);

        carSpinner = findViewById(R.id.carSpinner);
        selectedServicesContainer = findViewById(R.id.selectedServicesContainer);
        dateInput = findViewById(R.id.dateInput);
        timeInput = findViewById(R.id.timeInput);
        commentInput = findViewById(R.id.commentInput);
        bookButton = findViewById(R.id.bookButton);
        totalPriceView = new TextView(this);
        sharedPreferences = getSharedPreferences("OrderPrefs", MODE_PRIVATE);

        Button clearServicesButton = findViewById(R.id.clearServicesButton);
        clearServicesButton.setOnClickListener(v -> clearServices());

        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();

            if (token != null) {
                fetchCars(token); // Загрузка списка машин
                loadSelectedServices(); // Загрузка выбранных услуг
                bookButton.setOnClickListener(v -> submitOrder(token)); // Отправка заказа
            } else {
                Toast.makeText(this, "Ошибка: токен не найден", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("AddOrderActivity", "Ошибка инициализации: " + e.getMessage());
            Toast.makeText(this, "Ошибка инициализации", Toast.LENGTH_SHORT).show();
            finish();
        }

        dateInput.setOnClickListener(v -> showDatePicker());
        timeInput.setOnClickListener(v -> showTimePicker());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSelectedServices(); // Обновление списка услуг при возвращении на экран
    }

    private void saveTotalPrice() {
        sharedPreferences.edit().putFloat("totalPrice", (float) totalPrice).apply();
    }

    private void clearServices() {
        selectedServices.clear();
        selectedServicesContainer.removeAllViews();
        totalPrice = 0;
        updateTotalPrice();

        sharedPreferences.edit()
                .remove("selectedServices")
                .remove("totalPrice")
                .apply();

        Toast.makeText(this, "Услуги очищены", Toast.LENGTH_SHORT).show();
    }

    private void fetchCars(String token) {
        if (token == null || token.isEmpty()) {
            Toast.makeText(AddOrderActivity.this, "Ошибка: отсутствует токен", Toast.LENGTH_SHORT).show();
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/cars")
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(AddOrderActivity.this, "Ошибка загрузки машин", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray carsArray = new JSONArray(response.body().string());
                        runOnUiThread(() -> {
                            List<String> carNames = new ArrayList<>();
                            carIds.clear();
                            for (int i = 0; i < carsArray.length(); i++) {
                                try {
                                    JSONObject car = carsArray.getJSONObject(i);
                                    carNames.add(car.getString("model"));
                                    carIds.add(car.getInt("id_car"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(AddOrderActivity.this,
                                    android.R.layout.simple_spinner_dropdown_item, carNames);
                            carSpinner.setAdapter(adapter);
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() ->
                                Toast.makeText(AddOrderActivity.this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    if (response.code() == 401) {
                        runOnUiThread(() -> {
                            Toast.makeText(AddOrderActivity.this, "Ошибка авторизации. Пожалуйста, войдите заново.", Toast.LENGTH_SHORT).show();
                            encryptedSharedPrefs.saveToken(null);
                            finish();
                        });
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(AddOrderActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show());
                    }
                }
            }
        });
    }

    private void loadSelectedServices() {
        selectedServices.clear();
        selectedServicesContainer.removeAllViews();

        totalPrice = sharedPreferences.getFloat("totalPrice", 0);

        String servicesJson = sharedPreferences.getString("selectedServices", "[]");
        try {
            JSONArray servicesArray = new JSONArray(servicesJson);
            for (int i = 0; i < servicesArray.length(); i++) {
                JSONObject service = servicesArray.getJSONObject(i);
                String serviceName = service.getString("service_name");
                double price = service.getDouble("price");

                selectedServices.add(service.toString());
                totalPrice += price;

                addServiceToView(serviceName, price, i);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        updateTotalPrice();
    }

    private void addServiceToView(String serviceName, double price, int index) {
        if (price <= 0) return;

        LinearLayout serviceLayout = new LinearLayout(this);
        serviceLayout.setOrientation(LinearLayout.HORIZONTAL);
        serviceLayout.setPadding(0, 10, 0, 10);

        TextView serviceView = new TextView(this);
        serviceView.setText(serviceName + " - " + price + " ₽");
        serviceView.setTextSize(16f);
        serviceView.setTextColor(Color.BLACK);

        Button removeButton = new Button(this);
        removeButton.setText("X");
        removeButton.setTextSize(16f);
        removeButton.setTextColor(Color.RED);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(20, 0, 0, 0);
        removeButton.setLayoutParams(params);

        removeButton.setOnClickListener(v -> {
            removeService(serviceLayout, index, price);
        });

        serviceLayout.addView(serviceView);
        serviceLayout.addView(removeButton);

        selectedServicesContainer.addView(serviceLayout, selectedServicesContainer.getChildCount() - 1);
    }

    private void removeService(LinearLayout serviceLayout, int index, double price) {
        selectedServicesContainer.removeView(serviceLayout);
        selectedServices.remove(index);
        totalPrice -= price;
        updateTotalPrice();
        saveTotalPrice();
        saveSelectedServices();
    }

    private void saveSelectedServices() {
        JSONArray updatedServicesArray = new JSONArray();
        for (String service : selectedServices) {
            try {
                updatedServicesArray.put(new JSONObject(service));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        sharedPreferences.edit().putString("selectedServices", updatedServicesArray.toString()).apply();
    }

    private void updateTotalPrice() {
        totalPriceView.setText("Итого: " + totalPrice + " ₽");
        totalPriceView.setTextSize(18f);
        totalPriceView.setTextColor(Color.BLACK);

        if (selectedServicesContainer.getChildCount() > 0) {
            selectedServicesContainer.removeViewAt(selectedServicesContainer.getChildCount() - 1);
        }
        selectedServicesContainer.addView(totalPriceView);
    }

    private void showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateInput();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePicker = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
            updateTimeInput();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
        timePicker.show();
    }

    private void updateDateInput() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateInput.setText(dateFormat.format(calendar.getTime()));
    }

    private void updateTimeInput() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeInput.setText(timeFormat.format(calendar.getTime()));
    }

    private void submitOrder(String token) {
        int selectedCarIndex = carSpinner.getSelectedItemPosition();
        if (selectedCarIndex < 0 || selectedServices.isEmpty()) {
            Toast.makeText(this, "Выберите машину и услуги", Toast.LENGTH_SHORT).show();
            return;
        }

        int carId = carIds.get(selectedCarIndex);
        String date = dateInput.getText().toString();
        String time = timeInput.getText().toString();
        String comment = commentInput.getText().toString();

        try {
            JSONArray servicesArray = new JSONArray(selectedServices);

            JSONObject orderData = new JSONObject();
            orderData.put("id_car", carId);
            orderData.put("order_date", date);
            orderData.put("order_time", time);
            orderData.put("comment", comment);
            orderData.put("total_price", totalPrice);
            orderData.put("services", servicesArray);

            sendOrderToServer(orderData, token);
        } catch (JSONException e) {
            Toast.makeText(this, "Ошибка подготовки данных", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void sendOrderToServer(JSONObject orderData, String token) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(orderData.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/orders")
                .addHeader("Authorization", token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(AddOrderActivity.this, "Ошибка отправки данных", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        clearFields();
                        Toast.makeText(AddOrderActivity.this, "Заказ успешно создан", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(AddOrderActivity.this, "Ошибка создания заказа", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void clearFields() {
        dateInput.setText("");
        timeInput.setText("");
        commentInput.setText("");

        selectedServicesContainer.removeAllViews();
        selectedServices.clear();

        totalPrice = 0;
        updateTotalPrice();

        sharedPreferences.edit()
                .remove("selectedServices")
                .remove("totalPrice")
                .apply();
    }
}