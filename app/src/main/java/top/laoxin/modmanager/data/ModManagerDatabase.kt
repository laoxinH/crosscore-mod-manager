package top.laoxin.modmanager.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import top.laoxin.modmanager.bean.BackupBean
import top.laoxin.modmanager.bean.ModBean
import top.laoxin.modmanager.data.backups.BackupDao
import top.laoxin.modmanager.data.mods.ModDao

@Database(entities = [ModBean::class, BackupBean::class], version = 2, exportSchema = false)

@TypeConverters(Converters::class)
abstract class ModManagerDatabase : RoomDatabase()  {

    abstract fun modDao(): ModDao
    abstract fun backupDao(): BackupDao

    companion object {

        @Volatile
        private var Instance: ModManagerDatabase? = null
        fun getDatabase(context: Context): ModManagerDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, ModManagerDatabase::class.java, "mod_database")
                    //.allowMainThreadQueries() // 允许在主线程查询数据
                    .build()
                    .also { Instance = it }
            }
        }
    }

}

