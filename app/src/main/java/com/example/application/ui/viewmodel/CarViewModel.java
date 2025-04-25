package com.example.application.ui.viewmodel;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.application.data.model.Car;
import com.example.application.data.repository.CarRepository;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class CarViewModel extends ViewModel {
    private final CarRepository repository = new CarRepository();
    private final MutableLiveData<Car> selectedCar = new MutableLiveData<>();
    private final MutableLiveData<Boolean> carAddedStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> imageUploadStatus = new MutableLiveData<>();

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

    public LiveData<Boolean> uploadCarImage(Uri imageUri, String token, int carId, Context context) {
        repository.uploadCarImage(imageUri, token, carId, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("CarViewModel", "Ошибка загрузки изображения: " + e.getMessage());
                imageUploadStatus.postValue(false);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    // Читаем тело ответа только здесь
                    String responseBody = response.body() != null ? response.body().string() : "null";
                    Log.d("CarViewModel", "Ответ сервера: " + response.code() + " " + responseBody);
                    imageUploadStatus.postValue(response.isSuccessful());
                } catch (IOException e) {
                    Log.e("CarViewModel", "Ошибка чтения ответа: " + e.getMessage());
                    imageUploadStatus.postValue(false);
                }
            }
        }, context);

    return imageUploadStatus;
    }
}