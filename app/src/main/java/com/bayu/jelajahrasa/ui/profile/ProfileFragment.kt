package com.bayu.jelajahrasa.ui.profile

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.bayu.jelajahrasa.JelajahRasaApp
import com.bayu.jelajahrasa.MainActivity
import com.bayu.jelajahrasa.R
import com.bayu.jelajahrasa.ThemePreferences
import com.bayu.jelajahrasa.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProfileBinding.bind(view)

        setupAccount()
        setupTheme()
        setupLogout()
        setupScrollListener()
    }

    private fun setupAccount() {
        val app = requireActivity().application as JelajahRasaApp

        binding.profileEmail.text =
            app.authRepository.currentUser?.email
                ?: getString(R.string.detail_unknown)
    }

    private fun setupTheme() {
        val selectedTheme = ThemePreferences.getTheme(requireContext())

        val selectedButton = when (selectedTheme) {
            ThemePreferences.LIGHT -> R.id.theme_light
            ThemePreferences.DARK -> R.id.theme_dark
            else -> R.id.theme_system
        }

        binding.themeToggleGroup.check(selectedButton)
        updateThemeToggleStyles(selectedButton)

        binding.themeToggleGroup.addOnButtonCheckedListener {
                _, checkedId, isChecked ->

            if (!isChecked) return@addOnButtonCheckedListener

            updateThemeToggleStyles(checkedId)

            val theme = when (checkedId) {
                R.id.theme_light -> ThemePreferences.LIGHT
                R.id.theme_dark -> ThemePreferences.DARK
                else -> ThemePreferences.SYSTEM
            }

            if (theme != ThemePreferences.getTheme(requireContext())) {
                ThemePreferences.saveTheme(
                    requireContext(),
                    theme
                )
            }
        }
    }

    private fun updateThemeToggleStyles(checkedId: Int) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.theme_primary)
        val surfaceColor = ContextCompat.getColor(requireContext(), R.color.theme_surface)
        val textSecondaryColor = ContextCompat.getColor(requireContext(), R.color.theme_text_secondary)
        val onPrimaryColor = ContextCompat.getColor(requireContext(), R.color.theme_on_primary)
        val outlineColor = ContextCompat.getColor(requireContext(), R.color.theme_outline)

        val buttons = listOf(binding.themeSystem, binding.themeLight, binding.themeDark)
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

    private fun setupLogout() {
        binding.logoutButton.setOnClickListener {
            showLogoutConfirmation()
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

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.profile_logout_title)
            .setMessage(R.string.profile_logout_confirmation)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_logout) { _, _ ->
                logout()
            }
            .show()
    }

    private fun logout() {
        val app = requireActivity().application as JelajahRasaApp

        app.authRepository.logout()

        findNavController().navigate(
            R.id.loginFragment,
            null,
            navOptions {
                popUpTo(R.id.nav_graph) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        )
    }

    override fun onDestroyView() {
        (requireActivity() as? MainActivity)?.setBottomNavVisibility(true)
        super.onDestroyView()
        _binding = null
    }
}
