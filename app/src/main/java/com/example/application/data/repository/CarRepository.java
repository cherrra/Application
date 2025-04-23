package com.example.application.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.application.data.model.Brand;
import com.example.application.data.model.Car;
import com.example.application.data.model.Model;
import com.example.application.network.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CarRepository {
    private static final String BASE_URL = "http://10.0.2.2:5000/api/cars";
    private final OkHttpClient client = new OkHttpClient();
    private final ApiClient apiClient = ApiClient.getInstance();

    public LiveData<List<Car>> getCars(String token) {
        MutableLiveData<List<Car>> carsLiveData = new MutableLiveData<>();

        Request request = new Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", token)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("CarRepository", "Ошибка получения данных: " + e.getMessage());
                carsLiveData.postValue(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String responseBody = response.body().string();
                        Log.d("CarRepository", "Ответ сервера: " + responseBody);

                        JSONArray carsArray = new JSONArray(responseBody);
                        List<Car> cars = new ArrayList<>();

                        for (int i = 0; i < carsArray.length(); i++) {
                            JSONObject carObject = carsArray.getJSONObject(i);

                            // Создаем объект Brand
                            Brand brand = new Brand();
                            brand.setBrandName(carObject.getString("brand_name"));

                            // Создаем объект Model
                            Model model = new Model();
                            model.setModelName(carObject.getString("model_name"));
                            model.setBrand(brand);

                            // Создаем объект Car
                            Car car = new Car();
                            car.setIdCar(carObject.getInt("id_car"));
                            car.setModel(model);
                            car.setYear(carObject.getInt("year"));
                            car.setMileage(carObject.getInt("mileage"));
                            car.setVinCode(carObject.getString("vin_code"));
                            car.setLicensePlate(carObject.getString("license_plate"));

                            cars.add(car);
                        }
                        carsLiveData.postValue(cars);
                    } catch (Exception e) {
                        Log.e("CarRepository", "Ошибка обработки ответа: " + e.getMessage());
                        carsLiveData.postValue(null);
                    }
                } else {
                    Log.e("CarRepository", "Ошибка сервера: " + response.code());
                    carsLiveData.postValue(null);
                }
            }
        });
        return carsLiveData;
    }


    public void addCar(Car car, String token, MutableLiveData<Boolean> result) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("year", car.getYear());
            jsonObject.put("mileage", car.getMileage());
            jsonObject.put("vin_code", car.getVinCode());
            jsonObject.put("license_plate", car.getLicensePlate());

            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);

            apiClient.addCar(body, token, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("CarRepository", "Ошибка добавления: " + e.getMessage());
                    result.postValue(false);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    result.postValue(response.isSuccessful());
                }
            });
        } catch (Exception e) {
            Log.e("CarRepository", "Ошибка формирования JSON: " + e.getMessage());
            result.postValue(false);
        }
    }

}
