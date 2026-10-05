package com.pineapple.downloader

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class PineappleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // تهيئة Chaquopy مرة واحدة قبل أي استدعاء لـ Python.getInstance()
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}
