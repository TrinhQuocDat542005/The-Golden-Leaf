package com.example.giaodien

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp  // ✅ thêm import

@HiltAndroidApp // ✅ bắt buộc để tạo Hilt component
class MyApp : Application(), coil.ImageLoaderFactory {
    override fun newImageLoader() = coil.ImageLoader.Builder(this)
        .components { add(coil.decode.SvgDecoder.Factory()) }.build()
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        if (!BuildConfig.DEMO_MODE) com.example.giaodien.notifications.DeviceBinding.start()
    }
}
