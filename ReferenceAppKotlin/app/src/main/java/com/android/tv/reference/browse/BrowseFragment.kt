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
package com.android.tv.reference.browse

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.BrowseSupportFragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.android.tv.reference.repository.VideoRepositoryFactory
import com.android.tv.reference.shared.datamodel.Series
import com.android.tv.reference.shared.datamodel.Video
import com.android.tv.reference.shared.datamodel.VideoGroup
import com.android.tv.reference.shared.image.BlurImageTransformation
import com.android.tv.reference.shared.image.OverlayImageTransformation
import com.android.tv.reference.shared.watchprogress.WatchProgressDatabase
import com.android.tv.reference.shared.watchprogress.WatchProgressRepository
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.takeoutfm.tv.R
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Fragment displaying the main content browsing UI
 *
 * This shows a menu of categories and a row for each category. Users can pick which content to
 * play by navigating and pressing the main select button.
 */
class BrowseFragment : BrowseSupportFragment(), Target {

    companion object {
        private const val BACKGROUND_UPDATE_DELAY_MILLIS = 500L
    }

    private lateinit var viewModel: BrowseViewModel
    private lateinit var backgroundManager: BackgroundManager
    private lateinit var handler: Handler
    private var selectedVideo: Video? = null

    private val overlayImageTransformation =
        OverlayImageTransformation()
    private lateinit var blurImageTransformation: BlurImageTransformation

    // The DisplayMetrics instance is used to get the screen dimensions
    private val displayMetrics = DisplayMetrics()

    // The URI of the background we are currently displaying to avoid reloading the same one
    private var backgroundUri = ""

    private val backgroundRunnable: Runnable = Runnable {
        updateBackgroundImmediate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayMetrics.setTo(resources.displayMetrics)
        blurImageTransformation = BlurImageTransformation(requireContext())

        handler = Handler(Looper.getMainLooper())
        backgroundManager = BackgroundManager.getInstance(requireActivity())

        val signInMenuItem = BrowseCustomMenu.MenuItem(getString(R.string.sign_in)) {
            findNavController().navigate(R.id.action_global_signInFragment)
        }
        val signOutMenuItem = BrowseCustomMenu.MenuItem(getString(R.string.sign_out)) {
            viewModel.signOut()
        }
        val refreshMenuItem = BrowseCustomMenu.MenuItem(getString(R.string.refresh)) {
            viewModel.refresh()
        }
        val clearProgressItem = BrowseCustomMenu.MenuItem(getString(R.string.clear_watch_next)) {
            val watchProgressRepository = WatchProgressRepository(
                WatchProgressDatabase.getDatabase(requireContext()).watchProgressDao()
            )
            runBlocking {
                watchProgressRepository.deleteAll()
                // TODO remote progress
            }
        }

        viewModel = ViewModelProvider(this).get(BrowseViewModel::class.java)
        viewModel.watchProgress.observe(
            this
        ) {
            adapter = BrowseAdapter(
                doWatchProgress(),
                viewModel.browseContent.value ?: listOf(),
                viewModel.seriesContent.value ?: listOf(),
                viewModel.customMenuItems.value ?: listOf()
            )
        }
        viewModel.browseContent.observe(
            this
        ) {
            adapter = BrowseAdapter(
                doWatchProgress(), it,
                viewModel.seriesContent.value ?: listOf(),
                viewModel.customMenuItems.value ?: listOf()
            )
        }
        viewModel.customMenuItems.observe(
            this
        ) {
            adapter = BrowseAdapter(
                doWatchProgress(),
                viewModel.browseContent.value ?: listOf(),
                viewModel.seriesContent.value ?: listOf(),
                it
            )
        }
        viewModel.isSignedIn.observe(
            this
        ) {
            val settings = mutableListOf(
                if (it) {
                    signOutMenuItem
                } else {
                    signInMenuItem
                }
            )
            if (it) {
                settings.add(refreshMenuItem)
            }
            settings.add(clearProgressItem)
            viewModel.customMenuItems.postValue(
                listOf(BrowseCustomMenu(getString(R.string.settings), settings))
            )
            if (it) {
                viewModel.refresh()
            }
        }

        setOnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is Video -> {
                    selectedVideo = item
                    findNavController().navigate(
                        BrowseFragmentDirections.actionBrowseFragmentToDetailsFragment(item)
                    )
//                    clearBackground()
                }

                is Series -> {
                    findNavController().navigate(
                        BrowseFragmentDirections.actionBrowseFragmentToSeriesFragment(item)
                    )
                }

                is BrowseCustomMenu.MenuItem -> item.handler()
            }
        }

        setOnItemViewSelectedListener { _, item, _, _ ->
            if (item is Video) {
                updateBackgroundDelayed(item.backgroundImageUri)
            } else if (item is Series) {
                updateBackgroundDelayed(item.backgroundImageUri)
            }
        }

        setOnSearchClickedListener {
            findNavController().navigate(
                BrowseFragmentDirections.actionBrowseFragmentToSearchFragment()
            )
        }

        // BrowseSupportFragment allows for adding either text (with setTitle) or a Drawable
        // (with setBadgeDrawable) to the top right of the screen. Since we don't have a suitable
        // Drawable, we just display the app name in text.
        title = getString(R.string.app_name)
    }

    override fun onResume() {
        super.onResume()
        backgroundManager.clearDrawable()
        if (selectedVideo != null) {
            backgroundUri = ""
            updateBackgroundDelayed(selectedVideo!!.backgroundImageUri)
        }
    }

    private fun doWatchProgress(): VideoGroup {
        viewModel.watchProgress.value?.let {
            val videoRepository = VideoRepositoryFactory.getVideoRepository()
            val videos =
                it.mapNotNull { watchProgress -> videoRepository.getVideoById(watchProgress.videoId) }
            return VideoGroup(getString(R.string.watch_next), videos)
        }
        return VideoGroup("", emptyList())
    }

    /**
     * Updates the main fragment background after a delay
     *
     * This delay allows the user to quickly scroll through content without flashing a changing
     * background with every item that is passed.
     */
    private fun updateBackgroundDelayed(uri: String) {
        if (backgroundUri != uri) {
            handler.removeCallbacks(backgroundRunnable)
            backgroundUri = uri

            if (backgroundUri.isEmpty()) {
                backgroundManager.clearDrawable()
            } else {
                handler.postDelayed(backgroundRunnable, BACKGROUND_UPDATE_DELAY_MILLIS)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Picasso.get().cancelRequest(this)
    }

    private fun updateBackgroundImmediate() {
        if (activity == null) {
            // Triggered after fragment detached from activity, ignore
            return
        }
        if (backgroundUri.isEmpty()) {
            return
        }

        Picasso.get()
            .load(backgroundUri)
            .centerCrop()
            .resize(displayMetrics.widthPixels, displayMetrics.heightPixels)
            .onlyScaleDown()
            .transform(overlayImageTransformation).transform(blurImageTransformation)
            .into(this)
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        // Keep displaying the previous background
    }

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
        Timber.w(e, "Failed to load background")
        showDefaultBackground()
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        if (bitmap == null) {
            Timber.w("Background loaded but was null")
            showDefaultBackground()
        } else {
            backgroundManager.setBitmap(bitmap)
        }
    }

    private fun showDefaultBackground() {
        backgroundUri = ""
        backgroundManager.clearDrawable()
    }
}
