package com.munch1182.p1.base

import com.munch1182.p1.domain.LanguageRepo
import com.munch1182.p1.domain.LanguageRepoImpl
import com.munch1182.p1.domain.ThemeRepo
import com.munch1182.p1.domain.ThemeRepoImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ObjectProvider {

    @Binds
    abstract fun bindThemeRepo(impl: ThemeRepoImpl): ThemeRepo

    @Binds
    abstract fun bindLanguageRepo(impl: LanguageRepoImpl): LanguageRepo
}