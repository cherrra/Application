package com.example.application.ui.view;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
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
import com.example.application.data.model.Service;
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

    private static final String TAG = "AddOrderActivity";
    private Spinner carSpinner;
    private LinearLayout selectedServicesContainer;
    private EditText dateInput, timeInput, commentInput;
    private Button bookButton, clearButton;
    private TextView totalPriceView;

    private List<Car> userCars = new ArrayList<>();
    private List<Service> selectedServices = new ArrayList<>();
    private double totalPrice = 0;
    private Calendar calendar = Calendar.getInstance();
    private SharedPreferences sharedPreferences;
    private CarViewModel carViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_order);

        Log.d(TAG, "Инициализация активности");

        carViewModel = new ViewModelProvider(this).get(CarViewModel.class);
        initViews();
        setupDateTimePickers();
        setupButtons();

        try {
            EncryptedSharedPrefs encryptedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedPrefs.getAccessToken();
            Log.d(TAG, "Полученный токен: " + (token != null ? "*****" : "null"));

            if (token == null || token.isEmpty()) {
                Log.w(TAG, "Токен отсутствует или пуст");
                handleAuthError();
                return;
            }

            loadUserCars(token);
            loadSelectedServices();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации", e);
            showToast("Ошибка инициализации");
            handleAuthError();
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
        Log.d(TAG, "Загрузка автомобилей пользователя");
        carViewModel.getCars(token).observe(this, cars -> {
            if (cars != null && !cars.isEmpty()) {
                Log.d(TAG, "Получено автомобилей: " + cars.size());
                userCars = cars;
                updateCarSpinner();
            } else {
                Log.w(TAG, "Автомобили не найдены");
                showToast("У вас нет автомобилей в гараже");
                finish();
            }
        });
    }

    private void updateCarSpinner() {
        List<String> carDisplayNames = new ArrayList<>();
        for (Car car : userCars) {
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
        String date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.getTime());
        dateInput.setText(date);
        Log.d(TAG, "Установлена дата: " + date);
    }

    private void updateTimeInput() {
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.getTime());
        timeInput.setText(time);
        Log.d(TAG, "Установлено время: " + time);
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
        Log.d(TAG, "Загружаемые сервисы: " + servicesJson);

        try {
            JSONArray services = new JSONArray(servicesJson);
            for (int i = 0; i < services.length(); i++) {
                JSONObject serviceJson = services.getJSONObject(i);
                int idService = serviceJson.getInt("id_service");
                String serviceName = serviceJson.getString("service_name");
                double price = serviceJson.getDouble("price");

                Service service = new Service(idService, serviceName, "", price, 0);
                addServiceView(service, i);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка загрузки услуг", e);
        }

        updateTotalPrice();
    }

    private void addServiceView(Service service, int index) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);

        TextView tv = new TextView(this);
        tv.setText(service.getServiceName() + " - " + service.getPrice() + " ₽");
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
        btn.setOnClickListener(v -> removeService(layout, index, service.getPrice()));

        layout.addView(tv);
        layout.addView(btn);
        selectedServicesContainer.addView(layout);

        selectedServices.add(service);
        totalPrice += service.getPrice();
        Log.d(TAG, "Добавлен сервис: " + service.getServiceName() + " (id: " + service.getIdService() + ", цена: " + service.getPrice() + ")");
    }

    private void removeService(View view, int index, double price) {
        selectedServicesContainer.removeView(view);
        selectedServices.remove(index);
        totalPrice -= price;
        updateTotalPrice();
        saveServices();
    }

    private void updateTotalPrice() {
        String formattedPrice = String.format(Locale.getDefault(), "%.2f ₽*", totalPrice);
        totalPriceView.setText("Предварительная стоимость: " + formattedPrice);
        Log.d(TAG, "Обновлена общая стоимость: " + formattedPrice);
    }

    private void saveServices() {
        JSONArray servicesArray = new JSONArray();
        for (Service service : selectedServices) {
            JSONObject serviceObj = new JSONObject();
            try {
                serviceObj.put("id_service", service.getIdService());
                serviceObj.put("service_name", service.getServiceName());
                serviceObj.put("price", service.getPrice());
                servicesArray.put(serviceObj);
            } catch (JSONException e) {
                Log.e(TAG, "Ошибка сохранения услуги", e);
            }
        }

        sharedPreferences.edit()
                .putString("selectedServices", servicesArray.toString())
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
        Log.d(TAG, "Все сервисы очищены");
    }

    private void submitOrder() {
        Log.d(TAG, "Попытка создания заказа");

        if (carSpinner.getSelectedItemPosition() < 0 || userCars.isEmpty()) {
            Log.w(TAG, "Не выбран автомобиль");
            showToast("Выберите автомобиль");
            return;
        }

        if (selectedServices.isEmpty()) {
            Log.w(TAG, "Не выбраны услуги");
            showToast("Добавьте услуги");
            return;
        }

        if (dateInput.getText().toString().isEmpty() || timeInput.getText().toString().isEmpty()) {
            Log.w(TAG, "Не указаны дата/время");
            showToast("Укажите дату и время");
            return;
        }

        try {
            EncryptedSharedPrefs encryptedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedPrefs.getAccessToken();
            Log.d(TAG, "Токен для запроса: " + (token != null ? "*****" : "null"));

            if (token == null || token.isEmpty()) {
                Log.w(TAG, "Токен недействителен");
                handleAuthError();
                return;
            }

            Car selectedCar = userCars.get(carSpinner.getSelectedItemPosition());
            Log.d(TAG, "Выбран автомобиль ID: " + selectedCar.getIdCar());

            JSONArray servicesArray = new JSONArray();
            for (Service service : selectedServices) {
                JSONObject serviceObj = new JSONObject();
                serviceObj.put("id_service", service.getIdService());
                serviceObj.put("price", service.getPrice());
                servicesArray.put(serviceObj);
            }

            JSONObject order = new JSONObject();
            order.put("id_car", selectedCar.getIdCar());
            String orderDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
            order.put("order_date", orderDate);
            order.put("order_time", timeInput.getText().toString());
            order.put("comment", commentInput.getText().toString());
            order.put("total_price", totalPrice);
            order.put("services", servicesArray);

            Log.d(TAG, "Формируемый заказ: " + order.toString());
            sendOrder(order, token);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка создания заказа", e);
            showToast("Ошибка создания заказа: " + e.getMessage());
        }
    }

    private void sendOrder(JSONObject order, String token) {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(
                order.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://automser.store/api/orders")
                .addHeader("Authorization", "Bearer " + token)
                .post(body)
                .build();

        Log.d(TAG, "Отправка запроса на: " + request.url());
        Log.d(TAG, "Заголовки запроса: " + request.headers());
        Log.d(TAG, "Тело запроса: " + order.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Ошибка сети", e);
                runOnUiThread(() -> showToast("Ошибка соединения: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";
                Log.d(TAG, "Ответ сервера. Код: " + response.code() + ", Тело: " + responseBody);

                runOnUiThread(() -> {
                    try {
                        if (response.isSuccessful()) {
                            showToast("Запись успешно создана");
                            clearServices(); // Очищаем выбранные услуги после успешного создания
                            finish();
                        } else {
                            handleServerError(response.code(), responseBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка обработки ответа", e);
                        showToast("Ошибка обработки ответа сервера");
                    }
                });
            }
        });
    }

    private void handleServerError(int code, String responseBody) {
        try {
            JSONObject errorResponse = new JSONObject(responseBody);
            String errorMessage = errorResponse.optString("message", "Неизвестная ошибка сервера");

            switch (code) {
                case 400:
                    Log.w(TAG, "Ошибка 400: " + errorMessage);
                    showToast("Неверный запрос: " + errorMessage);
                    break;

                case 401:
                    Log.w(TAG, "Ошибка 401: " + errorMessage);
                    handleAuthError();
                    break;

                case 500:
                    Log.e(TAG, "Ошибка 500: " + errorMessage);
                    showToast("Ошибка сервера: " + errorMessage);
                    break;

                default:
                    Log.e(TAG, "Неизвестная ошибка " + code + ": " + errorMessage);
                    showToast("Ошибка сервера (" + code + "): " + errorMessage);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Ошибка парсинга ответа сервера", e);
            showToast("Ошибка сервера " + code + ": Не удалось обработать ответ");
        }
    }

    private void handleAuthError() {
        showToast("Требуется авторизация");
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}