package com.m21deck.playback

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.m21deck.data.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlin.math.roundToInt

/** Session controller; the service survives activity recreation and screen-off. */
class DeckPlayer(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    var ready by mutableStateOf(false); private set
    var playing by mutableStateOf(false); private set
    var mediaId by mutableStateOf<Long?>(null); private set
    var title by mutableStateOf("No music playing"); private set
    var artist by mutableStateOf("Choose a song from Library"); private set
    var position by mutableStateOf(0L); private set
    var duration by mutableStateOf(0L); private set
    var index by mutableStateOf(-1); private set
    var shuffle by mutableStateOf(false); private set
    var repeat by mutableStateOf(Player.REPEAT_MODE_OFF); private set
    var volume by mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)); private set
    val maxVolume: Int get() = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

    private val events = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) { snapshot() }
    }
    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        future = MediaController.Builder(context, token).buildAsync().also { pending ->
            pending.addListener({
                try {
                    controller = pending.get().also { it.addListener(events) }
                    ready = true
                    snapshot()
                } catch (_: Exception) { ready = false }
            }, ContextCompat.getMainExecutor(context))
        }
    }
    private fun snapshot() {
        controller?.let { c ->
            playing = c.isPlaying
            mediaId = c.currentMediaItem?.mediaId?.toLongOrNull()
            title = c.mediaMetadata.title?.toString() ?: "No music playing"
            artist = c.mediaMetadata.artist?.toString() ?: "Choose a song from Library"
            duration = c.duration.takeIf { it > 0 && it != androidx.media3.common.C.TIME_UNSET } ?: 0L
            position = c.currentPosition.coerceAtLeast(0)
            index = c.currentMediaItemIndex
            shuffle = c.shuffleModeEnabled
            repeat = c.repeatMode
        }
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }
    fun tick() = snapshot()
    fun playTracks(tracks: List<Track>, start: Int = 0) {
        if (tracks.isEmpty()) return
        val items = tracks.map { t ->
            MediaItem.Builder().setMediaId(t.id.toString()).setUri(t.uri)
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album)
                    .setArtworkUri(t.artwork).build()).build()
        }
        controller?.apply {
            setMediaItems(items, start.coerceIn(0, items.lastIndex), 0L)
            prepare()
            play()
        }
        snapshot()
    }
    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() }; snapshot() }
    fun next() { controller?.seekToNextMediaItem(); snapshot() }
    fun previous() { controller?.seekToPreviousMediaItem(); snapshot() }
    fun seek(ms: Long) { controller?.seekTo(ms); snapshot() }
    fun setShuffle(enabled: Boolean) { controller?.shuffleModeEnabled = enabled; snapshot() }
    fun setRepeat(mode: Int) { controller?.repeatMode = mode; snapshot() }
    fun cycleMode() {
        val c = controller ?: return
        when {
            c.shuffleModeEnabled -> { c.shuffleModeEnabled = false; c.repeatMode = Player.REPEAT_MODE_OFF }
            c.repeatMode == Player.REPEAT_MODE_OFF -> c.repeatMode = Player.REPEAT_MODE_ALL
            c.repeatMode == Player.REPEAT_MODE_ALL -> c.repeatMode = Player.REPEAT_MODE_ONE
            else -> { c.repeatMode = Player.REPEAT_MODE_OFF; c.shuffleModeEnabled = true }
        }
        snapshot()
    }
    fun moveQueue(from: Int, to: Int) {
        val c = controller ?: return
        if (from in 0 until c.mediaItemCount && to in 0 until c.mediaItemCount) c.moveMediaItem(from, to)
        snapshot()
    }
    fun queueIds(): List<Long> {
        val c = controller ?: return emptyList()
        return (0 until c.mediaItemCount).mapNotNull { c.getMediaItemAt(it).mediaId.toLongOrNull() }
    }
    fun setVolumeFraction(fraction: Float) {
        val value = (maxVolume * fraction.coerceIn(0f,1f)).roundToInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }
    fun adjustVolume(delta: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volume + delta).coerceIn(0,maxVolume), 0)
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }
    fun release() {
        controller?.removeListener(events)
        future?.let { MediaController.releaseFuture(it) }
        controller = null; future = null; ready = false
    }
}
