package cberg.myapp.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TodoTest {

    @Test
    fun serializeTodoDraft() {
        val todoDraft = TodoDraft("theDescription", true)
        val todoDraftJson = Json.encodeToString(todoDraft)
        assertEquals("""{"description":"theDescription","done":true}""", todoDraftJson)
    }

    @Test
    fun deserializeTodoDraft() {
        val todoDraft = Json.decodeFromString<TodoDraft>(
            """{"description":"theDescription","done":true}"""
        )
        assertEquals(TodoDraft("theDescription", true), todoDraft)
    }

    @Test
    fun serializeTodoWithId() {
        val todoWithId = TodoWithId(UUID.fromString("91eb6c98-c829-43e9-a0a4-e15d7e1a82d2"), "theDescription", true)
        val todoWithIdJson = Json.encodeToString(todoWithId)
        assertEquals(
            """{"id":"91eb6c98-c829-43e9-a0a4-e15d7e1a82d2","description":"theDescription","done":true}""",
            todoWithIdJson
        )
    }

    @Test
    fun deserializeTodoWithId() {
        val todoWithId = Json.decodeFromString<TodoWithId>(
            """{"id":"91eb6c98-c829-43e9-a0a4-e15d7e1a82d2","description":"theDescription","done":true}"""
        )
        assertEquals(
            TodoWithId(UUID.fromString("91eb6c98-c829-43e9-a0a4-e15d7e1a82d2"), "theDescription", true),
            todoWithId
        )
    }
}
