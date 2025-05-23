package com.example.application.ui.view.admin;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.application.R;
import com.example.application.data.model.Service;
import com.example.application.ui.viewmodel.ServiceViewModel;
import com.example.application.utils.EncryptedSharedPrefs;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ServicesAdminActivity extends AppCompatActivity {
    private GridLayout servicesContainer;
    private ServiceViewModel serviceViewModel;
    private String token;
    private int categoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services_admin);

        categoryId = getIntent().getIntExtra("id_category", -1);
        servicesContainer = findViewById(R.id.servicesContainer);

        try {
            token = new EncryptedSharedPrefs(this).getAccessToken();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка получения токена", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        serviceViewModel = new ViewModelProvider(this).get(ServiceViewModel.class);

        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(v -> finish());
        backArrow.setOnTouchListener(createTouchListener());

        Button addButton = findViewById(R.id.addServiceButton);
        addButton.setOnClickListener(v -> showAddServiceDialog());

        if (categoryId != -1 && token != null) {
            observeServices();
        } else {
            Toast.makeText(this, "Ошибка: категория не выбрана", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private View.OnTouchListener createTouchListener() {
        return (v, event) -> {
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
        };
    }

    private void observeServices() {
        serviceViewModel.getServices(categoryId, token).observe(this, services -> {
            servicesContainer.removeAllViews();
            if (services != null && !services.isEmpty()) {
                for (Service service : services) {
                    addServiceCard(service);
                }
            } else {
                Toast.makeText(this, "Нет доступных услуг", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addServiceCard(Service service) {
        // Основной контейнер карточки
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 32, 32, 32);
        card.setGravity(Gravity.CENTER);

        // Стиль карточки
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(55f);
        shape.setColor(Color.parseColor("#80CAD6FF"));
        shape.setStroke(2, Color.parseColor("#CAD6FF"));
        card.setBackground(shape);

        // Параметры карточки
        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.width = 0;
        cardParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        cardParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cardParams.setMargins(16, 16, 16, 16);
        card.setLayoutParams(cardParams);

        // Название услуги
        TextView nameView = new TextView(this);
        nameView.setText(service.getServiceName());
        nameView.setTextSize(18f);
        nameView.setTextColor(Color.parseColor("#2260FF"));
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setGravity(Gravity.CENTER);
        card.addView(nameView);

        // Описание услуги
        if (service.getDescription() != null && !service.getDescription().isEmpty()) {
            TextView descView = new TextView(this);
            descView.setText(service.getDescription());
            descView.setTextSize(14f);
            descView.setTextColor(Color.BLACK);
            descView.setGravity(Gravity.CENTER);
            descView.setPadding(0, 8, 0, 8);
            card.addView(descView);
        }

        // Цена услуги
        TextView priceView = new TextView(this);
        priceView.setText(String.format("%.2f ₽", service.getPrice()));
        priceView.setTextSize(16f);
        priceView.setTextColor(Color.parseColor("#2260FF"));
        priceView.setTypeface(null, Typeface.BOLD);
        priceView.setGravity(Gravity.CENTER);
        card.addView(priceView);

        // Контейнер для кнопок
        LinearLayout buttonsContainer = new LinearLayout(this);
        buttonsContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonsContainer.setGravity(Gravity.CENTER);
        buttonsContainer.setPadding(0, 0, 0, 0);

        // Кнопка редактирования
        Button editButton = createActionButton("Изменить", "#2260FF");
        editButton.setOnClickListener(v -> showEditServiceDialog(service));
        buttonsContainer.addView(editButton);

        // Кнопка удаления
        Button deleteButton = createActionButton("Удалить", "#FF5252");
        deleteButton.setOnClickListener(v -> confirmDeleteService(service));
        buttonsContainer.addView(deleteButton);

        card.addView(buttonsContainer);
        servicesContainer.addView(card);
    }

    private Button createActionButton(String text, String color) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12f);
        button.setAllCaps(false);

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(24f);
        shape.setColor(Color.parseColor(color));
        button.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(70), dpToPx(32)
        );
        params.setMargins(8, 0, 8, 0);
        button.setLayoutParams(params);

        button.setOnTouchListener(createTouchListener());
        return button;
    }

    private void showAddServiceDialog() {
        showServiceDialog(null, "Добавить услугу", (name, price, description) -> {
            addService(name, price, description);
        });
    }

    private void showEditServiceDialog(Service service) {
        showServiceDialog(service, "Редактировать услугу", (name, price, description) -> {
            updateService(service.getIdService(), name, price, description);
        });
    }

    private void showServiceDialog(Service service, String title, ServiceDialogListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Название услуги");
        if (service != null) nameInput.setText(service.getServiceName());
        layout.addView(nameInput);

        EditText priceInput = new EditText(this);
        priceInput.setHint("Цена");
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        if (service != null) priceInput.setText(String.valueOf(service.getPrice()));
        layout.addView(priceInput);

        EditText descriptionInput = new EditText(this);
        descriptionInput.setHint("Описание");
        if (service != null) descriptionInput.setText(service.getDescription());
        layout.addView(descriptionInput);

        builder.setView(layout);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String priceStr = priceInput.getText().toString();
            String description = descriptionInput.getText().toString();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Заполните название и цену", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                listener.onSave(name, price, description);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Введите корректную цену", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void addService(String name, double price, String description) {
        try {
            JSONObject serviceData = new JSONObject();
            serviceData.put("service_name", name);
            serviceData.put("price", price);
            serviceData.put("description", description);
            serviceData.put("id_category", categoryId);

            serviceViewModel.addService(serviceData, token, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(ServicesAdminActivity.this, "Ошибка добавления", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            observeServices();
                            Toast.makeText(ServicesAdminActivity.this, "Услуга добавлена", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ServicesAdminActivity.this, "Ошибка сервера", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка формирования данных", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateService(int serviceId, String name, double price, String description) {
        try {
            JSONObject serviceData = new JSONObject();
            serviceData.put("service_name", name);
            serviceData.put("price", price);
            serviceData.put("description", description);

            serviceViewModel.updateService(serviceId, serviceData, token, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(ServicesAdminActivity.this, "Ошибка обновления", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            observeServices();
                            Toast.makeText(ServicesAdminActivity.this, "Услуга обновлена", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ServicesAdminActivity.this, "Ошибка сервера", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка формирования данных", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteService(Service service) {
        new AlertDialog.Builder(this)
                .setTitle("Удаление услуги")
                .setMessage("Вы уверены, что хотите удалить услугу \"" + service.getServiceName() + "\"?")
                .setPositiveButton("Удалить", (dialog, which) -> deleteService(service))
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteService(Service service) {
        serviceViewModel.deleteService(service.getIdService(), token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(ServicesAdminActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        observeServices();
                        Toast.makeText(ServicesAdminActivity.this, "Услуга удалена", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ServicesAdminActivity.this, "Ошибка сервера", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    interface ServiceDialogListener {
        void onSave(String name, double price, String description);
    }
}
