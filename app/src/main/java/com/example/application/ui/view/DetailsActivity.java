package com.example.application.ui.view;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

    private TextView usernameTextView, emailTextView, birthDateTextView, phoneNumberTextView;
    private ImageView profileImageViewDetails;
    private UserViewModel userViewModel;
    private static final String BASE_URL = "https://automser.store/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        usernameTextView = findViewById(R.id.usernameDetailsTextView);
        emailTextView = findViewById(R.id.emailDetailsTextView);
        birthDateTextView = findViewById(R.id.birthDateDetailsTextView);
        phoneNumberTextView = findViewById(R.id.phoneNumberDetailsTextView);
        profileImageViewDetails = findViewById(R.id.profileImageViewDetails);
        ImageView backArrow = findViewById(R.id.backArrow);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        fetchUserDetails();

        Button editButton = findViewById(R.id.editButton);
        Button deleteAccountButton = findViewById(R.id.deleteAccountButton);
        setupButtonAnimation(editButton);
        setupButtonAnimation(deleteAccountButton);

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

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(DetailsActivity.this, EditActivity.class);
            intent.putExtra("username", usernameTextView.getText().toString());
            intent.putExtra("email", emailTextView.getText().toString());
            intent.putExtra("birthDate", birthDateTextView.getText().toString());
            intent.putExtra("phoneNumber", phoneNumberTextView.getText().toString());
            startActivity(intent);
        });

        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showDeleteAccountDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_account, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        confirmButton.setOnClickListener(v -> {
            deleteAccount();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void fetchUserDetails() {
        try {
            String token = new EncryptedSharedPrefs(this).getAccessToken();
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
        usernameTextView.setText(user.getUsername());
        emailTextView.setText(user.getEmail());

        if (user.getBirthDate() != null && !user.getBirthDate().isEmpty()) {
            birthDateTextView.setText(user.getBirthDate());
        } else {
            birthDateTextView.setText("Дата рождения не указана");
        }

        if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
            phoneNumberTextView.setText(user.getPhoneNumber());
        } else {
            phoneNumberTextView.setText("Телефон не указан");
        }

        if (user.getLinkImg() != null && !user.getLinkImg().isEmpty()) {
            loadImage(user.getLinkImg());
        } else {
            profileImageViewDetails.setImageResource(R.drawable.ic_placeholder);
        }
    }

    private void loadImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            profileImageViewDetails.setImageResource(R.drawable.ic_placeholder);
            return;
        }

        try {
            String baseUrl = "https://automser.store/";
            String cleanPath = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
            String fullImageUrl = baseUrl + cleanPath;
            Log.d("ImageLoad", "Final image URL: " + fullImageUrl);

            Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .circleCrop()
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
            String token = new EncryptedSharedPrefs(this).getAccessToken();
            if (token != null) {
                ApiClient.getInstance().deleteAccount(token, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(DetailsActivity.this,
                                    "Ошибка сети при удалении аккаунта",
                                    Toast.LENGTH_SHORT).show();
                        });
                        Log.e("DetailsActivity", "Ошибка удаления аккаунта: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                try {
                                    new EncryptedSharedPrefs(DetailsActivity.this).clearTokens();
                                } catch (Exception e) {
                                    Log.e("DetailsActivity", "Ошибка очистки токенов: " + e.getMessage());
                                }
                                Intent intent = new Intent(DetailsActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                Toast.makeText(DetailsActivity.this,
                                        "Аккаунт успешно удален",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(DetailsActivity.this,
                                        "Ошибка сервера: " + response.code(),
                                        Toast.LENGTH_SHORT).show();
                                Log.e("DetailsActivity",
                                        "Ошибка сервера при удалении аккаунта: " + response.code());
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            Log.e("DetailsActivity", "Ошибка при удалении аккаунта: " + e.getMessage());
            Toast.makeText(this,
                    "Ошибка при удалении аккаунта",
                    Toast.LENGTH_SHORT).show();
        }
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