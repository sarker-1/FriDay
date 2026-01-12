package com.friday.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestMicPermission()
        initSpeech()

        val powerButton = findViewById<ImageView>(R.id.powerButton)

        powerButton.setOnClickListener {
            if (!isListening) {
                Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show()
                startListening()
                isListening = true
            } else {
                speechRecognizer.stopListening()
                Toast.makeText(this, "Stopped listening", Toast.LENGTH_SHORT).show()
                isListening = false
            }
        }
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

    private fun initSpeech() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(
            SimpleRecognitionListener { text ->
                handleCommand(text)
                isListening = false
            }
        )
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.startListening(intent)
    }

    // 🔴 MAIN LOGIC — এখানে সব voice command handle হবে
    private fun handleCommand(command: String) {
        val text = command.lowercase()

        when {
            text.contains("hello") -> {
                Toast.makeText(this, "Hello Boss 👋", Toast.LENGTH_SHORT).show()
            }

            text.contains("wifi on") -> {
                Toast.makeText(this, "WiFi ON command detected", Toast.LENGTH_SHORT).show()
            }

            text.contains("wifi off") -> {
                Toast.makeText(this, "WiFi OFF command detected", Toast.LENGTH_SHORT).show()
            }

            text.contains("flashlight on") -> {
                Toast.makeText(this, "Flashlight ON command detected", Toast.LENGTH_SHORT).show()
            }

            text.contains("flashlight off") -> {
                Toast.makeText(this, "Flashlight OFF command detected", Toast.LENGTH_SHORT).show()
            }

            else -> {
                Toast.makeText(this, "Command not recognized", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
