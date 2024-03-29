package com.example.imageia;


import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;


public class HttpPostManager {
    public static TextToSpeech textToSpeech;

    public static interface OnResponseListener {
        void onResponse(String response);
        void onError(Exception e);
    }

    public static String encodeImageToBase64(File imageFile) throws IOException {
        // Check if the file exists
        if (!imageFile.exists()) {
            throw new FileNotFoundException("File does not exist: " + imageFile.getAbsolutePath());
        }

        // Read the image file into a byte array
        byte[] imageBytes = new byte[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            imageBytes = Files.readAllBytes(imageFile.toPath());
        }

        // Encode the byte array to base64 string
        String encodedImage = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            encodedImage = Base64.getEncoder().encodeToString(imageBytes);
        }

        return encodedImage;
    }


    public static void sendPostRequest(String url, JSONObject jsonObject, OnResponseListener listener) {
        // Use a new thread for network operations
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.MINUTES).build();
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                RequestBody requestBody = RequestBody.create(jsonObject.toString(), JSON);
                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    final String responseBody = response.body().string();

                    // Use a handler to post back to the main thread for UI updates
                    new Handler(Looper.getMainLooper()).post(() -> {
                        listener.onResponse(responseBody);
                    });
                } else {
                    throw new Exception("Request failed: " + response.code());
                }
            } catch (Exception e) {
                // Use a handler to post back to the main thread for error handling
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onError(e);
                });
            }
        }).start();
    }

    public static void sendPostImageRequest(String key,String url, JSONObject jsonObject, OnResponseListener listener) {
        // Use a new thread for network operations

        if (key == null) {
            key = "xd";
        }

        try {
            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.MINUTES).build();
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");

            RequestBody requestBody = RequestBody.create(JSON, jsonObject.toString());
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization","Bearer "+ key)
                    .post(requestBody)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                final String responseBody = response.body().string();

                // Use a handler to post back to the main thread for UI updates
                new Handler(Looper.getMainLooper()).post(() -> {
                    listener.onResponse(responseBody);
                });
            } else {
                throw new Exception("Request failed: " + response.code());
            }
        } catch (Exception e) {
            // Use a handler to post back to the main thread for error handling
            new Handler(Looper.getMainLooper()).post(() -> {
                listener.onError(e);
            });
        }
    }


}