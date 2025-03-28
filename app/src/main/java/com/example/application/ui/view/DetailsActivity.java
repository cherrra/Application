package com.example.application.ui.view;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.application.R;
import com.example.application.data.model.User;
import com.example.application.network.ApiClient;
import com.example.application.ui.viewmodel.UserViewModel;
import com.example.application.utils.EncryptedSharedPrefs;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class DetailsActivity extends AppCompatActivity {

    private TextView usernameTextView, emailTextView, birthDateTextView, genderTextView, phoneNumberTextView;
    private ImageView profileImageViewDetails;
    private UserViewModel userViewModel;
    private static final String BASE_URL = "http://10.0.2.2:5000/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Инициализация всех View
        usernameTextView = findViewById(R.id.usernameDetailsTextView);
        emailTextView = findViewById(R.id.emailDetailsTextView);
        birthDateTextView = findViewById(R.id.birthDateDetailsTextView);
        genderTextView = findViewById(R.id.genderDetailsTextView);
        phoneNumberTextView = findViewById(R.id.phoneNumberDetailsTextView);
        profileImageViewDetails = findViewById(R.id.profileImageViewDetails);

        // Инициализация ViewModel
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class); // Используем ViewModelProvider

        // Получаем данные пользователя
        fetchUserDetails();

        // Остальной код обработки кнопок остается без изменений
        Button backButton = findViewById(R.id.backButton);
        Button editButton = findViewById(R.id.editButton);
        Button deleteAccountButton = findViewById(R.id.deleteAccountButton);

        backButton.setOnClickListener(v -> finish());

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(DetailsActivity.this, EditActivity.class);
            intent.putExtra("username", usernameTextView.getText().toString());
            intent.putExtra("email", emailTextView.getText().toString());
            intent.putExtra("birthDate", birthDateTextView.getText().toString());
            intent.putExtra("gender", genderTextView.getText().toString());
            intent.putExtra("phoneNumber", phoneNumberTextView.getText().toString());
            startActivity(intent);
        });

        deleteAccountButton.setOnClickListener(v -> {
            new AlertDialog.Builder(DetailsActivity.this)
                    .setMessage("Вы уверены, что хотите удалить аккаунт?")
                    .setPositiveButton("Да", (dialog, which) -> deleteAccount())
                    .setNegativeButton("Нет", null)
                    .show();
        });
    }

    private void fetchUserDetails() {
        try {
            String token = new EncryptedSharedPrefs(this).getToken();
            if (token != null) {
                userViewModel.getUser(token).observe(this, user -> {
                    if (user != null) {
                        updateUI(user);
                    } else {
                        Log.e("DetailsActivity", "Данные пользователя null");
                    }
                });
            } else {
                Log.e("DetailsActivity", "Токен отсутствует");
            }
        } catch (Exception e) {
            Log.e("DetailsActivity", "Ошибка получения токена: " + e.getMessage());
        }
    }

    private void updateUI(User user) {
        // Устанавливаем все данные пользователя
        usernameTextView.setText(user.getUsername());
        emailTextView.setText(user.getEmail());

        // Проверяем и устанавливаем дату рождения
        if (user.getBirthDate() != null && !user.getBirthDate().isEmpty()) {
            birthDateTextView.setText(user.getBirthDate());
        } else {
            birthDateTextView.setText("Дата рождения не указана");
        }

        // Проверяем и устанавливаем пол
        if (user.getGender() != null) {
            genderTextView.setText(user.getGender().getValue());
        } else {
            genderTextView.setText("Пол не указан");
        }

        // Проверяем и устанавливаем телефон
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            phoneNumberTextView.setText(user.getPhoneNumber());
        } else {
            phoneNumberTextView.setText("Телефон не указан");
        }

        // Загружаем изображение профиля
        if (user.getLinkImg() != null && !user.getLinkImg().isEmpty()) {
            loadImage(user.getLinkImg());
        } else {
            profileImageViewDetails.setImageResource(R.drawable.ic_placeholder);
        }
    }

//    private void loadImage(String imageUrl) {
//        Glide.with(this)
//                .load(imageUrl)
//                .placeholder(R.drawable.ic_placeholder)
//                .listener(new RequestListener<Drawable>() {
//                    @Override
//                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
//                        Log.e("Glide", "Ошибка загрузки изображения", e);
//                        return false;
//                    }
//
//                    @Override
//                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
//                        Log.d("Glide", "Изображение успешно загружено");
//                        return false;
//                    }
//                })
//                .into(profileImageViewDetails);
//    }
private void loadImage(String imageUrl) {
    if (imageUrl == null || imageUrl.isEmpty()) {
        profileImageViewDetails.setImageResource(R.drawable.ic_placeholder);
        return;
    }

    try {
        // Базовый URL сервера (должен заканчиваться на /)
        String baseUrl = "http://10.0.2.2:5000/";

        // Удаляем начальный слэш из пути изображения, если он есть
        String cleanPath = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;

        // Формируем полный URL
        String fullImageUrl = baseUrl + cleanPath;

        Log.d("ImageLoad", "Final image URL: " + fullImageUrl);

        Glide.with(this)
                .load(fullImageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        Log.e("Glide", "Failed to load image: " + fullImageUrl);
                        if (e != null) {
                            for (Throwable t : e.getRootCauses()) {
                                Log.e("Glide", "Cause: " + t.getMessage());
                            }
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        Log.d("Glide", "Successfully loaded: " + fullImageUrl);
                        return false;
                    }
                })
                .into(profileImageViewDetails);

    } catch (Exception e) {
        Log.e("ImageLoad", "Error: " + e.getMessage());
        profileImageViewDetails.setImageResource(R.drawable.ic_placeholder);
    }
}

    private void deleteAccount() {
        try {
            String token = new EncryptedSharedPrefs(this).getToken();
            if (token != null) {
                ApiClient.getInstance().deleteAccount(token, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e("DetailsActivity", "Ошибка удаления аккаунта: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> {
                                Intent intent = new Intent(DetailsActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            });
                        } else {
                            Log.e("DetailsActivity", "Ошибка сервера при удалении аккаунта: " + response.code());
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e("DetailsActivity", "Ошибка при удалении аккаунта: " + e.getMessage());
        }
    }
}