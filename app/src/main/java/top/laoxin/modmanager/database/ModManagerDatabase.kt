package top.laoxin.modmanager.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import top.laoxin.modmanager.bean.AntiHarmonyBean
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.database.antiHarmony.AntiHarmonyDao
import top.laoxin.modmanager.database.backups.BackupDao
import top.laoxin.modmanager.database.mods.ModDao

@Database(
    entities = [ModBean::class, BackupBean::class, AntiHarmonyBean::class],
    version = 4,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class ModManagerDatabase : RoomDatabase() {

    abstract fun modDao(): ModDao
    abstract fun backupDao(): BackupDao
    abstract fun antiHarmonyDao(): AntiHarmonyDao


    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 在这里添加创建新表的SQL语句
                database.execSQL(
                    "CREATE TABLE antiHarmony (\n" +
                            "    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                            "    gamePackageName TEXT NOT NULL,\n" +
                            "    isEnable INTEGER NOT NULL\n" +
                            ")"
                )
                database.execSQL("CREATE INDEX index_antiHarmony_gamePackageName ON antiHarmony(gamePackageName)")
            }
        }

        // 数据库迁移3-4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // mod表添加字段val virtualPaths: String?,
                database.execSQL("ALTER TABLE mods ADD COLUMN virtualPaths TEXT")
                // 向所有virtualPaths字段插入""值
                database.execSQL("UPDATE mods SET virtualPaths = ''")
            }
        }

        @Volatile
        private var Instance: ModManagerDatabase? = null
        fun getDatabase(context: Context): ModManagerDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, ModManagerDatabase::class.java, "mod_database")
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    //.allowMainThreadQueries() // 允许在主线程查询数据
                    .build()
                    .also { Instance = it }
            }
        }
    }

}

