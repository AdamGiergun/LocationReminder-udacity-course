package com.udacity.project4.authentication

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.udacity.project4.utils.SingleLiveEvent

abstract class AuthenticationViewModel: ViewModel() {

    abstract val showSnackBarInt: SingleLiveEvent<Int>

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED
    }

    abstract val authenticationState: LiveData<AuthenticationState>

    abstract fun launchSignIn(signInLauncher: ActivityResultLauncher<Intent>?)

    abstract fun onSignInResult(result: FirebaseAuthUIAuthenticationResult)
}