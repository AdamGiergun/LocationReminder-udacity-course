package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentNavTest : KoinTest {

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

    // test the app navigation
    @Test
    fun clickListItemOrFab_toNavigate() = runTest(StandardTestDispatcher()) {
        val title = "title1"
        val reminder = ReminderDataItem(
            title,
            "test1",
            "test1",
            0.0,
            0.0,
            true,
            "test1"
        )
        reminder.run {
            fakeDataSource.saveReminder(
                ReminderDTO(
                    title,
                    description,
                    location,
                    latitude,
                    longitude,
                    active,
                    id
                )
            )
        }

        // GIVEN: List of items on the reminders screen
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme).run {
            val navController = mock(NavController::class.java)

            onFragment {
                Navigation.setViewNavController(it.view!!, navController)
            }

            // WHEN: click on the list item
            onView(withText(title)).perform(click())

            // THEN - Verify that we navigate to the ReminderDescriptionActivity
            verify(navController).navigate(
                ReminderListFragmentDirections.actionReminderListFragmentToReminderDescriptionActivity(
                    reminder
                )
            )

            // WHEN: click on the add reminder FAB
            onView(withId(R.id.addReminderFAB)).perform(click())

            // THEN - Verify that we navigate to the SaveReminderFragment
            verify(navController).navigate(
                ReminderListFragmentDirections.toSaveReminder()
            )
        }
    }
}