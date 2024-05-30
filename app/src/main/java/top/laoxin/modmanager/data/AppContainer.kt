package top.laoxin.modmanager.data

import android.content.Context
import top.laoxin.modmanager.data.backups.BackupRepository
import top.laoxin.modmanager.data.backups.OfflineBackupRepository
import top.laoxin.modmanager.data.mods.ModRepository
import top.laoxin.modmanager.data.mods.OfflineModsRepository

interface AppContainer {
    val modRepository: ModRepository
    val backupRepository: BackupRepository
}

class AppDataContainer (private val context: Context): AppContainer {
    override val modRepository: ModRepository by lazy {
        OfflineModsRepository(ModManagerDatabase.getDatabase(context).modDao())

    }

    override val backupRepository: BackupRepository by lazy {
        OfflineBackupRepository(ModManagerDatabase.getDatabase(context).backupDao())
    }
}