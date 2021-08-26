package com.android.tv.reference.browse

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*
import androidx.navigation.fragment.findNavController
import com.android.tv.reference.repository.VideoRepositoryFactory
import com.android.tv.reference.shared.datamodel.Cast
import com.android.tv.reference.shared.datamodel.Profile
import com.android.tv.reference.shared.datamodel.Video
import com.defsub.takeout.tv.R
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class DetailsFragment : DetailsSupportFragment(), Target, OnItemViewClickedListener {
    companion object {
        private const val PLAY_ACTION = 1L
        private const val CAST_ACTION = 2L
    }

    private lateinit var video: Video
    private lateinit var detailsOverview: DetailsOverviewRow
    private lateinit var backgroundManager: BackgroundManager
    private lateinit var handler: Handler

    private val displayMetrics = DisplayMetrics()
    private var backgroundUri: String = ""
    private val backgroundRunnable: Runnable = Runnable {
        updateBackgroundImmediate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displayMetrics.setTo(resources.displayMetrics)
        handler = Handler(Looper.getMainLooper())
        backgroundManager = BackgroundManager.getInstance(requireActivity())
        onItemViewClickedListener = this

        // Get the video data.
        video = DetailsFragmentArgs.fromBundle(requireArguments()).video
        title = video.name

        // Details
        val rowPresenter =
            FullWidthDetailsOverviewRowPresenter(object : AbstractDetailsDescriptionPresenter() {
                override fun onBindDescription(vh: ViewHolder?, item: Any?) {
                    vh?.title?.text = video.name
                    vh?.subtitle?.text =
                        "${video.year} \u2022 ${video.rating} \u2022 ${video.tagline}"
                    vh?.body?.text = video.description
                }
            })

        // Setup PresenterSelector to distinguish between the different rows.
        val rowPresenterSelector = ClassPresenterSelector()
        rowPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, rowPresenter)
        rowPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        val mRowsAdapter = ArrayObjectAdapter(rowPresenterSelector)

        // Setup action and detail row.
        detailsOverview = DetailsOverviewRow(video)
        mRowsAdapter.add(detailsOverview)

        val actionsAdapter = ArrayObjectAdapter()
        val playAction = Action(PLAY_ACTION, getString(R.string.header_play))
        val castAction = Action(CAST_ACTION, getString(R.string.header_cast))
        actionsAdapter.add(playAction)
        actionsAdapter.add(castAction)
        detailsOverview.actionsAdapter = actionsAdapter

        // Setup related row.
        val castRowAdapter = ArrayObjectAdapter(CastCardPresenter())
        var header = HeaderItem(0, getString(R.string.header_cast))
        mRowsAdapter.add(ListRow(header, castRowAdapter))

        // Setup recommended row.
        val relatedRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
        header = HeaderItem(1, getString(R.string.header_recommended))
        mRowsAdapter.add(ListRow(header, relatedRowAdapter))

        runBlocking {
            withContext(Dispatchers.IO) {
                val detail = VideoRepositoryFactory.getVideoRepository().getVideoDetail(video.id)
                if (detail != null) {
                    detail.cast.forEach {
                        castRowAdapter.add(it)
                    }
                    detail.related.forEach {
                        relatedRowAdapter.add(it)
                    }
                }
            }
        }

        Picasso.get()
            .load(video.thumbnailUri)
            .into(this)

        adapter = mRowsAdapter
    }

    override fun onResume() {
        super.onResume()
        backgroundUri = ""
        updateBackgroundDelayed(video)
    }

    private fun updateBackgroundDelayed(video: Video) {
        if (backgroundUri != video.backgroundImageUri) {
            handler.removeCallbacks(backgroundRunnable)
            backgroundUri = video.backgroundImageUri

            if (backgroundUri.isEmpty()) {

            } else {
                handler.postDelayed(backgroundRunnable, 500L)
            }
        }
    }

    private val backgroundTarget = object : Target {
        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
            if (bitmap != null) {
                backgroundManager.setBitmap(bitmap)
            }
        }

        override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: Drawable?) {
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        }
    }

    private fun updateBackgroundImmediate() {
        if (activity == null) {
            return
        }

        Picasso.get()
            .load(backgroundUri)
            .centerCrop()
            .resize(displayMetrics.widthPixels, displayMetrics.heightPixels)
            .onlyScaleDown()
            .into(backgroundTarget)
    }

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?
    ) {
        if (item is Video) {
            findNavController().navigate(
                DetailsFragmentDirections.actionDetailsFragmentToDetailsFragment(item)
            )
            return
        }

        if (item is Cast) {
            var profile: Profile? = null
            runBlocking {
                withContext(Dispatchers.IO) {
                    profile = VideoRepositoryFactory.getVideoRepository().getProfile(item.person.id)
                }
            }
            if (profile != null) {
                findNavController().navigate(
                    DetailsFragmentDirections.actionDetailsFragmentToProfileFragment(profile!!)
                )
            }
            return
        }

        if (item is Action) {
            when (item.id) {
                CAST_ACTION -> setSelectedPosition(1)
                PLAY_ACTION -> findNavController().navigate(
                    DetailsFragmentDirections.actionDetailsFragmentToPlaybackFragment(video)
                )
            }
        }
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        if (bitmap != null) {
            detailsOverview.setImageBitmap(context, bitmap)
        }
    }

    override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: Drawable?) {
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
    }
}