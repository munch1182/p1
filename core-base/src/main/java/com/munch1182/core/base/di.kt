package com.munch1182.core.base

import javax.inject.Qualifier

/**
 * 特别标注该变量注入是非单例的,
 * 用于该对象在有单例对象的同时可以提供一个非单例的对象;
 */
@Qualifier
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER
)
@Retention(AnnotationRetention.BINARY)
annotation class NotSingleton