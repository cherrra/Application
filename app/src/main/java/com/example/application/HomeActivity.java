package com.example.application;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class HomeActivity extends AppCompatActivity {
    private EncryptedSharedPrefs encryptedSharedPrefs;
    private GridLayout categoryContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        WebView webView = findViewById(R.id.webview);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        String url = "https://yandex.ru/maps/?ll=37.618423%2C55.751244&z=10&pt=37.618423,55.751244,pm2rdl";
        webView.loadUrl(url);

        TextView homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        TextView accountButton = findViewById(R.id.accountButton);
        accountButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AccountActivity.class);
            startActivity(intent);
        });

        TextView garageButton = findViewById(R.id.garageButton);
        garageButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, GarageActivity.class);
            startActivity(intent);
        });

        TextView orderButton = findViewById(R.id.orderButton);
        orderButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, OrdersActivity.class);
            startActivity(intent);
        });

        categoryContainer = findViewById(R.id.categoryContainer);

        try {
            encryptedSharedPrefs = new EncryptedSharedPrefs(this);
            String token = encryptedSharedPrefs.getToken();
            if (token != null) {
                fetchCategories(token);
            } else {
                Toast.makeText(this, "Необходима авторизация", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("HomeActivity", "Ошибка инициализации: " + e.getMessage());
        }
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
                        Toast.makeText(HomeActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(HomeActivity.this, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
                        );
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(HomeActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

   private void addCategoryCard(JSONObject categoryObject) throws JSONException {
       GridLayout card = new GridLayout(this);
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

       card.setOnClickListener(v -> {
           try {
               int categoryId = categoryObject.getInt("id_category");
               Intent intent = new Intent(HomeActivity.this, ServicesActivity.class);
               intent.putExtra("id_category", categoryId);
               serviceLauncher.launch(intent);
           } catch (JSONException e) {
               e.printStackTrace();
           }
       });

       categoryContainer.addView(card);
   }

    private ActivityResultLauncher<Intent> serviceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    setResult(RESULT_OK, result.getData());
                    finish();
                }
            }
    );
}
