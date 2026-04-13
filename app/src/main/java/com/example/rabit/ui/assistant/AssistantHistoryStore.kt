package com.example.rabit.ui.assistant

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.rabit.domain.model.gemini.GeminiResponse

/**
 * Stores prompt/response history for the session.
 */
object AssistantHistoryStore {
    val history: SnapshotStateList<Pair<String, String>> = mutableStateListOf()

    fun add(prompt: String, response: String) {
        history.add(0, prompt to response)
        if (history.size > 50) history.removeAt(history.lastIndex)
    }

    fun clear() {
        history.clear()
    }
}
