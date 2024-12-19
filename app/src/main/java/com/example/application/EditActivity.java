package com.example.application;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditActivity extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, birthDateEditText, genderEditText, phoneNumberEditText;
    private Button saveButton, cancelButton, uploadImageButton;
    private ImageView profileImageView;

    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

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

        saveButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImageToServer(selectedImageUri);
            }
            updateUserData();
        });

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

    private void uploadImageToServer(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            String fileName = getFileName(imageUri);

            String token = new EncryptedSharedPrefs(this).getToken();

            OkHttpClient client = new OkHttpClient();

            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            builder.addFormDataPart("image", fileName, RequestBody.create(bitmapToBytes(bitmap), MediaType.parse("image/jpeg")));

            Request request = new Request.Builder()
                    .url("http://10.0.2.2:5000/api/upload")
                    .addHeader("Authorization", token)
                    .post(builder.build())
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("EditActivity", "Ошибка загрузки изображения: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("EditActivity", "Изображение успешно загружено");
                    } else {
                        Log.e("EditActivity", "Ошибка сервера: " + response.code());
                    }
                }
            });
        } catch (Exception e) {
            Log.e("EditActivity", "Ошибка обработки изображения: " + e.getMessage());
        }
    }

    private String getFileName(Uri uri) {
        String result = null;

        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }



    private byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        return stream.toByteArray();
    }

    private void updateUserData() {
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            Log.e("EditActivity", "Ошибка получения токена: " + e.getMessage());
            return;
        }

        Map<String, String> updatedData = new HashMap<>();
        updatedData.put("username", usernameEditText.getText().toString());
        updatedData.put("email", emailEditText.getText().toString());
        updatedData.put("birth_date", birthDateEditText.getText().toString());
        updatedData.put("gender", genderEditText.getText().toString());
        updatedData.put("phone_number", phoneNumberEditText.getText().toString());

        OkHttpClient client = new OkHttpClient();
        String json = new JSONObject(updatedData).toString();

        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/auth/user")
                .addHeader("Authorization", token)
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("EditActivity", "Ошибка обновления: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Intent intent = new Intent(EditActivity.this, DetailsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    Log.e("EditActivity", "Ошибка сервера: " + response.code());
                }
            }
        });
    }
}