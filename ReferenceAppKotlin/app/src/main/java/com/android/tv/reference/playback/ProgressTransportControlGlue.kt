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
import android.media.MediaCodecInfo
import android.provider.MediaStore
import androidx.annotation.VisibleForTesting
import androidx.leanback.media.*
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow.*
import androidx.leanback.widget.PlaybackControlsRow.ClosedCaptioningAction.INDEX_OFF
import androidx.leanback.widget.PlaybackControlsRow.ClosedCaptioningAction.INDEX_ON
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.TracksInfo
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.trackselection.TrackSelectionOverrides
import com.google.android.exoplayer2.trackselection.TrackSelectionOverrides.TrackSelectionOverride
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

    var audioAction: AudioAction? = null

    var textAction: TextAction? = null

    private var primaryActionsAdapter: ArrayObjectAdapter? = null

    override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
        // super.onCreatePrimaryActions() will create the play / pause action.
        super.onCreatePrimaryActions(primaryActionsAdapter)
        this.primaryActionsAdapter = primaryActionsAdapter

        // TODO default to ON
        closedCaptioningAction.index = INDEX_ON

        // Add the rewind and fast forward actions following the play / pause action.
        primaryActionsAdapter.apply {
            add(skipBackwardAction)
            add(skipForwardAction)
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
            audioAction -> nextAudioTrack()
            textAction -> nextTextTrack()
            else -> super.onActionClicked(action)
        }
    }

    fun updateTrackSelections(trackSelections: TrackSelectionArray) {
        for (i in 0 until trackSelections.length) {
            val selection = trackSelections[i] ?: continue
            // selection.type is unset, ugh, need to use mimetype?
            val selectedTrackGroup = selection.trackGroup
            val format = selectedTrackGroup.getFormat(0)
            val mimeType = format.sampleMimeType ?: continue

            if (mimeType.startsWith("audio/")) {
                if (audioAction != null) {
                    val action = audioAction!!
                    for (trackIndex in action.trackGroups.indices) {
                        if (action.trackGroups[trackIndex].id == selectedTrackGroup.id) {
                            action.index = trackIndex
                            notifyItemChanged(primaryActionsAdapter, action)
                            break
                        }
                    }
                }
            } else if (mimeType.startsWith("text/") || mimeType.startsWith("application/pgs")) {
                if (textAction != null) {
                    val action = textAction!!
                    for (trackIndex in action.trackGroups.indices) {
                        if (action.trackGroups[trackIndex].id == selectedTrackGroup.id) {
                            action.index = trackIndex
                            notifyItemChanged(primaryActionsAdapter, action)
                            break
                        }
                    }
                }
            }
        }
    }

    // only call when player state is ready
    fun addTrackActions(context: Context, tracksInfo: TracksInfo) {
        if (audioAction != null || textAction != null) {
            return
        }
        val supportedAudio = mutableListOf<TrackGroup>()
        val supportedText = mutableListOf<TrackGroup>()
        val trackGroupInfos = tracksInfo.trackGroupInfos
        for (infosIndex in 0 until trackGroupInfos.size) {
            val trackInfo = trackGroupInfos[infosIndex]
            val trackGroup = trackInfo.trackGroup
            if (trackInfo.isSupported) {
                if (trackInfo.trackType == C.TRACK_TYPE_AUDIO) {
                    supportedAudio.add(trackGroup)
                } else if (trackInfo.trackType == C.TRACK_TYPE_TEXT) {
                    supportedText.add(trackGroup)
                }
            }
        }
        if (supportedText.isNotEmpty()) {
            textAction = TextAction(context, supportedText)
            primaryActionsAdapter?.add(closedCaptioningAction)
            primaryActionsAdapter?.add(textAction)
        }
        if (supportedAudio.isNotEmpty()) {
            audioAction = AudioAction(context, supportedAudio)
            primaryActionsAdapter?.add(audioAction)
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

//    private fun printGroup(groups: TrackGroupArray) {
//        for (i in 0 until groups.length) {
//            for (j in 0 until groups[i].length) {
//                Timber.d("xxx TrackGroup[$i] format ${groups[i].getFormat(j)}")
//            }
//        }
//    }

    fun applySelection(action: MultiAction, trackGroup: TrackGroup) {
        notifyItemChanged(primaryActionsAdapter, action)
        val overrides = TrackSelectionOverrides.Builder()
            .setOverrideForType(TrackSelectionOverride(trackGroup))
            .build()
        trackSelector.parameters =
            trackSelector.buildUponParameters().setTrackSelectionOverrides(overrides).build()
    }

    private fun nextAudioTrack() {
        val action = audioAction!!
        action.nextIndex()
        applySelection(action, action.trackGroups[action.index])
    }

    private fun nextTextTrack() {
        val action = textAction!!
        action.nextIndex()
        applySelection(action, action.trackGroups[action.index])
    }

    private fun toggleClosedCaptions() {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo ?: return
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
