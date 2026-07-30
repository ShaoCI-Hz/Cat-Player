package com.hezi.juyumao.di

import com.hezi.juyumao.data.remote.smb.SmbClientWrapper
import com.hezi.juyumao.data.remote.smb.SmbConnectionPool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SmbModule {

    @Provides
    @Singleton
    fun provideSmbClient(): SmbClientWrapper {
        return SmbClientWrapper()
    }

    @Provides
    @Singleton
    fun provideSmbConnectionPool(smbClient: SmbClientWrapper): SmbConnectionPool {
        return SmbConnectionPool(smbClient)
    }
}
