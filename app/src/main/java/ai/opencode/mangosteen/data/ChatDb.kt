package ai.opencode.mangosteen.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ChatDb(context: Context) : SQLiteOpenHelper(context, NAME, null, VERSION) {

    companion object {
        const val NAME = "mangosteen.db"
        const val VERSION = 6
        const val T_CONV = "conversations"
        const val T_MSG = "messages"
        const val T_REM = "reminders"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $T_CONV (" +
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
            "CREATE TABLE $T_MSG (" +
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
                "reasoning_millis INTEGER," +
                "FOREIGN KEY(conversation_id) REFERENCES $T_CONV(id) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX idx_msg_conv ON $T_MSG(conversation_id)")
        createReminders(db)
    }

    private fun createReminders(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $T_REM (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "conversation_id INTEGER NOT NULL," +
                "fire_at INTEGER NOT NULL," +
                "note TEXT NOT NULL," +
                "status TEXT NOT NULL DEFAULT 'pending'," +
                "created_at INTEGER NOT NULL," +
                "FOREIGN KEY(conversation_id) REFERENCES $T_CONV(id) ON DELETE CASCADE)"
        )
        db.execSQL("CREATE INDEX idx_rem_pending ON $T_REM(status, fire_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $T_MSG ADD COLUMN prompt_tokens INTEGER")
            db.execSQL("ALTER TABLE $T_MSG ADD COLUMN completion_tokens INTEGER")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $T_CONV ADD COLUMN last_seen_at INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 4) {
            createReminders(db)
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE $T_MSG ADD COLUMN attachment_path TEXT")
            db.execSQL("ALTER TABLE $T_MSG ADD COLUMN attachment_mime TEXT")
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE $T_MSG ADD COLUMN reasoning_millis INTEGER")
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }
}
