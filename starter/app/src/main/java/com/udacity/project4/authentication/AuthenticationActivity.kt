package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.*
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
private const val TAG = "AuthenticationActivity"

class AuthenticationActivity : AppCompatActivity() {

    private val viewModel: AuthenticationViewModel by viewModels()
    private val binding: ActivityAuthenticationBinding by lazy {
        ActivityAuthenticationBinding.inflate(layoutInflater)
    }

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result ->
        this.onSignInResult(result)
    }

    private val customAuthLayout = AuthMethodPickerLayout
        .Builder(R.layout.auth_method_picker)
        .setGoogleButtonId(R.id.google_button)
        .setEmailButtonId(R.id.email_button)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            launchSignInFlow()
        }

        if (FirebaseAuth.getInstance().currentUser != null) {
            startRemindersActivity()
        }
    }

    private fun startRemindersActivity() {
        Intent(this, RemindersActivity::class.java).run {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(this)
            finish()
        }
    }

    private fun launchSignInFlow() {

        // Give users the option to sign in / register with their email or Google account. If users
        // choose to register with their email, they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent. We listen to the response of this activity with the
        // SIGN_IN_RESULT_CODE code.
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setIsSmartLockEnabled(!BuildConfig.DEBUG, true) //temporary for testing
            .setAuthMethodPickerLayout(customAuthLayout)
            .setAvailableProviders(providers)
            .build()
        signInLauncher.launch(signInIntent)

        viewModel.authenticationState.observe(this) { authenticationState ->
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
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "Successfully signed in user ${getDisplayName()}!")
        } else {
            Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            // Sign in failed
            if (response == null) {
                // User pressed back button
                showSnackbar(R.string.sign_in_cancelled)
                return
            }

            response.error?.let {
                if (it.errorCode == ErrorCodes.NO_NETWORK) {
                    showSnackbar(R.string.no_internet_connection)
                    return
                }
            }

            showSnackbar(R.string.unknown_error)
            Log.e(TAG, "Sign-in error: ", response.error)
        }
    }

    private fun showSnackbar(stringId: Int) {
        Snackbar.make(
            binding.root,
            stringId,
            Snackbar.LENGTH_INDEFINITE
        ).show()
    }

    private fun getDisplayName(): String {
        return FirebaseAuth.getInstance().currentUser.let { user ->
            user?.displayName.let {
                var displayName = it
                if (displayName == null || displayName == "") {
                    val userInfoList = user?.providerData?.iterator()
                    userInfoList?.forEach uil@{ userInfo ->
                        displayName = userInfo.displayName
                        if (displayName != null) return@uil
                    }
                }
                if (displayName == null || displayName == "")
                    user?.email ?: getString(R.string.unknown_user)
                else
                    displayName
            } ?: getString(R.string.unknown_user)
        }
    }
}