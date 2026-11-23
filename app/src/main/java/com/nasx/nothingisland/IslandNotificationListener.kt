
package com.nasx.nothingisland

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf

data class IslandNotification(
    val appName: String,
    val title: String,
    val message: String,
    val packageName: String
)

object NotificationStore {

    val notifications = mutableStateListOf<IslandNotification>()

    fun add(notification: IslandNotification) {
        notifications.removeAll {
            it.packageName == notification.packageName &&
                    it.title == notification.title
        }

        notifications.add(0, notification)

        while (notifications.size > 20) {
            notifications.removeAt(notifications.lastIndex)
        }
    }

    fun remove(packageName: String, title: String) {
        notifications.removeAll {
            it.packageName == packageName &&
                    it.title == title
        }
    }

    fun clear() {
        notifications.clear()
    }
}

class IslandNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val extras = sbn.notification.extras

        val title = extras.getCharSequence(
            android.app.Notification.EXTRA_TITLE
        )?.toString().orEmpty()

        val message = extras.getCharSequence(
            android.app.Notification.EXTRA_BIG_TEXT
        )?.toString()
            ?: extras.getCharSequence(
                android.app.Notification.EXTRA_TEXT
            )?.toString().orEmpty()

        if (title.isBlank() && message.isBlank()) return

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        NotificationStore.add(
            IslandNotification(
                appName = appName,
                title = title,
                message = message,
                packageName = sbn.packageName
            )
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val title = sbn.notification.extras.getCharSequence(
            android.app.Notification.EXTRA_TITLE
        )?.toString().orEmpty()

        NotificationStore.remove(sbn.packageName, title)
    }
}
