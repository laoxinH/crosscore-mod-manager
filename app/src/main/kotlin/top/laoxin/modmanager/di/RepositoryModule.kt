package top.laoxin.modmanager.di

// app/src/main/kotlin/top/laoxin/modmanager/di/RepositoryModule.kt


import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import top.laoxin.modmanager.data.repository.antiharmony.AntiHarmonyRepository
import top.laoxin.modmanager.data.repository.antiharmony.OfflineAntiHarmonyRepository
import top.laoxin.modmanager.data.repository.backup.BackupRepository
import top.laoxin.modmanager.data.repository.backup.OfflineBackupRepository
import top.laoxin.modmanager.data.repository.mod.ModRepository
import top.laoxin.modmanager.data.repository.mod.OfflineModsRepository
import top.laoxin.modmanager.data.repository.scanfile.OfflineScanFileRepository
import top.laoxin.modmanager.data.repository.scanfile.ScanFileRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindModRepository(
        offlineModsRepository: OfflineModsRepository
    ): ModRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        offlineBackupRepository: OfflineBackupRepository
    ): BackupRepository

    @Binds
    @Singleton
    abstract fun bindAntiHarmonyRepository(
        offlineAntiHarmonyRepository: OfflineAntiHarmonyRepository
    ): AntiHarmonyRepository

    @Binds
    @Singleton
    abstract fun bindScanFileRepository(
        offlineScanFileRepository: OfflineScanFileRepository
    ): ScanFileRepository
}