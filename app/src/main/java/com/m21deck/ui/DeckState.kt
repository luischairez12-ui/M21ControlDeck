package com.m21deck.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.m21deck.data.MusicLibrary
import com.m21deck.data.Playlist
import com.m21deck.data.PlaylistStore
import com.m21deck.data.Track
import com.m21deck.playback.DeckPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Page { HOME, LIBRARY, PLAYLISTS, PLAYLIST_DETAIL, SETTINGS, QUEUE, EQ, APPS, QUICK }
enum class Source { LOCAL, STREAMING }

class DeckState(val context: Context, private val scope: CoroutineScope) {
    private val library = MusicLibrary(context)
    private val database = PlaylistStore(context)
    val player = DeckPlayer(context)
    val tracks = mutableStateListOf<Track>()
    val playlists = mutableStateListOf<Playlist>()
    val favorites = mutableStateListOf<Long>()
    var page by mutableStateOf(Page.HOME)
    var source by mutableStateOf(Source.LOCAL)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var selectedPlaylist by mutableStateOf<Long?>(null)
    private val prefs = context.getSharedPreferences("control_deck", Context.MODE_PRIVATE)
    var haptics by mutableStateOf(prefs.getBoolean("haptics", true))
    var dialSensitivity by mutableStateOf(prefs.getFloat("dialSensitivity", 1f))
    var compact by mutableStateOf(prefs.getBoolean("compact", false))

    init { player.connect(); refreshCollections() }
    fun refreshLibrary() {
        scope.launch {
            busy = true; error = null
            try {
                val result = library.scan()
                tracks.clear(); tracks.addAll(result)
            } catch (t: Exception) { error = "Library scan failed: ${t.message}" }
            finally { busy = false }
        }
    }
    fun refreshCollections() = scope.launch {
        val list = withContext(Dispatchers.IO) { database.playlists() to database.favorites() }
        playlists.clear(); playlists.addAll(list.first)
        favorites.clear(); favorites.addAll(list.second)
    }
    fun createPlaylist(name: String) = scope.launch {
        withContext(Dispatchers.IO) { database.create(name) }
        refreshCollections()
    }
    fun renamePlaylist(id: Long, name: String) = scope.launch {
        withContext(Dispatchers.IO) { database.rename(id, name) }; refreshCollections()
    }
    fun deletePlaylist(id: Long) = scope.launch {
        withContext(Dispatchers.IO) { database.delete(id) }; selectedPlaylist = null
        page = Page.PLAYLISTS; refreshCollections()
    }
    fun addToPlaylist(id: Long, trackId: Long) = scope.launch {
        withContext(Dispatchers.IO) { database.addTrack(id, trackId) }; refreshCollections()
    }
    fun removeFromPlaylist(id: Long, trackId: Long) = scope.launch {
        withContext(Dispatchers.IO) { database.removeTrack(id, trackId) }; refreshCollections()
    }
    fun reorderPlaylist(id: Long, from: Int, to: Int) = scope.launch {
        val playlist = playlists.firstOrNull { it.id == id } ?: return@launch
        if (from !in playlist.trackIds.indices || to !in playlist.trackIds.indices) return@launch
        val order = playlist.trackIds.toMutableList()
        order.add(to, order.removeAt(from))
        withContext(Dispatchers.IO) { database.replaceOrder(id, order) }
        refreshCollections()
    }
    fun toggleFavorite(id: Long) = scope.launch {
        val selected = id !in favorites
        withContext(Dispatchers.IO) { database.setFavorite(id, selected) }
        refreshCollections()
    }
    fun importM3u(name: String, text: String) = scope.launch {
        val ids = withContext(Dispatchers.Default) { com.m21deck.data.M3u.importList(text.reader(), tracks.toList()) }
        withContext(Dispatchers.IO) {
            val id = database.create(name)
            if (id > 0) ids.forEach { database.addTrack(id, it) }
        }
        refreshCollections()
    }
    fun updateHaptics(value: Boolean) { haptics = value; prefs.edit().putBoolean("haptics",value).apply() }
    fun updateCompact(value: Boolean) { compact = value; prefs.edit().putBoolean("compact",value).apply() }
    fun updateSensitivity(value: Float) { dialSensitivity = value; prefs.edit().putFloat("dialSensitivity",value).apply() }
    fun trackById(id: Long): Track? = tracks.firstOrNull { it.id == id }
    fun openPlaylist(id: Long) { selectedPlaylist = id; page = Page.PLAYLIST_DETAIL }
    fun back() { page = when(page) {
        Page.PLAYLIST_DETAIL -> Page.PLAYLISTS
        else -> Page.HOME
    } }
    fun close() { player.release(); database.close() }
}
