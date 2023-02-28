package com.udacity.project4.authentication

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.MutableLiveData
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.udacity.project4.utils.SingleLiveEvent

class MockAuthenticationViewModelImpl: AuthenticationViewModel() {

    var signInLaunched = false

    override val showSnackBarInt: SingleLiveEvent<Int> = SingleLiveEvent()

    override val authenticationState = MutableLiveData(AuthenticationState.UNAUTHENTICATED)

    override fun launchSignIn(signInLauncher: ActivityResultLauncher<Intent>?) {
        signInLaunched = true
    }

    override fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        // implementation not needed for mock
    }
}