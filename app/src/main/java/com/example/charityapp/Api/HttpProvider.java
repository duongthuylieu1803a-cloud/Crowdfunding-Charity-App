package com.example.charityapp.Api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class HttpProvider {
    public static JSONObject sendPost(String urlString, RequestBody formBody) {
        JSONObject data = null;

        // Cấu hình bảo mật TLS cho kết nối HTTPS tới ZaloPay Sandbox
        ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .cipherSuites(
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
                .build();

        // Tăng timeout lên 15 giây để tránh lỗi kết nối chậm trên môi trường Sandbox
        OkHttpClient client = new OkHttpClient.Builder()
                .connectionSpecs(Collections.singletonList(spec))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(urlString)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                String responseBodyString = response.body().string();
                if (!response.isSuccessful()) {
                    Log.e("HTTP_ERROR", "Code: " + response.code() + " Body: " + responseBodyString);
                } else {
                    data = new JSONObject(responseBodyString);
                }
            }
        } catch (IOException | JSONException e) {
            Log.e("HTTP_EXCEPTION", e.getMessage(), e);
        }

        return data;
    }
}