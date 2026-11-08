package com.lifeos.launcher.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(
    val label: String,
    val pkg: String,
    val cls: String,
    val icon: ImageBitmap,
) {
    /** Stable identifier used to remember pinned apps. */
    val key: String get() = "$pkg/$cls"
}

object AppsRepo {
    fun load(c: Context): List<AppEntry> {
        val pm = c.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { ri ->
                AppEntry(
                    label = ri.loadLabel(pm).toString(),
                    pkg = ri.activityInfo.packageName,
                    cls = ri.activityInfo.name,
                    icon = ri.loadIcon(pm).toBitmap(128, 128).asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launch(c: Context, a: AppEntry) {
        val i = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(a.pkg, a.cls))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try { c.startActivity(i) } catch (_: Exception) {}
    }

    fun appInfo(c: Context, a: AppEntry) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${a.pkg}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { c.startActivity(i) } catch (_: Exception) {}
    }

    fun uninstall(c: Context, a: AppEntry) {
        val i = Intent(Intent.ACTION_DELETE, Uri.parse("package:${a.pkg}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { c.startActivity(i) } catch (_: Exception) {}
    }
}
