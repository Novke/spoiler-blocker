package trif.novica.spoilerchecker.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import trif.novica.spoilerchecker.data.model.NotificationInfo

class SpoilerNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _activeNotifications = MutableStateFlow<List<NotificationInfo>>(emptyList())
        val activeNotifications: StateFlow<List<NotificationInfo>> = _activeNotifications.asStateFlow()

        private var instance: SpoilerNotificationListenerService? = null

        fun isPermissionGranted(context: Context): Boolean {
            val componentName = ComponentName(context, SpoilerNotificationListenerService::class.java)
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return enabledListeners?.contains(componentName.flattenToString()) == true
        }

        fun cancelNotifications(keys: List<String>) {
            instance?.let { service ->
                keys.forEach { key ->
                    try {
                        service.cancelNotification(key)
                    } catch (e: Exception) {
                        // Notification might already be dismissed
                    }
                }
                service.refreshNotifications()
            }
        }

        fun refreshActiveNotifications() {
            instance?.refreshNotifications()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        refreshNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        _activeNotifications.value = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshNotifications()
    }

    private fun refreshNotifications() {
        try {
            val notifications = activeNotifications
                .mapNotNull { sbn -> sbn.toNotificationInfo() }
                .filter { it.fullText.isNotBlank() }

            _activeNotifications.value = notifications
        } catch (e: Exception) {
            _activeNotifications.value = emptyList()
        }
    }

    private fun StatusBarNotification.toNotificationInfo(): NotificationInfo? {
        return try {
            val extras = notification.extras
            NotificationInfo(
                key = key,
                packageName = packageName,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                postTime = postTime
            )
        } catch (e: Exception) {
            null
        }
    }
}
