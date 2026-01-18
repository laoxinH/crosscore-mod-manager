package top.laoxin.modmanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import top.laoxin.modmanager.data.repository.AntiHarmonyRepositoryImpl
import top.laoxin.modmanager.data.repository.AppDataRepositoryImpl
import javax.inject.Singleton
import top.laoxin.modmanager.data.repository.GameInfoRepositoryImpl
import top.laoxin.modmanager.data.repository.InformationRepositoryImpl
import top.laoxin.modmanager.data.repository.ReplacedFileRepositoryImpl
import top.laoxin.modmanager.data.repository.UserPreferencesRepositoryImpl
import top.laoxin.modmanager.data.repository.VersionRepositoryImpl
import top.laoxin.modmanager.data.repository.backup.BackupRepositoryImpl
import top.laoxin.modmanager.data.repository.mod.ModsRepositoryImpl
import top.laoxin.modmanager.data.repository.scanfile.ScanFileRepositoryImpl
import top.laoxin.modmanager.domain.repository.AntiHarmonyRepository
import top.laoxin.modmanager.domain.repository.AppDataRepository
import top.laoxin.modmanager.domain.repository.BackupRepository
import top.laoxin.modmanager.domain.repository.GameInfoRepository
import top.laoxin.modmanager.domain.repository.InformationRepository
import top.laoxin.modmanager.domain.repository.ModRepository
import top.laoxin.modmanager.domain.repository.ScanFileRepository
import top.laoxin.modmanager.domain.repository.ScanStateRepository
import top.laoxin.modmanager.domain.repository.UserPreferencesRepository
import top.laoxin.modmanager.domain.repository.VersionRepository
import top.laoxin.modmanager.data.repository.ScanStateRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindModRepository(modsRepositoryImpl: ModsRepositoryImpl): ModRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(backupRepositoryImpl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindAntiHarmonyRepository(
            antiHarmonyRepositoryImpl: AntiHarmonyRepositoryImpl
    ): AntiHarmonyRepository

    @Binds
    @Singleton
    abstract fun bindScanFileRepository(
            scanFileRepositoryImpl: ScanFileRepositoryImpl
    ): ScanFileRepository

    /*   @Binds
    @Singleton
    abstract fun bindModScanRepository(
            modScanRepositoryImpl: ModScanRepositoryImpl
    ): ModScanRepository*/

    //    @Binds
    //    @Singleton
    //    abstract fun bindModParseRepository(
    //            modParseRepositoryImpl: ModParseRepositoryImpl
    //    ): ModParseRepository

    // gameInfoRepository
    @Binds
    @Singleton
    abstract fun bindGameInfoRepository(
            gameInfoRepositoryImpl: GameInfoRepositoryImpl
    ): GameInfoRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
            userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindVersionRepository(
            versionRepositoryImpl: VersionRepositoryImpl
    ): VersionRepository

    @Binds
    @Singleton
    abstract fun bindInformationRepository(
            infoRepositoryImpl: InformationRepositoryImpl
    ): InformationRepository

    @Binds
    @Singleton
    abstract fun bindReplacedFileRepository(
            replacedFileRepositoryImpl: ReplacedFileRepositoryImpl
    ): top.laoxin.modmanager.domain.repository.ReplacedFileRepository

    @Binds
    @Singleton
    abstract fun bindAppDataRepository(
            appDataRepositoryImpl: AppDataRepositoryImpl
    ): AppDataRepository

    @Binds
    @Singleton
    abstract fun bindFileBrowserRepository(
            fileBrowserRepositoryImpl: top.laoxin.modmanager.data.repository.FileBrowserRepositoryImpl
    ): top.laoxin.modmanager.domain.repository.FileBrowserRepository

    @Binds
    @Singleton
    abstract fun bindScanStateRepository(
            scanStateRepositoryImpl: ScanStateRepositoryImpl
    ): ScanStateRepository
}
