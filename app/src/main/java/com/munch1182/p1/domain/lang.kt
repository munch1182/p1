package com.munch1182.p1.domain

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.core.android.DataHelper
import com.munch1182.core.common.get
import com.munch1182.core.common.launchIO
import com.munch1182.p1.base.stateIn
import com.munch1182.p1.data.KEY_SAVE_LANG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

sealed class LanguageType {
    object FollowSystem : LanguageType()
    data class Specific(val lang: String) : LanguageType()
}

interface LanguageRepo {
    fun getLanguageType(): Flow<LanguageType>
    suspend fun saveLanguageType(languageType: LanguageType)
}

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

@HiltViewModel
class LanguageVM @Inject constructor(private val repo: LanguageRepo) : ViewModel() {

    val currLanguageType = repo.getLanguageType().stateIn(viewModelScope, LanguageType.FollowSystem)

    /**
     * 重置为默认
     */
    fun reset() = switch(LanguageType.FollowSystem)

    /**
     * 简化写法
     */
    fun switch(lang: String) = switch(LanguageType.Specific(lang))

    /**
     * 切换应用内语言模式
     */
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