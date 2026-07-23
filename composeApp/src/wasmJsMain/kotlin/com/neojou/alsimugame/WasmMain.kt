package com.neojou.alsimugame

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/**
 * WebAssembly (Wasm) entry point.
 *
 * Creates a Compose render target via [ComposeViewport], mounts it into
 * `document.body`, and hosts the shared [App] composable.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App()
    }
}