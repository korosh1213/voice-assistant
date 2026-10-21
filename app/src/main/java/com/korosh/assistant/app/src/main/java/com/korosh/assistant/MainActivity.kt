package com.korosh.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class MainActivity : Activity(), RecognitionListener {

    private var model: Model? = null
    private var loading = false
    private var speechService: SpeechService? = null
    private var listening = false
    private var handled = false

    private lateinit var statusText: TextView
    private lateinit var heardText: TextView
    private lateinit var button: Button

    private val openWords = listOf("باز کن", "بازکن", "اجرا کن", "باز بکن", "بازش کن", "باز کردن")
    private val callWords = listOf("زنگ بزن", "تماس بگیر", "شماره بگیر", "تلفن بزن")
    private val openFillers = setOf("برنامه", "اپلیکیشن", "اپ", "لطفا", "رو", "را", "برام", "برای", "من", "یه", "اون")
    private val callFillers = setOf("به", "با", "رو", "را", "لطفا", "برای", "من", "برام", "شماره")

    private val digits = mapOf(
        "صفر" to "0", "یک" to "1", "دو" to "2", "سه" to "3", "چهار" to "4",
        "پنج" to "5", "شش" to "6", "هفت" to "7", "هشت" to "8", "نه" to "9"
    )

    private val apps = listOf(
        Pair(listOf("واتساپ", "واتس اپ", "واتس آپ"), "com.whatsapp"),
        Pair(listOf("تلگرام"), "org.telegram.messenger"),
        Pair(listOf("اینستاگرام", "اینستا"), "com.instagram.android"),
        Pair(listOf("کروم", "مرورگر"), "com.android.chrome"),
        Pair(listOf("یوتیوب"), "com.google.android.youtube"),
        Pair(listOf("ایتا"), "ir.eitaa.messenger"),
        Pair(listOf("بله"), "ir.nasim"),
        Pair(listOf("روبیکا"), "ir.resaneh1.iptv"),
        Pair(listOf("بازار"), "com.farsitel.bazaar"),
        Pair(listOf("اسنپ"), "cab.snapp.passenger"),
        Pair(listOf("شاد"), "ir.medu.shad"),
        Pair(listOf("نقشه"), "com.google.android.apps.maps")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        val all = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )
        val missing = all.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            loadModel()
        } else {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            loadModel()
        } else {
            statusText.text = "برای شنیدن صدات به اجازه میکروفون نیاز
