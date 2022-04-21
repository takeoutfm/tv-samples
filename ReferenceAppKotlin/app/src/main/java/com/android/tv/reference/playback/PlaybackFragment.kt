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

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.CaptioningManager
import androidx.fragment.app.viewModels
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.navigation.fragment.findNavController
import com.android.tv.reference.castconnect.CastHelper
import com.android.tv.reference.shared.datamodel.Video
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.SubtitleView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.gms.cast.tv.CastReceiverContext
import timber.log.Timber
import java.time.Duration
import java.util.*

/** Fragment that plays video content with ExoPlayer. */
class PlaybackFragment : VideoSupportFragment() {

    private lateinit var video: Video

    private var exoplayer: ExoPlayer? = null
    private lateinit var subtitles: SubtitleView
    private lateinit var trackSelector: DefaultTrackSelector
    private val viewModel: PlaybackViewModel by viewModels()
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var glue: ProgressTransportControlGlue<LeanbackPlayerAdapter>
    private lateinit var backgroundManager: BackgroundManager

    private val uiPlaybackStateListener = object : PlaybackStateListener {
        override fun onChanged(state: VideoPlaybackState) {
            // While a video is playing, the screen should stay on and the device should not go to
            // sleep. When in any other state such as if the user pauses the video, the app should
            // not prevent the device from going to sleep.
            view?.keepScreenOn = state is VideoPlaybackState.Play

            when (state) {
                is VideoPlaybackState.Prepare -> startPlaybackFromWatchProgress(state.startPosition)
                is VideoPlaybackState.End -> {
                    // To get to playback, the user always goes through browse first. Deep links for
                    // directly playing a video also go to browse before playback. If playback
                    // finishes the entire video, the PlaybackFragment is popped off the back stack
                    // and the user returns to browse.
                    findNavController().popBackStack()
                }
                is VideoPlaybackState.Error ->
                    findNavController().navigate(
                        PlaybackFragmentDirections
                            .actionPlaybackFragmentToPlaybackErrorFragment(
                                state.video,
                                state.exception
                            )
                    )
                else -> {
                    // Do nothing.
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backgroundManager = BackgroundManager.getInstance(requireActivity())

        // Get the video data.
        video = PlaybackFragmentArgs.fromBundle(requireArguments()).video

        // Create the MediaSession that will be used throughout the lifecycle of this Fragment.
        createMediaSession()

        backgroundType = BG_DARK
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subtitles = SubtitleView(requireContext())
        // text/x-ssa doesn't seem to take the user defaults correctly.
        // Some style elements appear to work
        // -- these don't seem to work
//        subtitles.setUserDefaultStyle()
//        subtitles.setUserDefaultTextSize()

        if (view is ViewGroup) {
            val root = view as ViewGroup
            root.addView(subtitles)
        } else {
            Timber.e("cannot add subtitles view")
        }

        viewModel.addPlaybackStateListener(uiPlaybackStateListener)
    }

    override fun onResume() {
        super.onResume()
        backgroundManager.clearDrawable()
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        subtitles = null
        viewModel.removePlaybackStateListener(uiPlaybackStateListener)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        destroyPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Releasing the mediaSession due to inactive playback and setting token for cast to null.
        mediaSession.release()
        CastHelper.setMediaSessionTokenForCast(
            /* mediaSession =*/ null,
            CastReceiverContext.getInstance().mediaManager
        )
    }

    private fun initializePlayer() {
        val factory = DefaultHttpDataSource.Factory()
        factory.setDefaultRequestProperties(video.headers)

        val item = MediaItem.Builder()
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(video.name).setSubtitle(video.tagline).build()
            )
            .setUri(video.videoUri).build()

        val mediaSource = ProgressiveMediaSource.Factory(factory)
            .createMediaSource(item)

        trackSelector = DefaultTrackSelector(requireContext())
        trackSelector.parameters = trackSelector.buildUponParameters()
            .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
            .setRendererDisabled(C.TRACK_TYPE_VIDEO, false)
            .setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE)
            .setPreferredAudioLanguage("en")
            .setPreferredTextLanguage("en")
            .setMaxAudioChannelCount(6) // 5.1
            .setPreferredAudioMimeTypes(
// temp fix for Bravia picking AC3 which are director commentary! See Civil War.
//                "audio/true-hd",
//                "audio/vnd.dts.hd",
//                "audio/vnd.dts",
//                "audio/ac3",
                "audio/mp4a-latm"
            )
            .setPreferredTextLanguageAndRoleFlagsToCaptioningManagerSettings(requireContext())
            .build()


        exoplayer =
            ExoPlayer.Builder(requireContext())
                .setTrackSelector(trackSelector)
                .build()
                .apply {
                    setMediaSource(mediaSource)
                    addListener(subtitles!!)
                    prepare()
                    addListener(PlayerEventListener())
                    glue = prepareGlue(this, this@PlaybackFragment.trackSelector)
                    mediaSessionConnector.setPlayer(this)
                    mediaSession.isActive = true
                }

        viewModel.onStateChange(VideoPlaybackState.Load(video))
    }

    private fun destroyPlayer() {
        mediaSession.isActive = false
        mediaSessionConnector.setPlayer(null)
        exoplayer?.let {
            // Pause the player to notify listeners before it is released.
            it.pause()
            it.release()
            exoplayer = null
        }
    }

    private fun prepareGlue(
        localExoplayer: ExoPlayer,
        trackSelector: DefaultTrackSelector
    ): ProgressTransportControlGlue<LeanbackPlayerAdapter> {
        return ProgressTransportControlGlue(
            requireContext(),
            LeanbackPlayerAdapter(
                requireContext(),
                localExoplayer,
                PLAYER_UPDATE_INTERVAL_MILLIS.toInt()
            ),
            onProgressUpdate,
            trackSelector
        ).apply {
            host = VideoSupportFragmentGlueHost(this@PlaybackFragment)
            title = video.name
            subtitle = video.tagline
            // Enable seek manually since PlaybackTransportControlGlue.getSeekProvider() is null,
            // so that PlayerAdapter.seekTo(long) will be called during user seeking.
            // TODO(gargsahil@): Add a PlaybackSeekDataProvider to support video scrubbing.
            isSeekEnabled = true
        }
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(requireContext(), MEDIA_SESSION_TAG)

        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setQueueNavigator(SingleVideoQueueNavigator(video, mediaSession))
            // not supported in 2.16.0 - is another solution needed?
//            setControlDispatcher(object : DefaultControlDispatcher() {
//                override fun dispatchStop(player: Player, reset: Boolean): Boolean {
//                    // Treat stop commands as pause, this keeps ExoPlayer, MediaSession, etc.
//                    // in memory to allow for quickly resuming. This also maintains the playback
//                    // position so that the user will resume from the current position when backing
//                    // out and returning to this video
//                    Timber.v("Playback stopped at ${player.currentPosition}")
//                    // This both prevents playback from starting automatically and pauses it if
//                    // it's already playing
//                    player.playWhenReady = false
//                    return true
//                }
//            })
        }

        CastHelper.setMediaSessionTokenForCast(
            mediaSession,
            CastReceiverContext.getInstance().mediaManager
        )
    }

