package top.laoxin.modmanager.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import top.laoxin.modmanager.data.repository.ModManagerDatabase
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
    fun provideApplicationScope(): CoroutineScope {
         // SupervisorJob() //确保一个子协程的失败不会导致整个 Scope 被取消
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideModManagerDatabase(@ApplicationContext context: Context): ModManagerDatabase {
        return ModManagerDatabase.getDatabase(context)
    }


}
