package com.voxaid.core.common.di

//import com.voxaid.core.content.repository.EmergencyProtocolRepository
//import com.voxaid.core.content.repository.ProtocolRepository
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
///**
// * Hilt module for repository dependencies.
// */
//@Module
//@InstallIn(SingletonComponent::class)
//object RepositoryModule {
//
//    @Provides
//    @Singleton
//    fun provideProtocolRepository(
//        protocolRepository: ProtocolRepository
//    ): ProtocolRepository = protocolRepository
//
//    @Provides
//    @Singleton
//    fun provideEmergencyProtocolRepository(
//        emergencyProtocolRepository: EmergencyProtocolRepository
//    ): EmergencyProtocolRepository = emergencyProtocolRepository
//}