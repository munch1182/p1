package com.munch1182.p1.base

import com.munch1182.p1.domain.LanguageRepo
import com.munch1182.p1.domain.LanguageRepoImpl
import com.munch1182.p1.domain.ThemeRepo
import com.munch1182.p1.domain.ThemeRepoImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 使用hilt提供全局基本的变量
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ObjectProvider {

    /**
     * 使用[ThemeRepoImpl]提供一个[ThemeRepo]
     *
     * Binds简化写法
     */
    @Binds
    abstract fun bindThemeRepo(impl: ThemeRepoImpl): ThemeRepo

    /**
     * 使用[LanguageRepoImpl]提供一个[LanguageRepo]
     */
    @Binds
    abstract fun bindLanguageRepo(impl: LanguageRepoImpl): LanguageRepo
}