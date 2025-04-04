package com.example.application.ui.view;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.utils.EncryptedSharedPrefs;
import com.example.application.R;
import com.example.application.data.model.Car;
import com.example.application.network.ApiClient;
import com.example.application.ui.viewmodel.CarViewModel;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.GeneralSecurityException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditCarActivity extends AppCompatActivity {

    private CarViewModel carViewModel;
    private Car car;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_car);

        EditText modelEdit = findViewById(R.id.editModel);
        EditText brandEdit = findViewById(R.id.editBrand);
        EditText yearEdit = findViewById(R.id.editYear);
        EditText mileageEdit = findViewById(R.id.editMileage);
        EditText vinCodeEdit = findViewById(R.id.editVinCode);
        EditText licensePlateEdit = findViewById(R.id.editLicensePlate);
        EditText bodyTypeEdit = findViewById(R.id.editBodyType);
        EditText engineTypeEdit = findViewById(R.id.editEngineType);
        Button saveButton = findViewById(R.id.saveButton);
        ImageView backArrow = findViewById(R.id.backArrow);

        setupButtonAnimation(saveButton);

        backArrow.setOnClickListener(v -> {
            finish();
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

        carViewModel = new ViewModelProvider(this).get(CarViewModel.class);

        // Получаем JSON из Intent
        String carDetailsJson = getIntent().getStringExtra("carDetails");

        if (carDetailsJson != null) {
            car = new Gson().fromJson(carDetailsJson, Car.class);
            carViewModel.setSelectedCar(car); // Устанавливаем в ViewModel
        } else {
            car = carViewModel.getSelectedCar().getValue(); // Фолбэк, если JSON пустой
        }

        if (car != null) {
            modelEdit.setText(car.getModel());
            brandEdit.setText(car.getBrand());
            yearEdit.setText(String.valueOf(car.getYear()));
            mileageEdit.setText(String.valueOf(car.getMileage()));
            vinCodeEdit.setText(car.getVinCode());
            licensePlateEdit.setText(car.getLicensePlate());
            bodyTypeEdit.setText(car.getBodyType());
            engineTypeEdit.setText(car.getEngineType());
        }

//        saveButton.setOnClickListener(v -> {
//            try {
//                car.setModel(modelEdit.getText().toString());
//                car.setBrand(brandEdit.getText().toString());
//                car.setYear(Integer.parseInt(yearEdit.getText().toString()));
//                car.setMileage(Integer.parseInt(mileageEdit.getText().toString()));
//                car.setVinCode(vinCodeEdit.getText().toString());
//                car.setLicensePlate(licensePlateEdit.getText().toString());
//                car.setBodyType(bodyTypeEdit.getText().toString());
//                car.setEngineType(engineTypeEdit.getText().toString());
//
//                carViewModel.setSelectedCar(car); // Обновляем данные в ViewModel
//                updateCar(car);
//
//            } catch (Exception e) {
//                Toast.makeText(this, "Ошибка при сохранении", Toast.LENGTH_SHORT).show();
//            }
//        });
        saveButton.setOnClickListener(v -> {
            try {
                // Извлекаем значения из EditText и проверяем, не пусто ли
                String model = modelEdit.getText().toString();
                String brand = brandEdit.getText().toString();
                String yearString = yearEdit.getText().toString();
                String mileageString = mileageEdit.getText().toString();
                String vinCode = vinCodeEdit.getText().toString();
                String licensePlate = licensePlateEdit.getText().toString();
                String bodyType = bodyTypeEdit.getText().toString();
                String engineType = engineTypeEdit.getText().toString();

                // Если поля пустые, не присваиваем их объекту, а передаем null
                car.setModel(model.isEmpty() ? null : model);
                car.setBrand(brand.isEmpty() ? null : brand);
                car.setYear(yearString.isEmpty() ? 0 : Integer.parseInt(yearString));
                car.setMileage(mileageString.isEmpty() ? 0 : Integer.parseInt(mileageString));
                car.setVinCode(vinCode.isEmpty() ? null : vinCode);
                car.setLicensePlate(licensePlate.isEmpty() ? null : licensePlate);
                car.setBodyType(bodyType.isEmpty() ? null : bodyType);
                car.setEngineType(engineType.isEmpty() ? null : engineType);

                // Логируем JSON перед отправкой
                String json = new Gson().toJson(car);
                Log.d("EditCarActivity", "Отправляем JSON: " + json);

                updateCar(car);

            } catch (Exception e) {
                Toast.makeText(this, "Ошибка при сохранении", Toast.LENGTH_SHORT).show();
            }
        });


    }


//    private void updateCar(Car car) throws GeneralSecurityException, IOException {
////        String token = new EncryptedSharedPrefs(this).getToken();
////        String json = new Gson().toJson(car);
////        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
////
////        ApiClient.getInstance().updateCar(car.getIdCar(), body, token, new Callback() {
////            @Override
////            public void onFailure(Call call, IOException e) {
////                runOnUiThread(() -> Toast.makeText(EditCarActivity.this, "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_SHORT).show());
////            }
////
////            @Override
////            public void onResponse(Call call, Response response) throws IOException {
////                if (response.isSuccessful()) {
////                    runOnUiThread(() -> {
////                        Toast.makeText(EditCarActivity.this, "Данные успешно обновлены", Toast.LENGTH_SHORT).show();
////                        finish();
////                    });
////                } else {
////                    runOnUiThread(() -> Toast.makeText(EditCarActivity.this, "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show());
////                }
////            }
////        });
////    }
    private void updateCar(Car car) throws GeneralSecurityException, IOException {
        String token = new EncryptedSharedPrefs(this).getToken();
        String json = new Gson().toJson(car);

        Log.d("EditCarActivity", "Отправляем JSON: " + json); // Отладочный лог

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        ApiClient.getInstance().updateCar(car.getIdCar(), body, token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(EditCarActivity.this, "Ошибка обновления: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "null";
                Log.d("EditCarActivity", "Ответ сервера: " + response.code() + " " + responseBody);

                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditCarActivity.this, "Данные успешно обновлены", Toast.LENGTH_SHORT).show();
                        carViewModel.setSelectedCar(car); // Обновляем ViewModel
                        finish();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(EditCarActivity.this, "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
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

}
