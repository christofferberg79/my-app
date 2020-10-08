package cberg.myapp

import kotlinx.browser.document
import kotlinx.browser.window
import react.dom.render

fun main() {
    window.onload = {
        document.getElementById("root")
            ?.let { render(it) { app() } }
            ?: throw IllegalStateException("Root element not found")
    }
}
