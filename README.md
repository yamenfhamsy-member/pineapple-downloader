# Pineapple Downloader 🍍

تطبيق أندرويد (Kotlin + Jetpack Compose) لتحميل الفيديو والصوت من يوتيوب ومئات المواقع عبر **yt-dlp** (يعمل داخل التطبيق بواسطة Chaquopy) مع دمج وتحويل عبر FFmpeg. الواجهة عربية كاملة (RTL).

## الهوية البصرية
مستوحاة من شعار "Pineapple mango" — ممنوع استخدام أي لون خارج الجدول إلا `#121212` للوضع الداكن:

| الدور | Hex |
|---|---|
| Primary أخضر الأوراق | `#1B8A3E` |
| Primary Variant | `#4CAF50` |
| Secondary جسم الأناناس | `#F5A623` |
| Secondary Variant | `#E8850C` |
| On Background أسود ناعم | `#1A1A1A` |
| Background أبيض | `#FFFFFF` |
| Surface Variant رمادي فاتح | `#F5F5F5` |
| Outline رمادي متوسط | `#E0E0E0` |
| Error | `#D32F2F` |
| Dark background | `#121212` |

- الخط: **Cairo** (Google Fonts، OFL) عبر Downloadable Fonts — `res/font/cairo.xml` + شهادات GMS الرسمية في `res/values/font_certs.xml` (لا TTF مُضمّن؛ fallback تلقائي لخط النظام).
- الشعار الحالي `drawable/pineapple_logo.xml` هو placeholder مرسوم كـ vector؛ لاستبداله بـ PNG حقيقي أضف `pineapple_logo.png` في `drawable/` (نفس الاسم يُلتقط في الـ TopBar والـ Splash والأيقونة تلقائيًا).
- الأيقونة: `mipmap-anydpi-v26` adaptive بخلفية بيضاء (API 26+)، والـ manifest يشير مؤقتًا إلى الـ vector مباشرة ليعمل على API 24–25؛ عند إضافة PNG بدّل السطر إلى `@mipmap/ic_launcher`.

## البنية
- `python/yt_bridge.py` — غلاف yt-dlp (جلب معلومات + تحميل + progress hook).
- `python/YtDlpBridge.kt` — استدعاء Chaquopy في `Dispatchers.IO`.
- `download/` — إدارة التحميل + معالجة FFmpeg + سجل DataStore.
- `service/DownloadService.kt` — Foreground service بإشعار أخضر وزر إلغاء.
- `ui/` — theme/components/screens بتصميم غير متمركز (عنوان يمين، إجراء في BottomBar).

## قرارات تقنية
- **Chaquopy 17.0.0 + Python 3.11 + yt-dlp + certifi** (آخر مستقر؛ يدعم AGP حتى 9.2).
- **FFmpeg:** الأصل `com.arthenica` متقاعد ومحذوف من Maven (أبريل 2025)، لذا نستخدم البديل الموثق `dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.7` — نفس الحزمة والـ API (`FFmpegKit.execute`)، تغيير group فقط. `full` لأن mp3 يحتاج `libmp3lame`. الـ fork يبني **arm64-v8a فقط** → `abiFilters ["arm64-v8a"]`؛ الأجهزة الأخرى تُحمّل mp4/m4a مباشرة بدون تحويل (انظر `FfmpegPostProcessor.isSupported`).
- **SDK:** compile/target 35 (شرط Play + شرط الـ fork)، min 24.
- **الحفظ:** MediaStore `Downloads/Pineapple/` على Android 10+، لا حاجة لـ MANAGE_STORAGE.

## البناء محليًا
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## إخلاء المسؤولية
حمّل فقط المحتوى الذي تملكه أو المسموح بتحميله. احترام شروط الاستخدام الخاصة بكل موقع وحقوق النشر مسؤوليتك وحدك.
