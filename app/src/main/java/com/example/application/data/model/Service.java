package com.example.application.data.model;

public class Service {
    private int idService;
    private String serviceName;
    private String description;
    private double price;
    private int idCategory;

    public Service(int idService, String serviceName, String description, double price, int idCategory) {
        this.idService = idService;
        this.serviceName = serviceName;
        this.description = description;
        this.price = price;
        this.idCategory = idCategory;
    }

    public int getIdService() {
        return idService;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public int getIdCategory() {
        return idCategory;
    }
}
