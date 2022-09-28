package com.android.tv.reference.playback

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.leanback.widget.PlaybackControlsRow
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.source.TrackGroup

class AudioAction(context: Context, val trackGroups: List<Tracks.Group>) : PlaybackControlsRow.MultiAction(-1) {

    init {
        val drawables = arrayOfNulls<Drawable>(trackGroups.size)
        for (i in trackGroups.indices) {
            val format = trackGroups[i].getTrackFormat(0)
            val info = FormatInfo(format)
            drawables[i] = TextDrawable(
                context.resources,
                info.audioMix()
            )
        }
        setDrawables(drawables)
    }
}