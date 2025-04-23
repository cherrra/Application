package com.example.application.ui.view;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.R;
import com.example.application.data.model.Car;
import com.example.application.ui.viewmodel.CarViewModel;
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
    private Button bookButton, clearButton;
    private TextView totalPriceView;

    private List<Car> userCars = new ArrayList<>();
    private List<String> selectedServices = new ArrayList<>();
    private double totalPrice = 0;
    private Calendar calendar = Calendar.getInstance();
    private SharedPreferences sharedPreferences;
    private CarViewModel carViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_order);

        carViewModel = new ViewModelProvider(this).get(CarViewModel.class);
        initViews();
        setupDateTimePickers();
        setupButtons();

        try {
            String token = new EncryptedSharedPrefs(this).getToken();
            if (token != null) {
                loadUserCars(token);
                loadSelectedServices();
            } else {
                showToast("Требуется авторизация");
                finish();
            }
        } catch (Exception e) {
            Log.e("AddOrderActivity", "Ошибка инициализации", e);
            showToast("Ошибка инициализации");
            finish();
        }
    }

    private void initViews() {
        carSpinner = findViewById(R.id.carSpinner);
        selectedServicesContainer = findViewById(R.id.selectedServicesContainer);
        dateInput = findViewById(R.id.dateInput);
        timeInput = findViewById(R.id.timeInput);
        commentInput = findViewById(R.id.commentInput);
        bookButton = findViewById(R.id.bookButton);
        clearButton = findViewById(R.id.clearServicesButton);
        totalPriceView = findViewById(R.id.totalPriceView);
        sharedPreferences = getSharedPreferences("OrderPrefs", MODE_PRIVATE);
    }

    private void loadUserCars(String token) {
        carViewModel.getCars(token).observe(this, cars -> {
            if (cars != null && !cars.isEmpty()) {
                userCars = cars;
                updateCarSpinner();
            } else {
                showToast("У вас нет автомобилей в гараже");
                finish();
            }
        });
    }

    private void updateCarSpinner() {
        List<String> carDisplayNames = new ArrayList<>();
        for (Car car : userCars) {
            // Формируем строку для отображения: "Бренд Модель"
            String displayName = car.getModel().getBrand().getBrandName() + " " + car.getModel().getModelName();
            carDisplayNames.add(displayName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                carDisplayNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        carSpinner.setAdapter(adapter);
    }

    private void setupDateTimePickers() {
        // Date Picker
        dateInput.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                calendar.set(year, month, day);
                updateDateInput();
            },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH))
                    .show();
        });

        // Time Picker
        timeInput.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                updateTimeInput();
            },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true)
                    .show();
        });
    }

    private void updateDateInput() {
        dateInput.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                .format(calendar.getTime()));
    }

    private void updateTimeInput() {
        timeInput.setText(new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(calendar.getTime()));
    }

    private void setupButtons() {
        bookButton.setOnClickListener(v -> submitOrder());
        clearButton.setOnClickListener(v -> clearServices());
    }

    private void loadSelectedServices() {
        selectedServices.clear();
        selectedServicesContainer.removeAllViews();
        totalPrice = 0;

        String servicesJson = sharedPreferences.getString("selectedServices", "[]");
        try {
            JSONArray services = new JSONArray(servicesJson);
            for (int i = 0; i < services.length(); i++) {
                JSONObject service = services.getJSONObject(i);
                addServiceView(
                        service.getString("service_name"),
                        service.getDouble("price"),
                        i
                );
            }
        } catch (JSONException e) {
            Log.e("AddOrderActivity", "Ошибка загрузки услуг", e);
        }

        updateTotalPrice();
    }

    private void addServiceView(String name, double price, int index) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);

        TextView tv = new TextView(this);
        tv.setText(name + " - " + price + " ₽");
        tv.setTextColor(Color.BLACK);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        Button btn = new Button(this);
        btn.setText("X");
        btn.setTextColor(Color.parseColor("#2260FF"));
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setOnClickListener(v -> removeService(layout, index, price));

        layout.addView(tv);
        layout.addView(btn);
        selectedServicesContainer.addView(layout);

        selectedServices.add(name);
        totalPrice += price;
    }

    private void removeService(View view, int index, double price) {
        selectedServicesContainer.removeView(view);
        selectedServices.remove(index);
        totalPrice -= price;
        updateTotalPrice();
        saveServices();
    }

    private void updateTotalPrice() {
        totalPriceView.setText("Предварительная стоимость: " + String.format(Locale.getDefault(), "%.2f ₽*", totalPrice));
    }

    private void saveServices() {
        sharedPreferences.edit()
                .putString("selectedServices", new JSONArray(selectedServices).toString())
                .putFloat("totalPrice", (float) totalPrice)
                .apply();
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
        showToast("Услуги очищены");
    }

    private void submitOrder() {
        if (carSpinner.getSelectedItemPosition() < 0 || userCars.isEmpty()) {
            showToast("Выберите автомобиль");
            return;
        }

        if (selectedServices.isEmpty()) {
            showToast("Добавьте услуги");
            return;
        }

        if (dateInput.getText().toString().isEmpty() || timeInput.getText().toString().isEmpty()) {
            showToast("Укажите дату и время");
            return;
        }

        try {
            String token = new EncryptedSharedPrefs(this).getToken();
            if (token == null) {
                showToast("Требуется авторизация");
                return;
            }

            // Получаем выбранный автомобиль
            Car selectedCar = userCars.get(carSpinner.getSelectedItemPosition());

            JSONObject order = new JSONObject();
            order.put("id_car", selectedCar.getIdCar());
            order.put("order_date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(calendar.getTime()));
            order.put("order_time", timeInput.getText().toString());
            order.put("comment", commentInput.getText().toString());
            order.put("total_price", totalPrice);
            order.put("services", new JSONArray(selectedServices));

            sendOrder(order, token);
        } catch (Exception e) {
            Log.e("AddOrderActivity", "Ошибка создания заказа", e);
            showToast("Ошибка создания заказа");
        }
    }

    private void sendOrder(JSONObject order, String token) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                order.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/orders")
                .addHeader("Authorization", token)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> showToast("Ошибка соединения"));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        showToast("Запись создана");
                        finish();
                    });
                } else {
                    runOnUiThread(() -> showToast("Ошибка сервера: " + response.code()));
                }
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}