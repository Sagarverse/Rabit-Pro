package com.example.rabit.data.network

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * HandoffActivity - Feature 3: Screen Handoff
 *
 * This transparent Activity registers as a Share target for URLs.
 * When the user taps "Share > Rabit" in Chrome or any other browser,
 * Android invokes this Activity with the URL as an Intent extra.
 *
 * The Activity then:
 *  1. Reads the Mac's IP from SharedPreferences (set by the Settings screen)
 *  2. POSTs the URL to the Mac's companion server endpoint via OkHttp
 *  3. Finishes immediately (invisible to the user)
 */
class HandoffActivity : Activity() {

    @Serializable
    data class HandoffPayload(val url: String)

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = when {
            intent?.action == Intent.ACTION_SEND && intent.type == "text/plain" ->
                intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }

        if (url.isNullOrBlank()) {
            Toast.makeText(this, "Rabit: Nothing to hand off.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("HandoffActivity", "Handing off URL: $url")

        val prefs: SharedPreferences = getSharedPreferences("rabit_prefs", MODE_PRIVATE)
        val macIp = prefs.getString("mac_ip", "") ?: ""

        if (macIp.isBlank()) {
            Toast.makeText(this, "Rabit: Mac IP not set. Go to Settings.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val macPort = prefs.getInt("mac_handoff_port", 8766)
        val endpoint = "http://$macIp:$macPort/handoff"

        scope.launch {
            try {
                val json = Json.encodeToString(HandoffPayload(url))
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(endpoint).post(body).build()

                withContext(Dispatchers.IO) { client.newCall(request).execute() }
                Toast.makeText(this@HandoffActivity, "✅ Opened on Mac!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("HandoffActivity", "Handoff failed: ${e.message}")
                Toast.makeText(this@HandoffActivity, "Rabit: Could not reach Mac.", Toast.LENGTH_SHORT).show()
            } finally {
                finish()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
