package com.munch1182.core.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.lib.android.DataHelper
import com.munch1182.lib.common.get
import com.munch1182.lib.common.launchIO
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val KEY_SAVE_LANG = "KEY_SAVE_LANG"

/**
 * 语言类型。
 */
sealed class LanguageType {
    /** 跟随系统语言。 */
    object FollowSystem : LanguageType()
    /** 指定语言标签（如 "zh", "en"）。 */
    data class Specific(val lang: String) : LanguageType()
}

/**
 * 语言数据仓库接口。
 */
interface LanguageRepo {
    /** 获取当前语言类型 Flow。 */
    fun getLanguageType(): Flow<LanguageType>
    /** 持久化保存语言设置。 */
    suspend fun saveLanguageType(languageType: LanguageType)
}

/**
 * 基于 DataHelper(MMKV) 的语言数据仓库实现。
 */
class LanguageRepoImpl @Inject constructor() : LanguageRepo {

    companion object {
        private const val STR_FOLLOW_SYSTEM = "FollowSystem"
    }

    override fun getLanguageType(): Flow<LanguageType> {
        return DataHelper.get(KEY_SAVE_LANG, STR_FOLLOW_SYSTEM).map(::str2Type)
    }

    override suspend fun saveLanguageType(languageType: LanguageType) {
        DataHelper.put(KEY_SAVE_LANG, languageType.type2Str())
    }

    private fun LanguageType.type2Str() = when (this) {
        LanguageType.FollowSystem -> STR_FOLLOW_SYSTEM
        is LanguageType.Specific -> this.lang
    }

    private fun str2Type(str: String) = when (str) {
        STR_FOLLOW_SYSTEM -> LanguageType.FollowSystem
        else -> LanguageType.Specific(str)
    }
}

/**
 * 语言 ViewModel，管理应用内语言切换。
 */
@HiltViewModel
class LanguageVM @Inject constructor(private val repo: LanguageRepo) : ViewModel() {

    /** 当前语言类型。 */
    val currLanguageType = repo.getLanguageType().stateInWithStarted(viewModelScope, LanguageType.FollowSystem)

    /** 重置为跟随系统。 */
    fun reset() = switch(LanguageType.FollowSystem)

    /** 切换到指定语言标签（如 "zh", "en"）。 */
    fun switch(lang: String) = switch(LanguageType.Specific(lang))

    /** 切换到指定语言类型。 */
    fun switch(languageType: LanguageType) {
        viewModelScope.launchIO {
            val locales = when (languageType) {
                is LanguageType.FollowSystem -> LocaleListCompat.getEmptyLocaleList()
                is LanguageType.Specific -> LocaleListCompat.forLanguageTags(languageType.lang)
            }
            AppCompatDelegate.setApplicationLocales(locales)
            repo.saveLanguageType(languageType)
        }
    }
}
