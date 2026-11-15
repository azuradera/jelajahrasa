package com.bayu.jelajahrasa.ui.movie

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bayu.jelajahrasa.R
import com.bayu.jelajahrasa.data.local.entity.MovieEntity
import com.bayu.jelajahrasa.databinding.ItemMovieGridBinding
import com.bumptech.glide.Glide

class ExploreMovieAdapter(
    private val onMovieClick: (MovieEntity) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = {}
) : ListAdapter<MovieEntity, ExploreMovieAdapter.MovieViewHolder>(MovieDiffCallback()) {

    val selectedMovieIds = mutableSetOf<Int>()
    var isSelectionMode = false

    fun clearSelection() {
        selectedMovieIds.clear()
        isSelectionMode = false
        onSelectionChanged(0)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MovieViewHolder(
        private val binding: ItemMovieGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: MovieEntity) {
            binding.movieTitle.text = movie.title
            binding.movieRating.text =
                "★ ${String.format(java.util.Locale.getDefault(), "%.1f", movie.voteAverage)}"

            val posterUrl = movie.posterPath?.let {
                "https://image.tmdb.org/t/p/w500$it"
            }

            Glide.with(binding.moviePoster)
                .load(posterUrl)
                .centerCrop()
                .into(binding.moviePoster)

            val isSelected = selectedMovieIds.contains(movie.movieId)

            if (isSelected) {
                binding.root.strokeColor = binding.root.context.getColor(R.color.theme_primary)
                binding.root.strokeWidth = 4
                binding.root.alpha = 0.8f
            } else {
                binding.root.strokeColor = binding.root.context.getColor(R.color.theme_outline)
                binding.root.strokeWidth = 1
                binding.root.alpha = 1.0f
            }

            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(movie.movieId)
                } else {
                    onMovieClick(movie)
                }
            }

            binding.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    toggleSelection(movie.movieId)
                } else {
                    toggleSelection(movie.movieId)
                }
                true
            }
        }

        private fun toggleSelection(movieId: Int) {
            if (selectedMovieIds.contains(movieId)) {
                selectedMovieIds.remove(movieId)
                if (selectedMovieIds.isEmpty()) {
                    isSelectionMode = false
                }
            } else {
                selectedMovieIds.add(movieId)
                isSelectionMode = true
            }
            onSelectionChanged(selectedMovieIds.size)
            notifyDataSetChanged()
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<MovieEntity>() {
        override fun areItemsTheSame(
            oldItem: MovieEntity,
            newItem: MovieEntity
        ): Boolean = oldItem.movieId == newItem.movieId

        override fun areContentsTheSame(
            oldItem: MovieEntity,
            newItem: MovieEntity
        ): Boolean = oldItem == newItem
    }
}
