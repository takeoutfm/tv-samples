package com.android.tv.reference.browse

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.leanback.app.BackgroundManager
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.*
import androidx.navigation.fragment.findNavController
import com.android.tv.reference.repository.VideoRepositoryFactory
import com.android.tv.reference.shared.datamodel.Video
import com.defsub.takeout.tv.R

class SearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {

    companion object {
        private const val SEARCH_DELAY_MILLIS = 500L

        // check for things like:
        //   title:"text text" or cast:text*
        private val SPECIAL_CHARS = Regex("""[:"\\*]+""")
    }

    private lateinit var rowsAdapter : ArrayObjectAdapter
    private lateinit var handler: Handler
    private lateinit var backgroundManager: BackgroundManager

    private var searchQuery: String = ""

    private val searchRunnable: Runnable = Runnable {
        searchImmediate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        handler = Handler(Looper.getMainLooper())
        backgroundManager = BackgroundManager.getInstance(requireActivity())

        setSearchResultProvider(this)

        setOnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is Video ->
                    findNavController().navigate(
                        SearchFragmentDirections.actionSearchFragmentToDetailsFragment(item)
                    )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        backgroundManager.clearDrawable()
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return rowsAdapter
    }

    override fun onQueryTextChange(newQuery: String?): Boolean {
        if (newQuery != null && newQuery.length > 2) {
            searchDelayed(newQuery)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null) {
            searchDelayed(query)
        }
        return true
    }

    private fun searchDelayed(query: String) {
        if (searchQuery != query) {
            handler.removeCallbacks(searchRunnable)
        }
        if (query.isNotEmpty()) {
            searchQuery = query
            handler.postDelayed(searchRunnable, SEARCH_DELAY_MILLIS)
        }
    }

    private fun searchImmediate() {
        var pattern = searchQuery
        if (!pattern.contains(SPECIAL_CHARS)) {
            pattern = "title:$pattern* cast:$pattern* genre:$pattern* directing:$pattern* writing:$pattern*"
        }

        val videos = VideoRepositoryFactory.getVideoRepository().search(pattern) ?: emptyList()

        rowsAdapter.clear()
        val listRowAdapter = ArrayObjectAdapter(VideoCardPresenter())
        listRowAdapter.addAll(0, videos)
        rowsAdapter.add(ListRow(listRowAdapter))
    }
}