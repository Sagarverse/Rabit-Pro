package com.example.rabit.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import com.example.rabit.ui.assistant.ChatMessage

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)

object AssistantHistoryPersistence {
    private const val SESSIONS_DIR = "chat_sessions"
    private const val SESSIONS_INDEX = "sessions_index.json"
    
    // Legacy file name for migration
    private const val LEGACY_FILE_NAME = "chat_history.json"
    
    // Ignore unknown keys to prevent crashes when model changes
    private val json = Json { ignoreUnknownKeys = true }

    private fun sessionsDir(context: Context): File {
        val dir = File(context.filesDir, SESSIONS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun sessionFile(context: Context, sessionId: String): File {
        return File(sessionsDir(context), "session_$sessionId.json")
    }

    private fun indexFile(context: Context): File {
        return File(sessionsDir(context), SESSIONS_INDEX)
    }

    /**
     * Migrate legacy single-file history to new session-based system.
     */
    suspend fun migrateLegacyIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        try {
            val legacyFile = File(context.filesDir, LEGACY_FILE_NAME)
            if (legacyFile.exists()) {
                val messages = try {
                    json.decodeFromString<List<ChatMessage>>(legacyFile.readText())
                } catch (e: Exception) { emptyList() }
                
                if (messages.isNotEmpty()) {
                    val session = ChatSession(
                        title = generateTitle(messages),
                        createdAt = messages.firstOrNull()?.timestamp ?: System.currentTimeMillis(),
                        lastModified = messages.lastOrNull()?.timestamp ?: System.currentTimeMillis(),
                        messageCount = messages.size
                    )
                    saveSession(context, session.id, messages)
                    saveSessionIndex(context, listOf(session))
                }
                legacyFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Lists all saved sessions sorted by most recent first.
     */
    suspend fun listSessions(context: Context): List<ChatSession> = withContext(Dispatchers.IO) {
        try {
            val file = indexFile(context)
            if (file.exists()) {
                val sessions = json.decodeFromString<List<ChatSession>>(file.readText())
                return@withContext sessions.sortedByDescending { it.lastModified }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    /**
     * Saves a session's messages.
     */
    suspend fun saveSession(context: Context, sessionId: String, messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        try {
            val file = sessionFile(context, sessionId)
            file.writeText(json.encodeToString(messages))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads messages for a specific session.
     */
    suspend fun loadSession(context: Context, sessionId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val file = sessionFile(context, sessionId)
            if (file.exists()) {
                return@withContext json.decodeFromString<List<ChatMessage>>(file.readText())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    /**
     * Deletes a session and its messages.
     */
    suspend fun deleteSession(context: Context, sessionId: String) = withContext(Dispatchers.IO) {
        try {
            sessionFile(context, sessionId).delete()
            val sessions = listSessions(context).filter { it.id != sessionId }
            saveSessionIndex(context, sessions)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Creates a new session and returns it.
     */
    fun createNewSession(): ChatSession {
        return ChatSession()
    }

    /**
     * Updates the session index with the given sessions list.
     */
    suspend fun saveSessionIndex(context: Context, sessions: List<ChatSession>) = withContext(Dispatchers.IO) {
        try {
            val file = indexFile(context)
            // Keep max 50 sessions
            val trimmed = sessions.sortedByDescending { it.lastModified }.take(50)
            file.writeText(json.encodeToString(trimmed))
            
            // Clean up session files that are no longer in the index
            val validIds = trimmed.map { it.id }.toSet()
            sessionsDir(context).listFiles()?.forEach { f ->
                if (f.name.startsWith("session_") && f.name.endsWith(".json")) {
                    val id = f.name.removePrefix("session_").removeSuffix(".json")
                    if (id !in validIds) f.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Updates a specific session's metadata in the index.
     */
    suspend fun updateSessionInIndex(context: Context, session: ChatSession) = withContext(Dispatchers.IO) {
        try {
            val sessions = listSessions(context).toMutableList()
            val idx = sessions.indexOfFirst { it.id == session.id }
            if (idx >= 0) {
                sessions[idx] = session
            } else {
                sessions.add(0, session)
            }
            saveSessionIndex(context, sessions)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Generate a title from the first user message.
     */
    fun generateTitle(messages: List<ChatMessage>): String {
        val firstUserMsg = messages.firstOrNull { it.isUser }?.content ?: "New Chat"
        return firstUserMsg.take(50).let {
            if (firstUserMsg.length > 50) "$it..." else it
        }
    }

    // ── Legacy support ──
    
    suspend fun saveHistory(context: Context, messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        // Legacy method - redirect to session system
        // This is kept for backward compatibility but callers should use saveSession
        try {
            val file = File(context.filesDir, LEGACY_FILE_NAME)
            val jsonString = json.encodeToString(messages)
            file.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadHistory(context: Context): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, LEGACY_FILE_NAME)
            if (file.exists()) {
                val jsonString = file.readText()
                return@withContext json.decodeFromString<List<ChatMessage>>(jsonString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }
}
