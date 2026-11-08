package com.lifeos.launcher.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val playing: Boolean,
    val durationMs: Long,
    val basePosMs: Long,
    val baseTime: Long,
    val speed: Float,
    val pkg: String,
    val appLabel: String,
) {
    fun position(now: Long = SystemClock.elapsedRealtime()): Long {
        val p = if (playing) basePosMs + ((now - baseTime) * speed).toLong() else basePosMs
        return if (durationMs > 0) p.coerceIn(0L, durationMs) else p.coerceAtLeast(0L)
    }
}

/**
 * Reads the active media session (Spotify, YouTube Music, etc.) and forwards transport controls.
 * Requires the user to grant "Notification access" to this app once.
 */
class MediaRepo private constructor(private val app: Context) {
    private val msm = app.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    private val component = ComponentName(app, MediaListenerService::class.java)

    private val _state = MutableStateFlow<NowPlaying?>(null)
    val state: StateFlow<NowPlaying?> = _state.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private var controller: MediaController? = null
    private var registered = false

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onSessionDestroyed() = refresh()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pick(list)
    }

    fun refresh() {
        val ok = NotificationManagerCompat.getEnabledListenerPackages(app).contains(app.packageName)
        _hasAccess.value = ok
        if (!ok) {
            detach()
            _state.value = null
            return
        }
        try {
            if (!registered) {
                msm.addOnActiveSessionsChangedListener(sessionsListener, component)
                registered = true
            }
            pick(msm.getActiveSessions(component))
        } catch (_: SecurityException) {
            _hasAccess.value = false
        }
    }

    private fun detach() {
        controller?.unregisterCallback(callback)
        controller = null
        if (registered) {
            try { msm.removeOnActiveSessionsChangedListener(sessionsListener) } catch (_: Exception) {}
            registered = false
        }
    }

    private fun pick(list: List<MediaController>?) {
        val best = list?.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list?.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        if (c == null) {
            _state.value = null
            return
        }
        val m = c.metadata
        val ps = c.playbackState
        if (m == null) {
            _state.value = null
            return
        }
        val title = m.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        if (title.isNullOrBlank()) {
            _state.value = null
            return
        }
        val artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: m.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val art = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: m.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val pm = app.packageManager
        val label = try {
            pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString()
        } catch (_: Exception) {
            c.packageName
        }
        _state.value = NowPlaying(
            title = title,
            artist = artist,
            art = art,
            playing = ps?.state == PlaybackState.STATE_PLAYING,
            durationMs = m.getLong(MediaMetadata.METADATA_KEY_DURATION),
            basePosMs = ps?.position ?: 0L,
            baseTime = ps?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
            speed = ps?.playbackSpeed ?: 1f,
            pkg = c.packageName,
            appLabel = label,
        )
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }

    fun seekBy(deltaMs: Long) {
        val c = controller ?: return
        val pos = _state.value?.position() ?: return
        c.transportControls.seekTo((pos + deltaMs).coerceAtLeast(0L))
    }

    companion object {
        @Volatile private var inst: MediaRepo? = null
        fun get(context: Context): MediaRepo = inst ?: synchronized(this) {
            inst ?: MediaRepo(context.applicationContext).also { inst = it }
        }
    }
}
