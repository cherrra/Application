package com.example.application.data.model;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private int idOrder;
    private String carModel;
    private String orderDate;
    private String orderTime;
    private String services;
    private double price;
    private Status status;
    private String comment;
    private int id;

    // Конструктор
    public Order(int idOrder, String carModel, String orderDate, String orderTime, String services, double price, Status status, String comment, int id) {
        this.idOrder = idOrder;
        this.carModel = carModel;
        this.orderDate = orderDate;
        this.orderTime = orderTime;
        this.services = services;
        this.price = price;
        this.status = status;
        this.comment = comment;
        this.id = id;
    }

    // Геттеры и сеттеры
    public int getIdOrder() {
        return idOrder;
    }

    public void setIdOrder(int idOrder) {
        this.idOrder = idOrder;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(String orderTime) {
        this.orderTime = orderTime;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getComment() {return comment;}

    public void setComment(String comment) {
        this.comment = comment;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    // Enum для статуса заказа
    public enum Status {
        ACCEPTED("accepted"),
        IN_PROGRESS("in_progress"),
        FINISHED("finished"),
        CREATED("created");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Status fromString(String value) {
            for (Status status : Status.values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown status value: " + value);
        }
    }

    // Метод для парсинга из JSON массива
    public static List<Order> fromJsonArray(JSONArray jsonArray) {
        List<Order> orders = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                orders.add(new Order(
                        obj.getInt("id_order"),
                        obj.getString("car_model"),
                        obj.getString("order_date"),
                        obj.getString("order_time"),
                        obj.getString("services"),
                        obj.getDouble("total_price"),
                        Status.fromString(obj.getString("status")),
                        obj.optString("comment", ""),
                        obj.getInt("id")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orders;
    }
}
