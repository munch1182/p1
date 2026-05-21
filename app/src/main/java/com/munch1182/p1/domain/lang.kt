package com.munch1182.p1.domain

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.munch1182.core.android.DataHelper
import com.munch1182.core.common.get
import com.munch1182.core.common.launchIO
import com.munch1182.p1.base.stateInWithStarted
import com.munch1182.p1.data.KEY_SAVE_LANG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 语言类型
 */
sealed class LanguageType {
    /**
     * 跟随系统语言
     */
    object FollowSystem : LanguageType()

    /**
     * 设置一种定义的语言
     *
     * 可以使用固定语言列表替换这个定义, 但也可以使用此定义以保留拓展性
     *
     * @param lang 语言标识符
     */
    data class Specific(val lang: String) : LanguageType()
}

/**
 * 提供语言数据
 */
interface LanguageRepo {
    /**
     * 获取当前语言类型
     *
     * 注意: 这里返回的是Flow, 主题更改时流会返回(除非被重启)
     */
    fun getLanguageType(): Flow<LanguageType>

    /**
     * 保持当前数据
     */
    suspend fun saveLanguageType(languageType: LanguageType)
}

/**
 * 提供语言数据
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
 * 语言vm
 */
@HiltViewModel
class LanguageVM @Inject constructor(private val repo: LanguageRepo) : ViewModel() {

    /**
     * 当前语言类型, 注意: 当语言更改时, 此值会同步更新
     */
    val currLanguageType = repo.getLanguageType().stateInWithStarted(viewModelScope, LanguageType.FollowSystem)

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