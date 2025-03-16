package com.example.application.data.model;

public class Category {
    private int idCategory;
    private String categoryName;

    public Category(int idCategory, String categoryName) {
        this.idCategory = idCategory;
        this.categoryName = categoryName;
    }

    public int getIdCategory() {
        return idCategory;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
