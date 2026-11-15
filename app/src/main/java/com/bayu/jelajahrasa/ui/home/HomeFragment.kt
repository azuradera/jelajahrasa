package com.bayu.jelajahrasa.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bayu.jelajahrasa.JelajahRasaApp
import com.bayu.jelajahrasa.MainActivity
import com.bayu.jelajahrasa.R
import com.bayu.jelajahrasa.databinding.FragmentHomeBinding
import com.bayu.jelajahrasa.model.Mood
import com.bayu.jelajahrasa.ui.movie.MovieAdapter
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels {
        val app = requireActivity().application as JelajahRasaApp
        HomeViewModelFactory(app.movieRepository)
    }

    private lateinit var popularAdapter: MovieAdapter
    private lateinit var currentYearAdapter: MovieAdapter
    private lateinit var topRatedAdapter: MovieAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)

        setupMovieLists()
        setupMoodSelection()
        setupNavigation()
        setupScrollListener()
        observeMovies()
    }

    private fun setupMovieLists() {
        val onMovieClick: (com.bayu.jelajahrasa.data.local.entity.MovieEntity) -> Unit = { movie ->
            val bundle = Bundle().apply {
                putInt("movieId", movie.movieId)
            }
            findNavController().navigate(
                R.id.action_home_to_detail,
                bundle
            )
        }

        popularAdapter = MovieAdapter(onMovieClick)
        currentYearAdapter = MovieAdapter(onMovieClick)
        topRatedAdapter = MovieAdapter(onMovieClick)

        binding.popularMovies.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = popularAdapter
        }

        binding.currentYearMovies.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = currentYearAdapter
        }

        binding.topRatedMovies.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = topRatedAdapter
        }
    }

    private fun setupScrollListener() {
        binding.root.setOnScrollChangeListener(androidx.core.widget.NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 10) {
                (requireActivity() as? MainActivity)?.setBottomNavVisibility(false)
            } else if (dy < -10 || scrollY <= 0) {
                (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
            }
        })
    }

    private fun setupMoodSelection() {
        binding.moodHappy.setOnClickListener { openMood(Mood.HAPPY) }
        binding.moodSad.setOnClickListener { openMood(Mood.SAD) }
        binding.moodEnergetic.setOnClickListener { openMood(Mood.ENERGETIC) }
        binding.moodTense.setOnClickListener { openMood(Mood.TENSE) }
        binding.moodRomantic.setOnClickListener { openMood(Mood.ROMANTIC) }
        binding.moodCurious.setOnClickListener { openMood(Mood.CURIOUS) }
    }

    private fun openMood(mood: Mood) {
        val bundle = Bundle().apply {
            putString("mood", mood.name)
        }
        findNavController().navigate(
            R.id.action_home_to_explore,
            bundle
        )
    }

    private fun setupNavigation() {
        binding.profileButton.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        binding.seeAllMovies.setOnClickListener {
            findNavController().navigate(R.id.exploreFragment)
        }
    }

    private fun observeMovies() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.uiState.collect { state ->
                    popularAdapter.submitList(state.popularMovies)
                    currentYearAdapter.submitList(state.currentYearMovies)
                    topRatedAdapter.submitList(state.topRatedMovies)

                    binding.currentYearMoviesTitle.text = getString(
                        R.string.home_section_current_year,
                        state.currentYear
                    )

                    binding.homeLoading.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    binding.homeError.text =
                        state.errorMessage ?: getString(R.string.home_movie_empty)

                    binding.homeError.visibility =
                        if (state.errorMessage != null) View.VISIBLE else View.GONE
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
