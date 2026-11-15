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
import com.bayu.jelajahrasa.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        val app = requireActivity().application as JelajahRasaApp
        AuthViewModelFactory(app.authRepository)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentLoginBinding.bind(view)

        binding.registerLink.setOnClickListener {
            findNavController().navigate(
                R.id.action_login_to_register
            )
        }

        binding.forgotPasswordLink.setOnClickListener {
            findNavController().navigate(
                R.id.action_login_to_forgot_password
            )
        }

        binding.loginButton.setOnClickListener {
            login()
        }

        observeUiState()
    }

    private fun login() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()

        binding.emailLayout.error = null
        binding.passwordLayout.error = null

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

        hideKeyboard()
        viewModel.login(email, password)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.loadingIndicator.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    binding.loginButton.isEnabled = !state.isLoading
                    binding.registerLink.isEnabled = !state.isLoading

                    binding.errorMessage.text = state.errorMessage
                    binding.errorMessage.visibility =
                        if (state.errorMessage != null) View.VISIBLE else View.GONE

                    if (
                        state.user != null &&
                        !state.isLoading &&
                        findNavController().currentDestination?.id == R.id.loginFragment
                    ) {
                        findNavController().navigate(
                            R.id.action_login_to_home,
                            null,
                            navOptions {
                                popUpTo(R.id.loginFragment) {
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