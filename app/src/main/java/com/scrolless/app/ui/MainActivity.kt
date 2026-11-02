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
package com.scrolless.app.ui

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.scrolless.app.accessibility.ScrollessBlockAccessibilityService
import com.scrolless.app.core.model.ThemeMode
import com.scrolless.app.core.repository.UserSettingsStore
import com.scrolless.app.debug.DebugOverlayConfig
import com.scrolless.app.designsystem.theme.LocalSharedTransitionScope
import com.scrolless.app.designsystem.theme.ScrollessTheme
import com.scrolless.app.feature.home.HomeScreen
import com.scrolless.app.feature.settings.SettingsScreen
import com.scrolless.app.util.requestAppReview
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userSettingsStore: UserSettingsStore

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val isTelevision = isTelevisionDevice()

        setContent {

            val appState: ScrollessAppState = rememberScrollessAppState()
            val forceLegacyOverlay by DebugOverlayConfig.forceLegacyOverlay.collectAsStateWithLifecycle()
            val themeMode by userSettingsStore.getThemeMode().collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val dynamicColorEnabled by userSettingsStore.getDynamicColorEnabled().collectAsStateWithLifecycle(true)

            ScrollessTheme(
                darkTheme = themeMode.resolveDarkTheme(),
                dynamicColor = dynamicColorEnabled,
            ) {
                SharedTransitionLayout(modifier = Modifier.tvSafeArea(isTelevision)) {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        NavDisplay(
                            appState.backStack,
                            onBack = { appState.navigateBack() },
                            entryProvider = entryProvider {
                                entry<ScrollessRoute.Home> {
                                    HomeScreen(
                                        onNavigateToSettings = appState::navigateToSettings,
                                        accessibilityServiceClass = ScrollessBlockAccessibilityService::class.java,
                                        onRequestAppReview = ::requestAppReview,
                                        forceLegacyOverlay = forceLegacyOverlay,
                                        onForceLegacyOverlayChanged = {
                                            DebugOverlayConfig.forceLegacyOverlay.value = it
                                        },
                                    )
                                }
                                entry<ScrollessRoute.Settings> {
                                    SettingsScreen(
                                        onNavigateBack = appState::navigateBack,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** True when running on Android TV / Google TV (UI mode, with the Leanback feature as a fallback). */
private fun Context.isTelevisionDevice(): Boolean {
    val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

/**
 * Keeps content inside the TV "safe area" so nothing is cut off by TV overscan.
 * Uses the 5% margins recommended for TV layouts (48dp sides, 27dp top and bottom).
 */
@Composable
private fun Modifier.tvSafeArea(isTelevision: Boolean): Modifier = if (isTelevision) {
    // Paint the theme background first so the margins match the screens inside them.
    this
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 48.dp, vertical = 27.dp)
} else {
    this
}

/**
 * Resolves the persisted preference against the device setting.
 *
 * Must be composable because [ThemeMode.SYSTEM] reads the current configuration.
 */
@Composable
private fun ThemeMode.resolveDarkTheme(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
