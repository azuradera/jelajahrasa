package com.bayu.jelajahrasa.ui.collection

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bayu.jelajahrasa.JelajahRasaApp
import com.bayu.jelajahrasa.MainActivity
import com.bayu.jelajahrasa.R
import com.bayu.jelajahrasa.databinding.FragmentCollectionBinding
import com.bayu.jelajahrasa.model.CollectionType
import com.bayu.jelajahrasa.model.Mood
import com.bayu.jelajahrasa.ui.movie.ExploreMovieAdapter
import com.bayu.jelajahrasa.util.NoFilterArrayAdapter
import com.bayu.jelajahrasa.util.showAppSnackbar
import kotlinx.coroutines.launch

class CollectionFragment : Fragment(R.layout.fragment_collection) {

    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!

    private val collectionViewModel: CollectionViewModel by viewModels {
        val app = requireActivity().application as JelajahRasaApp

        CollectionViewModelFactory(
            movieRepository = app.movieRepository,
            authRepository = app.authRepository
        )
    }

    private lateinit var movieAdapter: ExploreMovieAdapter

    private val sortOptions by lazy {
        listOf(
            getString(R.string.collection_sort_newest),
            getString(R.string.collection_sort_oldest)
        )
    }

    private val moodOptions by lazy {
        listOf(
            getString(R.string.mood_all),
            getString(R.string.mood_happy),
            getString(R.string.mood_sad),
            getString(R.string.mood_energetic),
            getString(R.string.mood_tense),
            getString(R.string.mood_romantic),
            getString(R.string.mood_curious)
        )
    }

    private val moods = listOf(null, Mood.HAPPY, Mood.SAD, Mood.ENERGETIC, Mood.TENSE, Mood.ROMANTIC, Mood.CURIOUS)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCollectionBinding.bind(view)

        setupMovieList()
        setupDropdowns()
        setupCollectionTabs()
        observeCollections()

