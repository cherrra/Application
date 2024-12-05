package com.example.application;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeAdminActivity extends AppCompatActivity {
    private EncryptedSharedPrefs encryptedSharedPrefs;
    private GridLayout categoryContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_admin);

        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();
            if (token != null) {
                fetchCategories(token);
            } else {
                Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
            }
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Log.e("ActivityName", "Ошибка инициализации EncryptedSharedPrefs: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка при работе с токеном", Toast.LENGTH_SHORT).show();
        }

        TextView homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeAdminActivity.this, HomeAdminActivity.class);
            startActivity(intent);
        });

        TextView usersButton = findViewById(R.id.usersButton);
        usersButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeAdminActivity.this, UsersActivity.class);
            startActivity(intent);
        });

        TextView orderButton = findViewById(R.id.orderButton);
        orderButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeAdminActivity.this, OrdersAdminActivity.class);
            startActivity(intent);
        });

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> logout());

        Button addCategoryButton = findViewById(R.id.addCategoryButton);
        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());

        categoryContainer = findViewById(R.id.categoryContainer);
    }

    private void fetchCategories(String token) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/categories")
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(HomeAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray categoriesArray = new JSONArray(response.body().string());
                        runOnUiThread(() -> {
                            categoryContainer.removeAllViews();
                            for (int i = 0; i < categoriesArray.length(); i++) {
                                try {
                                    JSONObject categoryObject = categoriesArray.getJSONObject(i);
                                    addCategoryCard(categoryObject);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() ->
                                Toast.makeText(HomeAdminActivity.this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(HomeAdminActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void addCategoryCard(JSONObject categoryObject) throws JSONException {
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
        nameView.setText(categoryObject.getString("category_name"));
        nameView.setTextSize(16f);
        nameView.setTextColor(Color.BLACK);
        card.addView(nameView);

        Button deleteButton = new Button(this);
        deleteButton.setText("✖");
        deleteButton.setTextSize(12f);
        deleteButton.setBackgroundColor(Color.TRANSPARENT);
        deleteButton.setTextColor(Color.MAGENTA);
        card.addView(deleteButton);

        Button editButton = new Button(this);
        editButton.setText("✎");
        editButton.setTextSize(12f);
        editButton.setBackgroundColor(Color.TRANSPARENT);
        editButton.setTextColor(Color.MAGENTA);
        card.addView(editButton);

        editButton.setOnClickListener(v -> {
            try {
                String currentName = categoryObject.getString("category_name");
                int categoryId = categoryObject.getInt("id_category");
                showEditCategoryDialog(categoryId, currentName);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        deleteButton.setOnClickListener(v -> {
            try {
                int categoryId = categoryObject.getInt("id_category");
                confirmAndDeleteCategory(categoryId, card);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        card.setOnClickListener(v -> {
            try {
                int categoryId = categoryObject.getInt("id_category");
                Intent intent = new Intent(HomeAdminActivity.this, ServicesAdminActivity.class);
                intent.putExtra("id_category", categoryId);
                startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });

        categoryContainer.addView(card);
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить категорию");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Название категории");
        layout.addView(nameInput);

        builder.setView(layout);

        builder.setPositiveButton("Добавить", (dialog, which) -> {
            String categoryName = nameInput.getText().toString();
            if (!categoryName.isEmpty()) {
                addCategory(categoryName);
            } else {
                Toast.makeText(this, "Введите название категории", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void addCategory(String categoryName) {
        OkHttpClient client = new OkHttpClient();

        String token;
        try {
            token = encryptedSharedPrefs.getToken();
        } catch (Exception e) {
            Log.e("HomeAdminActivity", "Ошибка получения токена: " + e.getMessage());
            return;
        }

        JSONObject newCategoryData = new JSONObject();
        try {
            newCategoryData.put("category_name", categoryName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(newCategoryData.toString(), MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/categories")
                .post(body)
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(HomeAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(HomeAdminActivity.this, "Категория добавлена", Toast.LENGTH_SHORT).show();
                        fetchCategories(token); // Обновляем список категорий
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(HomeAdminActivity.this, "Ошибка добавления категории", Toast.LENGTH_SHORT).show());
                }
            }
        });
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

    private void updateCategory(int categoryId, String newName) {
        OkHttpClient client = new OkHttpClient();

        String token;
        try {
            token = encryptedSharedPrefs.getToken();
        } catch (Exception e) {
            Log.e("HomeAdminActivity", "Ошибка получения токена: " + e.getMessage());
            return;
        }

        JSONObject updatedCategoryData = new JSONObject();
        try {
            updatedCategoryData.put("category_name", newName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(updatedCategoryData.toString(), MediaType.get("application/json"));

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/categories/" + categoryId)
                .put(body)
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(HomeAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(HomeAdminActivity.this, "Категория обновлена", Toast.LENGTH_SHORT).show();
                        fetchCategories(token);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(HomeAdminActivity.this, "Ошибка обновления категории", Toast.LENGTH_SHORT).show());
                }
            }
        });
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

    private void deleteCategory(int categoryId, GridLayout card) {
        OkHttpClient client = new OkHttpClient();

        String token;
        try {
            token = encryptedSharedPrefs.getToken();
        } catch (Exception e) {
            Log.e("HomeAdminActivity", "Ошибка получения токена: " + e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/api/categories/" + categoryId)
                .delete()
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(HomeAdminActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(HomeAdminActivity.this, "Категория удалена", Toast.LENGTH_SHORT).show();
                        categoryContainer.removeView(card);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(HomeAdminActivity.this, "Ошибка удаления категории", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }


    private void logout() {
        try {
            encryptedSharedPrefs.saveToken(null);
            String clearedToken = encryptedSharedPrefs.getToken();
            Log.d("HomeAdminActivity", "Токен после выхода: " + (clearedToken == null ? "успешно очищен" : clearedToken));

            Intent intent = new Intent(HomeAdminActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}