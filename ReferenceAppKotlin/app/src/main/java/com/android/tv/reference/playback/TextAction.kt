package com.android.tv.reference.playback

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.leanback.widget.PlaybackControlsRow
import androidx.leanback.widget.PlaybackControlsRow.PlayPauseAction
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray

class TextAction(context: Context, val trackGroups: List<TrackGroup>) :
    PlaybackControlsRow.MultiAction(-1) {

    init {
        val drawables = arrayOfNulls<Drawable>(trackGroups.size)
        for (i in trackGroups.indices) {
            val format = trackGroups[i].getFormat(0)
            drawables[i] = TextDrawable(
                context.resources, format.language
            )
        }
        setDrawables(drawables)
    }
}