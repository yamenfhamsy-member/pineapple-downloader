"""غلاف yt-dlp يعمل داخل Chaquopy.

يُستدعى من Kotlin عبر:
    Python.getInstance().getModule("yt_bridge")
على Dispatchers.IO حصرًا (عمليات شبكية/ثقيلة).

الدوال العامة ترجع JSON string يفكّه Kotlin عبر org.json.
التقدم يُمرَّر عبر progress hook يخزن آخر حالة في متغير عام يقرأه Kotlin
polling — أبسط من callback عكسي ويعمل بثبات مع Chaquopy.
"""

import json

# ملاحظة: كل الدوال العامة ترجع JSON string (وليس dict) لأن Kotlin يفكّه
# عبر org.json — أثبت من asMap عبر حدود Chaquopy.

_state = {
    "status": "idle",      # idle | downloading | finished | error
    "downloaded": 0,
    "total": 0,
    "speed": 0.0,          # bytes/sec
    "eta": 0,              # seconds
    "filename": "",
    "error": "",
}


def _reset_state():
    _state.update(
        status="idle",
        downloaded=0,
        total=0,
        speed=0.0,
        eta=0,
        filename="",
        error="",
    )


def _progress_hook(d):
    try:
        status = d.get("status")
        if status == "downloading":
            _state["status"] = "downloading"
            _state["downloaded"] = int(d.get("downloaded_bytes") or 0)
            _state["total"] = int(d.get("total_bytes") or d.get("total_bytes_estimate") or 0)
            _state["speed"] = float(d.get("speed") or 0)
            _state["eta"] = int(d.get("eta") or 0)
            _state["filename"] = str(d.get("filename") or "")
        elif status == "finished":
            _state["status"] = "finished"
            _state["filename"] = str(d.get("filename") or "")
    except Exception:
        pass


def get_info(url):
    """يرجع معلومات الفيديو بدون تحميل. يرمي استثناء برسالة نصية عند الفشل."""
    import yt_dlp

    opts = {
        "quiet": True,
        "no_warnings": True,
        "socket_timeout": 20,
        "noplaylist": True,
        # certifi لإصلاح SSL داخل Chaquopy
        "cacert": __import__("certifi").where(),
    }
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)
        if info is None:
            raise ValueError("تعذر جلب المعلومات")
        formats = []
        for f in info.get("formats") or []:
            try:
                formats.append(
                    {
                        "format_id": str(f.get("format_id") or ""),
                        "ext": str(f.get("ext") or ""),
                        "resolution": str(f.get("resolution") or f.get("format_note") or ""),
                        "vcodec": str(f.get("vcodec") or ""),
                        "acodec": str(f.get("acodec") or ""),
                        "filesize": int(f.get("filesize") or f.get("filesize_approx") or 0),
                        "tbr": float(f.get("tbr") or 0),
                        "is_audio_only": bool(
                            (f.get("vcodec") in (None, "none"))
                            and (f.get("acodec") not in (None, "none"))
                        ),
                    }
                )
            except Exception:
                continue
        return json.dumps(
            {
                "title": str(info.get("title") or "بدون عنوان"),
                "duration": int(info.get("duration") or 0),
                "thumbnail": str(info.get("thumbnail") or ""),
                "uploader": str(info.get("uploader") or info.get("channel") or ""),
                "webpage_url": str(info.get("webpage_url") or url),
                "formats": formats,
            },
            ensure_ascii=False,
        )
    except Exception as e:
        msg = str(e).strip()
        raise ValueError(_friendly_error(msg))


def download(url, outtmpl, format_selector, audio_only):
    """يحمّل ملفًا واحدًا. outtmpl مسار كامل بدون امتداد (يضيف yt-dlp الامتداد).

    يعيد اسم الملف النهائي. التقدم يُقرأ عبر poll_progress().
    """
    import yt_dlp

    _reset_state()
    _state["status"] = "downloading"
    opts = {
        "quiet": True,
        "no_warnings": True,
        "socket_timeout": 30,
        "noplaylist": True,
        "outtmpl": outtmpl + ".%(ext)s",
        "format": format_selector,
        "progress_hooks": [_progress_hook],
        "cacert": __import__("certifi").where(),
    }
    if audio_only:
        # أفضل صوت بدون إعادة ترميز هنا — التحويل لـ mp3 يتم عبر FFmpeg في Kotlin
        opts["format"] = "bestaudio/best"
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            ydl.download([url])
        _state["status"] = "finished"
        return json.dumps({"filename": _state.get("filename", ""), "status": "finished"}, ensure_ascii=False)
    except Exception as e:
        _state["status"] = "error"
        _state["error"] = _friendly_error(str(e).strip())
        raise ValueError(_state["error"])


def poll_progress():
    """لقطة من آخر حالة تقدم (يستدعيها Kotlin دوريًا) — JSON string."""
    return json.dumps(dict(_state), ensure_ascii=False)


def _friendly_error(msg):
    low = (msg or "").lower()
    if "unsupported url" in low or "unsupported" in low:
        return "هذا الرابط غير مدعوم. تأكد أنه رابط فيديو مباشر."
    if "private" in low:
        return "هذا الفيديو خاص ولا يمكن تحميله."
    if "age" in low and "confirm" in low:
        return "هذا الفيديو مقيد بالعمر ولا يمكن جلبه بدون تسجيل دخول."
    if "timeout" in low or "timed out" in low:
        return "انتهت مهلة الاتصال. تحقق من الإنترنت وحاول مجددًا."
    if "network" in low or "name resolution" in low or "failed to resolve" in low:
        return "لا يوجد اتصال بالإنترنت."
    if "http error 403" in low:
        return "رفض الموقع الطلب (403). جرّب رابطًا آخر."
    if "http error 404" in low:
        return "الفيديو غير موجود (404)."
    if not msg:
        return "حدث خطأ غير معروف أثناء العملية."
    return msg
