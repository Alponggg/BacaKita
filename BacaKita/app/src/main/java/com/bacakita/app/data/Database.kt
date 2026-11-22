package com.bacakita.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val salt: String,
    val createdAt: Long,
)

@Entity(
    tableName = "documents",
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("userId")],
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val sourceType: String,
    val text: String,
    val importedAt: Long,
)

/** Ringkasan untuk daftar riwayat (tanpa memuat seluruh isi teks). */
data class DocumentSummary(
    val id: Long,
    val title: String,
    val sourceType: String,
    val importedAt: Long,
    val charCount: Int,
)

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): UserEntity?
}

@Dao
interface DocumentDao {
    @Insert
    suspend fun insert(doc: DocumentEntity): Long

    @Query("SELECT id, title, sourceType, importedAt, LENGTH(text) AS charCount FROM documents WHERE userId = :userId ORDER BY importedAt DESC, id DESC")
    fun observe(userId: Long): Flow<List<DocumentSummary>>

    @Query("SELECT text FROM documents WHERE id = :id AND userId = :userId")
    suspend fun getText(id: Long, userId: Long): String?

    @Query("DELETE FROM documents WHERE id = :id AND userId = :userId")
    suspend fun delete(id: Long, userId: Long)

    @Query("DELETE FROM documents WHERE userId = :userId")
    suspend fun clear(userId: Long)

    /** Menyimpan hanya [keep] dokumen terbaru per pengguna. */
    @Query("DELETE FROM documents WHERE userId = :userId AND id NOT IN (SELECT id FROM documents WHERE userId = :userId ORDER BY importedAt DESC, id DESC LIMIT :keep)")
    suspend fun trim(userId: Long, keep: Int)
}

@Database(entities = [UserEntity::class, DocumentEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "bacakita.db")
                .build().also { instance = it }
        }
    }
}
