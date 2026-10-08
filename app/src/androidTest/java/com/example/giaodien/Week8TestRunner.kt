package com.example.giaodien

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import com.jakewharton.threetenabp.AndroidThreeTen

/** Component/API tests must not start Firebase/FCM DeviceBinding from the real Application. */
class Week8TestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl,Week8TestApplication::class.java.name,context)
}
class Week8TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        check(BuildConfig.WEEK8_ISOLATED_TESTS) { "Use -Pweek8IsolatedTests=true for disposable instrumentation; never initialize live Firebase" }
        check(com.google.firebase.FirebaseApp.getApps(this).isEmpty()) { "Firebase auto-init must be disabled in the test target manifest" }
        AndroidThreeTen.init(this)
    }
}
