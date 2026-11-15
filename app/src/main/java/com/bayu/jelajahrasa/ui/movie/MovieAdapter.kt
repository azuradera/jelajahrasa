package com.bayu.jelajahrasa.ui.movie

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bayu.jelajahrasa.data.local.entity.MovieEntity
import com.bayu.jelajahrasa.databinding.ItemMovieBinding
import com.bumptech.glide.Glide

class MovieAdapter(
    private val onMovieClick: (MovieEntity) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    private val movies = mutableListOf<MovieEntity>()

    fun submitList(newMovies: List<MovieEntity>) {
        movies.clear()
        movies.addAll(newMovies)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MovieViewHolder,
        position: Int
    ) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    inner class MovieViewHolder(
        private val binding: ItemMovieBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: MovieEntity) {
            binding.movieTitle.text = movie.title
            binding.movieRating.text = "★ ${String.format("%.1f", movie.voteAverage)}"

            val posterUrl = movie.posterPath?.let {
                "https://image.tmdb.org/t/p/w500$it"
            }

            Glide.with(binding.moviePoster)
                .load(posterUrl)
                .centerCrop()
                .into(binding.moviePoster)

            binding.root.setOnClickListener {
                onMovieClick(movie)
            }
        }
    }
}