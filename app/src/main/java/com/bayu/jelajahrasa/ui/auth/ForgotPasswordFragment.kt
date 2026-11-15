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
import com.bayu.jelajahrasa.JelajahRasaApp
import com.bayu.jelajahrasa.R
import com.bayu.jelajahrasa.databinding.FragmentForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment(R.layout.fragment_forgot_password) {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        val app = requireActivity().application as JelajahRasaApp
        AuthViewModelFactory(app.authRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentForgotPasswordBinding.bind(view)

        binding.loginLink.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.sendButton.setOnClickListener {
            sendResetEmail()
        }

        observeUiState()
    }

    private fun sendResetEmail() {
        val email = binding.emailInput.text.toString().trim()
        binding.emailLayout.error = null

        if (email.isBlank()) {
            binding.emailLayout.error = getString(R.string.error_empty_email)
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.error_invalid_email)
            return
        }

        hideKeyboard()
        viewModel.resetPassword(email)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingIndicator.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    binding.sendButton.isEnabled = !state.isLoading
                    binding.loginLink.isEnabled = !state.isLoading

                    binding.errorMessage.text = state.errorMessage
                    binding.errorMessage.visibility =
                        if (state.errorMessage != null) View.VISIBLE else View.GONE

                    binding.successMessage.text = state.resetEmailMessage
                    binding.successMessage.visibility =
                        if (state.isPasswordResetSent && state.resetEmailMessage != null) View.VISIBLE else View.GONE

                    if (state.isPasswordResetSent) {
                        binding.emailInput.isEnabled = false
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
        viewModel.clearResetState()
        _binding = null
    }
}
