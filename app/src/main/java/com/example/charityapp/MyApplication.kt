package com.example.charityapp

import android.app.Application
import vn.zalopay.sdk.Environment
import vn.zalopay.sdk.ZaloPaySDK

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo ZaloPay SDK ở môi trường Sandbox (Dùng cho test)
        ZaloPaySDK.init(2553, Environment.SANDBOX) // AppID sandbox mặc định của ZaloPay là 2553
    }
}