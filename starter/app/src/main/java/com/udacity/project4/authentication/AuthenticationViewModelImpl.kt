package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.map
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.BuildConfig
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.udacity.project4.R
import com.udacity.project4.utils.SingleLiveEvent

private const val TAG = "AuthenticationViewModel"

class AuthenticationViewModelImpl : AuthenticationViewModel() {

    override val showSnackBarInt: SingleLiveEvent<Int> = SingleLiveEvent()

    private val currentUser = FirebaseUserLiveData()

    override val authenticationState = currentUser.map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

    // Give users the option to sign in / register with their email or Google account. If users
    // choose to register with their email, they will need to create a password as well.
    private val providers
        get() = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

    private val customAuthLayout
        get() = AuthMethodPickerLayout
            .Builder(R.layout.auth_method_picker)
            .setGoogleButtonId(R.id.google_button)
            .setEmailButtonId(R.id.email_button)
            .build()

    // Create and launch sign-in intent. We listen to the response of this activity with the
    // SIGN_IN_RESULT_CODE code.
    private val signInIntent
        get() = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAuthMethodPickerLayout(customAuthLayout)
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(!BuildConfig.DEBUG, true) //disables Smart Lock for testing purposes
            .build()

    override fun launchSignIn(signInLauncher: ActivityResultLauncher<Intent>?) {
        signInLauncher?.launch(signInIntent)
    }

    override fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == Activity.RESULT_OK) {
            Log.i(TAG, "Successfully signed in user ${userDisplayName}!")
        } else {
            Log.i(TAG, "Sign in unsuccessful ${response?.error?.errorCode}")
            // Sign in failed
            if (response == null) {
                // User pressed back button
                showSnackBarInt.value = R.string.sign_in_cancelled
                return
            }

            response.error?.let {
                if (it.errorCode == ErrorCodes.NO_NETWORK) {
                    showSnackBarInt.value = R.string.no_internet_connection
                    return
                }
            }

            showSnackBarInt.value = R.string.unknown_error
            Log.e(TAG, "Sign-in error: ", response.error)
        }
    }

    private val userDisplayName
        get() = currentUser.value.let { user ->
            val unknownUserString = " unknown user"
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
                    user?.email ?: unknownUserString
                else
                    displayName
            } ?: unknownUserString
        }
}