package top.laoxin.modmanager.data.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import top.laoxin.modmanager.domain.bean.AntiHarmonyBean
import top.laoxin.modmanager.domain.bean.BackupBean
import top.laoxin.modmanager.domain.bean.ModBean
import top.laoxin.modmanager.domain.bean.ReplacedFileBean
import top.laoxin.modmanager.domain.bean.ScanFileBean
import top.laoxin.modmanager.data.dao.AntiHarmonyDao
import top.laoxin.modmanager.data.dao.BackupDao
import top.laoxin.modmanager.data.dao.ModDao
import top.laoxin.modmanager.data.dao.ReplacedFileDao
import top.laoxin.modmanager.data.dao.ScanFileDao

@Database(
        entities =
                [
                        ModBean::class,
                        BackupBean::class,
                        AntiHarmonyBean::class,
                        ScanFileBean::class,
                        ReplacedFileBean::class],
        version = 13,
        exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ModManagerDatabase : RoomDatabase() {

        abstract fun modDao(): ModDao
        abstract fun backupDao(): BackupDao
        abstract fun antiHarmonyDao(): AntiHarmonyDao
        abstract fun scanFileDao(): ScanFileDao
        abstract fun replacedFileDao(): ReplacedFileDao

        companion object {
                private val MIGRATION_2_3 =
                        object : Migration(2, 3) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // 在这里添加创建新表的SQL语句
                                        db.execSQL(
                                                "CREATE TABLE antiHarmony (\n" +
                                                        "    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,\n" +
                                                        "    gamePackageName TEXT NOT NULL,\n" +
                                                        "    isEnable INTEGER NOT NULL\n" +
                                                        ")"
                                        )
                                        db.execSQL(
                                                "CREATE INDEX index_antiHarmony_gamePackageName ON antiHarmony(gamePackageName)"
                                        )
                                }
                        }

                // 数据库迁移3-4
                private val MIGRATION_3_4 =
                        object : Migration(3, 4) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // mod表添加字段val virtualPaths: String?,
                                        db.execSQL("ALTER TABLE mods ADD COLUMN virtualPaths TEXT")
                                        // 向所有virtualPaths字段插入""值
                                        db.execSQL("UPDATE mods SET virtualPaths = ''")
                                }
                        }

                // 数据库迁移4-5
                private val MIGRATION_4_5 =
                        object : Migration(4, 5) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // Perform the necessary schema changes
                                        db.execSQL(
                                                "CREATE TABLE IF NOT EXISTS `scanFiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `path` TEXT NOT NULL, `name` TEXT NOT NULL, `modifyTime` INTEGER NOT NULL, `size` INTEGER NOT NULL)"
                                        )
                                }
                        }

                // 数据库迁移5-6: ModBean空安全重构 + 新增字段
                private val MIGRATION_5_6 =
                        object : Migration(5, 6) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // 更新现有NULL值为默认值
                                        db.execSQL("UPDATE mods SET name = '' WHERE name IS NULL")
                                        db.execSQL(
                                                "UPDATE mods SET description = '' WHERE description IS NULL"
                                        )
                                        db.execSQL("UPDATE mods SET path = '' WHERE path IS NULL")
                                        db.execSQL(
                                                "UPDATE mods SET modFiles = '[]' WHERE modFiles IS NULL"
                                        )
                                        db.execSQL(
                                                "UPDATE mods SET password = '' WHERE password IS NULL"
                                        )
                                        db.execSQL(
                                                "UPDATE mods SET gamePackageName = '' WHERE gamePackageName IS NULL"
                                        )
                                        db.execSQL(
                                                "UPDATE mods SET gameModPath = '' WHERE gameModPath IS NULL"
                                        )
                                        db.execSQL(
                                                "UPDATE mods SET modType = '' WHERE modType IS NULL"
                                        )

                                        // 添加新字段
                                        db.execSQL(
                                                "ALTER TABLE mods ADD COLUMN gameFilesPath TEXT NOT NULL DEFAULT '[]'"
                                        )
                                        db.execSQL(
                                                "ALTER TABLE mods ADD COLUMN modForm TEXT NOT NULL DEFAULT 'TRADITIONAL'"
                                        )
                                        db.execSQL("ALTER TABLE mods ADD COLUMN modConfig TEXT")
                                }
                        }

                // 数据库迁移6-7: BackupBean空安全重构 + 字段变更
                private val MIGRATION_6_7 =
                        object : Migration(6, 7) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // 更新现有NULL值为默认值
                                        db.execSQL(
                                                "UPDATE backups SET filename = '' WHERE filename IS NULL"
                                        )
                                        db.execSQL(
                                                "UPDATE backups SET gamePath = '' WHERE gamePath IS NULL"
                                        )
                                        db.execSQL(
                                                "UPDATE backups SET gameFilePath = '' WHERE gameFilePath IS NULL"
                                        )
                                        db.execSQL(
                                                "UPDATE backups SET backupPath = '' WHERE backupPath IS NULL"
                                        )
                                        db.execSQL(
                                                "UPDATE backups SET gamePackageName = '' WHERE gamePackageName IS NULL"
                                        )

                                        // 添加新字段
                                        db.execSQL(
                                                "ALTER TABLE backups ADD COLUMN modId INTEGER NOT NULL DEFAULT 0"
                                        )
                                        db.execSQL(
                                                "ALTER TABLE backups ADD COLUMN backupTime INTEGER NOT NULL DEFAULT 0"
                                        )
                                        db.execSQL(
                                                "ALTER TABLE backups ADD COLUMN copyTime INTEGER NOT NULL DEFAULT 0"
                                        )

                                        // 注意：modName 字段将在下次重建表时移除
                                        // SQLite 不支持 DROP COLUMN，保留该列不影响功能
                                }
                        }

                // 数据库迁移7-8: 添加modRelativePath字段
                private val MIGRATION_7_8 =
                        object : Migration(7, 8) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // 添加 modRelativePath 字段，用于存储压缩包内的相对路径
                                        db.execSQL(
                                                "ALTER TABLE mods ADD COLUMN modRelativePath TEXT"
                                        )
                                }
                        }

                // 数据库迁移8-9: ScanFileBean 添加 md5, gamePackageName 字段
                private val MIGRATION_8_9 =
                        object : Migration(8, 9) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // 添加 md5 字段
                                        db.execSQL(
                                                "ALTER TABLE scanFiles ADD COLUMN md5 TEXT NOT NULL DEFAULT ''"
                                        )
                                        // 添加 gamePackageName 字段
                                        db.execSQL(
                                                "ALTER TABLE scanFiles ADD COLUMN gamePackageName TEXT NOT NULL DEFAULT ''"
                                        )
                                }
                        }

                // 数据库迁移9-10: BackupBean 添加 originalMd5, modFileMd5 字段
                private val MIGRATION_9_10 =
                        object : Migration(9, 10) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        db.execSQL(
                                                "ALTER TABLE backups ADD COLUMN originalMd5 TEXT NOT NULL DEFAULT ''"
                                        )
                                        db.execSQL(
                                                "ALTER TABLE backups ADD COLUMN modFileMd5 TEXT NOT NULL DEFAULT ''"
                                        )
                                }
                        }

                // 数据库迁移10-11: 新增 replaced_files 表
                private val MIGRATION_10_11 =
                        object : Migration(10, 11) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        db.execSQL(
                                                """CREATE TABLE IF NOT EXISTS replaced_files (
                                                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                                        modId INTEGER NOT NULL,
                                                        filename TEXT NOT NULL,
                                                        gameFilePath TEXT NOT NULL,
                                                        md5 TEXT NOT NULL,
                                                        gamePackageName TEXT NOT NULL,
                                                        replaceTime INTEGER NOT NULL
                                                )"""
                                        )
                                }
                        }

                // 数据库迁移11-12: ModBean 添加 updateAt 字段
                private val MIGRATION_11_12 =
                        object : Migration(11, 12) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // 添加 updateAt 字段，默认值为当前时间戳
                                        db.execSQL(
                                                "ALTER TABLE mods ADD COLUMN updateAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}"
                                        )
                                }
                        }

                // 数据库迁移12-13: 重建 mods 和 backups 表，修正 schema 与实体定义一致
                private val MIGRATION_12_13 =
                        object : Migration(12, 13) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // ========== 重建 mods 表 ==========
                                        // 1. 创建新表，与 ModBean 定义完全一致
                                        db.execSQL(
                                                """CREATE TABLE IF NOT EXISTS mods_new (
                                                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                                        name TEXT NOT NULL,
                                                        description TEXT NOT NULL,
                                                        date INTEGER NOT NULL,
                                                        path TEXT NOT NULL,
                                                        modFiles TEXT NOT NULL,
                                                        gameFilesPath TEXT NOT NULL,
                                                        modForm TEXT NOT NULL,
                                                        isEncrypted INTEGER NOT NULL,
                                                        password TEXT NOT NULL,
                                                        gamePackageName TEXT NOT NULL,
                                                        gameModPath TEXT NOT NULL,
                                                        modType TEXT NOT NULL,
                                                        isEnable INTEGER NOT NULL,
                                                        isZipFile INTEGER NOT NULL,
                                                        version TEXT,
                                                        virtualPaths TEXT,
                                                        icon TEXT,
                                                        images TEXT,
                                                        readmePath TEXT,
                                                        fileReadmePath TEXT,
                                                        author TEXT,
                                                        modConfig TEXT,
                                                        modRelativePath TEXT,
                                                        updateAt INTEGER NOT NULL
                                                )"""
                                        )
                                        
                                        // 2. 从旧表复制数据，处理可能的 NULL 值
                                        db.execSQL(
                                                """INSERT INTO mods_new (
                                                        id, name, description, date, path, modFiles, gameFilesPath,
                                                        modForm, isEncrypted, password, gamePackageName, gameModPath,
                                                        modType, isEnable, isZipFile, version, virtualPaths, icon,
                                                        images, readmePath, fileReadmePath, author, modConfig,
                                                        modRelativePath, updateAt
                                                ) SELECT
                                                        id,
                                                        COALESCE(name, ''),
                                                        COALESCE(description, ''),
                                                        date,
                                                        COALESCE(path, ''),
                                                        COALESCE(modFiles, '[]'),
                                                        COALESCE(gameFilesPath, '[]'),
                                                        COALESCE(modForm, 'TRADITIONAL'),
                                                        isEncrypted,
                                                        COALESCE(password, ''),
                                                        COALESCE(gamePackageName, ''),
                                                        COALESCE(gameModPath, ''),
                                                        COALESCE(modType, ''),
                                                        isEnable,
                                                        isZipFile,
                                                        version,
                                                        virtualPaths,
                                                        icon,
                                                        images,
                                                        readmePath,
                                                        fileReadmePath,
                                                        author,
                                                        modConfig,
                                                        modRelativePath,
                                                        COALESCE(updateAt, ${System.currentTimeMillis()})
                                                FROM mods"""
                                        )
                                        
                                        // 3. 删除旧表
                                        db.execSQL("DROP TABLE mods")
                                        
                                        // 4. 重命名新表
                                        db.execSQL("ALTER TABLE mods_new RENAME TO mods")

                                        // ========== 重建 backups 表 ==========
                                        // 1. 创建新表，与 BackupBean 定义完全一致 (移除 modName 字段)
                                        db.execSQL(
                                                """CREATE TABLE IF NOT EXISTS backups_new (
                                                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                                        modId INTEGER NOT NULL,
                                                        filename TEXT NOT NULL,
                                                        gamePath TEXT NOT NULL,
                                                        gameFilePath TEXT NOT NULL,
                                                        backupPath TEXT NOT NULL,
                                                        gamePackageName TEXT NOT NULL,
                                                        backupTime INTEGER NOT NULL,
                                                        copyTime INTEGER NOT NULL,
                                                        originalMd5 TEXT NOT NULL,
                                                        modFileMd5 TEXT NOT NULL
                                                )"""
                                        )

                                        // 2. 从旧表复制数据，处理可能的 NULL 值
                                        db.execSQL(
                                                """INSERT INTO backups_new (
                                                        id, modId, filename, gamePath, gameFilePath, backupPath,
                                                        gamePackageName, backupTime, copyTime, originalMd5, modFileMd5
                                                ) SELECT
                                                        id,
                                                        COALESCE(modId, 0),
                                                        COALESCE(filename, ''),
                                                        COALESCE(gamePath, ''),
                                                        COALESCE(gameFilePath, ''),
                                                        COALESCE(backupPath, ''),
                                                        COALESCE(gamePackageName, ''),
                                                        COALESCE(backupTime, 0),
                                                        COALESCE(copyTime, 0),
                                                        COALESCE(originalMd5, ''),
                                                        COALESCE(modFileMd5, '')
                                                FROM backups"""
                                        )

                                        // 3. 删除旧表
                                        db.execSQL("DROP TABLE backups")

                                        // 4. 重命名新表
                                        db.execSQL("ALTER TABLE backups_new RENAME TO backups")

                                        // ========== 重建 scanFiles 表 ==========
                                        // 1. 创建新表，与 ScanFileBean 定义完全一致 (添加 isDirectory 字段)
                                        db.execSQL(
                                                """CREATE TABLE IF NOT EXISTS scanFiles_new (
                                                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                                        path TEXT NOT NULL,
                                                        name TEXT NOT NULL,
                                                        modifyTime INTEGER NOT NULL,
                                                        size INTEGER NOT NULL,
                                                        isDirectory INTEGER NOT NULL,
                                                        md5 TEXT NOT NULL,
                                                        gamePackageName TEXT NOT NULL
                                                )"""
                                        )

                                        // 2. 从旧表复制数据，isDirectory默认为0(false)
                                        db.execSQL(
                                                """INSERT INTO scanFiles_new (
                                                        id, path, name, modifyTime, size, isDirectory, md5, gamePackageName
                                                ) SELECT
                                                        id,
                                                        path,
                                                        name,
                                                        modifyTime,
                                                        size,
                                                        0,
                                                        COALESCE(md5, ''),
                                                        COALESCE(gamePackageName, '')
                                                FROM scanFiles"""
                                        )

                                        // 3. 删除旧表
                                        db.execSQL("DROP TABLE scanFiles")

                                        // 4. 重命名新表
                                        db.execSQL("ALTER TABLE scanFiles_new RENAME TO scanFiles")
                                }
                        }

                @Volatile private var Instance: ModManagerDatabase? = null
                fun getDatabase(context: Context): ModManagerDatabase {
                        return Instance
                                ?: synchronized(this) {
                                        Room.databaseBuilder(
                                                        context,
                                                        ModManagerDatabase::class.java,
                                                        "mod_database"
                                                )
                                                .addMigrations(MIGRATION_2_3)
                                                .addMigrations(MIGRATION_3_4)
                                                .addMigrations(MIGRATION_4_5)
                                                .addMigrations(MIGRATION_5_6)
                                                .addMigrations(MIGRATION_6_7)
                                                .addMigrations(MIGRATION_7_8)
                                                .addMigrations(MIGRATION_8_9)
                                                .addMigrations(MIGRATION_9_10)
                                                .addMigrations(MIGRATION_10_11)
                                                .addMigrations(MIGRATION_11_12)
                                                .addMigrations(MIGRATION_12_13)
                                                // .allowMainThreadQueries() // 允许在主线程查询数据
                                                .build()
                                                .also { Instance = it }
                                }
                }
        }
}
