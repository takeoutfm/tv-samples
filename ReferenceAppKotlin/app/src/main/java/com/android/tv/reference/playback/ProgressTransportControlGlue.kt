/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tv.reference.playback

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.leanback.media.*
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow.*
import androidx.leanback.widget.PlaybackControlsRow.ClosedCaptioningAction.INDEX_OFF
import androidx.leanback.widget.PlaybackControlsRow.ClosedCaptioningAction.INDEX_ON
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import timber.log.Timber
import java.util.concurrent.TimeUnit


/**
 * Custom [PlaybackTransportControlGlue] that exposes a callback when the progress is updated.
 *
 * The callback is triggered based on a progress interval defined in several ways depending on the
 * [PlayerAdapter].
 *
 * [LeanbackPlayerAdapter] example:
 * ```
 *     private val updateMillis = 16
 *     LeanbackPlayerAdapter(context, exoplayer, updateMillis)
 * ```
 *
 * [MediaPlayerAdapter] example:
 * ```
 *     object : MediaPlayerAdapter(context) {
 *         private val updateMillis = 16
 *         override fun getProgressUpdatingInterval(): Int {
 *             return updateMillis
 *         }
 *     }
 * ```
 */
class ProgressTransportControlGlue<T : PlayerAdapter>(
    context: Context,
    impl: T,
    private val updateProgress: () -> Unit,
    private val trackSelector: DefaultTrackSelector
) : PlaybackTransportControlGlue<T>(context, impl) {

    // Define actions for fast forward and rewind operations.
    @VisibleForTesting
    var skipForwardAction: FastForwardAction = FastForwardAction(context)

    @VisibleForTesting
    var skipBackwardAction: RewindAction = RewindAction(context)

    var closedCaptioningAction: ClosedCaptioningAction = ClosedCaptioningAction(context)

    override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
        // super.onCreatePrimaryActions() will create the play / pause action.
        super.onCreatePrimaryActions(primaryActionsAdapter)

        // TODO default to ON
        closedCaptioningAction.index = INDEX_ON

        // Add the rewind and fast forward actions following the play / pause action.
        primaryActionsAdapter.apply {
            add(skipBackwardAction)
            add(skipForwardAction)
            add(closedCaptioningAction)
        }
    }

    override fun onUpdateProgress() {
        super.onUpdateProgress()
        updateProgress()
    }

    override fun onActionClicked(action: Action) {
        // Primary actions are handled manually. The superclass handles default play/pause action.
        when (action) {
            skipBackwardAction -> skipBackward()
            skipForwardAction -> skipForward()
            closedCaptioningAction -> toggleClosedCaptions()
            else -> super.onActionClicked(action)
        }
    }

    /** Skips backward 30 seconds.  */
    private fun skipBackward() {
        var newPosition: Long = currentPosition - THIRTY_SECONDS
        newPosition = newPosition.coerceAtLeast(0L)
        playerAdapter.seekTo(newPosition)
    }

    /** Skips forward 30 seconds.  */
    private fun skipForward() {
        var newPosition: Long = currentPosition + THIRTY_SECONDS
        newPosition = newPosition.coerceAtMost(duration)
        playerAdapter.seekTo(newPosition)
    }

    private fun printGroup(groups: TrackGroupArray) {
        for (i in 0 until groups.length) {
            for (j in 0 until groups[i].length) {
                Timber.d("TrackGroup[$i] format ${groups[i].getFormat(j)}")
            }
        }
    }

    private fun toggleClosedCaptions() {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return
//        for (rendererIndex in 0 until mappedTrackInfo.rendererCount) {
//            val trackType = mappedTrackInfo.getRendererType(rendererIndex)
//            if (trackType == C.TRACK_TYPE_AUDIO) {
//                val groups = mappedTrackInfo.getTrackGroups(trackType)
//                Timber.d("audio groups ${groups.length}")
//                printGroup(groups)
//            } else if (trackType == C.TRACK_TYPE_VIDEO) {
//                val groups = mappedTrackInfo.getTrackGroups(trackType)
//                Timber.d("video groups ${groups.length}")
//                printGroup(groups)
//
//            } else if (trackType == C.TRACK_TYPE_TEXT) {
//                val groups = mappedTrackInfo.getTrackGroups(trackType)
//                Timber.d("text groups ${groups.length}")
//                printGroup(groups)
//            }
//        }

        val trackGroups = mappedTrackInfo.getTrackGroups(C.TRACK_TYPE_VIDEO)
        if (closedCaptioningAction.index == INDEX_ON) {
            closedCaptioningAction.index = INDEX_OFF
            trackSelector.parameters =
                trackSelector.buildUponParameters()
                    .setRendererDisabled(C.TRACK_TYPE_VIDEO, true)
                    .clearSelectionOverride(C.TRACK_TYPE_VIDEO, trackGroups)
                    .build()

        } else {
            closedCaptioningAction.index = INDEX_ON
            trackSelector.parameters =
                trackSelector.buildUponParameters().setRendererDisabled(C.TRACK_TYPE_VIDEO, false)
                    .build()
        }
    }

    companion object {
        private val THIRTY_SECONDS = TimeUnit.SECONDS.toMillis(30)
    }
}
