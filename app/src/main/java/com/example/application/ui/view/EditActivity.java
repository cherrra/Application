package com.example.application.ui.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
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
    private Button saveButton, cancelButton, uploadImageButton;
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
        cancelButton = findViewById(R.id.cancelButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        profileImageView = findViewById(R.id.profileImageView);

        Intent intent = getIntent();
        usernameEditText.setText(intent.getStringExtra("username"));
        emailEditText.setText(intent.getStringExtra("email"));
        birthDateEditText.setText(intent.getStringExtra("birthDate"));
        genderEditText.setText(intent.getStringExtra("gender"));
        phoneNumberEditText.setText(intent.getStringExtra("phoneNumber"));

        cancelButton.setOnClickListener(v -> finish());
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
}
