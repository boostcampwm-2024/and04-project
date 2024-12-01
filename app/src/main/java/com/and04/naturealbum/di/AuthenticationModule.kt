package com.and04.naturealbum.di

import com.and04.naturealbum.data.repository.firebase.FireBaseRepository
import com.and04.naturealbum.data.repository.firebase.UserRepository
import com.and04.naturealbum.ui.mypage.AuthenticationManager
import com.and04.naturealbum.ui.mypage.UserManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthenticationModule {
    @Singleton
    @Provides
    fun providerAuthenticationManager(
        userRepository: UserRepository
    ): AuthenticationManager = AuthenticationManager(userRepository)

    @Provides
    fun providerUserManager() = UserManager
}
