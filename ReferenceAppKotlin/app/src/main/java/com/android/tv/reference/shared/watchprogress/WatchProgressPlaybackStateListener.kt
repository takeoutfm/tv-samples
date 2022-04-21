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
package com.android.tv.reference.shared.watchprogress

import com.android.tv.reference.playback.PlaybackStateListener
import com.android.tv.reference.playback.VideoPlaybackState
import com.android.tv.reference.repository.VideoRepositoryFactory
import com.android.tv.reference.shared.datamodel.Progress
import com.defsub.takeout.client.Client
import com.defsub.takeout.client.Offset
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Updates the watch progress for a video. The progress is updated in [VideoPlaybackState.Pause] or
 * [VideoPlaybackState.End] states.
 */
class WatchProgressPlaybackStateListener(
    private val watchProgressRepository: WatchProgressRepository,
    private val coroutineScope: CoroutineScope,
    private val coroutineDispatcher: CoroutineDispatcher
) : PlaybackStateListener {

    override fun onChanged(state: VideoPlaybackState) {
        if (!(state is VideoPlaybackState.Pause || state is VideoPlaybackState.End)) {
            return
        }

        val progress = when (state) {
            is VideoPlaybackState.Pause ->
                Progress(state.video.id, state.position, state.duration)
            is VideoPlaybackState.End -> Progress(
                state.video.id,
                state.video.duration().toMillis(),
                state.video.duration().toMillis()
            )
            else -> throw IllegalStateException(
                "WatchProgress is only updated in Pause or End states."
            )
        }

        coroutineScope.launch(coroutineDispatcher) {
            Timber.d("Saving watch progress: %s", progress)
            watchProgressRepository.insert(WatchProgress(progress.id, progress.position))
            VideoRepositoryFactory.getVideoRepository().updateProgress(listOf(progress))
        }
    }
}
