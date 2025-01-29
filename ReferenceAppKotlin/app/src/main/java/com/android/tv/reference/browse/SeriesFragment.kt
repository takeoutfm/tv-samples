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
import com.android.tv.reference.shared.datamodel.Series
import com.android.tv.reference.shared.datamodel.Video
import com.android.tv.reference.shared.watchprogress.WatchProgressDatabase
import com.android.tv.reference.shared.watchprogress.WatchProgressRepository
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import com.takeoutfm.tv.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.lang.StringBuilder

class SeriesFragment : DetailsSupportFragment(), Target, OnItemViewClickedListener {

    private lateinit var series: Series
    private lateinit var detailsOverview: DetailsOverviewRow
    private lateinit var backgroundManager: BackgroundManager
    private lateinit var handler: Handler
    private lateinit var watchProgressRepository: WatchProgressRepository

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

        watchProgressRepository = WatchProgressRepository(
            WatchProgressDatabase.getDatabase(requireContext()).watchProgressDao()
        )

        // Get the video data.
        series = SeriesFragmentArgs.fromBundle(requireArguments()).series
        title = getString(R.string.app_name)

        val separator = " \u2022 "
        val detail = StringBuilder()
        if (series.year != -1) {
            detail.append(series.year)
        }
        if (series.rating.isNotEmpty()) {
            if (detail.isNotEmpty()) detail.append(separator)
            detail.append(series.rating)
        }
        if (series.tagline.isNotEmpty()) {
            if (detail.isNotEmpty()) detail.append(separator)
            detail.append(series.tagline)
        }

        // Details
        val rowPresenter =
            FullWidthDetailsOverviewRowPresenter(object : AbstractDetailsDescriptionPresenter() {
                override fun onBindDescription(vh: ViewHolder?, item: Any?) {
                    vh?.title?.text = series.name
                    vh?.subtitle?.text = detail.toString()
                    vh?.body?.text = ""
                }
            })

        // Setup PresenterSelector to distinguish between the different rows.
        val rowPresenterSelector = ClassPresenterSelector()
        rowPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, rowPresenter)
        rowPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        val mRowsAdapter = ArrayObjectAdapter(rowPresenterSelector)

        // Setup action and detail row.
        detailsOverview = DetailsOverviewRow(series)
        mRowsAdapter.add(detailsOverview)

        runBlocking {
            withContext(Dispatchers.IO) {
                for (i in 0..series.seasonCount) {
                    val seasonNumber = i + 1
                    var added = false
                    val relatedRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
                    for (e in series.episodes) {
                        if (e.seasonNumber.toInt() == seasonNumber) {
                            if (!added) {
                                val header =
                                    HeaderItem(1, getString(R.string.header_season, seasonNumber))
                                mRowsAdapter.add(ListRow(header, relatedRowAdapter))
                                added = true
                            }
                            relatedRowAdapter.add(e)
                        }
                    }
                }
            }
        }

        Picasso.get()
            .load(series.thumbnailUri)
            .into(this)

        adapter = mRowsAdapter
    }

    override fun onResume() {
        super.onResume()
        backgroundUri = ""
        updateBackgroundDelayed(series)

        val actionsAdapter = ArrayObjectAdapter()
        for (s in 0..series.seasonCount) {
            val seasonNumber = s + 1
            for (e in series.episodes) {
                if (e.seasonNumber == seasonNumber.toString()) {
                    val action = Action(
                        seasonNumber.toLong(),
                        getString(R.string.header_season, seasonNumber)
                    )
                    actionsAdapter.add(action);
                    break
                }
            }
        }

        detailsOverview.actionsAdapter = actionsAdapter
    }

    private fun updateBackgroundDelayed(series: Series) {
        if (backgroundUri != series.backgroundImageUri) {
            handler.removeCallbacks(backgroundRunnable)
            backgroundUri = series.backgroundImageUri

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

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
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
        if (item is Action) {
            val id = item.id
            setSelectedPosition(id.toInt())
        } else if (item is Video) {
            findNavController().navigate(
                SeriesFragmentDirections.actionSeriesFragmentToDetailsFragment(item)
            )
            return
        }
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        if (bitmap != null) {
            detailsOverview.setImageBitmap(context, bitmap)
        }
    }

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
    }
}