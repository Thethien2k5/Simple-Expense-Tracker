package com.T2V.simple_expense_tracker.di

import com.T2V.simple_expense_tracker.data.repository.LanguageRepositoryImpl
import com.T2V.simple_expense_tracker.data.repository.ThemeRepositoryImpl
import com.T2V.simple_expense_tracker.domain.repository.LanguageRepository
import com.T2V.simple_expense_tracker.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindLanguageRepository(
        languageRepositoryImpl: LanguageRepositoryImpl
    ): LanguageRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(
        themeRepositoryImpl: ThemeRepositoryImpl
    ): ThemeRepository
}