package com.bayu.jelajahrasa

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.bayu.jelajahrasa.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val mainDestinations = setOf(
        R.id.homeFragment,
        R.id.exploreFragment,
        R.id.collectionFragment,
        R.id.profileFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePreferences.applyTheme(
            ThemePreferences.getTheme(this)
        )

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController

        setupCustomBottomNav()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val show = destination.id in mainDestinations
            binding.bottomNavCard.visibility = if (show) View.VISIBLE else View.GONE
            updateBottomNavSelection(destination.id)
        }

        if (
            savedInstanceState == null &&
            FirebaseAuth.getInstance().currentUser != null
        ) {
            navController.navigate(
                R.id.homeFragment,
                null,
                navOptions {
                    popUpTo(R.id.loginFragment) {
                        inclusive = true
                    }
                }
            )
        }
    }

    private fun setupCustomBottomNav() {
        binding.navHome.setOnClickListener {
            navigateToDestination(R.id.homeFragment)
        }
        binding.navExplore.setOnClickListener {
            navigateToDestination(R.id.exploreFragment)
        }
        binding.navCollection.setOnClickListener {
            navigateToDestination(R.id.collectionFragment)
        }
        binding.navProfile.setOnClickListener {
            navigateToDestination(R.id.profileFragment)
        }
    }

    private fun navigateToDestination(destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return

        val options = navOptions {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        navController.navigate(destinationId, null, options)
    }

    private fun updateBottomNavSelection(currentId: Int) {
        val primaryColor = ContextCompat.getColor(this, R.color.theme_primary)
        val secondaryColor = ContextCompat.getColor(this, R.color.theme_text_secondary)

        val activeIndex = when (currentId) {
            R.id.homeFragment -> 0
            R.id.exploreFragment -> 1
            R.id.collectionFragment -> 2
            R.id.profileFragment -> 3
            else -> 0
        }

        val items = listOf(
            Triple(binding.navHomeIcon, binding.navHomeText, 0),
            Triple(binding.navExploreIcon, binding.navExploreText, 1),
            Triple(binding.navCollectionIcon, binding.navCollectionText, 2),
            Triple(binding.navProfileIcon, binding.navProfileText, 3)
        )

        for ((index, item) in items.withIndex()) {
            val (icon, text, _) = item
            if (index == activeIndex) {
                icon.setColorFilter(primaryColor)
                text.setTextColor(primaryColor)
            } else {
                icon.setColorFilter(secondaryColor)
                text.setTextColor(secondaryColor)
            }
        }
    }

    fun setBottomNavVisibility(visible: Boolean) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

        val isMultiSelection = when (currentFragment) {
            is com.bayu.jelajahrasa.ui.explore.ExploreFragment -> currentFragment.isMultiSelectionActive()
            is com.bayu.jelajahrasa.ui.collection.CollectionFragment -> currentFragment.isMultiSelectionActive()
            else -> false
        }

        val show = visible && !isMultiSelection && (::navController.isInitialized && navController.currentDestination?.id in mainDestinations)
        binding.bottomNavCard.visibility = if (show) View.VISIBLE else View.GONE
    }
}
