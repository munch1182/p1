package com.munch1182.core.ui

import com.munch1182.core.ui.theme.ThemeRepo
import com.munch1182.core.ui.theme.ThemeRepoImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt DI 模块：绑定 core-ui 层的接口到实现。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreUiObjectProvider {
    @Binds
    abstract fun bindThemeRepo(impl: ThemeRepoImpl): ThemeRepo

    @Binds
    abstract fun bindLanguageRepo(impl: LanguageRepoImpl): LanguageRepo
}
