package com.sumit.paymentalert.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.sumit.paymentalert.data.PreferencesHelper
import java.util.Locale

class TTSManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val prefs = PreferencesHelper(context)

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("TTSManager", "Initialization error: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            configureLanguage()
            isInitialized = true
        } else {
            Log.e("TTSManager", "TTS initialization failed.")
        }
    }

    fun configureLanguage() {
        val lang = prefs.ttsLanguage
        val locale = when (lang) {
            "Hindi" -> Locale.Builder().setLanguage("hi").setRegion("IN").build()
            "English" -> Locale.US
            else -> Locale.Builder().setLanguage("hi").setRegion("IN").build() // Default to Hindi structure
        }
        try {
            tts?.language = locale
            tts?.setSpeechRate(prefs.ttsSpeed)
            tts?.setPitch(prefs.ttsPitch)

            // Dynamic lookup for human-like voices (network-driven, wavenet, or high quality local)
            val availableVoices = tts?.voices
            if (!availableVoices.isNullOrEmpty()) {
                val localeVoices = availableVoices.filter { 
                    it.locale.language == locale.language && 
                    (locale.country.isEmpty() || it.locale.country == locale.country)
                }

                // Retrieve high-fidelity/network/neural voices first.
                // Google TTS uses names like 'hi-in-x-hie-network' or 'en-us-x-sfg-network' for natural speech.
                val naturalVoice = localeVoices.firstOrNull { 
                    it.name.contains("network", ignoreCase = true) || 
                    it.name.contains("wavenet", ignoreCase = true) || 
                    it.name.contains("neural", ignoreCase = true) ||
                    it.name.contains("coho", ignoreCase = true)
                } ?: localeVoices.firstOrNull { 
                    !it.isNetworkConnectionRequired 
                } ?: localeVoices.firstOrNull()

                if (naturalVoice != null) {
                    tts?.voice = naturalVoice
                    Log.d("TTSManager", "Active natural human-like voice: ${naturalVoice.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("TTSManager", "Error setting speech preferences", e)
        }
    }

    fun speakAlert(amount: Double, sender: String) {
        if (!isInitialized) {
            try {
                tts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        isInitialized = true
                        configureLanguage()
                        performSpeak(amount, sender)
                    }
                }
            } catch (e: Exception) {
                Log.e("TTSManager", "Failed lazy init", e)
            }
        } else {
            performSpeak(amount, sender)
        }
    }

    private fun performSpeak(amount: Double, sender: String) {
        val finalSender = cleanSenderName(sender)

        // 1. Play Pre-Chime Tone if enabled
        if (prefs.isChimeEnabled) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
            } catch (e: Exception) {
                Log.e("TTSManager", "Error playing chime tone", e)
            }
        }

        // 2. Set max volume if needed
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVolume * 0.9).toInt(), 0)
        } catch (e: Exception) {
            Log.e("TTSManager", "Error setting volume", e)
        }

        // 3. Formulate Speech Message
        val amountInt = amount.toInt()
        val amountStr = if (amount % 1.0 == 0.0) "$amountInt" else String.format(Locale.US, "%.2f", amount)

        val hWords = formatAmountToHindiWords(amount)
        val speechText = when (prefs.ttsLanguage) {
            "Hindi" -> "$hWords, , रुपये, , प्राप्त हुए।"
            "English" -> "$amountStr, , Rupees, , received."
            else -> "$hWords, , रुपये, , प्राप्त हुए।" // Mixed
        }

        try {
            tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "PaymentAlertID")
        } catch (e: Exception) {
            Log.e("TTSManager", "Error in speaking", e)
        }
    }

    fun testSpeak(onCompleted: () -> Unit = {}) {
        if (!isInitialized) {
            try {
                tts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        isInitialized = true
                        configureLanguage()
                        performTestSpeak()
                    }
                }
            } catch (e: Exception) {}
        } else {
            performTestSpeak()
        }
    }

    private fun performTestSpeak() {
        configureLanguage()
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
        } catch (e: Exception) {}

        val testText = when (prefs.ttsLanguage) {
            "Hindi" -> "पचास, , रुपये, , प्राप्त हुए।"
            "English" -> "50, , Rupees, , received."
            else -> "पचास, , रुपये, , प्राप्त हुए।"
        }
        tts?.speak(testText, TextToSpeech.QUEUE_FLUSH, null, "TestAlertID")
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("TTSManager", "Error shutting down TTS", e)
        }
    }

    private fun cleanSenderName(sender: String): String {
        var clean = sender.replace("(?i)\\b(deposit|deposited|transfer|via|from|upi|ref|txn|a/c|account|credited)\\b.*".toRegex(), "").trim()
        clean = clean.replace("[^a-zA-Z0-9\\s]+".toRegex(), "").trim()
        if (clean.isEmpty()) {
            clean = "अज्ञात यूज़र"
        }
        return clean
    }

    private val HINDI_NUMBERS = arrayOf(
        "शून्य", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस",
        "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस", "बीस",
        "इक्कीस", "बाईस", "तेईस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताइस", "अठाइस", "उनतीस", "तीस",
        "इकतीस", "बत्तीस", "तैंतीस", "चौंतीस", "पैंतीस", "छत्तीस", "सैंतीस", "अड़ीस", "उनतालीस", "चालीस",
        "इकतालीस", "बयालीस", "तैंतालीस", "चवालीस", "पैंतालीस", "छियालीस", "सैंतालीस", "अड़तालीस", "उनचाas", "पचाas"
    )

    private val HINDI_NUMBERS_MAP = mapOf(
        0L to "शून्य", 1L to "एक", 2L to "दो", 3L to "तीन", 4L to "चार", 5L to "पाँच", 
        6L to "छह", 7L to "सात", 8L to "आठ", 9L to "नौ", 10L to "दस", 11L to "ग्यारह", 
        12L to "बारह", 13L to "तेरह", 14L to "चौदह", 15L to "पंद्रह", 16L to "सोलह", 
        17L to "सत्रह", 18L to "अठारह", 19L to "उन्नीस", 20L to "बीस", 21L to "इक्कीस", 
        22L to "बाईस", 23L to "तेईस", 24L to "चौबीस", 25L to "पच्चीस", 26L to "छब्बीस", 
        27L to "सत्ताइस", 28L to "अठाइस", 29L to "उनतीस", 30L to "तीस", 31L to "इकतीस", 
        32L to "बत्तीस", 33L to "तैंतीस", 34L to "चौंतीस", 35L to "पैंतीस", 36L to "छत्तीस", 
        37L to "सैंतीस", 38L to "अड़तीस", 39L to "उनतालीस", 40L to "चालीस", 41L to "इकतालीस", 
        42L to "बयालीस", 43L to "तैंतालीस", 44L to "चवालीस", 45L to "पैंतालीस", 46L to "छियालीस", 
        47L to "सैंतालीस", 48L to "अड़तालीस", 49L to "उनचास", 50L to "पचास", 51L to "इक्यावन", 
        52L to "बावन", 53L to "तिरेपन", 54L to "चौवन", 55L to "पचपन", 56L to "छप्पन", 
        57L to "सत्तावन", 58L to "अठ्ठावन", 59L to "उनसठ", 60L to "साठ", 61L to "इक्सठ", 
        62L to "बासठ", 63L to "तिरेसठ", 64L to "चौंसठ", 65L to "पैंसठ", 66L to "छियासठ", 
        67L to "सरसठ", 68L to "अड़सठ", 69L to "उनहत्तर", 70L to "सत्तर", 71L to "इकहत्तर", 
        72L to "बहत्तर", 73L to "तिहत्तर", 74L to "चौहत्तर", 75L to "पचहत्तर", 76L to "छिहत्तर", 
        77L to "सतहत्तर", 78L to "अठहत्तर", 79L to "उनासी", 80L to "अस्सी", 81L to "इक्यासी", 
        82L to "बयासी", 83L to "तिरासी", 84L to "चौरासी", 85L to "पचासी", 86L to "छियासी", 
        87L to "सतासी", 88L to "अष्टासी", 89L to "नवासी", 90L to "नब्बे", 91L to "इक्यानवे", 
        92L to "बानवे", 93L to "तिरानवे", 94L to "चौरानवे", 95L to "पचानवे", 96L to "छियानवे", 
        97L to "सत्तानवे", 98L to "अट्ठानवे", 99L to "निनानवे"
    )

    private fun convertNumberToHindiWords(num: Long): String {
        if (num == 0L) return ""

        if (num in 1L..99L) {
            return HINDI_NUMBERS_MAP[num] ?: ""
        }

        if (num >= 10000000) { // Crore
            val crorePart = num / 10000000
            val remaining = num % 10000000
            val prefix = convertNumberToHindiWords(crorePart)
            val suffix = convertNumberToHindiWords(remaining)
            return "$prefix करोड़ ${if (suffix.isNotEmpty()) " $suffix" else ""}".trim()
        }

        if (num >= 100000) { // Lakh
            val lakhPart = num / 100000
            val remaining = num % 100000
            val prefix = convertNumberToHindiWords(lakhPart)
            val suffix = convertNumberToHindiWords(remaining)
            return "$prefix लाख ${if (suffix.isNotEmpty()) " $suffix" else ""}".trim()
        }

        if (num >= 1000) { // Thousand
            val thousandPart = num / 1000
            val remaining = num % 1000
            val prefix = convertNumberToHindiWords(thousandPart)
            val suffix = convertNumberToHindiWords(remaining)
            return "$prefix हजार ${if (suffix.isNotEmpty()) " $suffix" else ""}".trim()
        }

        // Hundreds
        val hundredPart = num / 100
        val remaining = num % 100
        val prefix = convertNumberToHindiWords(hundredPart)
        val suffix = convertNumberToHindiWords(remaining)
        return "$prefix सौ ${if (suffix.isNotEmpty()) " $suffix" else ""}".trim()
    }

    private fun formatAmountToHindiWords(amount: Double): String {
        val totalRupees = amount.toLong()
        val paise = Math.round((amount - totalRupees) * 100).toInt()

        val rupeesPartWords = if (totalRupees == 0L) {
            if (paise == 0) "शून्य" else ""
        } else {
            convertNumberToHindiWords(totalRupees)
        }

        val result = StringBuilder()
        if (rupeesPartWords.isNotEmpty()) {
            result.append(rupeesPartWords)
        }

        if (paise > 0) {
            if (result.isNotEmpty()) {
                result.append(" दशमलव ")
            }
            result.append(convertNumberToHindiWords(paise.toLong()))
        }

        return result.toString().trim()
    }
}
