package top.laoxin.modmanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import top.laoxin.modmanager.tools.filetools.BaseFileTools
import top.laoxin.modmanager.tools.filetools.impl.DocumentFileTools
import top.laoxin.modmanager.tools.filetools.impl.FileTools
import top.laoxin.modmanager.tools.filetools.impl.ShizukuFileTools
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
abstract class FileToolsModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class FileToolsImpl

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class ShizukuFileToolsImpl

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class DocumentFileToolsImpl

    @Binds
    @FileToolsImpl
    abstract fun bindFileTools(fileTools: FileTools): BaseFileTools

    @Binds
    @ShizukuFileToolsImpl
    abstract fun bindShizukuFileTools(shizukuFileTools: ShizukuFileTools): BaseFileTools

    @Binds
    @DocumentFileToolsImpl
    abstract fun bindDocumentFileTools(documentFileTools: DocumentFileTools): BaseFileTools
}