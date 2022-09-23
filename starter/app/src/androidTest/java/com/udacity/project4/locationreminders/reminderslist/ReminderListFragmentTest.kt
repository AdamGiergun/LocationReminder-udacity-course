package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.agoda.kakao.common.views.KView
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.utils.toDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.inject
import org.mockito.Mockito
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

    private val fakeDataSource: ReminderDataSource by inject()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        stopKoin()

        val viewModelModule = module {
            viewModel {
                RemindersListViewModel(
                    ApplicationProvider.getApplicationContext(),
                    get() as ReminderDataSource
                )
            }
            single<ReminderDataSource> { FakeDataSource() }
        }

        startKoin {
            modules(viewModelModule)
        }
    }

    // test the displayed data on the UI.
    @Test(expected = PerformException::class)
    fun itemWithText_doesNotExist() = runTest(StandardTestDispatcher()) {
        val list = listOf(
            ReminderDTO(
                "test1",
                "test1",
                "test1",
                0.0,
                0.0,
                100,
                "test_id1"
            ),
            ReminderDTO(
                "test2",
                "test2",
                "test2",
                180.0,
                180.0,
                100,
                null
            )
        )

        list.forEach {
            fakeDataSource.saveReminder(it)
        }
        // GIVEN: List of items on the reminders screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).run {
            // WHEN: Attempt to scroll to an item that contains the special text (not present in the list).
            // THEN: scrollTo will fail if no item matches.
            onView(withId(R.id.remindersRecyclerView))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("not in the list"))
                    )
                )

            close()
        }
    }

    @Test
    fun itemsFromDataSource_existsInTheRecyclerView() = runTest(StandardTestDispatcher()) {
        val list = listOf(
            ReminderDTO(
                "test1",
                "test1",
                "test1",
                0.0,
                0.0,
                100,
                "test_id1"
            ),
            ReminderDTO(
                "test2",
                "test2",
                "test2",
                180.0,
                180.0,
                100,
                null
            )
        )

        list.forEach {
            fakeDataSource.saveReminder(it)
        }
        // GIVEN: List of items on the reminders screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).run {
            // WHEN: Attempt to scroll to items that contain the special text.
            // THEN: scrollTo will succeed
            list.forEach { reminderDTO ->
                onView(withId(R.id.remindersRecyclerView)).run {
                    perform(
                        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                            hasDescendant(withText(reminderDTO.title))
                        )
                    )
                    perform(
                        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                            hasDescendant(withText(reminderDTO.description))
                        )
                    )
                    perform(
                        RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                            hasDescendant(withText(reminderDTO.location))
                        )
                    )
                }
            }

            close()
        }
    }

    //    TODO: add testing for the error messages.
    @Test
    fun emptyDataStore_errorSnackbarShown() = runTest(StandardTestDispatcher()) {
        // GIVEN empty datastore
        // WHEN fragment starts
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).run {

            // THAN snackbar is shown
            val snackbar = KView {
                withId(com.google.android.material.R.id.snackbar_text)
            }
            snackbar.isDisplayed()

            close()
        }
    }

    // test the app navigation
    /*
    If the app  navigation tests fail turn off in Developers options:
        Window animation scale
        Transition animation scale
        Animator duration scale
     */
    @Test
    fun clickFab_toNavigate() = runTest(StandardTestDispatcher()) {
        val title = "title1"
        val reminder = ReminderDataItem(
            title,
            "test1",
            "test1",
            0.0,
            0.0,
            100,
            "test_id1"
        )
        fakeDataSource.saveReminder(reminder.toDTO())

        // GIVEN: List of items on the reminders screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).run {
            val navController = mock(NavController::class.java)

            onFragment {
                Navigation.setViewNavController(it.requireView(), navController)
            }

            // WHEN: click on the add reminder FAB
            onView(withId(R.id.addReminderFAB)).perform(click())

            // THEN - Verify that we navigate to the SaveReminderFragment
            Mockito.verify(navController).navigate(
                ReminderListFragmentDirections.toEditReminder(null)
            )

            close()
        }
    }

    @Test
    fun clickListItem_toNavigate() = runTest(StandardTestDispatcher()) {
        val newTitle = "title2"
        val reminder = ReminderDataItem(
            newTitle,
            "test2",
            "test2",
            0.0,
            0.0,
            100,
            "test_id2"
        ).let { sourceData ->
            fakeDataSource.saveReminder(sourceData.toDTO())
            fakeDataSource.getReminder(sourceData.geofenceId ?: "").let { result ->
                if (result is com.udacity.project4.locationreminders.data.dto.Result.Success<ReminderDTO>) {
                    sourceData.run {
                        ReminderDataItem(
                            title,
                            description,
                            location,
                            latitude,
                            longitude,
                            radiusInMeters,
                            geofenceId,
                            result.data.id
                        )
                    }
                } else null
            }
        }

        // GIVEN: List of items on the reminders screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).run {
            val navController = mock(NavController::class.java)

            onFragment {
                Navigation.setViewNavController(it.requireView(), navController)
            }

            // WHEN: click on the list item
            onView(withText(newTitle)).perform(click())

            // THEN - Verify that we navigate to the ReminderDescriptionActivity
            Mockito.verify(navController).navigate(
                ReminderListFragmentDirections
                    .toEditReminder(reminder)
            )

            close()
        }
    }
}