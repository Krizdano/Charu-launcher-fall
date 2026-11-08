package com.lifeos.launcher.media

import android.service.notification.NotificationListenerService

/**
 * Intentionally empty: holding notification-listener access is what authorises
 * MediaSessionManager.getActiveSessions() for this app.
 */
class MediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        MediaRepo.get(applicationContext).refresh()
    }
}
