package com.example.application.ui.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.application.R;
import com.example.application.data.model.Car;
import com.example.application.ui.viewmodel.CarViewModel;
import com.example.application.utils.EncryptedSharedPrefs;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AddCarActivity extends AppCompatActivity {
    private EditText modelInput, brandInput, yearInput, mileageInput, vinCodeInput, licensePlateInput, bodyTypeInput, engineTypeInput;
    private final CarViewModel carViewModel = new CarViewModel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);

        modelInput = findViewById(R.id.modelInput);
        brandInput = findViewById(R.id.brandInput);
        yearInput = findViewById(R.id.yearInput);
        mileageInput = findViewById(R.id.mileageInput);
        vinCodeInput = findViewById(R.id.vinCodeInput);
        licensePlateInput = findViewById(R.id.licensePlateInput);
        bodyTypeInput = findViewById(R.id.bodyTypeInput);
        engineTypeInput = findViewById(R.id.engineTypeInput);
        Button saveButton = findViewById(R.id.saveButton);

        saveButton.setOnClickListener(view -> saveCar());

        carViewModel.getCarAddedStatus().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Машина добавлена", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Ошибка добавления машины", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveCar() {
        try {
            Car car = new Car();
            car.setModel(modelInput.getText().toString());
            car.setBrand(brandInput.getText().toString());
            car.setYear(Integer.parseInt(yearInput.getText().toString()));
            car.setMileage(Integer.parseInt(mileageInput.getText().toString()));
            car.setVinCode(vinCodeInput.getText().toString());
            car.setLicensePlate(licensePlateInput.getText().toString());
            car.setBodyType(bodyTypeInput.getText().toString());
            car.setEngineType(engineTypeInput.getText().toString());

            // Получаем токен из EncryptedSharedPrefs
            String token;
            try {
                token = new EncryptedSharedPrefs(this).getToken();
            } catch (GeneralSecurityException | IOException e) {
                Log.e("AddCarActivity", "Ошибка получения токена: " + e.getMessage());
                return;
            }

            if (token == null) {
                Toast.makeText(this, "Не авторизован", Toast.LENGTH_SHORT).show();
                return;
            }

            carViewModel.addCar(car, token);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Некорректный формат числовых данных", Toast.LENGTH_SHORT).show();
        }
    }
}
