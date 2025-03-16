package com.example.application.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.application.data.model.Service;
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

public class ServiceRepository {
    public LiveData<List<Service>> fetchServices(int categoryId, String token) {
        MutableLiveData<List<Service>> servicesLiveData = new MutableLiveData<>();

        ApiClient.getInstance().getServices(categoryId, token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                servicesLiveData.postValue(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(response.body().string());
                        List<Service> services = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            services.add(new Service(
                                    jsonObject.getInt("id_service"),
                                    jsonObject.getString("service_name"),
                                    jsonObject.optString("description", ""),
                                    jsonObject.getDouble("price"),
                                    jsonObject.getInt("id_category")
                            ));
                        }
                        servicesLiveData.postValue(services);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        servicesLiveData.postValue(null);
                    }
                } else {
                    servicesLiveData.postValue(null);
                }
            }
        });

        return servicesLiveData;
    }


    public void addService(JSONObject newServiceData, String token, Callback callback) {
        RequestBody body = RequestBody.create(newServiceData.toString(), MediaType.get("application/json"));

        ApiClient.getInstance().addService(body, token, callback);
    }

    public void updateService(int serviceId, JSONObject updateData, String token, Callback callback) {
        RequestBody body = RequestBody.create(updateData.toString(), MediaType.get("application/json"));

        ApiClient.getInstance().updateService(serviceId, body, token, callback);
    }

    public void deleteService(int serviceId, String token, Callback callback) {
        ApiClient.getInstance().deleteService(serviceId, token, callback);
    }
}
