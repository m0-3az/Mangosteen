package ai.opencode.mangosteen.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ChatDbMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(ChatDb.NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(ChatDb.NAME)
    }

    @Test
    fun `v5 database upgrades to v6 and preserves existing history`() {
        createVersion5Database()

        val helper = ChatDb(context)
        try {
            val db = helper.readableDatabase
            assertEquals(6, db.version)

            val columns = mutableSetOf<String>()
            db.rawQuery("PRAGMA table_info(${ChatDb.T_MSG})", null).use { cursor ->
                val name = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) columns.add(cursor.getString(name))
            }
            assertTrue("reasoning_millis column was not added", columns.contains("reasoning_millis"))

            db.rawQuery(
                "SELECT content, reasoning_millis FROM ${ChatDb.T_MSG} WHERE id=1",
                null,
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("keep this message", cursor.getString(0))
                assertTrue(cursor.isNull(1))
            }
        } finally {
            helper.close()
        }

        // The repository query must also work on the upgraded database.
        val repo = ChatRepository(context)
        try {
            val old = repo.messages(1L)
            assertEquals(1, old.size)
            assertEquals("keep this message", old[0].content)
            assertNull(old[0].reasoningMillis)

            val newId = repo.addMessage(
                Message(
                    id = 0L,
                    conversationId = 1L,
                    role = "assistant",
                    content = "measured reply",
                    reasoning = "thinking",
                    modelId = "test-model",
                    createdAt = 2_000L,
                    reasoningMillis = 1_500L,
                )
            )
            assertTrue(newId > 0L)

            val all = repo.messages(1L)
            assertEquals(2, all.size)
            assertEquals(1_500L, all.last().reasoningMillis)
        } finally {
            repo.close()
        }
    }

    /** Recreates the exact schema shipped in version 2.1 (database version 5). */
    private fun createVersion5Database() {
        val file = context.getDatabasePath(ChatDb.NAME)
        file.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            db.execSQL(
                "CREATE TABLE ${ChatDb.T_CONV} (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "session_id TEXT NOT NULL," +
                    "model_id TEXT NOT NULL," +
                    "system_prompt TEXT," +
                    "created_at INTEGER NOT NULL," +
                    "updated_at INTEGER NOT NULL," +
                    "last_seen_at INTEGER NOT NULL DEFAULT 0)"
            )
            db.execSQL(
                "CREATE TABLE ${ChatDb.T_MSG} (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "conversation_id INTEGER NOT NULL," +
                    "role TEXT NOT NULL," +
                    "content TEXT NOT NULL," +
                    "reasoning TEXT," +
                    "model_id TEXT," +
                    "created_at INTEGER NOT NULL," +
                    "prompt_tokens INTEGER," +
                    "completion_tokens INTEGER," +
                    "attachment_path TEXT," +
                    "attachment_mime TEXT," +
                    "FOREIGN KEY(conversation_id) REFERENCES ${ChatDb.T_CONV}(id) " +
                    "ON DELETE CASCADE)"
            )
            db.execSQL("CREATE INDEX idx_msg_conv ON ${ChatDb.T_MSG}(conversation_id)")
            db.execSQL(
                "CREATE TABLE ${ChatDb.T_REM} (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "conversation_id INTEGER NOT NULL," +
                    "fire_at INTEGER NOT NULL," +
                    "note TEXT NOT NULL," +
                    "status TEXT NOT NULL DEFAULT 'pending'," +
                    "created_at INTEGER NOT NULL," +
                    "FOREIGN KEY(conversation_id) REFERENCES ${ChatDb.T_CONV}(id) " +
                    "ON DELETE CASCADE)"
            )
            db.execSQL("CREATE INDEX idx_rem_pending ON ${ChatDb.T_REM}(status, fire_at)")

            db.execSQL(
                "INSERT INTO ${ChatDb.T_CONV} " +
                    "(id,title,session_id,model_id,system_prompt,created_at,updated_at,last_seen_at) " +
                    "VALUES (1,'Legacy chat','session-1','test-model',NULL,1000,1000,0)"
            )
            db.execSQL(
                "INSERT INTO ${ChatDb.T_MSG} " +
                    "(id,conversation_id,role,content,reasoning,model_id,created_at," +
                    "prompt_tokens,completion_tokens,attachment_path,attachment_mime) " +
                    "VALUES (1,1,'user','keep this message',NULL,'test-model',1000," +
                    "NULL,NULL,NULL,NULL)"
            )
            db.version = 5
        } finally {
            db.close()
        }
    }
}
