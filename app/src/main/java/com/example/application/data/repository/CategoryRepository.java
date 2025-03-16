package com.example.application.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.application.data.model.Category;
import com.example.application.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CategoryRepository {
    public LiveData<List<Category>> fetchCategories(String token) {
        MutableLiveData<List<Category>> categoriesLiveData = new MutableLiveData<>();

        ApiClient.getInstance().getCategories(token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                categoriesLiveData.postValue(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<Category> categories = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            categories.add(new Category(
                                    jsonObject.getInt("id_category"),
                                    jsonObject.getString("category_name")
                            ));
                        }
                        categoriesLiveData.postValue(categories);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        categoriesLiveData.postValue(null);
                    }
                } else {
                    categoriesLiveData.postValue(null);
                }
            }
        });

        return categoriesLiveData;
    }

    // Метод для обновления категории
    public void updateCategory(int categoryId, String newCategoryName, String token, Callback callback) {
        // Создание JSON объекта для тела запроса
        JSONObject requestBodyJson = new JSONObject();
        try {
            requestBodyJson.put("category_name", newCategoryName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Преобразование JSON в RequestBody
        RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json"),
                requestBodyJson.toString()
        );

        // Отправка запроса на сервер
        ApiClient.getInstance().updateCategory(categoryId, requestBody, token, callback);
    }
    // Метод для удаления категории
    public void deleteCategory(int categoryId, String token, Callback callback) {
        // Отправка запроса на удаление категории
        ApiClient.getInstance().deleteCategory(categoryId, token, callback);
    }

}
