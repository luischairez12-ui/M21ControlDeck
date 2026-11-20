package com.m21deck.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Tracks are referenced by MediaStore URIs, never by unrestricted filesystem access. */
data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val folder: String,
    val fileName: String,
    val dateAdded: Long,
    val mimeType: String,
    val genre: String = "Unknown"
) {
    val artwork: Uri get() = Uri.parse("content://media/external/audio/albumart/$albumId")
}

class MusicLibrary(private val context: Context) {
    suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        val records = mutableListOf<Track>()
        val contentUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE
        )
        val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        context.contentResolver.query(
            contentUri, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, sort
        )?.use { cursor ->
            fun col(name: String) = cursor.getColumnIndexOrThrow(name)
            val id = col(MediaStore.Audio.Media._ID)
            val title = col(MediaStore.Audio.Media.TITLE)
            val artist = col(MediaStore.Audio.Media.ARTIST)
            val album = col(MediaStore.Audio.Media.ALBUM)
            val albumId = col(MediaStore.Audio.Media.ALBUM_ID)
            val duration = col(MediaStore.Audio.Media.DURATION)
            val relativePath = col(MediaStore.Audio.Media.RELATIVE_PATH)
            val fileName = col(MediaStore.Audio.Media.DISPLAY_NAME)
            val date = col(MediaStore.Audio.Media.DATE_ADDED)
            val mime = col(MediaStore.Audio.Media.MIME_TYPE)
            while (cursor.moveToNext()) {
                val audioId = cursor.getLong(id)
                val ms = cursor.getLong(duration)
                if (ms < 1000) continue
                records += Track(
                    id = audioId,
                    uri = ContentUris.withAppendedId(contentUri, audioId),
                    title = cursor.getString(title) ?: "Untitled",
                    artist = (cursor.getString(artist) ?: "Unknown Artist").replace("<unknown>", "Unknown Artist"),
                    album = cursor.getString(album) ?: "Unknown Album",
                    albumId = cursor.getLong(albumId),
                    durationMs = ms,
                    folder = cursor.getString(relativePath) ?: "Music",
                    fileName = cursor.getString(fileName) ?: "audio-$audioId",
                    dateAdded = cursor.getLong(date),
                    mimeType = cursor.getString(mime) ?: "audio/*"
                )
            }
        }
        records
    }
}
