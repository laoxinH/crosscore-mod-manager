package top.laoxin.modmanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import top.laoxin.modmanager.data.service.AppInfoServiceImpl
import top.laoxin.modmanager.data.service.ArchiveServiceImpl
import top.laoxin.modmanager.data.service.BackupServiceImpl
import top.laoxin.modmanager.data.service.FileServiceImpl
import top.laoxin.modmanager.data.service.ModDecryptServiceImpl
import top.laoxin.modmanager.data.service.ModEnableServiceImpl
import top.laoxin.modmanager.data.service.ModScanServiceImpl
import top.laoxin.modmanager.data.service.ModSourcePrepareServiceImpl
import top.laoxin.modmanager.data.service.PermissionServiceImpl
import top.laoxin.modmanager.data.service.TraditionalModEnableServiceImpl
import top.laoxin.modmanager.data.service.TraditionalModScanServiceImpl
import top.laoxin.modmanager.data.service.specialgame.SpecialGameServiceImpl
import top.laoxin.modmanager.domain.service.AppInfoService
import top.laoxin.modmanager.domain.service.ArchiveService
import top.laoxin.modmanager.domain.service.BackupService
import top.laoxin.modmanager.domain.service.FileService
import top.laoxin.modmanager.domain.service.ModDecryptService
import top.laoxin.modmanager.domain.service.ModEnableService
import top.laoxin.modmanager.domain.service.ModScanService
import top.laoxin.modmanager.domain.service.ModSourcePrepareService
import top.laoxin.modmanager.domain.service.PermissionService
import top.laoxin.modmanager.domain.service.SpecialGameService
import top.laoxin.modmanager.domain.service.TraditionalModEnableService
import top.laoxin.modmanager.domain.service.TraditionalModScanService

/** Service 依赖注入模块 绑定 Service 接口到具体实现 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

        @Binds @Singleton abstract fun bindAppInfoService(impl: AppInfoServiceImpl): AppInfoService

        @Binds @Singleton abstract fun bindFileService(impl: FileServiceImpl): FileService

        @Binds @Singleton abstract fun bindArchiveService(impl: ArchiveServiceImpl): ArchiveService

        @Binds
        @Singleton
        abstract fun bindPermissionService(impl: PermissionServiceImpl): PermissionService

        @Binds
        @Singleton
        abstract fun bindSpecialGameService(impl: SpecialGameServiceImpl): SpecialGameService

        @Binds
        @Singleton
        abstract fun bindTraditionalModScanService(
                impl: TraditionalModScanServiceImpl
        ): TraditionalModScanService

        @Binds @Singleton abstract fun bindModScanService(impl: ModScanServiceImpl): ModScanService

        @Binds
        @Singleton
        abstract fun bindModSourcePrepareService(
                impl: ModSourcePrepareServiceImpl
        ): ModSourcePrepareService

        @Binds @Singleton abstract fun bindBackupService(impl: BackupServiceImpl): BackupService

        @Binds
        @Singleton
        abstract fun bindTraditionalModEnableService(
                impl: TraditionalModEnableServiceImpl
        ): TraditionalModEnableService

        @Binds
        @Singleton
        abstract fun bindModEnableService(impl: ModEnableServiceImpl): ModEnableService

        @Binds
        @Singleton
        abstract fun bindModDecryptService(impl: ModDecryptServiceImpl): ModDecryptService
}
