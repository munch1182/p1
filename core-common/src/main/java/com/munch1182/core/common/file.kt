package com.munch1182.core.common

import java.io.File


/**
 * 创建一个新的文件路径，通过父文件和一系列路径组件组合而成
 * @param parent 父文件对象，作为新路径的基础
 * @param path 可变数量的字符串参数，表示路径的各个组件
 * @return 返回一个由父文件和路径组件组合而成的新的File对象
 */
fun newPath(parent: File, vararg path: String) = File(parent, path.joinToString(File.separator))
