package com.udacity.project4.authentication

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.EditReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
@LargeTest
class AuthenticationActivityTest: AutoCloseKoinTest() {

    private val dataBindingIdlingResource = DataBindingIdlingResource()
    private val mockAuthenticationViewModelImpl = MockAuthenticationViewModelImpl()
    private val fakeDataSource: ReminderDataSource by inject()

    @Before
    fun init() {
        stopKoin()//stop the original app koin

        val appContext: Application = ApplicationProvider.getApplicationContext()

        val myModule = module {
            viewModel<AuthenticationViewModel> {
                mockAuthenticationViewModelImpl
            }

            viewModel {
                RemindersListViewModel(
                    fakeDataSource
                )
            }

            single {
                EditReminderViewModel(
                    fakeDataSource
                )
            }

            single<ReminderDataSource> { RemindersLocalRepository(get()) }

            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(myModule)
        }
    }

    @Test
    fun unauthenticatedUserCanLaunchSignIn() {
        ActivityScenario.launch(AuthenticationActivity::class.java).run {
            // GIVEN - user is unauthenticated
            mockAuthenticationViewModelImpl.authenticationState.postValue(AuthenticationViewModel.AuthenticationState.UNAUTHENTICATED)
            dataBindingIdlingResource.monitorActivity(this)

            // WHEN - users clicks login button
            onView(withId(R.id.login_button)).perform(click())
            
            // THEN - sign in is launched from vieModel
            assert(mockAuthenticationViewModelImpl.signInLaunched)
        }
    }

    @Test
    fun authenticatedUserSeesEmptyRemindersList() {
        ActivityScenario.launch(AuthenticationActivity::class.java).run {
            // GIVEN - user is authenticated
            mockAuthenticationViewModelImpl.authenticationState.postValue(AuthenticationViewModel.AuthenticationState.AUTHENTICATED)

            // WHEN - there are no reminders in db
            // THEN - user sees reminders activity with empty list
            onView(withId(R.id.noDataTextView)).check(matches(withText("No Data")))
        }
    }
}