        collectionViewModel.loadCollections(
            collectionViewModel.uiState.value.selectedType
        )
    }

    private fun setupDropdowns() {
        val sortAdapter = NoFilterArrayAdapter(requireContext(), R.layout.item_dropdown, sortOptions)
        binding.collectionSortDropdown.setAdapter(sortAdapter)
        if (binding.collectionSortDropdown.text.isNullOrBlank()) {
            binding.collectionSortDropdown.setText(sortOptions.first(), false)
        }
        binding.collectionSortDropdown.setOnItemClickListener { _, _, position, _ ->
            val sortOrder = if (position == 0) "DESC" else "ASC"
            collectionViewModel.setSortOrder(sortOrder)
            binding.collectionMovies.scrollToPosition(0)
        }

        val moodAdapter = NoFilterArrayAdapter(requireContext(), R.layout.item_dropdown, moodOptions)
        binding.collectionMoodDropdown.setAdapter(moodAdapter)
        if (binding.collectionMoodDropdown.text.isNullOrBlank()) {
            binding.collectionMoodDropdown.setText(moodOptions.first(), false)
        }
        binding.collectionMoodDropdown.setOnItemClickListener { _, _, position, _ ->
            collectionViewModel.setMoodFilter(moods.getOrNull(position))
            binding.collectionMovies.scrollToPosition(0)
        }

        binding.collectionSearchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                val query = binding.collectionSearchInput.text?.toString().orEmpty()
                collectionViewModel.setSearchQuery(query)
                true
            } else {
                false
            }
        }

        binding.collectionSearchInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                (requireActivity() as? MainActivity)?.setBottomNavVisibility(false)
            }
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireContext()
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            binding.root.windowToken,
            0
        )
    }

    fun isMultiSelectionActive(): Boolean {
        return ::movieAdapter.isInitialized && movieAdapter.selectedMovieIds.isNotEmpty()
    }

    private fun setupMovieList() {
        movieAdapter = ExploreMovieAdapter(
            onMovieClick = { movie ->
                val bundle = Bundle().apply {
                    putInt("movieId", movie.movieId)
                }

                findNavController().navigate(
                    R.id.action_collection_to_detail,
                    bundle
                )
            },
            onSelectionChanged = { count ->
                if (count > 0) {
                    binding.selectionActionBar.visibility = View.VISIBLE
                    binding.selectionCountText.text = getString(R.string.selection_count, count)
                    binding.batchMarkWatchedButton.visibility =
                        if (collectionViewModel.uiState.value.selectedType == CollectionType.WATCHED) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }
                    (requireActivity() as? MainActivity)?.setBottomNavVisibility(false)
                } else {
                    binding.selectionActionBar.visibility = View.GONE
                    (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
                }
            }
        )

        binding.batchRemoveButton.setOnClickListener {
            val selectedIds = movieAdapter.selectedMovieIds.toList()
            val currentType = collectionViewModel.uiState.value.selectedType
            if (selectedIds.isNotEmpty()) {
                collectionViewModel.removeMoviesFromCollection(selectedIds, currentType)
                movieAdapter.clearSelection()
                binding.selectionActionBar.visibility = View.GONE
                (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
                binding.root.showAppSnackbar("Film terpilih berhasil dihapus")
            }
        }

        binding.batchMarkWatchedButton.setOnClickListener {
            val selectedIds = movieAdapter.selectedMovieIds
            val currentList = movieAdapter.currentList
            val selectedMovies = currentList.filter { selectedIds.contains(it.movieId) }

            if (selectedMovies.isNotEmpty()) {
                collectionViewModel.addMoviesToCollection(selectedMovies, CollectionType.WATCHED)
                movieAdapter.clearSelection()
                binding.selectionActionBar.visibility = View.GONE
                (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
                binding.root.showAppSnackbar("Ditandai sebagai Sudah Ditonton")
            }
        }

        binding.batchCancelButton.setOnClickListener {
            movieAdapter.clearSelection()
            binding.selectionActionBar.visibility = View.GONE
            (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
        }

        binding.scrollToTopButton.setOnClickListener {
            binding.collectionMovies.smoothScrollToPosition(0)
            binding.collectionAppBar.setExpanded(true, true)
            binding.scrollToTopButton.visibility = View.GONE
        }

        binding.collectionMovies.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = movieAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val canScrollUp = recyclerView.canScrollVertically(-1)
                    if (canScrollUp) {
                        binding.scrollToTopButton.visibility = View.VISIBLE
                    } else {
                        binding.scrollToTopButton.visibility = View.GONE
                        binding.collectionAppBar.setExpanded(true, true)
                    }

                    if (isMultiSelectionActive()) {
                        (requireActivity() as? MainActivity)?.setBottomNavVisibility(false)
                        return
                    }
                    if (dy > 10) {
                        (requireActivity() as? MainActivity)?.setBottomNavVisibility(false)
                    } else if (dy < -10) {
                        (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
                    }
                }
            })
        }
    }

    private fun setupCollectionTabs() {
        updateTabStyles(binding.collectionTabs.checkedButtonId)

        binding.collectionTabs.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            updateTabStyles(checkedId)

            val type = when (checkedId) {
                R.id.tab_watchlist -> CollectionType.WATCHLIST
                R.id.tab_watched -> CollectionType.WATCHED
                R.id.tab_favorite -> CollectionType.FAVORITE
                else -> return@addOnButtonCheckedListener
            }

            collectionViewModel.loadCollections(type)
            binding.collectionMovies.scrollToPosition(0)
        }
    }

    private fun updateTabStyles(checkedId: Int) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.theme_primary)
        val surfaceColor = ContextCompat.getColor(requireContext(), R.color.theme_surface)
        val textSecondaryColor = ContextCompat.getColor(requireContext(), R.color.theme_text_secondary)
        val onPrimaryColor = ContextCompat.getColor(requireContext(), R.color.theme_on_primary)
        val outlineColor = ContextCompat.getColor(requireContext(), R.color.theme_outline)

        val buttons = listOf(binding.tabWatchlist, binding.tabWatched, binding.tabFavorite)
        for (btn in buttons) {
            if (btn.id == checkedId) {
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
                btn.setTextColor(onPrimaryColor)
                btn.strokeColor = android.content.res.ColorStateList.valueOf(primaryColor)
            } else {
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(surfaceColor)
                btn.setTextColor(textSecondaryColor)
                btn.strokeColor = android.content.res.ColorStateList.valueOf(outlineColor)
            }
        }
    }

    private fun observeCollections() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collectionViewModel.uiState.collect { state ->
                    movieAdapter.submitList(state.movies)

                    binding.collectionLoading.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    binding.collectionMovies.visibility =
                        if (
                            state.isLoading ||
                            state.errorMessage != null ||
                            state.movies.isEmpty()
                        ) {
                            View.GONE
                        } else {
                            View.VISIBLE
                        }

                    binding.collectionEmpty.visibility =
                        if (
                            !state.isLoading &&
                            state.movies.isEmpty() &&
                            state.errorMessage == null
                        ) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.collectionError.visibility =
                        if (state.errorMessage != null) View.VISIBLE else View.GONE

                    binding.collectionError.text = state.errorMessage

                    binding.collectionEmpty.text = when (state.selectedType) {
                        CollectionType.WATCHLIST ->
                            getString(R.string.collection_empty_watchlist)

                        CollectionType.WATCHED ->
                            getString(R.string.collection_empty_watched)

                        CollectionType.FAVORITE ->
                            getString(R.string.collection_empty_favorite)
                    }

                    binding.collectionResultTitle.text = when (state.selectedType) {
                        CollectionType.WATCHLIST ->
                            getString(R.string.collection_watchlist)

                        CollectionType.WATCHED ->
                            getString(R.string.collection_watched)

                        CollectionType.FAVORITE ->
                            getString(R.string.collection_favorite)
                    }

                    val count = state.movies.size
                    binding.collectionResultCount.text = getString(R.string.collection_count_label, count)

                    binding.tabWatchlist.text = getString(R.string.collection_watchlist)
                    binding.tabWatched.text = getString(R.string.collection_watched)
                    binding.tabFavorite.text = getString(R.string.collection_favorite)

                    val selectedButton = when (state.selectedType) {
                        CollectionType.WATCHLIST -> R.id.tab_watchlist
                        CollectionType.WATCHED -> R.id.tab_watched
                        CollectionType.FAVORITE -> R.id.tab_favorite
                    }

                    if (binding.collectionTabs.checkedButtonId != selectedButton) {
                        binding.collectionTabs.check(selectedButton)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
        super.onDestroyView()
        _binding = null
    }
}
