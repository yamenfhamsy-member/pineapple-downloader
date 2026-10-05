plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.chaquo.python")
}

android {
    namespace = "com.pineapple.downloader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pineapple.downloader"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
        // arm64-v8a فقط: fork الـ ffmpeg الموثق (dev.ffmpegkit-maintained) يبني arm64 فقط
        // مع 16KB page alignment لمتطلبات Play (تشرين الثاني 2025+).
        // الأجهزة الأخرى تعمل بتحميل مباشر mp4/m4a بدون تحويل mp3 (fallback في FfmpegPostProcessor).
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    chaquopy {
        defaultConfig {
            // Python 3.11 ضمن نطاق Chaquopy 17 (3.10 - 3.14)
            version = "3.11"
            pip {
                // yt-dlp: محرك التحميل (يُحدَّث أسبوعيًا — حدّث الرقم عند الحاجة)
                install("yt-dlp")
                // certifi: إصلاح SSL داخل بيئة Chaquopy
                install("certifi")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources { excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*") }
        // Chaquopy + ffmpeg يجلبان .so مكررة أحيانًا — الأولوية للأولى
        jniLibs { pickFirsts += setOf("**/libc++_shared.so") }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose + Material3
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.7.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Splash + Notifications compat
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Thumbnails
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // خط Cairo عبر Downloadable Fonts (إصدار مُدار من Compose BOM)
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // FFmpeg — البديل الموثق بعد تقاعد com.arthenica (محذوف من Maven أبريل 2025).
    // نفس الـ API تمامًا (com.arthenica.ffmpegkit.FFmpegKit) — تغيير group فقط.
    // full (وليس audio) لأننا نحتاج libmp3lame + دمج mp4. مثبت الإصدار — ممنوع +.
    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.7")
}