    private fun startPlaybackFromWatchProgress(startPosition: Long) {
        Timber.v("Starting playback from $startPosition")
        exoplayer?.apply {
            seekTo(startPosition)
            playWhenReady = true
        }
    }

    private val onProgressUpdate: () -> Unit = {
        // TODO(benbaxter): Calculate when end credits are displaying and show the next episode for
        //  episodic content.
    }

    inner class PlayerEventListener : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Timber.w(error, "Playback error")
            viewModel.onStateChange(VideoPlaybackState.Error(video, error))
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray
        ) {
            // audio
            var enc = "None"
            var mix = ""
            // subtitles
            var sub = ""
            // video
            var vid = ""
            var def = ""
            for (i in 0 until trackSelections.length) {
                val s = trackSelections[i] ?: continue
                for (f in 0 until s.length()) {
                    val x = s.getFormat(f)
//                    Timber.d(s.getFormat(f).toString())
                    val sampleMimeType = x.sampleMimeType ?: continue
                    if (sampleMimeType.startsWith("audio/")) {
                        Timber.d("XXX -> audio ${x.sampleMimeType} ${x.label} ${x.channelCount} ${x.sampleRate}")
                        mix = when (x.channelCount) {
                            1 -> "Mono"
                            2 -> "Stereo"
                            6 -> "5.1"
                            7 -> "5.2"
                            8 -> "7.1"
                            else -> "${x.channelCount}"
                        }
                        enc = when (x.sampleMimeType) {
                            "audio/mp4a-latm" -> "AAC"
                            "audio/vnd.dts" -> "DTS"
                            "audio/vnd.dts.hd" -> "DTS HD"
                            "audio/true-hd" -> "True HD"
                            "audio/ac3" -> "AC3"
                            else -> "${x.sampleMimeType}"
                        }
                    } else if (
                    // "application/vobsub" not supported by exoplayer
                        sampleMimeType.startsWith("application/pgs") ||
                        sampleMimeType.startsWith("application/x-subrip") ||
                        sampleMimeType.startsWith("text/x-ssa")
                    ) {
                        sub = Locale(x.language!!).displayName
                    } else if (sampleMimeType.startsWith("video/")) {
                        vid = when (x.sampleMimeType) {
                            "video/hevc" -> "HEVC"
                            "video/avc" -> "AVC"
                            "video/av1" -> "AV1"
                            "video/vp9" -> "VP9"
                            "video/vp8" -> "VP8"
                            else -> "${x.sampleMimeType}"
                        }
                        // HD 1914 x 1036
                        // HD 1906 x 816
                        // HD 1920 x 804
                        // SD 662 x 478
                        val height = x.height
                        def = when {
                            height <= 480 -> "480p"
                            height in 481..720 -> "720p"
                            height in 721..1080 -> "1080p"
                            height in 1081..1440 -> "1440p"
                            height in 1441..2160 -> "4K"
                            height in 2161..4320 -> "8K"
                            else -> "$height"
                        }
                    } else {
                        Timber.d("XXX $sampleMimeType")
                    }
                }
            }
            var info = "$vid $def \u2022 $enc $mix"
            if (sub.isNotEmpty()) {
                info = "$info \u2022 $sub"
            }
            glue.subtitle = "${video.tagline}  ($info)"
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            when {
                isPlaying -> viewModel.onStateChange(
                    VideoPlaybackState.Play(video)
                )
                exoplayer!!.playbackState == Player.STATE_ENDED -> viewModel.onStateChange(
                    VideoPlaybackState.End(video)
                )
                else -> viewModel.onStateChange(
                    VideoPlaybackState.Pause(video, exoplayer!!.currentPosition, exoplayer!!.duration)
                )
            }
        }
    }

    companion object {
        // Update the player UI fairly often. The frequency of updates affects several UI components
        // such as the smoothness of the progress bar and time stamp labels updating. This value can
        // be tweaked for better performance.
        private val PLAYER_UPDATE_INTERVAL_MILLIS = Duration.ofMillis(50).toMillis()

        // A short name to identify the media session when debugging.
        private const val MEDIA_SESSION_TAG = "ReferenceAppKotlin"
    }
}
