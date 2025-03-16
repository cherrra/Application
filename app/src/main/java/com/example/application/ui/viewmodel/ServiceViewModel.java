package com.example.application.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.application.data.model.Service;
import com.example.application.data.repository.ServiceRepository;

import org.json.JSONObject;

import java.util.List;

import okhttp3.Callback;

public class ServiceViewModel extends ViewModel {
    private final ServiceRepository repository;

    public ServiceViewModel() {
        this.repository = new ServiceRepository();
    }

    public LiveData<List<Service>> getServices(int categoryId, String token) {
        return repository.fetchServices(categoryId, token);
    }


    public void addService(JSONObject newServiceData, String token, Callback callback) {
        repository.addService(newServiceData, token, callback);
    }

    public void updateService(int serviceId, JSONObject updateData, String token, Callback callback) {
        repository.updateService(serviceId, updateData, token, callback);
    }

    public void deleteService(int serviceId, String token, Callback callback) {
        repository.deleteService(serviceId, token, callback);
    }
}
