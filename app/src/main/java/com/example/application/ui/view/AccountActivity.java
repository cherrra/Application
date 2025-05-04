package com.example.application.ui.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.application.R;
import com.example.application.ui.viewmodel.UserViewModel;
import com.example.application.utils.EncryptedSharedPrefs;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AccountActivity extends AppCompatActivity {
    private UserViewModel userViewModel;
    private EncryptedSharedPrefs encryptedSharedPrefs;
    private ImageView profileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        TextView usernameTextView = findViewById(R.id.usernameTextView);
        TextView emailTextView = findViewById(R.id.emailTextView);
        Button logoutButton = findViewById(R.id.logoutButton);
        Button infoButton = findViewById(R.id.infoButton);
        profileImageView = findViewById(R.id.profileImageView);

        setupNavigationButtons();
        setupButtonAnimation(logoutButton);
        setupButtonAnimation(infoButton);


        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getAccessToken();
            Log.d("AccountActivity", "Полученный токен: " + token);

            if (token != null) {
                observeUserData(token, usernameTextView, emailTextView);
            } else {
                Log.e("AccountActivity", "Токен отсутствует");
            }
        } catch (Exception e) {
            Log.e("AccountActivity", "Ошибка доступа к токену: " + e.getMessage());
        }


        infoButton.setOnClickListener(v -> {
            try {
                String token = encryptedSharedPrefs.getAccessToken();
                if (token != null) {
                    userViewModel.getUser(token).observe(this, user -> {
                        if (user != null) {
                            Intent intent = new Intent(AccountActivity.this, DetailsActivity.class);
                            intent.putExtra("userId", user.getId());
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Ошибка загрузки данных пользователя", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

       logoutButton.setOnClickListener(v -> logout());
    }

    private void setupNavigationButtons() {
        findViewById(R.id.homeButton).setOnClickListener(v ->
                startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.accountButton).setOnClickListener(v ->
                startActivity(new Intent(this, AccountActivity.class)));
        findViewById(R.id.garageButton).setOnClickListener(v ->
                startActivity(new Intent(this, GarageActivity.class)));
        findViewById(R.id.orderButton).setOnClickListener(v ->
                startActivity(new Intent(this, OrdersActivity.class)));
    }

    private void observeUserData(String token, TextView usernameTextView, TextView emailTextView) {
        userViewModel.getUser(token).observe(this, user -> {
            if (user != null) {
                usernameTextView.setText(user.getUsername());
                emailTextView.setText(user.getEmail());

                if (user.getLinkImg() != null && !user.getLinkImg().isEmpty()) {
                    loadProfileImage(user.getLinkImg());
                } else {
                    profileImageView.setImageResource(R.drawable.ic_placeholder);
                }
            } else {
                Toast.makeText(this, "Ошибка загрузки данных пользователя", Toast.LENGTH_SHORT).show();
                Log.e("AccountActivity", "User data is null");
            }
        });
    }

    private void loadProfileImage(String imageUrl) {
        try {
            String baseUrl = "https://automser.store/";
            String cleanPath = imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
            String fullImageUrl = baseUrl + cleanPath;

            Glide.with(this)
                    .load(fullImageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .circleCrop()
                    .into(profileImageView);
        } catch (Exception e) {
            Log.e("AccountActivity", "Error loading profile image: " + e.getMessage());
            profileImageView.setImageResource(R.drawable.ic_placeholder);
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

    private void logout() {
        try {
            EncryptedSharedPrefs encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            encryptedSharedPrefs.clearTokens();

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}