package com.example.application.ui.viewmodel;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.application.data.model.User;
import com.example.application.data.repository.UserRepository;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserViewModel extends ViewModel {
    private final UserRepository userRepository;

    public UserViewModel() {
        this.userRepository = new UserRepository();
    }

    // Получение данных пользователя
    public LiveData<User> getUser(String token) {
        Log.d("UserViewModel", "Запрос данных пользователя с токеном: " + token);
        return userRepository.getUserData(token);
    }

    // Обновление данных пользователя
    public LiveData<Boolean> updateUser(Map<String, String> updatedData, String token) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();

        String json = new JSONObject(updatedData).toString();
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));

        userRepository.updateUser(body, token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("UserViewModel", "Ошибка обновления данных: " + e.getMessage());
                resultLiveData.postValue(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                resultLiveData.postValue(response.isSuccessful());
            }
        });

        return resultLiveData;
    }

    // Загрузка изображения
    public LiveData<Boolean> uploadImage(Uri imageUri, String token, Context context) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();

        userRepository.uploadImage(imageUri, token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("UserViewModel", "Ошибка загрузки изображения: " + e.getMessage());
                resultLiveData.postValue(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                resultLiveData.postValue(response.isSuccessful());
            }
        }, context);

        return resultLiveData;
    }

    // Получение списка пользователей
    public LiveData<Boolean> fetchUsers(String token, Callback callback) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        userRepository.fetchUsers(token, callback);
        return resultLiveData;
    }

    // Удаление пользователя
    public LiveData<Boolean> deleteUser(int userId, String token, Callback callback) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        userRepository.deleteUser(userId, token, callback);
        return resultLiveData;
    }
}
