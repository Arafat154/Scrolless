/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.feature.settings

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Receiver that lets Scrolless be an active device administrator.
 *
 * Being an active admin is what stops Android from uninstalling the app until the admin is
 * deactivated first. The policy file (`res/xml/device_admin.xml`) declares no policies, so this
 * gives Scrolless no extra control over the device, such as locking the screen or wiping data.
 */
class ScrollessDeviceAdminReceiver : DeviceAdminReceiver() {

    /** Text shown on Android's own "deactivate" confirmation screen. */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence = context.getString(R.string.device_admin_disable_warning)
}

private fun Context.deviceAdminComponent(): ComponentName = ComponentName(this, ScrollessDeviceAdminReceiver::class.java)

private fun Context.devicePolicyManager(): DevicePolicyManager? = getSystemService(DevicePolicyManager::class.java)

/** Some devices (for example many TVs) have no device admin support at all. */
internal fun Context.isDeviceAdminSupported(): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)

internal fun Context.isDeviceAdminActive(): Boolean = devicePolicyManager()?.isAdminActive(deviceAdminComponent()) == true

/** Opens Android's own screen where the user approves Scrolless as a device administrator. */
internal fun Context.createAddDeviceAdminIntent(): Intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent())
    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_explanation))
}

internal fun Context.removeDeviceAdmin() {
    val component = deviceAdminComponent()
    val manager = devicePolicyManager() ?: return
    if (manager.isAdminActive(component)) {
        manager.removeActiveAdmin(component)
    }
}
