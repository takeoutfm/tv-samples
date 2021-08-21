package com.android.tv.reference.browse

import android.os.Bundle
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.navigation.fragment.findNavController
import com.android.tv.reference.TvReferenceApplication
import com.android.tv.reference.repository.VideoRepository
import com.android.tv.reference.repository.VideoRepositoryFactory
import com.android.tv.reference.shared.datamodel.Video
import java.util.*

class SearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    private lateinit var rowsAdapter : ArrayObjectAdapter
    private lateinit var videoRepository : VideoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        videoRepository = VideoRepositoryFactory.getVideoRepository(TvReferenceApplication.instance)
        setSearchResultProvider(this)

        setOnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is Video ->
                    findNavController().navigate(
                        SearchFragmentDirections.actionSearchFragmentToPlaybackFragment(item)
                    )
            }
        }
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return rowsAdapter
    }

    override fun onQueryTextChange(newQuery: String?): Boolean {
        if (newQuery != null) {
            doSearch(newQuery)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null) {
            doSearch(query)
        }
        return true
    }

    private fun doSearch(query : String) {
        if (query.isEmpty()) {
            return
        }

        val results = mutableListOf<Video>()
        for (video in videoRepository.getAllVideos()) {
            if (video.name.toLowerCase(Locale.getDefault()).contains(query)) {
                results.add(video)
            }
        }

        rowsAdapter.clear()
        val cardPresenter = VideoCardPresenter()
        val listRowAdapter = ArrayObjectAdapter(cardPresenter)
        listRowAdapter.addAll(0, results)
        rowsAdapter.add(ListRow(listRowAdapter))
    }
}