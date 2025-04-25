package com.example.application.ui.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.application.R;
import com.example.application.data.model.Brand;
import com.example.application.data.model.Car;
import com.example.application.data.model.Model;
import com.example.application.network.ApiClient;
import com.example.application.ui.viewmodel.CarViewModel;
import com.example.application.utils.EncryptedSharedPrefs;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditCarActivity extends AppCompatActivity {
    private CarViewModel carViewModel;
    private Car car;
    private Uri selectedImageUri;

    private EditText modelEdit, brandEdit, yearEdit, mileageEdit, vinCodeEdit, licensePlateEdit;
    private Button saveButton, uploadImageButton;
    private ImageView carImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_car);

        initializeViews();
        setupButtonAnimations();
        setupClickListeners();

        carViewModel = new ViewModelProvider(this).get(CarViewModel.class);

        String carDetailsJson = getIntent().getStringExtra("carDetails");
        if (carDetailsJson != null) {
            car = new Gson().fromJson(carDetailsJson, Car.class);
            carViewModel.setSelectedCar(car);
            loadCarData();
        } else {
            car = carViewModel.getSelectedCar().getValue();
            if (car != null) {
                loadCarData();
            }
        }
    }

    private void initializeViews() {
        modelEdit = findViewById(R.id.editModel);
        brandEdit = findViewById(R.id.editBrand);
        yearEdit = findViewById(R.id.editYear);
        mileageEdit = findViewById(R.id.editMileage);
        vinCodeEdit = findViewById(R.id.editVinCode);
        licensePlateEdit = findViewById(R.id.editLicensePlate);
        saveButton = findViewById(R.id.saveButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        carImageView = findViewById(R.id.carImageView);
    }

    private void loadCarData() {
        if (car.getModel() != null && car.getModel().getBrand() != null) {
            brandEdit.setText(car.getModel().getBrand().getBrandName());
            modelEdit.setText(car.getModel().getModelName());
        }
        yearEdit.setText(String.valueOf(car.getYear()));
        mileageEdit.setText(String.valueOf(car.getMileage()));
        vinCodeEdit.setText(car.getVinCode());
        licensePlateEdit.setText(car.getLicensePlate());

        if (car.getLinkImg() != null && !car.getLinkImg().isEmpty()) {
            Glide.with(this)
                    .load(car.getLinkImg())
                    .into(carImageView);
        }
    }

    private void setupButtonAnimations() {
        setupButtonAnimation(saveButton);
        setupButtonAnimation(uploadImageButton);

        ImageView backArrow = findViewById(R.id.backArrow);
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
    }

    private void setupClickListeners() {
        findViewById(R.id.backArrow).setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveCarData());
        uploadImageButton.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            carImageView.setImageURI(selectedImageUri);
        }
    }

    private void saveCarData() {
        try {
            Brand brand = new Brand();
            brand.setBrandName(brandEdit.getText().toString());

            Model model = new Model();
            model.setModelName(modelEdit.getText().toString());
            model.setBrand(brand);

            car.setModel(model);
            car.setYear(yearEdit.getText().toString().isEmpty() ? 0 : Integer.parseInt(yearEdit.getText().toString()));
            car.setMileage(mileageEdit.getText().toString().isEmpty() ? 0 : Integer.parseInt(mileageEdit.getText().toString()));
            car.setVinCode(vinCodeEdit.getText().toString().isEmpty() ? null : vinCodeEdit.getText().toString());
            car.setLicensePlate(licensePlateEdit.getText().toString().isEmpty() ? null : licensePlateEdit.getText().toString());

            updateCar(car);
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при сохранении", Toast.LENGTH_SHORT).show();
            Log.e("EditCarActivity", "Ошибка при сохранении", e);
        }
    }

    private void updateCar(Car car) {
        try {
            String token = new EncryptedSharedPrefs(this).getToken();
            if (token == null || token.isEmpty()) {
                Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("brand_name", car.getModel().getBrand().getBrandName());
            jsonObject.put("model_name", car.getModel().getModelName());
            jsonObject.put("year", car.getYear());
            jsonObject.put("mileage", car.getMileage());
            jsonObject.put("vin_code", car.getVinCode());
            jsonObject.put("license_plate", car.getLicensePlate());

            RequestBody body = RequestBody.create(
                    jsonObject.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            ApiClient.getInstance().updateCar(car.getIdCar(), body, token, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditCarActivity.this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
                        Log.e("EditCarActivity", "Ошибка обновления", e);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            if (selectedImageUri != null) {
                                uploadCarImage(token);
                            } else {
                                Toast.makeText(EditCarActivity.this, "Данные успешно обновлены", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            try {
                                String errorBody = response.body() != null ? response.body().string() : "null";
                                Log.e("EditCarActivity", "Ошибка сервера: " + response.code() + " " + errorBody);
                                Toast.makeText(EditCarActivity.this, "Ошибка сервера: " + response.code(), Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при обновлении", Toast.LENGTH_SHORT).show();
            Log.e("EditCarActivity", "Ошибка при обновлении", e);
        }
    }

    private void uploadCarImage(String token) {
        carViewModel.uploadCarImage(selectedImageUri, token, car.getIdCar(), this).observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Данные и изображение успешно обновлены", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
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
