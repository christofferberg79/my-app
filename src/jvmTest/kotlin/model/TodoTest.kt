package cberg.myapp.model

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

@UnstableDefault
class TodoTest {
    @Test
    fun serializeTodoDraft() {
        val todoDraft = TodoDraft("theDescription", true)
        val todoDraftJson = Json(JsonConfiguration.Default).stringify(TodoDraft.serializer(), todoDraft)
        assertEquals("""{"description":"theDescription","done":true}""", todoDraftJson)
    }

    @Test
    fun deserializeTodoDraft() {
        val todoDraft = Json(JsonConfiguration.Default).parse(
            TodoDraft.serializer(),
            """{"description":"theDescription","done":true}"""
        )
        assertEquals(TodoDraft("theDescription", true), todoDraft)
    }

    @Test
    fun serializeTodoWithId() {
        val todoWithId = TodoWithId(UUID.fromString("91eb6c98-c829-43e9-a0a4-e15d7e1a82d2"), "theDescription", true)
        val todoWithIdJson = Json(JsonConfiguration.Default).stringify(TodoWithId.serializer(), todoWithId)
        assertEquals(
            """{"id":"91eb6c98-c829-43e9-a0a4-e15d7e1a82d2","description":"theDescription","done":true}""",
            todoWithIdJson
        )
    }

    @Test
    fun deserializeTodoWithId() {
        val todoWithId = Json(JsonConfiguration.Default).parse(
            TodoWithId.serializer(),
            """{"id":"91eb6c98-c829-43e9-a0a4-e15d7e1a82d2","description":"theDescription","done":true}"""
        )
        assertEquals(
            TodoWithId(UUID.fromString("91eb6c98-c829-43e9-a0a4-e15d7e1a82d2"), "theDescription", true),
            todoWithId
        )
    }
}
