@file:Suppress("unused")

package top.laoxin.modmanager.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import top.laoxin.modmanager.tools.specialGameTools.ArknightsTools
import top.laoxin.modmanager.tools.specialGameTools.BaseSpecialGameTools
import top.laoxin.modmanager.tools.specialGameTools.ProjectSnowTools
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
abstract class SpecialGameToolsModule {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class ArknightsToolsImpl

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class ProjectSnowToolsImpl

    @Binds
    @ArknightsToolsImpl
    abstract fun bindArknightsTools(arknightsTools: ArknightsTools): BaseSpecialGameTools

    @Binds
    @ProjectSnowToolsImpl
    abstract fun bindProjectSnowTools(projectSnowTools: ProjectSnowTools): BaseSpecialGameTools
}