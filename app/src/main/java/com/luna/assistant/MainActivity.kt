package com.luna.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.Locale

enum class LunaState {
    IDLE, LISTENING, PROCESSING, SPEAKING
}

class MainActivity : ComponentActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private var lunaState by mutableStateOf(LunaState.IDLE)
    private var spokenText by mutableStateOf("Listening for \"Hey Luna\"...")
    private var lunaResponse by mutableStateOf("")

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startListening()
            } else {
                spokenText = "Microphone permission required."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initSpeechRecognizer()

        setContent {
            LunaOverlayUI(
                state = lunaState,
                spokenText = spokenText,
                response = lunaResponse,
                onMicClick = {
                    if (lunaState == LunaState.LISTENING) {
                        stopListening()
                    } else {
                        checkAndStartListening()
                    }
                },
                onDismiss = { finish() }
            )
        }

        checkAndStartListening()
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                lunaState = LunaState.LISTENING
                spokenText = "Listening..."
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                lunaState = LunaState.PROCESSING
                spokenText = "Thinking..."
            }

            override fun onError(error: Int) {
                lunaState = LunaState.IDLE
                spokenText = "Tap orb or say \"Hey Luna\" to talk."
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val query = matches[0]
                    spokenText = "\"$query\""
                    simulateLunaResponse(query)
                } else {
                    lunaState = LunaState.IDLE
                    spokenText = "Could not hear clearly. Try again!"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun checkAndStartListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'Hey Luna'...")
        }
        speechRecognizer.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer.stopListening()
        lunaState = LunaState.IDLE
    }

    private fun simulateLunaResponse(query: String) {
        lunaState = LunaState.SPEAKING
        lunaResponse = "Luna: I'm connected to your Antigravity backend! Received: \"$query\""
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}

@Composable
fun LunaOverlayUI(
    state: LunaState,
    spokenText: String,
    response: String,
    onMicClick: () -> Unit,
    onDismiss: () -> Unit
) {
    // Pulsing Glowing Animated Siri / Gemini Orb Effect
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() }
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Glowing Siri/Gemini Bottom Floating Sheet Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .clickable(enabled = false) {}, // Prevent tap-through
            shape = RoundedCornerShape(28.dp),
            color = Color(0xDD0D0D14), // Glassmorphic translucent dark background
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Voice Orb Indicator
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(if (state == LunaState.LISTENING || state == LunaState.SPEAKING) scale else 1.0f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = when (state) {
                                    LunaState.LISTENING -> listOf(Color(0xFF00F2FE), Color(0xFF4FACFE), Color(0xFF6B11FF))
                                    LunaState.PROCESSING -> listOf(Color(0xFFFF0844), Color(0xFFFFB199), Color(0xFF6B11FF))
                                    LunaState.SPEAKING -> listOf(Color(0xFF00FF87), Color(0xFF60EFFF), Color(0xFF0061FF))
                                    else -> listOf(Color(0xFF434343), Color(0xFF000000))
                                }
                            ),
                            shape = CircleShape
                        )
                        .clickable { onMicClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (state) {
                            LunaState.LISTENING -> "🎙️"
                            LunaState.PROCESSING -> "⚡"
                            LunaState.SPEAKING -> "✨"
                            else -> "🌙"
                        },
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Spoken Text Display
                Text(
                    text = spokenText,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                if (response.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = response,
                        color = Color(0xFF80D0FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Luna Voice Assistant • Tap outside to dismiss",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}
