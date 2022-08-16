package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

    private val fakeDataSource: ReminderDataSource by inject()
//    private val remindersListViewModel: RemindersListViewModel by inject()

    private val viewModelModule = module {
//        single {
//            RemindersListViewModel(
//                get(),
//                get() as ReminderDataSource
//            )
//        }
        single<ReminderDataSource> { FakeDataSource() }
    }

    @Before
    fun setUp() {
        loadKoinModules(viewModelModule)
    }

    @After
    fun tearDown() {
        unloadKoinModules(viewModelModule)
    }

    @Test
    fun emptyDataStore_snackbarShown() = runTest(StandardTestDispatcher()) {
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val snackbar = KView {
            withId(com.google.android.material.R.id.snackbar_text)
        }
        snackbar.isDisplayed()
    }

    //    TODO: test the navigation of the fragments.
    @Test
    fun clickAddReminderFAB_navigateToSaveReminderFragment() = runTest(StandardTestDispatcher()) {
        //do it for no snackbar error info hiding button for Espresso
        fakeDataSource.saveReminder(
            ReminderDTO(
                "test1",
                "test1",
                "test1",
                0.0,
                0.0,
                true,
                "test1"
            )
        )

        // GIVEN: On the reminders screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN: click on the add reminder FAB
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - Verify that we navigate to the SaveReminderFragment
        advanceUntilIdle()
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    //    TODO: test the displayed data on the UI.
    @Test(expected = PerformException::class)
    fun itemWithText_doesNotExist() = runTest(StandardTestDispatcher()) {
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        // Attempt to scroll to an item that contains the special text.
        onView(withId(R.id.remindersRecyclerView)) // scrollTo will fail the test if no item matches.
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("not in the list"))
                )
            )
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
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        // Attempt to scroll to an item that contains the special text.
        list.forEach { reminderDTO ->
            onView(withId(R.id.remindersRecyclerView)).run {// scrollTo will fail the test if no item matches.
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


//    TODO: add testing for the error messages.
}