package com.m21deck.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Playlist(val id: Long, val name: String, val trackIds: List<Long>)

/** Local-only playlist and favorite storage. Never uploads music metadata. */
class PlaylistStore(context: Context) : SQLiteOpenHelper(context, "m21_collection.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE playlists (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE)")
        db.execSQL("CREATE TABLE playlist_tracks (playlist_id INTEGER NOT NULL, audio_id INTEGER NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(playlist_id,audio_id))")
        db.execSQL("CREATE TABLE favorites (audio_id INTEGER PRIMARY KEY)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun playlists(): List<Playlist> = readableDatabase.rawQuery("SELECT id,name FROM playlists ORDER BY name COLLATE NOCASE", null).use { cursor ->
        val result = mutableListOf<Playlist>()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            result += Playlist(id, cursor.getString(1), playlistTrackIds(id))
        }
        result
    }

    fun playlistTrackIds(id: Long): List<Long> = readableDatabase.rawQuery(
        "SELECT audio_id FROM playlist_tracks WHERE playlist_id=? ORDER BY position", arrayOf(id.toString())
    ).use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) }
    }

    fun create(name: String): Long {
        val normalized = name.trim()
        if (normalized.isEmpty()) return -1
        return writableDatabase.insertWithOnConflict("playlists", null, ContentValues().apply {
            put("name", normalized)
        }, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun rename(id: Long, name: String) {
        if (name.isBlank()) return
        writableDatabase.update("playlists", ContentValues().apply { put("name", name.trim()) }, "id=?", arrayOf(id.toString()))
    }
    fun delete(id: Long) {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("playlist_tracks", "playlist_id=?", arrayOf(id.toString()))
            writableDatabase.delete("playlists", "id=?", arrayOf(id.toString()))
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
    fun addTrack(playlistId: Long, audioId: Long) {
        val next = readableDatabase.rawQuery(
            "SELECT COALESCE(MAX(position),-1)+1 FROM playlist_tracks WHERE playlist_id=?", arrayOf(playlistId.toString())
        ).use { it.moveToFirst(); it.getInt(0) }
        writableDatabase.insertWithOnConflict("playlist_tracks", null, ContentValues().apply {
            put("playlist_id", playlistId); put("audio_id", audioId); put("position", next)
        }, SQLiteDatabase.CONFLICT_IGNORE)
    }
    fun removeTrack(playlistId: Long, audioId: Long) {
        writableDatabase.delete("playlist_tracks", "playlist_id=? AND audio_id=?", arrayOf(playlistId.toString(), audioId.toString()))
    }
    fun replaceOrder(playlistId: Long, ids: List<Long>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            ids.forEachIndexed { index, id ->
                db.update("playlist_tracks", ContentValues().apply { put("position", index) },
                    "playlist_id=? AND audio_id=?", arrayOf(playlistId.toString(), id.toString()))
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun favorites(): Set<Long> = readableDatabase.rawQuery("SELECT audio_id FROM favorites", null).use { cursor ->
        buildSet { while (cursor.moveToNext()) add(cursor.getLong(0)) }
    }
    fun setFavorite(audioId: Long, enabled: Boolean) {
        if (enabled) writableDatabase.insertWithOnConflict("favorites", null, ContentValues().apply {
            put("audio_id", audioId)
        }, SQLiteDatabase.CONFLICT_IGNORE)
        else writableDatabase.delete("favorites", "audio_id=?", arrayOf(audioId.toString()))
    }
}
