package com.friday.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔋 Start FriDay foreground background service
        val serviceIntent = Intent(this, FriDayService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        // 🔊 Text to Speech
        tts = TextToSpeech(this, this)

        // 🔦 Camera manager for flashlight
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull()

        requestMicPermission()
        requestIgnoreBatteryOptimization()
        initSpeech()
    }

    // 🔊 TTS init
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            speak("Hello Boss. FriDay is ready. Please allow battery optimization exemption for best performance.")
            startListening()
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun requestMicPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )
        }
    }

    // 🔋 Battery optimization ignore request (user consent)
    private fun requestIgnoreBatteryOptimization() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        } catch (e: Exception) {
            // silently ignore
        }
    }

    private fun initSpeech() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(
            SimpleRecognitionListener { text ->
                isListening = false
                handleCommand(text)
            }
        )
    }

    private fun startListening() {
        if (isListening) return
        isListening = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speechRecognizer.startListening(intent)
    }

    // 🎤 MAIN COMMAND LOGIC
    private fun handleCommand(command: String) {
        val text = command.lowercase()

        when {
            text.contains("hello") -> {
                speak("Hello Boss. How can I help you?")
            }

            text.contains("flashlight on") -> {
                turnFlashlight(true)
                speak("Flashlight is now on.")
            }

            text.contains("flashlight off") -> {
                turnFlashlight(false)
                speak("Flashlight is now off.")
            }

            // 📶 LEGIT WiFi control (System panel)
            text.contains("wifi on") || text.contains("wifi off") -> {
                speak("Opening WiFi control.")
                val intent = Intent(Settings.Panel.ACTION_WIFI)
                startActivity(intent)
            }

            // 📶 Fallback
            text.contains("open wifi settings") -> {
                speak("Opening WiFi settings.")
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                startActivity(intent)
            }

            else -> {
                speak("Sorry Boss, I did not understand.")
            }
        }

        // 🔁 Restart listening safely
        startListening()
    }

    // 🔦 REAL Flashlight control
    private fun turnFlashlight(state: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraId?.let {
                    cameraManager.setTorchMode(it, state)
                }
            }
        } catch (e: Exception) {
            speak("Flashlight control failed.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tts.shutdown()
    }
}
