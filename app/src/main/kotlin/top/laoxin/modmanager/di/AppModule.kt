package top.laoxin.modmanager.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import top.laoxin.modmanager.data.repository.ModManagerDatabase
import top.laoxin.modmanager.tools.PermissionTools
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideModManagerDatabase(@ApplicationContext context: Context): ModManagerDatabase {
        return ModManagerDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun providePermissionTools(): PermissionTools {
        return PermissionTools
    }
}