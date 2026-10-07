package com.handoff.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.handoff.app.core.model.Block
import com.handoff.app.core.model.Conversation
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Platform
import com.handoff.app.core.model.Role
import com.handoff.app.data.db.HandoffDatabase
import com.handoff.app.data.repo.HandoffRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HandoffRepositoryTest {

    private lateinit var db: HandoffDatabase
    private lateinit var repository: HandoffRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HandoffDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = HandoffRepository(db.handoffDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun conversation(title: String = "Test chat") = Conversation(
        platform = Platform.CLAUDE,
        title = title,
        url = "https://claude.ai/share/abc",
        messages = listOf(
            Message(Role.USER, listOf(Block.Text("Question one"))),
            Message(Role.ASSISTANT, listOf(Block.Text("Answer one"), Block.Code("kotlin", "val x = 1"))),
        ),
    )

    @Test
    fun saveThenGet_roundTripsConversation() = runTest {
        val id = repository.save(conversation(), tokenEstimate = 123)
        val loaded = repository.get(id)
        assertNotNull(loaded)
        assertEquals("Test chat", loaded!!.title)
        assertEquals(2, loaded.messages.size)
        assertEquals(Platform.CLAUDE, loaded.platform)
        val code = loaded.messages[1].blocks[1] as Block.Code
        assertEquals("kotlin", code.language)
    }

    @Test
    fun update_persistsExclusions() = runTest {
        val id = repository.save(conversation(), 100)
        val loaded = repository.get(id)!!
        val excluded = loaded.copy(messages = loaded.messages.mapIndexed { i, m ->
            m.copy(excluded = i == 0)
        })
        repository.update(id, excluded, 60)
        val reloaded = repository.get(id)!!
        assertTrue(reloaded.messages.first().excluded)
    }

    @Test
    fun deleteAndClear_work() = runTest {
        val id1 = repository.save(conversation("First"), 10)
        val id2 = repository.save(conversation("Second"), 10)
        repository.delete(id1)
        assertNull(repository.get(id1))
        // Undo: restore the deleted entry with its conversation.
        repository.restore(
            com.handoff.app.data.repo.HandoffEntry(
                id = id1, title = "First", platform = Platform.CLAUDE,
                url = "", createdAt = 1, messageCount = 2, tokenEstimate = 10,
            ),
            conversation("First"),
        )
        assertEquals(2, repository.observeHistory().first().size)
        repository.clear()
        assertEquals(0, repository.observeHistory().first().size)
    }

    @Test
    fun historySortedNewestFirst() = runTest {
        val id1 = repository.save(conversation("First"), 1)
        Thread.sleep(5)
        val id2 = repository.save(conversation("Second"), 2)
        val entries = repository.observeHistory().first()
        assertEquals(2, entries.size)
        assertEquals("Second", entries.first().title)
        assertEquals(id2, entries.first().id)
    }
}
