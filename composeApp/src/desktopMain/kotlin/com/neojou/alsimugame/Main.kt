package com.neojou.alsimugame

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Desktop entry point.
 *
 * Creates a Compose for Desktop [Window] within [application]
 * and hosts the shared [App] composable.
 */
fun main() {
    application {
        val windowState = rememberWindowState(
            size = DpSize(960.dp, 720.dp),
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "小小寨營",
            state = windowState,
        ) {
            App()
        }
    }
}
