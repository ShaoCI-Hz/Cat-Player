package com.hezi.juyumao.di

import com.hezi.juyumao.data.remote.discovery.SmbDiscovery
import com.hezi.juyumao.data.remote.smb.JcifsShareEnumerator
import com.hezi.juyumao.data.remote.smb.NetworkScanner
import com.hezi.juyumao.data.remote.smb.ShareDiscovery
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
    fun provideSmbConnectionPool(): SmbConnectionPool = SmbConnectionPool()

    @Provides
    @Singleton
    fun provideSmbDiscovery(): SmbDiscovery = SmbDiscovery()

    @Provides
    @Singleton
    fun provideNetworkScanner(): NetworkScanner = NetworkScanner()

    @Provides
    @Singleton
    fun provideJcifsShareEnumerator(): JcifsShareEnumerator = JcifsShareEnumerator()

    @Provides
    @Singleton
    fun provideShareDiscovery(
        jcifsEnumerator: JcifsShareEnumerator,
    ): ShareDiscovery = ShareDiscovery(jcifsEnumerator)
}
