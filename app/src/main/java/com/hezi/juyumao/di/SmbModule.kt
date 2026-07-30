package com.hezi.juyumao.di

import com.hezi.juyumao.data.remote.smb.SmbClient
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
    fun provideSmbClient(): SmbClient {
        return SmbClient()
    }

    @Provides
    @Singleton
    fun provideSmbConnectionPool(smbClient: SmbClient): SmbConnectionPool {
        return SmbConnectionPool(smbClient)
    }
}
