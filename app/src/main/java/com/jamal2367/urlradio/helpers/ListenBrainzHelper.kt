/*
 * ListenBrainzHelper.kt
 * Parses stream titles and submits listens ("scrobbles") to ListenBrainz.
 *
 * This file is part of URL Radio
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package com.jamal2367.urlradio.helpers

import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object ListenBrainzHelper {

    private val tag: String = ListenBrainzHelper::class.java.simpleName
    private const val SUBMIT_URL: String = "https://api.listenbrainz.org/1/submit-listens"
    const val MIN_LISTEN_TIME_MS: Long = 30_000L

    private val client: OkHttpClient by lazy { OkHttpClient() }
    private val gson: Gson = Gson()

    data class Track(val artist: String, val title: String)

    const val DEFAULT_FORMAT: String = "{artist} - {title}"

    private val placeholder = Regex("""\{(artist|title|any)\}""")

    /* Station format wins over the global one; the built-in default is the last resort */
    fun effectiveFormat(stationFormat: String, globalFormat: String): String =
        stationFormat.trim().ifEmpty { globalFormat.trim() }.ifEmpty { DEFAULT_FORMAT }

    /* A usable format has to name both {artist} and {title} */
    fun isValidFormat(format: String): Boolean =
        format.contains("{artist}") && format.contains("{title}")

    /*
     * Matches a stream title against a format such as "{artist} - {title}" or
     * "{title} by {artist}". {any} skips a part that should not be scrobbled.
     * Returns null when the title does not fit, so non-track titles (jingles, station
     * slogans) are never submitted.
     */
    fun parseStreamTitle(streamTitle: String, format: String = DEFAULT_FORMAT): Track? {
        if (!isValidFormat(format)) return null
        val pattern = StringBuilder()
        var artistGroup = 0
        var titleGroup = 0
        var groups = 0
        var last = 0
        for (match in placeholder.findAll(format)) {
            pattern.append(Regex.escape(format.substring(last, match.range.first)))
            val name = match.groupValues[1]
            val isFirst = when (name) {
                "artist" -> artistGroup == 0
                "title" -> titleGroup == 0
                else -> false
            }
            if (isFirst) {
                groups++
                if (name == "artist") artistGroup = groups else titleGroup = groups
                pattern.append("(.+?)")
            } else {
                pattern.append("(?:.+?)")
            }
            last = match.range.last + 1
        }
        pattern.append(Regex.escape(format.substring(last)))
        val result = Regex(pattern.toString()).matchEntire(streamTitle.trim()) ?: return null
        val artist = result.groupValues[artistGroup].trim()
        val title = result.groupValues[titleGroup].trim()
        if (artist.isEmpty() || title.isEmpty()) return null
        return Track(artist, title)
    }

    /* Blocking call - run off the main thread. Returns true on success. */
    fun submit(token: String, track: Track, playingNow: Boolean): Boolean {
        val trackMetadata = mapOf(
            "artist_name" to track.artist,
            "track_name" to track.title,
            "additional_info" to mapOf("media_player" to "URL Radio", "submission_client" to "URL Radio")
        )
        val entry = mutableMapOf<String, Any>("track_metadata" to trackMetadata)
        if (!playingNow) entry["listened_at"] = System.currentTimeMillis() / 1000L
        val body = mapOf(
            "listen_type" to if (playingNow) "playing_now" else "single",
            "payload" to listOf(entry)
        )
        val request = Request.Builder()
            .url(SUBMIT_URL)
            .header("Authorization", "Token $token")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) Log.w(tag, "ListenBrainz rejected submission: ${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(tag, "ListenBrainz submission failed: $e")
            false
        }
    }
}
