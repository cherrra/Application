package com.example.application.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.application.data.model.Car;
import com.example.application.data.repository.CarRepository;

import java.util.List;

public class CarViewModel extends ViewModel {
    private final CarRepository repository = new CarRepository();
    private final MutableLiveData<Car> selectedCar = new MutableLiveData<>();
    private final MutableLiveData<Boolean> carAddedStatus = new MutableLiveData<>();

    public LiveData<List<Car>> getCars(String token) {
        return repository.getCars(token);
    }

    public void setSelectedCar(Car car) {
        selectedCar.setValue(car);
    }

    public LiveData<Car> getSelectedCar() {
        return selectedCar;
    }


    public void addCar(Car car, String token) {
        repository.addCar(car, token, carAddedStatus);
    }

    public LiveData<Boolean> getCarAddedStatus() {
        return carAddedStatus;
    }

}
