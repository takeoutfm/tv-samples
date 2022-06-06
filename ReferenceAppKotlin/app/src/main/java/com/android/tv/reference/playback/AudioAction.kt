package com.android.tv.reference.playback

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.leanback.widget.PlaybackControlsRow
import androidx.leanback.widget.PlaybackControlsRow.PlayPauseAction
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray

class AudioAction(context: Context, val trackGroups: List<TrackGroup>) : PlaybackControlsRow.MultiAction(-1) {

    init {
        val drawables = arrayOfNulls<Drawable>(trackGroups.size)
        for (i in trackGroups.indices) {
            val format = trackGroups[i].getFormat(0)
            val mix = when (format.channelCount) {
                1 -> "1.0"
                2 -> "2.0"
                6 -> "5.1"
                7 -> "5.2"
                8 -> "7.1"
                else -> "${format.channelCount}"
            }
            drawables[i] = TextDrawable(
                context.resources,
                mix
            )
        }
        setDrawables(drawables)
    }
}