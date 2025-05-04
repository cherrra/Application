package com.example.application.data.repository;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.application.data.model.Brand;
import com.example.application.data.model.Car;
import com.example.application.data.model.Model;
import com.example.application.network.ApiClient;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CarRepository {
    private static final String BASE_URL = "https://automser.store/api/";
    private final OkHttpClient client = new OkHttpClient();
    private final ApiClient apiClient = ApiClient.getInstance();

    public LiveData<List<Car>> getCars(String token) {
        MutableLiveData<List<Car>> carsLiveData = new MutableLiveData<>();
        Request request = new Request.Builder()
                .url(BASE_URL + "cars")
                .addHeader("Authorization", "Bearer " + token)
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
                            car.setLinkImg(carObject.optString("link_img", null));

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
            // Получаем данные бренда и модели
            String brandName = car.getModel().getBrand().getBrandName();
            String modelName = car.getModel().getModelName();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("brand_name", brandName);
            jsonObject.put("model_name", modelName);
            jsonObject.put("year", car.getYear());
            jsonObject.put("mileage", car.getMileage());
            jsonObject.put("vin_code", car.getVinCode());
            jsonObject.put("license_plate", car.getLicensePlate());

            MediaType JSON = MediaType.get("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .post(body)
                    .addHeader("Authorization", token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("CarRepository", "Ошибка добавления: " + e.getMessage());
                    result.postValue(false);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e("CarRepository", "Ошибка сервера: " + response.code() + " " + response.body().string());
                    }
                    result.postValue(response.isSuccessful());
                }
            });
        } catch (Exception e) {
            Log.e("CarRepository", "Ошибка формирования JSON: " + e.getMessage());
            result.postValue(false);
        }
    }

    public void uploadCarImage(Uri imageUri, String token, int carId, Callback callback, Context context) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            String fileName = getFileName(imageUri, context);

            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("car_id", String.valueOf(carId))
                    .addFormDataPart("image", fileName,
                            RequestBody.create(bitmapToBytes(bitmap), MediaType.parse("image/jpeg")));

            RequestBody body = builder.build();

            Log.d("CarRepository", "Uploading image for car ID: " + carId);

            apiClient.uploadImageCar(body, token, carId, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("CarRepository", "Ошибка загрузки изображения: " + e.getMessage());
                    callback.onFailure(call, e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    Log.d("CarRepository", "Ответ сервера: " + response.code());
                    callback.onResponse(call, response);  // проксируем
                }

            });
        } catch (IOException e) {
            Log.e("CarRepository", "Ошибка обработки изображения: " + e.getMessage());
        }
    }

    private byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
        return stream.toByteArray();
    }

    private String getFileName(Uri uri, Context context) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }

    public void updateCar(Car car, String token, MutableLiveData<Boolean> result) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("brand_name", car.getModel().getBrand().getBrandName());
            jsonObject.put("model_name", car.getModel().getModelName());
            jsonObject.put("year", car.getYear());
            jsonObject.put("mileage", car.getMileage());
            jsonObject.put("vin_code", car.getVinCode());
            jsonObject.put("license_plate", car.getLicensePlate());

            RequestBody body = RequestBody.create(
                    jsonObject.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/" + car.getIdCar())
                    .put(body)
                    .addHeader("Authorization", token)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    result.postValue(false);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    result.postValue(response.isSuccessful());
                }
            });
        } catch (Exception e) {
            result.postValue(false);
        }
    }
}


