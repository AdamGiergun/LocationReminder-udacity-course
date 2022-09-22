package com.udacity.project4

import android.Manifest
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.EditReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
import org.koin.test.get

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
//    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    val grantAccessLocationPermissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin

        val appContext: Application = getApplicationContext()

        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                EditReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(myModule)
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

//    @Before
//    fun registerIdlingResource() {
//        IdlingRegistry.getInstance().register(
//            EspressoIdlingResource.countingIdlingResource,
//            dataBindingIdlingResource
//        )
//    }
//
//    @After
//    fun unregisterIdlingResource() {
//        IdlingRegistry.getInstance().unregister(
//            EspressoIdlingResource.countingIdlingResource,
//            dataBindingIdlingResource
//        )
//    }


    @Test
    fun editReminder(): Unit = runTest {
        val reminder = ReminderDataItem(
            "title1",
            "description1",
            "location1",
            0.0,
            1.0,
            "test_id1",
            "test1"
        )
        reminder.run {
            repository.saveReminder(
                ReminderDTO(
                    title,
                    description,
                    location,
                    latitude,
                    longitude,
                    geofenceId,
                    id ?: ""
                )
            )
        }

        ActivityScenario.launch(RemindersActivity::class.java).run {
            onView(withText("title1")).perform(click())
            onView(withId(R.id.reminder_title))
                .check(ViewAssertions.matches(withText("title1")))
            onView(withId(R.id.reminder_description))
                .check(ViewAssertions.matches(withText("description1")))
            onView(withId(R.id.selected_location))
                .check(ViewAssertions.matches(withText("location1")))

            close()
        }
    }

    @Test
    fun createOneReminder_deleteReminder() = runTest {

        ActivityScenario.launch(RemindersActivity::class.java).run {

            onView(withId(R.id.addReminderFAB)).perform(click())
            onView(withId(R.id.reminder_title))
                .perform(replaceText("TITLE2"))
            onView(withId(R.id.reminder_description))
                .perform(replaceText("DESCRIPTION2"))
            onView(withId(R.id.select_location)).perform(click())
            onView(withId(R.id.map)).perform(longClick())
            onView(withText("OK")).perform(click())

            onView(withId(R.id.saveReminder)).perform(click())
            //Thread.sleep(2100)
            onView(withText("TITLE2")).perform(click())
            onView(withId(R.id.reminder_title))
                .check(ViewAssertions.matches(withText("TITLE2")))
            onView(withId(R.id.reminder_description))
                .check(ViewAssertions.matches(withText("DESCRIPTION2")))
//            Thread.sleep(3000)

//            onView(withId(R.id.menu_delete)).perform(click())
////            try {
////
////            } catch (e: NoMatchingViewException) {
////                openActionBarOverflowOrOptionsMenu(getApplicationContext())
////                onView(withText(R.string.menu_delete_task)).perform(click())
////            }
//
//            onView(withId(R.id.menu_filter)).perform(click())
//            onView(withText(R.string.nav_all)).perform(click())
//            onView(withText("TITLE2")).check(doesNotExist())

            close()
        }
    }
}