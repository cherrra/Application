package com.example.application.ui.view;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.R;
import com.example.application.ui.viewmodel.UserViewModel;
import com.example.application.utils.EncryptedSharedPrefs;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditActivity extends AppCompatActivity {
    private EditText usernameEditText, emailEditText, birthDateEditText, phoneNumberEditText;
    private Button saveButton, uploadImageButton;
    private ImageView profileImageView;
    private Calendar birthDateCalendar;
    private UserViewModel userViewModel;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        initializeViews();
        setupDatePicker();
        loadUserData();
        setupButtonAnimations();
        setupClickListeners();
    }

    private void initializeViews() {
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        birthDateEditText = findViewById(R.id.birthDateEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        saveButton = findViewById(R.id.saveButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        profileImageView = findViewById(R.id.profileImageView);
        ImageView backArrow = findViewById(R.id.backArrow);
    }

    private void setupDatePicker() {
        birthDateCalendar = Calendar.getInstance();

        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                birthDateCalendar.set(Calendar.YEAR, year);
                birthDateCalendar.set(Calendar.MONTH, month);
                birthDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateBirthDateEditText();
            }
        };

        birthDateEditText.setOnClickListener(v -> {
            new DatePickerDialog(EditActivity.this, dateSetListener,
                    birthDateCalendar.get(Calendar.YEAR),
                    birthDateCalendar.get(Calendar.MONTH),
                    birthDateCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        birthDateEditText.setFocusable(false);
        birthDateEditText.setClickable(true);
    }

    private void updateBirthDateEditText() {
        String dateFormat = "dd.MM.yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        birthDateEditText.setText(sdf.format(birthDateCalendar.getTime()));
    }

    private void loadUserData() {
        Intent intent = getIntent();
        usernameEditText.setText(intent.getStringExtra("username"));
        emailEditText.setText(intent.getStringExtra("email"));

        // Обработка даты рождения
        String birthDate = intent.getStringExtra("birthDate");
        if (birthDate != null && !birthDate.isEmpty() && !birthDate.equals("Не указана")) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                birthDateCalendar.setTime(sdf.parse(birthDate));
                birthDateEditText.setText(birthDate);
            } catch (Exception e) {
                Log.e("EditActivity", "Error parsing birth date", e);
                birthDateEditText.setText("");
            }
        } else {
            birthDateEditText.setText("");
        }

        phoneNumberEditText.setText(intent.getStringExtra("phoneNumber"));
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
        saveButton.setOnClickListener(v -> saveUserData());
        uploadImageButton.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            profileImageView.setImageURI(selectedImageUri);
        }
    }

    private void saveUserData() {
        String token = getToken();
        if (token == null) return;

        Map<String, String> updatedData = new HashMap<>();
        updatedData.put("username", usernameEditText.getText().toString());
        updatedData.put("email", emailEditText.getText().toString());

        // Форматирование даты для сервера (yyyy-MM-dd)
        if (birthDateEditText.getText().length() > 0) {
            SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            updatedData.put("birth_date", serverFormat.format(birthDateCalendar.getTime()));
        } else {
            updatedData.put("birth_date", "");
        }

        updatedData.put("phone_number", phoneNumberEditText.getText().toString());

        // Сначала обновляем данные пользователя
        userViewModel.updateUser(updatedData, token).observe(this, success -> {
            if (success) {
                if (selectedImageUri != null) {
                    uploadImage(token);
                } else {
                    showSuccessAndFinish();
                }
            } else {
                showError("Ошибка обновления данных");
            }
        });
    }

    private String getToken() {
        try {
            String token = new EncryptedSharedPrefs(this).getAccessToken();
            if (token == null) {
                showError("Токен отсутствует");
            }
            return token;
        } catch (Exception e) {
            Log.e("EditActivity", "Ошибка получения токена", e);
            showError("Ошибка авторизации");
            return null;
        }
    }

    private void uploadImage(String token) {
        userViewModel.uploadImage(selectedImageUri, token, this).observe(this, success -> {
            if (success) {
                showSuccessAndFinish();
            } else {
                showError("Ошибка загрузки изображения");
            }
        });
    }

    private void showSuccessAndFinish() {
        Snackbar.make(findViewById(android.R.id.content),
                "Данные успешно обновлены", Snackbar.LENGTH_SHORT).show();

        // Возвращаем обновленные данные в DetailsActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra("username", usernameEditText.getText().toString());
        resultIntent.putExtra("email", emailEditText.getText().toString());
        resultIntent.putExtra("birthDate", birthDateEditText.getText().toString());
        resultIntent.putExtra("phoneNumber", phoneNumberEditText.getText().toString());
        setResult(RESULT_OK, resultIntent);

        finish();
    }

    private void showError(String message) {
        Snackbar.make(findViewById(android.R.id.content),
                message, Snackbar.LENGTH_LONG).show();
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