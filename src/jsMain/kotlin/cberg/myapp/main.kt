package cberg.myapp

import react.dom.render
import kotlin.browser.document
import kotlin.browser.window

fun main() {
    window.onload = {
        document.getElementById("root")
            ?.let { render(it) { app() } }
            ?: throw IllegalStateException("Root element not found")
    }
}
