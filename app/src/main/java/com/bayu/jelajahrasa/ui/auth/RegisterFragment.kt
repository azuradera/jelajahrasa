package com.bayu.jelajahrasa.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.bayu.jelajahrasa.JelajahRasaApp
import com.bayu.jelajahrasa.R
import com.bayu.jelajahrasa.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        val app = requireActivity().application as JelajahRasaApp
        AuthViewModelFactory(app.authRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRegisterBinding.bind(view)

        binding.loginLink.setOnClickListener {
            findNavController().navigate(
                R.id.action_register_to_login
            )
        }

        binding.registerButton.setOnClickListener {
            register()
        }

        observeUiState()
    }

    private fun register() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()

        binding.emailLayout.error = null
        binding.passwordLayout.error = null
        binding.confirmPasswordLayout.error = null

        if (email.isBlank()) {
            binding.emailLayout.error = getString(R.string.error_empty_email)
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.error_invalid_email)
            return
        }

        if (password.isBlank()) {
            binding.passwordLayout.error = getString(R.string.error_empty_password)
            return
        }

        if (password.length < 6) {
            binding.passwordLayout.error = getString(R.string.error_short_password)
            return
        }

        if (confirmPassword != password) {
            binding.confirmPasswordLayout.error =
                getString(R.string.error_password_mismatch)
            return
        }

        hideKeyboard()
        viewModel.register(email, password)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingIndicator.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    binding.registerButton.isEnabled = !state.isLoading
                    binding.loginLink.isEnabled = !state.isLoading

                    binding.errorMessage.text = state.errorMessage
                    binding.errorMessage.visibility =
                        if (state.errorMessage != null) View.VISIBLE else View.GONE

                    if (
                        state.user != null &&
                        !state.isLoading &&
                        findNavController().currentDestination?.id == R.id.registerFragment
                    ) {
                        findNavController().navigate(
                            R.id.action_register_to_home,
                            null,
                            navOptions {
                                popUpTo(R.id.registerFragment) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        )
                    }
                }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}