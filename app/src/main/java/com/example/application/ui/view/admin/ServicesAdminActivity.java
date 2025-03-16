package com.example.application.ui.view.admin;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
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
    private int categoryId;
    private GridLayout servicesContainer;
    private ServiceViewModel serviceViewModel;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services_admin);

        categoryId = getIntent().getIntExtra("id_category", -1);
        servicesContainer = findViewById(R.id.servicesContainer);
        serviceViewModel = new ViewModelProvider(this).get(ServiceViewModel.class);

        try {
            token = new EncryptedSharedPrefs(this).getToken();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Button addButton = findViewById(R.id.addServiceButton);
        addButton.setOnClickListener(v -> showAddServiceDialog());

        if (categoryId != -1 && token != null) {
            observeServices();
        } else {
            Toast.makeText(this, "Ошибка: категория не выбрана или нет токена", Toast.LENGTH_SHORT).show();
        }
    }

    private void observeServices() {
        serviceViewModel.getServices(categoryId, token).observe(this, services -> {
            servicesContainer.removeAllViews();
            if (services != null) {
                for (Service service : services) {
                    addServiceCard(service);
                }
            } else {
                Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addServiceCard(Service service) {
        TextView nameView = new TextView(this);
        nameView.setText(service.getServiceName());

        Button deleteButton = new Button(this);
        deleteButton.setText("Удалить");
        deleteButton.setOnClickListener(v -> serviceViewModel.deleteService(service.getIdService(), token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(ServicesAdminActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> observeServices());
            }
        }));

        servicesContainer.addView(nameView);
        servicesContainer.addView(deleteButton);
    }

    private void showAddServiceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить услугу");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Название услуги");
        layout.addView(nameInput);

        EditText priceInput = new EditText(this);
        priceInput.setHint("Цена");
        priceInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(priceInput);

        EditText descriptionInput = new EditText(this);
        descriptionInput.setHint("Описание");
        layout.addView(descriptionInput);

        builder.setView(layout);

        builder.setPositiveButton("Добавить", (dialog, which) -> {
            String name = nameInput.getText().toString();
            String price = priceInput.getText().toString();
            String description = descriptionInput.getText().toString();

            if (!name.isEmpty() && !price.isEmpty() && !description.isEmpty()) {
                addService(name, price, description);
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void addService(String name, String price, String description) {
        try {
            JSONObject newServiceData = new JSONObject();
            newServiceData.put("service_name", name);
            newServiceData.put("price", Double.parseDouble(price));
            newServiceData.put("description", description);
            newServiceData.put("id_category", categoryId);

            serviceViewModel.addService(newServiceData, token, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(ServicesAdminActivity.this, "Ошибка при добавлении услуги", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        runOnUiThread(() -> {
                            Toast.makeText(ServicesAdminActivity.this, "Услуга добавлена", Toast.LENGTH_SHORT).show();
                            observeServices();
                        });
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(ServicesAdminActivity.this, "Ошибка сервера при добавлении", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка формирования данных", Toast.LENGTH_SHORT).show();
        }
    }
}
