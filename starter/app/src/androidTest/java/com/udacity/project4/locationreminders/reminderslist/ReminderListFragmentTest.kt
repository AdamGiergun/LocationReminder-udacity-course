package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.PerformException
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.agoda.kakao.common.views.KView
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private val fakeDataSource: ReminderDataSource by inject()

    private val viewModelModule = module {
        single<ReminderDataSource> { FakeDataSource() }
    }

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        loadKoinModules(viewModelModule)
    }

    @After
    fun tearDown() {
        unloadKoinModules(viewModelModule)
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
                true,
                "test1"
            ),
            ReminderDTO(
                "test2",
                "test2",
                "test2",
                180.0,
                180.0,
                false,
                "test2"
            )
        )

        list.forEach {
            fakeDataSource.saveReminder(it)
        }
        // GIVEN: List of items on the reminders screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).run {
            dataBindingIdlingResource.monitorFragment(this)
            // WHEN: Attempt to scroll to an item that contains the special text (not present in the list).
            // THEN: scrollTo will fail if no item matches.
            onView(withId(R.id.remindersRecyclerView))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("not in the list"))
                    )
                )
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
                true,
                "test1"
            ),
            ReminderDTO(
                "test2",
                "test2",
                "test2",
                180.0,
                180.0,
                false,
                "test2"
            )
        )

        list.forEach {
            fakeDataSource.saveReminder(it)
        }
        // GIVEN: List of items on the reminders screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).run {
            dataBindingIdlingResource.monitorFragment(this)
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
        }
    }

    //    TODO: add testing for the error messages.
    @Test
    fun emptyDataStore_errorSnackbarShown() = runTest(StandardTestDispatcher()) {
        // GIVEN empty datastore
        // WHEN fragment starts
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THAN snackbar is shown
        val snackbar = KView {
            withId(com.google.android.material.R.id.snackbar_text)
        }
        snackbar.isDisplayed()
    }
}