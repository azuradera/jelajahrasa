package com.bayu.jelajahrasa.ui.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bayu.jelajahrasa.JelajahRasaApp
import com.bayu.jelajahrasa.R
import com.bayu.jelajahrasa.databinding.FragmentMovieDetailBinding
import com.bayu.jelajahrasa.model.CollectionType
import com.bayu.jelajahrasa.ui.collection.CollectionViewModel
import com.bayu.jelajahrasa.ui.collection.CollectionViewModelFactory
import com.bayu.jelajahrasa.util.showAppSnackbar
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class MovieDetailFragment : Fragment(R.layout.fragment_movie_detail) {

    private var _binding: FragmentMovieDetailBinding? = null
    private val binding get() = _binding!!

    private val detailViewModel: MovieDetailViewModel by viewModels {
        val app = requireActivity().application as JelajahRasaApp
        MovieDetailViewModelFactory(app.movieRepository)
    }

    private val collectionViewModel: CollectionViewModel by viewModels {
        val app = requireActivity().application as JelajahRasaApp

        CollectionViewModelFactory(
            movieRepository = app.movieRepository,
            authRepository = app.authRepository
        )
    }

    private var currentMovieId = -1
    private var lastCheckedMovieId = -1
    private var wasProcessing = false

    private var inWatchlist = false
    private var isWatched = false
    private var isFavorite = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMovieDetailBinding.bind(view)

        currentMovieId = requireArguments().getInt("movieId", -1)

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        setupCollectionButtons()
        observeMovieDetails()
        observeCollectionOperations()

        detailViewModel.loadMovieDetails(currentMovieId)
    }

    private fun setupCollectionButtons() {
        binding.addWatchlistButton.setOnClickListener {
            toggleCollection(CollectionType.WATCHLIST, inWatchlist)
        }

        binding.addWatchedButton.setOnClickListener {
            toggleCollection(CollectionType.WATCHED, isWatched)
        }

        binding.addFavoriteButton.setOnClickListener {
            toggleCollection(CollectionType.FAVORITE, isFavorite)
        }
    }

    private fun toggleCollection(
        type: CollectionType,
        alreadyAdded: Boolean
    ) {
        val movie = detailViewModel.uiState.value.movie ?: return

        if (alreadyAdded) {
            collectionViewModel.removeFromCollection(
                movieId = movie.id,
                type = type
            )
            val msg = when (type) {
                CollectionType.WATCHLIST -> "Dihapus dari Ingin Ditonton"
                CollectionType.WATCHED -> "Dihapus dari Sudah Ditonton"
                CollectionType.FAVORITE -> "Dihapus dari Favorit"
            }
            binding.root.showAppSnackbar(msg)
        } else {
            collectionViewModel.addMovieDetailToCollection(
                movie = movie,
                type = type
            )
            val msg = when (type) {
                CollectionType.WATCHLIST -> "Berhasil ditambahkan ke Ingin Ditonton"
                CollectionType.WATCHED -> "Ditandai sebagai Sudah Ditonton"
                CollectionType.FAVORITE -> "Berhasil ditambahkan ke Favorit"
            }
            binding.root.showAppSnackbar(msg)
        }
        findNavController().navigateUp()
    }

    private fun observeMovieDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                detailViewModel.uiState.collect { state ->
                    binding.detailLoading.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    state.movie?.let { movie ->
                        binding.movieTitle.text = movie.title
                        binding.movieTagline.text = movie.tagline.orEmpty()

                        binding.movieRating.text = getString(
                            R.string.detail_rating,
                            movie.voteAverage
                        )

                        binding.movieReleaseDate.text = getString(
                            R.string.detail_release_date,
                            movie.releaseDate?.takeIf { it.isNotBlank() }
                                ?: getString(R.string.detail_unknown)
                        )

                        binding.movieRuntime.text = movie.runtime?.let {
                            getString(R.string.detail_runtime, it)
                        } ?: ""

                        binding.movieGenres.text = movie.genres.joinToString(", ") {
                            it.name
                        }

                        binding.movieOverview.text =
                            movie.overview.ifBlank {
                                getString(R.string.detail_overview_empty)
                            }

                        Glide.with(this@MovieDetailFragment)
                            .load(movie.backdropPath?.let {
                                "https://image.tmdb.org/t/p/w780$it"
                            })
                            .centerCrop()
                            .into(binding.movieBackdrop)

                        Glide.with(this@MovieDetailFragment)
                            .load(movie.posterPath?.let {
                                "https://image.tmdb.org/t/p/w500$it"
                            })
                            .centerCrop()
                            .into(binding.moviePoster)

                        if (lastCheckedMovieId != movie.id) {
                            lastCheckedMovieId = movie.id
                            refreshCollectionStatus(movie.id)
                        }
                    }

                    val collectionError =
                        collectionViewModel.uiState.value.errorMessage

                    val errorMessage = collectionError ?: state.errorMessage

                    binding.detailError.visibility =
                        if (errorMessage != null) View.VISIBLE else View.GONE

                    binding.detailError.text = errorMessage.orEmpty()
                }
            }
        }
    }

    private fun refreshCollectionStatus(movieId: Int) {
        collectionViewModel.checkCollection(
            movieId,
            CollectionType.WATCHLIST
        ) {
            inWatchlist = it
            updateCollectionButtons()
        }

        collectionViewModel.checkCollection(
            movieId,
            CollectionType.WATCHED
        ) {
            isWatched = it
            updateCollectionButtons()
        }

        collectionViewModel.checkCollection(
            movieId,
            CollectionType.FAVORITE
        ) {
            isFavorite = it
            updateCollectionButtons()
        }
    }

    private fun updateCollectionButtons() {
        val currentBinding = _binding ?: return

        currentBinding.addWatchlistButton.text = getString(
            if (inWatchlist) {
                R.string.collection_remove_watchlist
            } else {
                R.string.collection_add_watchlist
            }
        )

        currentBinding.addWatchedButton.text = getString(
            if (isWatched) {
                R.string.collection_remove_watched
            } else {
                R.string.collection_add_watched
            }
        )

        currentBinding.addFavoriteButton.text = getString(
            if (isFavorite) {
                R.string.collection_remove_favorite
            } else {
                R.string.collection_add_favorite
            }
        )
    }

    private fun observeCollectionOperations() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                collectionViewModel.uiState.collect { state ->
                    binding.addWatchlistButton.isEnabled = !state.isProcessing
                    binding.addWatchedButton.isEnabled = !state.isProcessing
                    binding.addFavoriteButton.isEnabled = !state.isProcessing

                    val detailError = detailViewModel.uiState.value.errorMessage
                    val errorMessage = state.errorMessage ?: detailError

                    binding.detailError.visibility =
                        if (errorMessage != null) View.VISIBLE else View.GONE

                    binding.detailError.text = errorMessage.orEmpty()

                    if (
                        wasProcessing &&
                        !state.isProcessing &&
                        currentMovieId > 0
                    ) {
                        refreshCollectionStatus(currentMovieId)
                    }

                    wasProcessing = state.isProcessing
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
