package com.example.application.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.application.data.model.Category;
import com.example.application.data.repository.CategoryRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class CategoryViewModel extends ViewModel {
    private static final String BASE_URL = "https://automser.store/api/";
    private final CategoryRepository repository;

    public CategoryViewModel() {
        this.repository = new CategoryRepository();
    }

    public LiveData<List<Category>> getCategories(String token) {
        return repository.fetchCategories(token);
    }

    public void updateCategory(int categoryId, String newCategoryName, String token, Callback callback) {
        repository.updateCategory(categoryId, newCategoryName, token, callback);
    }

    public void deleteCategory(int categoryId, String token, Callback callback) {
        repository.deleteCategory(categoryId, token, callback);
    }

    public void createCategory(String categoryName, String token, Callback callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("category_name", categoryName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "/categories")
                .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + token)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(callback);
    }

}
