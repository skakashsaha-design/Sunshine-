package com.example.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.SkyPulseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WeatherSyncWorker.schedule(context)
                val app = context.applicationContext as? SkyPulseApp
                val ns = app?.container?.preferencesManager?.preferencesFlow?.first()?.notificationSettings
                if (ns != null) {
                    MorningBriefingWorker.scheduleDaily(
                        context = context,
                        enabled = ns.enabled && ns.dailySummaryEnabled,
                        hour = ns.dailyHour,
                        minute = ns.dailyMinute,
                        replaceExisting = true
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
