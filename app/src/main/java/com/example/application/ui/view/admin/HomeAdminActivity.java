package com.example.application.ui.view.admin;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.security.GeneralSecurityException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class HomeAdminActivity extends AppCompatActivity {
    private LinearLayout categoryContainer;
    private CategoryViewModel categoryViewModel;
    private EncryptedSharedPrefs encryptedSharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_admin);

        categoryContainer = findViewById(R.id.categoryContainer);

        ImageButton notificationButton = findViewById(R.id.notificationButton);
        notificationButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeAdminActivity.this, NotificationAdminActivity.class);
            startActivity(intent);
        });

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

        Button addCategoryButton = findViewById(R.id.addCategoryButton);
        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());


        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getAccessToken();
            Log.d("HomeAdminActivity", "Полученный токен: " + token);

        } catch (Exception e) {
            Log.e("HomeAdminActivity", "Ошибка доступа к токену: " + e.getMessage());
        }


        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> logout());

        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        try {
            EncryptedSharedPrefs encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getAccessToken();
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

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить новую категорию");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Введите название категории");
        layout.addView(nameInput);

        builder.setView(layout);

        builder.setPositiveButton("Добавить", (dialog, which) -> {
            String categoryName = nameInput.getText().toString();
            if (!categoryName.isEmpty()) {
                createCategory(categoryName);
            } else {
                Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void createCategory(String categoryName) {
        try {
            EncryptedSharedPrefs encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getAccessToken();

            if (token != null) {
                categoryViewModel.createCategory(categoryName, token, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() ->
                                Toast.makeText(HomeAdminActivity.this,
                                        "Ошибка создания категории",
                                        Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                Toast.makeText(HomeAdminActivity.this,
                                        "Категория создана",
                                        Toast.LENGTH_SHORT).show();
                                // Обновляем список категорий
                                observeCategories(token);
                            } else {
                                Toast.makeText(HomeAdminActivity.this,
                                        "Ошибка создания категории",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() ->
                    Toast.makeText(this,
                            "Ошибка получения токена",
                            Toast.LENGTH_SHORT).show());
        }
    }

    private void addCategoryCard(Category category) {
        // Основной контейнер карточки (горизонтальный)
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(16, 16, 16, 16); // Уменьшенные отступы
        card.setGravity(Gravity.CENTER_VERTICAL);

        // Стиль карточки
        GradientDrawable cardShape = new GradientDrawable();
        cardShape.setShape(GradientDrawable.RECTANGLE);
        cardShape.setCornerRadius(24f);
        cardShape.setColor(Color.parseColor("#FFFFFF"));
        cardShape.setStroke(2, Color.parseColor("#E3F2FD")); // Более тонкая обводка
        card.setBackground(cardShape);

        // Параметры карточки
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 16, 16, 16);
        card.setMinimumHeight(dpToPx(70));
        card.setLayoutParams(cardParams);

        // Контейнер для текста и кнопок (вертикальный)
        LinearLayout contentContainer = new LinearLayout(this);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // Название категории
        TextView nameView = new TextView(this);
        nameView.setText(category.getCategoryName());
        nameView.setTextSize(16f);
        nameView.setTextColor(Color.parseColor("#2260FF"));
        nameView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        nameView.setMaxLines(1);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        contentContainer.addView(nameView);

        // Контейнер для кнопок
        LinearLayout buttonsContainer = new LinearLayout(this);
        buttonsContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonsContainer.setGravity(Gravity.END);
        buttonsContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Кнопка редактирования (компактная)
        Button editButton = new Button(this);
        setupCompactButton(editButton, "Изменить");
        editButton.setOnClickListener(v -> showEditCategoryDialog(category.getIdCategory(), category.getCategoryName()));
        buttonsContainer.addView(editButton);

        // Кнопка удаления (компактная)
        Button deleteButton = new Button(this);
        setupCompactButton(deleteButton, "Удалить");
        deleteButton.setBackground(createRoundedButtonBackground("#FF5252"));
        deleteButton.setOnClickListener(v -> confirmAndDeleteCategory(category.getIdCategory(), card));
        buttonsContainer.addView(deleteButton);

        contentContainer.addView(buttonsContainer);
        card.addView(contentContainer);

        // Обработчик клика по карточке
        card.setOnClickListener(v -> {
            Intent intent = new Intent(HomeAdminActivity.this, ServicesAdminActivity.class);
            intent.putExtra("id_category", category.getIdCategory());
            startActivity(intent);
        });

        categoryContainer.addView(card);
    }

    // Метод для настройки компактных кнопок
    private void setupCompactButton(Button button, String text) {
        GradientDrawable buttonShape = new GradientDrawable();
        buttonShape.setShape(GradientDrawable.RECTANGLE);
        buttonShape.setCornerRadius(32f);
        buttonShape.setColor(Color.parseColor("#2260FF"));

        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setBackground(buttonShape);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(80),  // ширина
                dpToPx(26)   // высота
        );
        params.setMargins(4, 0, 4, 0);
        button.setLayoutParams(params);

        button.setTextSize(12f);
        button.setPadding(0, 0, 0, 0);
    }

    // Метод для конвертации dp в px
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // Метод для создания фона кнопок с закругленными углами
    private GradientDrawable createRoundedButtonBackground(String color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(32f);
        shape.setColor(Color.parseColor(color));
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
            String token = encryptedSharedPrefs.getAccessToken();

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
            String token = encryptedSharedPrefs.getAccessToken();

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
