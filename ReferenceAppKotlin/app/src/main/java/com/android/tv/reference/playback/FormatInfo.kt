package com.android.tv.reference.playback

import android.content.Context
import android.widget.Toast
import com.google.android.exoplayer2.Format
import java.util.*

class FormatInfo(private val format: Format) {
    private val mimeType: String = format.sampleMimeType ?: ""

    fun isText(): Boolean {
        // note that exoplayer doesn't support vobsub
        return mimeType.startsWith("text/") ||
                    mimeType.startsWith("application/pgs") ||
                    mimeType.startsWith("application/x-srt") ||
                    mimeType.startsWith("application/x-subrip")
    }

    fun isAudio(): Boolean {
        return mimeType.startsWith("audio/")
    }

    fun isVideo(): Boolean {
        return mimeType.startsWith("video/")
    }

    fun language(): String {
        return format.language!!
    }

    fun languageDisplayName(): String {
        return Locale(format.language!!).displayName
    }

    fun audioMixDesc(): String {
        return when (format.channelCount) {
            1 -> "Mono"
            2 -> "Stereo"
            6 -> "Surround"
            7 -> "Surround"
            8 -> "Surround"
            else -> "${format.channelCount}"
        }
    }

    fun audioMix(): String {
        return when (format.channelCount) {
            1 -> "1.0"
            2 -> "2.0"
            6 -> "5.1"
            7 -> "5.2"
            8 -> "7.1"
            else -> "${format.channelCount}"
        }
    }

    fun shortName(): String {
        return when (mimeType) {
            // audio
            "audio/mp4a-latm" -> "AAC"
            "audio/vnd.dts" -> "DTS"
            "audio/vnd.dts.hd" -> "DTS HD"
            "audio/true-hd" -> "True HD"
            "audio/ac3" -> "AC3"
            "audio/flac" -> "FLAC"
            "audio/mp3" -> "MP3"
            // video
            "video/hevc" -> "HEVC"
            "video/avc" -> "AVC"
            "video/av1" -> "AV1"
            "video/vp9" -> "VP9"
            "video/vp8" -> "VP8"
            // subtitle
            "application/pgs" -> "PGS"
            "application/x-srt", "application/x-subrip" -> "SRT"
            // other
            else -> mimeType
        }
    }

    fun videoDesc(): String {
        // HD 1914 x 1036
        // HD 1906 x 816
        // HD 1920 x 804
        // SD 662 x 478
        val height = format.height
        return when {
            height <= 480 -> "480p"
            height in 481..720 -> "720p"
            height in 721..1080 -> "1080p"
            height in 1081..1440 -> "1440p"
            height in 1441..2160 -> "4K"
            height in 2161..4320 -> "8K"
            else -> "$height"
        }
    }
}