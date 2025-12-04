package com.example.giaodien

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp  // ✅ thêm import

@HiltAndroidApp // ✅ bắt buộc để tạo Hilt component
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}
