// SPDX-License-Identifier: GPL-3.0-or-later

package com.ichi2.anki.powerstudy

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class PowerSessionStore(
    private val sharedPrefs: SharedPreferences,
) {
    constructor(context: Context) : this(context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE))

    private val json = Json { ignoreUnknownKeys = true }

    fun get(sessionId: String): PowerSession? =
        sharedPrefs.getString(key(sessionId), null)?.let { decode(sessionId, it) }

    fun sessions(): List<PowerSession> =
        sharedPrefs.all
            .asSequence()
            .filter { (key, value) -> key.startsWith(SESSION_KEY_PREFIX) && value is String }
            .mapNotNull { (key, value) -> decode(key.removePrefix(SESSION_KEY_PREFIX), value as String) }
            .sortedBy { it.name.lowercase() }
            .toList()

    fun save(session: PowerSession) {
        val saved =
            sharedPrefs
                .edit()
                .putString(key(session.id), json.encodeToString(session))
                .commit()
        check(saved) { "Failed to persist power study session " + session.id }
    }

    private fun decode(
        expectedSessionId: String,
        value: String,
    ): PowerSession? =
        try {
            json.decodeFromString<PowerSession>(value).also { session ->
                if (session.id != expectedSessionId) {
                    Timber.w("Ignoring mismatched power study session key %s for %s", expectedSessionId, session.id)
                    return null
                }
            }
        } catch (e: SerializationException) {
            Timber.w(e, "Failed to deserialize power study session %s", expectedSessionId)
            null
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Invalid persisted power study session %s", expectedSessionId)
            null
        }

    companion object {
        private const val PREFERENCES_FILE = "PowerStudyState"
        private const val SESSION_KEY_PREFIX = "session:"
        private fun key(sessionId: String) = SESSION_KEY_PREFIX + sessionId
    }
}
