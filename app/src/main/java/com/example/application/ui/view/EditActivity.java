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

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.utils.EncryptedSharedPrefs;
import com.example.application.R;
import com.example.application.ui.viewmodel.UserViewModel;

import java.util.HashMap;
import java.util.Map;

public class EditActivity extends AppCompatActivity {
    private EditText usernameEditText, emailEditText, birthDateEditText, genderEditText, phoneNumberEditText;
    private Button saveButton, uploadImageButton;
    private ImageView profileImageView;

    private UserViewModel userViewModel;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        birthDateEditText = findViewById(R.id.birthDateEditText);
        genderEditText = findViewById(R.id.genderEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        saveButton = findViewById(R.id.saveButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        profileImageView = findViewById(R.id.profileImageView);
        ImageView backArrow = findViewById(R.id.backArrow);

        Intent intent = getIntent();
        usernameEditText.setText(intent.getStringExtra("username"));
        emailEditText.setText(intent.getStringExtra("email"));
        birthDateEditText.setText(intent.getStringExtra("birthDate"));
        genderEditText.setText(intent.getStringExtra("gender"));
        phoneNumberEditText.setText(intent.getStringExtra("phoneNumber"));

        setupButtonAnimation(saveButton);
        setupButtonAnimation(uploadImageButton);

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
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            Log.e("EditActivity", "Ошибка инициализации EncryptedSharedPrefs: " + e.getMessage());
            return;
        }

        if (token == null) {
            Log.e("EditActivity", "Токен отсутствует");
            return;
        }

        Map<String, String> updatedData = new HashMap<>();
        updatedData.put("username", usernameEditText.getText().toString());
        updatedData.put("email", emailEditText.getText().toString());
        updatedData.put("birth_date", birthDateEditText.getText().toString());
        updatedData.put("gender", genderEditText.getText().toString());
        updatedData.put("phone_number", phoneNumberEditText.getText().toString());

        if (selectedImageUri != null) {
            userViewModel.uploadImage(selectedImageUri, token, this).observe(this, success -> {
                if (success) {
                    Log.d("EditActivity", "Изображение успешно загружено.");
                } else {
                    Log.e("EditActivity", "Ошибка загрузки изображения.");
                }
            });
        }

        userViewModel.updateUser(updatedData, token).observe(this, success -> {
            if (success) {
                Intent intent = new Intent(EditActivity.this, DetailsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                Log.e("EditActivity", "Ошибка обновления данных.");
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