/*
 * CoverArtHelper.kt
 * Looks up cover art for a track: Deezer first, then MusicBrainz + Cover Art Archive.
 *
 * This file is part of URL Radio
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */

package com.jamal2367.urlradio.helpers

import android.util.Log
import android.util.LruCache
import com.google.gson.JsonParser
import com.jamal2367.urlradio.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object CoverArtHelper {

    private val tag: String = CoverArtHelper::class.java.simpleName
    private const val MAX_IMAGE_BYTES: Long = 2_000_000L

    // MusicBrainz asks for an identifying User-Agent
    private val userAgent: String = "URLRadio/${BuildConfig.VERSION_NAME} (https://github.com/ThomasOspelt/URL-Radio-SE)"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    // Misses are cached too (as empty strings) so a repeated title does not hit the APIs again
    private val urlCache = LruCache<String, String>(100)

    /* Blocking - run off the main thread. Returns an image URL, or null when nothing was found. */
    fun findCoverUrl(artist: String, title: String): String? {
        val key = "${artist.lowercase()}\u0000${title.lowercase()}"
        urlCache.get(key)?.let { return it.ifEmpty { null } }
        val url = runCatching { fromDeezer(artist, title) }.onFailure { Log.w(tag, "Deezer lookup failed: $it") }.getOrNull()
            ?: runCatching { fromMusicBrainz(artist, title) }.onFailure { Log.w(tag, "MusicBrainz lookup failed: $it") }.getOrNull()
        urlCache.put(key, url.orEmpty())
        return url
    }

    /* Blocking - downloads the image, for the media notification. */
    fun downloadImage(url: String): ByteArray? {
        return runCatching {
            client.newCall(Request.Builder().url(url).header("User-Agent", userAgent).build()).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body.contentLength() > MAX_IMAGE_BYTES) null else body.bytes()
            }
        }.getOrNull()
    }

    private fun getJson(url: String): com.google.gson.JsonObject? {
        val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else JsonParser.parseString(response.body.string()).asJsonObject
        }
    }

    private fun fromDeezer(artist: String, title: String): String? {
        val url = "https://api.deezer.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("q", "artist:\"$artist\" track:\"$title\"")
            .addQueryParameter("limit", "1")
            .build()
        val first = getJson(url.toString())?.getAsJsonArray("data")?.firstOrNull()?.asJsonObject ?: return null
        val cover = first.getAsJsonObject("album")?.get("cover_big")?.asString.orEmpty()
        // Deezer serves a generic image for albums without art; those URLs contain no album hash
        return cover.takeIf { it.isNotEmpty() && !it.contains("/images/cover//") }
    }

    private fun fromMusicBrainz(artist: String, title: String): String? {
        val url = "https://musicbrainz.org/ws/2/recording/".toHttpUrl().newBuilder()
            .addQueryParameter("query", "artist:\"$artist\" AND recording:\"$title\"")
            .addQueryParameter("fmt", "json")
            .addQueryParameter("limit", "3")
            .build()
        val recordings = getJson(url.toString())?.getAsJsonArray("recordings") ?: return null
        val releaseIds = recordings.flatMap { recording ->
            recording.asJsonObject.getAsJsonArray("releases")?.map { it.asJsonObject.get("id").asString }.orEmpty()
        }.distinct().take(4)
        for (id in releaseIds) {
            val cover = "https://coverartarchive.org/release/$id/front-500"
            val request = Request.Builder().url(cover).head().header("User-Agent", userAgent).build()
            val found = client.newCall(request).execute().use { it.isSuccessful }
            if (found) return cover
        }
        return null
    }
}
