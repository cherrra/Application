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
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.application.utils.EncryptedSharedPrefs;
import com.example.application.R;
import com.example.application.data.model.User;
import com.example.application.network.ApiClient;
import com.example.application.ui.viewmodel.UserViewModel;

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

        usernameTextView = findViewById(R.id.usernameDetailsTextView);
        emailTextView = findViewById(R.id.emailDetailsTextView);
        birthDateTextView = findViewById(R.id.birthDateDetailsTextView);
        genderTextView = findViewById(R.id.genderDetailsTextView);
        phoneNumberTextView = findViewById(R.id.phoneNumberDetailsTextView);
        profileImageViewDetails = findViewById(R.id.profileImageViewDetails);

        Button backButton = findViewById(R.id.backButton);
        Button editButton = findViewById(R.id.editButton);

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

        Button deleteAccountButton = findViewById(R.id.deleteAccountButton);

        deleteAccountButton.setOnClickListener(v -> {
            new AlertDialog.Builder(DetailsActivity.this)
                    .setMessage("Вы уверены, что хотите удалить аккаунт?")
                    .setPositiveButton("Да", (dialog, which) -> deleteAccount())
                    .setNegativeButton("Нет", null)
                    .show();
        });

        // Инициализируем ViewModel
        userViewModel = new UserViewModel();

        // Получаем данные пользователя
        fetchUserDetails();
    }

    private void fetchUserDetails() {
        // Получаем токен из EncryptedSharedPrefs
        String token;
        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            Log.e("DetailsActivity", "Ошибка инициализации EncryptedSharedPrefs: " + e.getMessage());
            return;
        }

        if (token == null) {
            Log.e("DetailsActivity", "Токен отсутствует");
            return;
        }

        // Наблюдаем за данными из ViewModel
        userViewModel.getUser(token).observe(this, new Observer<User>() {
            @Override
            public void onChanged(User user) {
                if (user != null) {
                    updateUI(user);
                } else {
                    Log.e("DetailsActivity", "Ошибка получения данных пользователя");
                }
            }
        });
    }

    private void updateUI(User user) {
        // Обновляем UI на основе данных пользователя
        usernameTextView.setText(user.getUsername());
        emailTextView.setText(user.getEmail());
        birthDateTextView.setText(user.getBirthDate());
        genderTextView.setText(user.getGender() != null ? user.getGender().getValue() : "Не указано");
        phoneNumberTextView.setText(user.getPhoneNumber());

        String imageUrl = user.getLinkImg();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            loadImage(imageUrl);
        } else {
            profileImageViewDetails.setImageResource(R.drawable.ic_placeholder);
        }
    }

    private void loadImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e("Glide", "Ошибка загрузки изображения", e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("Glide", "Изображение успешно загружено");
                        return false;
                    }
                })
                .into(profileImageViewDetails);
    }

private void deleteAccount() {
    String token;
    try {
        token = new EncryptedSharedPrefs(this).getToken();
    } catch (Exception e) {
        Log.e("DetailsActivity", "Ошибка инициализации EncryptedSharedPrefs: " + e.getMessage());
        return;
    }

    if (token == null) {
        Log.e("DetailsActivity", "Токен отсутствует");
        return;
    }

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
                Log.d("DeleteAccount", "Token: " + token);
                Log.d("DeleteAccount", "URL: " + BASE_URL + "auth/delete");

            }
        }
    });
}

}