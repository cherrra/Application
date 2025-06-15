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

import com.example.application.data.model.User;
import com.example.application.network.ApiClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserRepository {
    public LiveData<User> getUserData(String token) {
        MutableLiveData<User> userLiveData = new MutableLiveData<>();

        ApiClient.getInstance().getUserDetails(token, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                userLiveData.postValue(null);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        Log.d("UserRepository", "Response JSON: " + jsonObject.toString());

                        int id = jsonObject.getInt("id");
                        String username = jsonObject.getString("username");
                        String email = jsonObject.getString("email");


                        String birthDateString = jsonObject.optString("birth_date", null);
                        String formattedDate = formatDate(birthDateString);

                        String phoneNumber = jsonObject.optString("phone_number", null);
                        String linkImg = jsonObject.optString("link_img", null);
                        boolean isAdmin = jsonObject.optBoolean("is_admin", false);
                        boolean idAdmin = jsonObject.optInt("id_admin", 0) == 1;
                        int idCar = jsonObject.optInt("id_car", 0);


                        User user = new User(
                                id,
                                username,
                                email,
                                formattedDate,
                                phoneNumber,
                                linkImg,
                                isAdmin,
                                idAdmin,
                                idCar
                        );

                        userLiveData.postValue(user);
                    } catch (Exception e) {
                        Log.e("UserRepository", "Ошибка парсинга ответа: " + e.getMessage());
                        userLiveData.postValue(null);
                    }
                } else {
                    Log.e("UserRepository", "Ответ сервера: " + response.code());
                    userLiveData.postValue(null);
                }
            }

        });

        return userLiveData;
    }


    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) {
            return "Не указана";
        }

        try {
            if (rawDate.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                return rawDate;
            }

            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat desiredFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            Date date = isoFormat.parse(rawDate);
            return date != null ? desiredFormat.format(date) : "Не указана";
        } catch (ParseException e) {
            e.printStackTrace();
            return "Не указана";
        }
    }

    public void updateUser(RequestBody body, String token, Callback callback) {
        ApiClient.getInstance().updateUser(body, token, callback);
    }

    public void uploadImage(Uri imageUri, String token, Callback callback, Context context) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            String fileName = getFileName(imageUri, context);

            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            builder.addFormDataPart("image", fileName, RequestBody.create(bitmapToBytes(bitmap), MediaType.parse("image/jpeg")));

            RequestBody body = builder.build();

            ApiClient.getInstance().uploadImage(body, token, callback);
        } catch (IOException e) {
            Log.e("UserRepository", "Ошибка обработки изображения: " + e.getMessage());
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

    public void fetchUsers(String token, Callback callback) {
        ApiClient.getInstance().getUsers(token, callback);
    }

    public void deleteUser(int userId, String token, Callback callback) {
        ApiClient.getInstance().deleteUser(userId, token, callback);
    }

}