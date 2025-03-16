package com.example.application.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.application.data.model.Category;
import com.example.application.data.repository.CategoryRepository;

import java.util.List;

import okhttp3.Callback;

public class CategoryViewModel extends ViewModel {
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

}
