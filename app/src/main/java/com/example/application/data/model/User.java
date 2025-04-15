package com.example.application.data.model;

public class User {
    private final int id;
    private final String username;
    private final String email;
    private final String birthDate; 
    private final String phoneNumber;
    private final String linkImg;
    private final boolean isAdmin;
    private final boolean idAdmin;
    private final int idCar;

    public User(int id, String username, String email, String birthDate,  String phoneNumber, String linkImg, boolean isAdmin, boolean idAdmin, int idCar) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.linkImg = linkImg;
        this.isAdmin = isAdmin;
        this.idAdmin = idAdmin;
        this.idCar = idCar;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getLinkImg() {
        return linkImg;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isIdAdmin() {
        return idAdmin;
    }

    public int getIdCar() {
        return idCar;
    }

}
