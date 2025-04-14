package com.example.application.ui.view.admin;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.R;
import com.example.application.data.model.Category;
import com.example.application.ui.view.MainActivity;
import com.example.application.ui.viewmodel.CategoryViewModel;
import com.example.application.utils.EncryptedSharedPrefs;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class HomeAdminActivity extends AppCompatActivity {
    private GridLayout categoryContainer;
    private CategoryViewModel categoryViewModel;
    private EncryptedSharedPrefs encryptedSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_admin);

        categoryContainer = findViewById(R.id.categoryContainer);

        TextView homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeAdminActivity.this, HomeAdminActivity.class);
            startActivity(intent);
        });

        TextView usersButton = findViewById(R.id.usersButton);
        usersButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeAdminActivity.this, UsersAdminActivity.class);
            startActivity(intent);
        });

        TextView orderButton = findViewById(R.id.orderButton);
        orderButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeAdminActivity.this, OrdersAdminActivity.class);
            startActivity(intent);
        });


        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();
            Log.d("HomeAdminActivity", "Полученный токен: " + token);
//
//            if (token != null) {
//                observeUserData(token, usernameTextView, emailTextView);
//            } else {
//                Log.e("AccountActivity", "Токен отсутствует");
//            }
        } catch (Exception e) {
            Log.e("HomeAdminActivity", "Ошибка доступа к токену: " + e.getMessage());
        }


        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> logout());

        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        try {
            EncryptedSharedPrefs encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();
            if (token != null) {
                observeCategories(token);
            } else {
                Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка инициализации", Toast.LENGTH_SHORT).show();
        }
    }


    private void observeCategories(String token) {
        categoryViewModel.getCategories(token).observe(this, categories -> {
            if (categories != null) {
                updateCategoryUI(categories);
            } else {
                Toast.makeText(this, "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCategoryUI(List<Category> categories) {
        categoryContainer.removeAllViews();
        for (Category category : categories) {
            addCategoryCard(category);
        }
    }

    private void addCategoryCard(Category category) {
        // Убедимся, что контейнер вертикальный
        categoryContainer.setOrientation(GridLayout.VERTICAL);

        // Основной контейнер карточки
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);

        // Стиль карточки
        GradientDrawable cardShape = new GradientDrawable();
        cardShape.setShape(GradientDrawable.RECTANGLE);
        cardShape.setCornerRadius(20f);
        cardShape.setColor(Color.parseColor("#FFFFFF"));
        cardShape.setStroke(4, Color.parseColor("#E3F2FD"));
        card.setBackground(cardShape);

        // Параметры карточки - важно WRAP_CONTENT для высоты
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 8, 16, 8);
        card.setLayoutParams(cardParams);

        // Иконка категории
        ImageView iconView = new ImageView(this);
        iconView.setImageResource(R.drawable.ic_category);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                100, 100
        );
        iconParams.gravity = Gravity.CENTER;
        iconParams.setMargins(0, 0, 0, 12);
        iconView.setLayoutParams(iconParams);
        card.addView(iconView);

        // Название категории
        TextView nameView = new TextView(this);
        nameView.setText(category.getCategoryName());
        nameView.setTextSize(20f);
        nameView.setTextColor(Color.parseColor("#2260FF"));
        nameView.setGravity(Gravity.CENTER);
        nameView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.setMargins(0, 0, 0, 16);
        nameView.setLayoutParams(textParams);
        card.addView(nameView);

        // Контейнер для кнопок
        LinearLayout buttonsContainer = new LinearLayout(this);
        buttonsContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonsContainer.setGravity(Gravity.CENTER);
        buttonsContainer.setPadding(8, 8, 8, 8);

        GradientDrawable buttonsBg = new GradientDrawable();
        buttonsBg.setShape(GradientDrawable.RECTANGLE);
        buttonsBg.setCornerRadius(20f);
        buttonsBg.setColor(Color.parseColor("#F5F5F5"));
        buttonsContainer.setBackground(buttonsBg);

        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonsContainer.setLayoutParams(buttonsParams);

        // Кнопка редактирования
        Button editButton = new Button(this);
        editButton.setText("Изменить");
        editButton.setTextSize(16f);
        editButton.setBackground(createRoundedButtonBackground("#2260FF"));
        editButton.setTextColor(Color.WHITE);
        editButton.setPadding(24, 8, 24, 8);
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        editParams.setMargins(4, 0, 2, 0);
        editButton.setLayoutParams(editParams);
        buttonsContainer.addView(editButton);

        // Кнопка удаления
        Button deleteButton = new Button(this);
        deleteButton.setText("Удалить");
        deleteButton.setTextSize(16f);
        deleteButton.setBackground(createRoundedButtonBackground("#2260FF"));
        deleteButton.setTextColor(Color.WHITE);
        deleteButton.setPadding(24, 8, 24, 8);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        deleteParams.setMargins(2, 0, 4, 0);
        deleteButton.setLayoutParams(deleteParams);
        buttonsContainer.addView(deleteButton);

        card.addView(buttonsContainer);

        // Обработчики событий
        editButton.setOnClickListener(v -> showEditCategoryDialog(category.getIdCategory(), category.getCategoryName()));
        deleteButton.setOnClickListener(v -> confirmAndDeleteCategory(category.getIdCategory(), card));

        // Анимация
        card.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.setAlpha(0.9f);
                    v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setAlpha(1f);
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });

        card.setOnClickListener(v -> {
            Intent intent = new Intent(HomeAdminActivity.this, ServicesAdminActivity.class);
            intent.putExtra("id_category", category.getIdCategory());
            startActivity(intent);
        });

        // Очищаем контейнер перед добавлением новых карточек (если нужно)
        // categoryContainer.removeAllViews();

        categoryContainer.addView(card);
    }
    private GradientDrawable createRoundedButtonBackground(String color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(24f); // Меньший радиус для кнопок
        shape.setColor(Color.parseColor(color));
        shape.setStroke(1, Color.parseColor("#BDBDBD")); // Тоньше обводка
        return shape;
    }


    private void showEditCategoryDialog(int categoryId, String currentName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Редактировать категорию");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        EditText nameInput = new EditText(this);
        nameInput.setText(currentName);
        layout.addView(nameInput);

        builder.setView(layout);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newName = nameInput.getText().toString();
            if (!newName.isEmpty()) {
                updateCategory(categoryId, newName);
            } else {
                Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void updateCategory(int categoryId, String newCategoryName) {
        try {
            EncryptedSharedPrefs encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();

            if (token != null) {
                categoryViewModel.updateCategory(categoryId, newCategoryName, token, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> Toast.makeText(HomeAdminActivity.this, "Ошибка обновления категории", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                Toast.makeText(HomeAdminActivity.this, "Категория обновлена", Toast.LENGTH_SHORT).show();
                                observeCategories(token); // Перезагружаем список категорий
                            } else {
                                Toast.makeText(HomeAdminActivity.this, "Ошибка обновления категории", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Ошибка получения токена", Toast.LENGTH_SHORT).show());
        }
    }

    private void deleteCategory(int categoryId, LinearLayout card) {
        try {
            EncryptedSharedPrefs encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();

            if (token != null) {
                categoryViewModel.deleteCategory(categoryId, token, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() -> Toast.makeText(HomeAdminActivity.this, "Ошибка удаления категории", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                Toast.makeText(HomeAdminActivity.this, "Категория удалена", Toast.LENGTH_SHORT).show();
                                categoryContainer.removeView(card); // Удаляем карточку из интерфейса
                            } else {
                                Toast.makeText(HomeAdminActivity.this, "Ошибка удаления категории", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Ошибка получения токена", Toast.LENGTH_SHORT).show());
        }
    }

    private void confirmAndDeleteCategory(int categoryId, LinearLayout card) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить категорию")
                .setMessage("Вы уверены, что хотите удалить эту категорию?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteCategory(categoryId, card))
                .setNegativeButton("Отмена", null)
                .create()
                .show();
    }

    private final ActivityResultLauncher<Intent> serviceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    setResult(RESULT_OK, result.getData());
                    finish();
                }
            }
    );


    private void logout() {
        try {
            encryptedSharedPrefs.saveToken(null);
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
