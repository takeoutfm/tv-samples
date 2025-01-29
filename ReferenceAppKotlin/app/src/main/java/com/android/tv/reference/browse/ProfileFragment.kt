package com.android.tv.reference.browse

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.*
import androidx.navigation.fragment.findNavController
import com.android.tv.reference.shared.datamodel.Profile
import com.android.tv.reference.shared.datamodel.Series
import com.android.tv.reference.shared.datamodel.Video
import com.takeoutfm.tv.R
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target

class ProfileFragment : DetailsSupportFragment(), Target, OnItemViewClickedListener {
    companion object {
        private const val MOVIES_ACTION = 1L
        private const val SERIES_ACTION = 2L
    }

    private lateinit var profile: Profile
    private lateinit var detailsOverview: DetailsOverviewRow
    private lateinit var backgroundManager: BackgroundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onItemViewClickedListener = this

        backgroundManager = BackgroundManager.getInstance(requireActivity())
        profile = ProfileFragmentArgs.fromBundle(requireArguments()).profile
        title = getString(R.string.app_name)

        val rowPresenter =
            FullWidthDetailsOverviewRowPresenter(object : AbstractDetailsDescriptionPresenter() {
                override fun onBindDescription(vh: ViewHolder?, item: Any?) {
                    vh?.title?.text = profile.person.name
                    vh?.subtitle?.text = "${profile.person.birthplace} \u2022 ${profile.person.birthday}"
                    vh?.body?.text = profile.person.bio
                }
            })

        // Setup PresenterSelector to distinguish between the different rows.
        val rowPresenterSelector = ClassPresenterSelector()
        rowPresenterSelector.addClassPresenter(DetailsOverviewRow::class.java, rowPresenter)
        rowPresenterSelector.addClassPresenter(ListRow::class.java, ListRowPresenter())
        val mRowsAdapter = ArrayObjectAdapter(rowPresenterSelector)

        // Setup action and detail row.
        detailsOverview = DetailsOverviewRow(profile)
        mRowsAdapter.add(detailsOverview)

        val actionsAdapter = ArrayObjectAdapter()
        if (profile.videos.isNotEmpty()) {
            val moviesAction = Action(MOVIES_ACTION, getString(R.string.header_movies))
            actionsAdapter.add(moviesAction)
        }
        if (profile.series.isNotEmpty()) {
            val seriesAction = Action(SERIES_ACTION, getString(R.string.header_series))
            actionsAdapter.add(seriesAction)
        }
        detailsOverview.actionsAdapter = actionsAdapter

        // Setup related row.
        if (profile.videos.isNotEmpty()) {
            val starringRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
            val header = HeaderItem(0, getString(R.string.header_movies))
            profile.videos.forEach {
                starringRowAdapter.add(it)
            }
            mRowsAdapter.add(ListRow(header, starringRowAdapter))
        }
        if (profile.series.isNotEmpty()) {
            val starringRowAdapter = ArrayObjectAdapter(SeriesCardPresenter())
            val header = HeaderItem(0, getString(R.string.header_series))
            profile.series.forEach {
                starringRowAdapter.add(it)
            }
            mRowsAdapter.add(ListRow(header, starringRowAdapter))
        }

        Picasso.get()
            .load(profile.person.thumbnailUri)
            .into(this)

        adapter = mRowsAdapter
    }

    override fun onResume() {
        super.onResume()
        backgroundManager.clearDrawable()
    }

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any?,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?
    ) {
        if (item is Video) {
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileFragmentToDetailsFragment(item)
            )
            return
        }

        if (item is Series) {
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileFragmentToSeriesFragment(item)
            )
            return
        }

        if (item is Action) {
            when (item.id) {
                MOVIES_ACTION -> setSelectedPosition(1)
                SERIES_ACTION -> setSelectedPosition(2)
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