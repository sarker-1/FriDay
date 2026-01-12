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
    private lateinit var geminiHelper: GeminiHelper

    private var cameraId: String? = null
    private var isListening = false
    private var isSpeaking = false
    private var wakeActive = false   // 🔑 Wake word state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ContextCompat.startForegroundService(
            this,
            Intent(this, FriDayService::class.java)
        )

        tts = TextToSpeech(this, this)
        geminiHelper = GeminiHelper()

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList.firstOrNull()

        requestMicPermission()
        requestIgnoreBatteryOptimization()
        initSpeech()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            speak("FriDay is listening.")
        }
    }

    private fun speak(text: String) {
        isSpeaking = true
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FRIDAY_TTS")

        tts.setOnUtteranceProgressListener(object :
            android.speech.tts.UtteranceProgressListener() {

            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                startListening()
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                startListening()
            }
        })
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

    private fun requestIgnoreBatteryOptimization() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            }
        } catch (_: Exception) {}
    }

    private fun initSpeech() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(
            SimpleRecognitionListener { text ->
                isListening = false
                handleSpeech(text)
            }
        )
    }

    private fun startListening() {
        if (isListening || isSpeaking) return
        isListening = true

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
        }
        speechRecognizer.startListening(intent)
    }

    // 🧠 Wake word logic
    private fun handleSpeech(spokenText: String) {
        val text = spokenText.lowercase().trim()

        if (!wakeActive) {
            if (text.contains("friday")) {
                wakeActive = true
                speak("Yes?")
            } else {
                startListening()
            }
            return
        }

        wakeActive = false
        handleCommand(text)
    }

    // 🎤 COMMAND LOGIC
    private fun handleCommand(text: String) {

        when {
            // ✅ Custom identity
            text.contains("who are you") -> {
                speak("I'm FriDay. Your personal robot.")
            }

            // ✅ Greeting (always Hi!)
            text.contains("hello") || text.contains("hi") -> {
                speak("Hi!")
            }

            text.contains("flashlight on") -> {
                turnFlashlight(true)
                speak("Flashlight is now on.")
            }

            text.contains("flashlight off") -> {
                turnFlashlight(false)
                speak("Flashlight is now off.")
            }

            text.contains("wifi on") || text.contains("wifi off") -> {
                speak("Opening WiFi control.")
                startActivity(Intent(Settings.Panel.ACTION_WIFI))
            }

            else -> {
                speak("Thinking.")
                geminiHelper.getGeminiReply(text) { reply ->
                    runOnUiThread {
                        speak(reply.take(350))
                    }
                }
            }
        }
    }

    private fun turnFlashlight(state: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraId?.let {
                    cameraManager.setTorchMode(it, state)
                }
            }
        } catch (_: Exception) {
            speak("Flashlight control failed.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        tts.shutdown()
    }
}
