package com.example.application.ui.view.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
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

//        Button logoutButton = findViewById(R.id.logoutButton);
//        logoutButton.setOnClickListener(v -> logout());

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
        GridLayout card = new GridLayout(this);
        card.setOrientation(GridLayout.VERTICAL);
        card.setPadding(16, 16, 16, 16);
        card.setBackgroundColor(Color.parseColor("#E0E0E0"));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        params.setMargins(16, 16, 16, 16);
        card.setLayoutParams(params);

        TextView nameView = new TextView(this);
        nameView.setText(category.getCategoryName());
        nameView.setTextSize(16f);
        nameView.setTextColor(Color.BLACK);
        card.addView(nameView);

        Button editButton = new Button(this);
        editButton.setText("✎");
        editButton.setTextSize(12f);
        editButton.setBackgroundColor(Color.TRANSPARENT);
        editButton.setTextColor(Color.MAGENTA);
        card.addView(editButton);

        Button deleteButton = new Button(this);
        deleteButton.setText("✖");
        deleteButton.setTextSize(12f);
        deleteButton.setBackgroundColor(Color.TRANSPARENT);
        deleteButton.setTextColor(Color.MAGENTA);
        card.addView(deleteButton);

        editButton.setOnClickListener(v -> showEditCategoryDialog(category.getIdCategory(), category.getCategoryName()));

        deleteButton.setOnClickListener(v -> confirmAndDeleteCategory(category.getIdCategory(), card));

        card.setOnClickListener(v -> {
            Intent intent = new Intent(HomeAdminActivity.this, ServicesAdminActivity.class);
            intent.putExtra("id_category", category.getIdCategory());
            startActivity(intent);
        });

        categoryContainer.addView(card);
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

    private void deleteCategory(int categoryId, GridLayout card) {
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

    private void confirmAndDeleteCategory(int categoryId, GridLayout card) {
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


//    private void logout() {
//        try {
//            encryptedSharedPrefs.saveToken(null);
//            String clearedToken = encryptedSharedPrefs.getToken();
//            Log.d("HomeAdminActivity", "Токен после выхода: " + (clearedToken == null ? "успешно очищен" : clearedToken));
//
//            Intent intent = new Intent(HomeAdminActivity.this, MainActivity.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(intent);
//            finish();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
