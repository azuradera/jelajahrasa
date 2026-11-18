package com.bayu.jelajahrasa.ui.explore

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
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
import com.bayu.jelajahrasa.ThemePreferences
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.bayu.jelajahrasa.ui.movie.SharedRecommendationViewModel
import com.bayu.jelajahrasa.databinding.FragmentExploreBinding
import com.bayu.jelajahrasa.model.CollectionType
import com.bayu.jelajahrasa.model.Mood
import com.bayu.jelajahrasa.model.MovieFilter
import com.bayu.jelajahrasa.ui.collection.CollectionViewModel
import com.bayu.jelajahrasa.ui.collection.CollectionViewModelFactory
import com.bayu.jelajahrasa.ui.movie.ExploreMovieAdapter
import com.bayu.jelajahrasa.ui.movie.MovieViewModel
import com.bayu.jelajahrasa.ui.movie.MovieViewModelFactory
import com.bayu.jelajahrasa.util.NoFilterArrayAdapter
import com.bayu.jelajahrasa.util.showAppSnackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val movieViewModel: MovieViewModel by viewModels {
        val app = requireActivity().application as JelajahRasaApp
        MovieViewModelFactory(app.movieRepository)
    }

    private val collectionViewModel: CollectionViewModel by viewModels {
        val app = requireActivity().application as JelajahRasaApp
        CollectionViewModelFactory(app.movieRepository, app.authRepository)
    }

    private val sharedViewModel: SharedRecommendationViewModel by activityViewModels()

    private lateinit var movieAdapter: ExploreMovieAdapter

    private var searchJob: Job? = null
    private var selectedGenreId: String? = null
    private var selectedMood: Mood? = null
    private var suppressSearchWatcher = false
    private var genreAdapterInitialized = false
    private var lastNotifiedSearchQuery: String = ""

    private val ratingOptions = listOf(
        "Semua rating",
        "0.0+",
        "1.0+",
        "2.0+",
        "3.0+",
        "4.0+",
        "5.0+",
        "6.0+",
        "7.0+",
        "8.0+",
        "9.0+"
    )

    private val ratingMinValues = listOf(
        null,
        0.0,
        1.0,
        2.0,
        3.0,
        4.0,
        5.0,
        6.0,
        7.0,
        8.0,
        9.0
    )

    private val ratingMaxValues = listOf(
        null,
        0.99,
        1.99,
        2.99,
        3.99,
        4.99,
        5.99,
        6.99,
        7.99,
        8.99,
        9.99
    )

    private val sortOptions = listOf(
        "Terpopuler",
        "Rating tertinggi",
        "Terbaru",
        "Rating terendah"
    )

    private val sortValues = listOf(
        "popularity.desc",
        "vote_average.desc",
        "primary_release_date.desc",
        "vote_average.asc"
    )

    private val countryOptions = listOf(
        "Semua negara",
        "Amerika Serikat",
        "Indonesia",
        "Korea Selatan",
        "Jepang",
        "Inggris",
        "Prancis",
        "India",
        "Tiongkok",
        "Thailand"
    )

    private val countryValues = listOf(
        null,
        "US",
        "ID",
        "KR",
        "JP",
        "GB",
        "FR",
        "IN",
        "CN",
        "TH"
    )

    private val ratingAdapter by lazy {
        NoFilterArrayAdapter(requireContext(), R.layout.item_dropdown, ratingOptions)
    }

    private val sortAdapter by lazy {
        NoFilterArrayAdapter(requireContext(), R.layout.item_dropdown, sortOptions)
    }

    private val countryAdapter by lazy {
        NoFilterArrayAdapter(requireContext(), R.layout.item_dropdown, countryOptions)
    }

    private var genreAdapter: NoFilterArrayAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentExploreBinding.bind(view)

        setupMovieList()
        setupDropdowns()
        setupSearch()
        setupFilters()
        setupMoodActionButtons()
        setupRecommendationModeToggle()
        observeMovies()

        movieViewModel.loadGenres()

        val modeArg = arguments?.getString("recommendationMode")
        if (modeArg != null) {
            sharedViewModel.setMode(modeArg)
        }
        movieViewModel.setRecommendationMode(sharedViewModel.recommendationMode.value)

        val moodName = arguments?.getString("mood")
        val mood = moodName?.let {
            runCatching { Mood.valueOf(it) }.getOrNull()
        }

        if (mood != null) {
            selectedMood = mood
            updateMoodFilterVisibility(mood)
            movieViewModel.recommendByMood(mood, isNewSession = true)
        } else if (movieViewModel.uiState.value.selectedMood != null) {
            val currentMood = movieViewModel.uiState.value.selectedMood!!
            selectedMood = currentMood
            updateMoodFilterVisibility(currentMood)
        } else {
            selectedMood = null
            updateMoodFilterVisibility(null)
            val currentState = movieViewModel.uiState.value
            if (currentState.movies.isEmpty() && currentState.searchQuery.isBlank()) {
                movieViewModel.clearFilters()
            }
        }

        arguments?.remove("mood")
        arguments?.remove("recommendationMode")
    }

    private fun setupRecommendationModeToggle() {
        val currentMode = sharedViewModel.recommendationMode.value
        updateToggleStyles(currentMode)

        binding.btnFast.setOnClickListener {
            sharedViewModel.setMode(ThemePreferences.MODE_FAST)
            movieViewModel.setRecommendationMode(ThemePreferences.MODE_FAST)
            updateToggleStyles(ThemePreferences.MODE_FAST)
        }

        binding.btnAccurate.setOnClickListener {
            sharedViewModel.setMode(ThemePreferences.MODE_ACCURATE)
            movieViewModel.setRecommendationMode(ThemePreferences.MODE_ACCURATE)
            updateToggleStyles(ThemePreferences.MODE_ACCURATE)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.recommendationMode.collect { mode ->
                    movieViewModel.setRecommendationMode(mode)
                    updateToggleStyles(mode)
                }
            }
        }
    }

    private fun updateToggleStyles(mode: String?) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.theme_primary)
        val surfaceColor = ContextCompat.getColor(requireContext(), R.color.theme_surface)
        val textSecondaryColor = ContextCompat.getColor(requireContext(), R.color.theme_text_secondary)
        val onPrimaryColor = ContextCompat.getColor(requireContext(), R.color.theme_on_primary)
        val outlineColor = ContextCompat.getColor(requireContext(), R.color.theme_outline)

        if (mode == ThemePreferences.MODE_FAST) {
            binding.btnFast.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            binding.btnFast.setTextColor(onPrimaryColor)
            binding.btnFast.strokeColor = android.content.res.ColorStateList.valueOf(primaryColor)

            binding.btnAccurate.backgroundTintList = android.content.res.ColorStateList.valueOf(surfaceColor)
            binding.btnAccurate.setTextColor(textSecondaryColor)
            binding.btnAccurate.strokeColor = android.content.res.ColorStateList.valueOf(outlineColor)
        } else {
            binding.btnAccurate.backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            binding.btnAccurate.setTextColor(onPrimaryColor)
            binding.btnAccurate.strokeColor = android.content.res.ColorStateList.valueOf(primaryColor)

            binding.btnFast.backgroundTintList = android.content.res.ColorStateList.valueOf(surfaceColor)
            binding.btnFast.setTextColor(textSecondaryColor)
            binding.btnFast.strokeColor = android.content.res.ColorStateList.valueOf(outlineColor)
        }
    }

    private fun setupMoodActionButtons() {
        binding.moreRecommendationsButton.setOnClickListener {
            if (!movieViewModel.uiState.value.isLoading) {
                movieViewModel.loadMoreMoodRecommendations()
                binding.exploreMovies.scrollToPosition(0)
            }
        }

        binding.changeMoodButton.setOnClickListener {
            showChangeMoodDialog()
        }
    }

    private fun showChangeMoodDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_mood, null)
        val container = dialogView.findViewById<android.widget.LinearLayout>(R.id.mood_container)

        val moods = listOf(
            Triple(Mood.HAPPY, "Bahagia", R.drawable.ic_mood_happy),
            Triple(Mood.SAD, "Sedih", R.drawable.ic_mood_sad),
            Triple(Mood.ENERGETIC, "Bersemangat", R.drawable.ic_mood_energetic),
            Triple(Mood.TENSE, "Tegang", R.drawable.ic_mood_tense),
            Triple(Mood.ROMANTIC, "Romantis", R.drawable.ic_mood_romantic),
            Triple(Mood.CURIOUS, "Penasaran", R.drawable.ic_mood_curious)
        )

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        for ((mood, title, iconRes) in moods) {
            val rowView = layoutInflater.inflate(R.layout.item_dialog_mood, container, false)
            val iconView = rowView.findViewById<android.widget.ImageView>(R.id.mood_icon)
            val textView = rowView.findViewById<android.widget.TextView>(R.id.mood_text)

            iconView.setImageResource(iconRes)
            textView.text = title

            val color = ContextCompat.getColor(requireContext(), mood.colorResId)
            iconView.setColorFilter(color)
            textView.setTextColor(color)

            rowView.setOnClickListener {
                dialog.dismiss()
                selectedMood = mood
                updateMoodFilterVisibility(mood)
                movieViewModel.recommendByMood(mood, isNewSession = true)
                binding.exploreMovies.scrollToPosition(0)
            }

            container.addView(rowView)
        }

        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_card))
        dialog.show()
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
                    R.id.action_explore_to_detail,
                    bundle
                )
            },
            onSelectionChanged = { count ->
                if (count > 0) {
                    binding.selectionActionBar.visibility = View.VISIBLE
                    binding.selectionCountText.text = getString(R.string.selection_count, count)
                    (requireActivity() as? MainActivity)?.setBottomNavVisibility(false)
                } else {
                    binding.selectionActionBar.visibility = View.GONE
                    (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
                }
            }
        )

        binding.batchAddWatchlistButton.setOnClickListener {
            val selectedIds = movieAdapter.selectedMovieIds
            val currentList = movieAdapter.currentList
            val selectedMovies = currentList.filter { selectedIds.contains(it.movieId) }

            if (selectedMovies.isNotEmpty()) {
                collectionViewModel.addMoviesToCollection(selectedMovies, CollectionType.WATCHLIST)
                movieAdapter.clearSelection()
                binding.selectionActionBar.visibility = View.GONE
                (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
                binding.root.showAppSnackbar("Berhasil ditambahkan ke Ingin Ditonton")
            }
        }

        binding.batchCancelButton.setOnClickListener {
            movieAdapter.clearSelection()
            binding.selectionActionBar.visibility = View.GONE
            (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
        }

        binding.scrollToTopButton.setOnClickListener {
            binding.exploreMovies.smoothScrollToPosition(0)
            binding.exploreAppBar.setExpanded(true, true)
            binding.scrollToTopButton.visibility = View.GONE
        }

        binding.exploreMovies.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = movieAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
                    super.onScrolled(recyclerView, dx, dy)

                    val canScrollUp = recyclerView.canScrollVertically(-1)
                    if (canScrollUp) {
                        binding.scrollToTopButton.visibility = View.VISIBLE
                    } else {
                        binding.scrollToTopButton.visibility = View.GONE
                        binding.exploreAppBar.setExpanded(true, true)
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

                    if (dy > 0 && !recyclerView.canScrollVertically(1)) {
                        movieViewModel.loadNextPage()
                    }
                }
            })
        }
    }

    private fun setupDropdowns() {
        binding.ratingFilter.setAdapter(ratingAdapter)
        binding.sortFilter.setAdapter(sortAdapter)
        binding.countryFilter.setAdapter(countryAdapter)

        if (binding.ratingFilter.text.isNullOrBlank()) {
            binding.ratingFilter.setText(ratingOptions.first(), false)
        }
        if (binding.sortFilter.text.isNullOrBlank()) {
            binding.sortFilter.setText(sortOptions.first(), false)
        }
        if (binding.countryFilter.text.isNullOrBlank()) {
            binding.countryFilter.setText(countryOptions.first(), false)
        }

        binding.genreFilter.setOnClickListener {
            binding.genreFilter.showDropDown()
        }

        binding.ratingFilter.setOnClickListener {
            binding.ratingFilter.showDropDown()
        }

        binding.sortFilter.setOnClickListener {
            binding.sortFilter.showDropDown()
        }

        binding.countryFilter.setOnClickListener {
            binding.countryFilter.showDropDown()
        }
    }

    private fun setupSearch() {
        binding.searchInput.doAfterTextChanged { editable ->
            if (suppressSearchWatcher) return@doAfterTextChanged

            val query = editable?.toString().orEmpty().trim()
            if (query.isBlank() && movieViewModel.uiState.value.searchQuery.isNotBlank()) {
                selectedMood = null
                updateMoodFilterVisibility(null)
                movieViewModel.clearFilters()
            }
        }

        binding.searchInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                (requireActivity() as? MainActivity)?.setBottomNavVisibility(false)
            }
        }

        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                searchJob?.cancel()

                val query = binding.searchInput.text
                    ?.toString()
                    .orEmpty()
                    .trim()

                if (query.isNotEmpty()) {
                    selectedMood = null
                    updateMoodFilterVisibility(null)
                    movieViewModel.searchMovies(query)
                }

                true
            } else {
                false
            }
        }
    }

    private fun setupFilters() {
        binding.applyFilterButton.setOnClickListener {
            hideKeyboard()
            applySelectedFilters()
        }

        binding.resetFilterButton.setOnClickListener {
            hideKeyboard()
            resetFilters()
        }

        binding.yearFilter.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_NEXT
            ) {
                hideKeyboard()
                applySelectedFilters()
                true
            } else {
                false
            }
        }
    }

    private fun updateMoodFilterVisibility(mood: Mood?) {
        val hasMood = mood != null
        binding.manualFilterSection.visibility = if (hasMood) View.GONE else View.VISIBLE
        binding.genreFilterLayout.visibility = if (mood == null) View.VISIBLE else View.GONE
        binding.selectedMoodLabel.visibility = if (hasMood) View.VISIBLE else View.GONE
        binding.recommendationModeLayout.visibility = if (hasMood) View.VISIBLE else View.GONE
        binding.moreRecommendationsButton.visibility = if (hasMood) View.VISIBLE else View.GONE
        binding.changeMoodButton.visibility = if (hasMood) View.VISIBLE else View.GONE

        if (mood != null) {
            binding.selectedMoodLabel.text = getString(
                R.string.explore_mood_label,
                getString(mood.titleResId)
            )
        }
    }

    private fun applySelectedFilters() {
        searchJob?.cancel()

        val yearText = binding.yearFilter.text
            ?.toString()
            .orEmpty()
            .trim()

        val year = yearText.toIntOrNull()

        if (yearText.isNotEmpty() && (year == null || year !in 1878..2100)) {
            binding.yearFilter.error = getString(R.string.explore_invalid_year)
            return
        }

        binding.yearFilter.error = null

        val ratingIndex = ratingOptions.indexOf(
            binding.ratingFilter.text.toString()
        ).coerceAtLeast(0)

        val sortIndex = sortOptions.indexOf(
            binding.sortFilter.text.toString()
        ).coerceAtLeast(0)

        val countryIndex = countryOptions.indexOf(
            binding.countryFilter.text.toString()
        ).coerceAtLeast(0)

        val filter = MovieFilter(
            genreIds = if (selectedMood == null) selectedGenreId else selectedMood?.getGenreIds(),
            year = year,
            minimumRating = ratingMinValues[ratingIndex],
            maximumRating = ratingMaxValues[ratingIndex],
            originCountry = countryValues[countryIndex],
            sortBy = sortValues[sortIndex]
        )

        suppressSearchWatcher = true
        binding.searchInput.setText("")
        suppressSearchWatcher = false

        movieViewModel.applyFilters(filter)
        binding.exploreMovies.scrollToPosition(0)
    }

    private fun resetFilters() {
        searchJob?.cancel()

        selectedGenreId = null
        selectedMood = null

        updateMoodFilterVisibility(null)

        if (genreAdapterInitialized) {
            binding.genreFilter.setText(
                getString(R.string.explore_all_genres),
                false
            )
        }

        binding.yearFilter.text?.clear()
        binding.yearFilter.error = null

        binding.ratingFilter.setText(ratingOptions.first(), false)
        binding.sortFilter.setText(sortOptions.first(), false)
        binding.countryFilter.setText(countryOptions.first(), false)

        suppressSearchWatcher = true
        binding.searchInput.setText("")
        suppressSearchWatcher = false

        movieViewModel.clearFilters()
        binding.exploreMovies.scrollToPosition(0)
    }

    private fun observeMovies() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                movieViewModel.uiState.collect { state ->
                    movieAdapter.submitList(state.movies)

                    binding.exploreLoading.visibility =
                        if (state.isLoading || state.isLoadingMore) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.exploreError.visibility =
                        if (
                            state.errorMessage != null ||
                            (!state.isLoading && state.movies.isEmpty())
                        ) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }

                    binding.exploreError.text = state.errorMessage
                        ?: getString(R.string.explore_empty_result)

                    binding.exploreResultTitle.text = getString(
                        R.string.explore_result_count,
                        state.totalResults
                    )

                    if (state.searchQuery.isNotBlank() && state.searchQuery != lastNotifiedSearchQuery && !state.isLoading) {
                        lastNotifiedSearchQuery = state.searchQuery
                        binding.root.showAppSnackbar("Ditemukan ${state.totalResults} film untuk \"${state.searchQuery}\"")
                    } else if (state.searchQuery.isBlank()) {
                        lastNotifiedSearchQuery = ""
                    }

                    if (binding.searchInput.text.toString() != state.searchQuery) {
                        suppressSearchWatcher = true
                        binding.searchInput.setText(state.searchQuery)
                        suppressSearchWatcher = false
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                movieViewModel.uiState.collect { state ->
                    if (
                        state.genres.isNotEmpty() &&
                        !genreAdapterInitialized
                    ) {
                        val allGenres = listOf(
                            getString(R.string.explore_all_genres)
                        ) + state.genres.map { it.name }

                        genreAdapter = NoFilterArrayAdapter(
                            requireContext(),
                            R.layout.item_dropdown,
                            allGenres
                        )
                        binding.genreFilter.setAdapter(genreAdapter)

                        if (binding.genreFilter.text.isNullOrBlank()) {
                            binding.genreFilter.setText(
                                getString(R.string.explore_all_genres),
                                false
                            )
                        }

                        binding.genreFilter.setOnItemClickListener {
                                _, _, position, _ ->

                            selectedGenreId = if (position == 0) {
                                null
                            } else {
                                state.genres.getOrNull(position - 1)
                                    ?.id
                                    ?.toString()
                            }
                        }

                        genreAdapterInitialized = true
                    }
                }
            }
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireContext()
            .getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager

        inputMethodManager.hideSoftInputFromWindow(
            binding.root.windowToken,
            0
        )
    }

    override fun onDestroyView() {
        (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
        searchJob?.cancel()
        genreAdapterInitialized = false
        _binding = null
        super.onDestroyView()
    }
}
