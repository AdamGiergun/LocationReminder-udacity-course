package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.serializable

/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        //        receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem) =
            Intent(context, ReminderDescriptionActivity::class.java).apply {
                putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityReminderDescriptionBinding.inflate(layoutInflater).run {
            (intent.serializable(EXTRA_ReminderDataItem) as ReminderDataItem?)?.let {
                reminderDataItem = it
            }
            setContentView(root)
        }
    }
}
