package com.m21deck.data

import java.io.Reader
import java.io.Writer

/** Portable plain-text M3U support. Content URIs export correctly for Android, but may not
 *  be portable to players that cannot resolve another app's MediaStore identifiers. */
object M3u {
    fun export(tracks: List<Track>, writer: Writer) {
        writer.write("#EXTM3U\n")
        tracks.forEach { track ->
            writer.write("#EXTINF:${track.durationMs / 1000},${track.artist} - ${track.title}\n")
            writer.write("${track.uri}\n")
        }
        writer.flush()
    }
    fun importList(reader: Reader, library: List<Track>): List<Long> {
        val byUri = library.associateBy { it.uri.toString() }
        val byName = library.associateBy { it.fileName.lowercase() }
        return reader.readText().lineSequence().map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                byUri[line]?.id ?: byName[line.substringAfterLast('/').lowercase()]?.id
            }.distinct().toList()
    }
}
