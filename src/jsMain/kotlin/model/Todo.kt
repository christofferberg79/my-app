package cberg.myapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Todo(val id: String, val description: String, val done: Boolean)

@Serializable
data class TodoDraft(val description: String, val done: Boolean = false)