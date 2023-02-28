package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */

class AuthenticationActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthenticationViewModel

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result ->
        authViewModel.onSignInResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authViewModel = getViewModel()

        ActivityAuthenticationBinding.inflate(layoutInflater).let { binding ->
            setContentView(binding.root)

            binding.loginButton.setOnClickListener {
                authViewModel.launchSignIn(signInLauncher)
            }

            authViewModel.authenticationState.observe(this) { authenticationState ->
                when (authenticationState) {
                    AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                        startRemindersActivity()
                    }
                    AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED -> {
                        binding.authText.text =
                            getString(R.string.unauthenticated)
                    }
                    else -> {
                        binding.authText.text =
                            getString(R.string.authentication_failed)
                    }
                }
            }

            authViewModel.showSnackBarInt.observe(this) {
                Snackbar.make(
                    binding.root,
                    it,
                    Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        }
    }

    private fun startRemindersActivity() {
        Intent(this, RemindersActivity::class.java).run {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(this)
            finish()
        }
    }
}