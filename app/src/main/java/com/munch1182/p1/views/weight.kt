package com.munch1182.p1.views

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.munch1182.lib.base.Loglog
import com.munch1182.lib.base.appSetting
import com.munch1182.lib.base.launchIO
import com.munch1182.lib.base.withUI
import com.munch1182.lib.helper.RecordHelper
import com.munch1182.lib.helper.result.isAllGranted
import com.munch1182.lib.helper.result.permission
import com.munch1182.p1.base.DialogHelper
import com.munch1182.p1.base.onDialog
import com.munch1182.p1.databinding.ViewSoundWaveBinding
import com.munch1182.p1.ui.ClickButton
import com.munch1182.p1.ui.Items
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.sqrt

@Composable
fun WeightView() {
    Items(Modifier.fillMaxWidth()) {
        ClickButton("SoundWave", onClick = ::showWave)
    }
}

@SuppressLint("MissingPermission")
private fun showWave() {
    DialogHelper.newBottom((ViewSoundWaveBinding::inflate), onViewCreated = { bind, fg ->
        val record = RecordHelper.recognition()
        bind.wave.setAnimationSpeed(40L)
        bind.wave.setAmplitudes(FloatArray(30) { 0.1f })

        // 滑动窗口参数
        val updateIntervalMs = 50
        var lastUpdateTime = 0L
        val amplitudeBuffer = mutableListOf<Double>()  // 存储时间窗口内的所有dB值

        fg.lifecycleScope.launchIO {
            if (!permission(Manifest.permission.RECORD_AUDIO)
                    .onDialog("录音", "录音")
                    .onIntent(appSetting())
                    .execute().isAllGranted()
            ) {
                return@launchIO
            }

            record.record().collect { audioData ->
                val currentTime = System.currentTimeMillis()

                // 将当前dB值添加到缓冲区
                val db = audioData.calculateDB()
                amplitudeBuffer.add(db)

                // 检查是否达到更新时间间隔
                if (currentTime - lastUpdateTime >= updateIntervalMs) {
                    lastUpdateTime = currentTime

                    if (amplitudeBuffer.isNotEmpty()) {
                        // 计算窗口内的聚合值（最大值、平均值或峰值）
                        val aggregatedDb = when {
                            amplitudeBuffer.size > 10 -> {
                                // 取最高的几个值的平均（避免瞬间噪音影响）
                                val sorted = amplitudeBuffer.sortedDescending()
                                val topCount = min(5, sorted.size / 3)
                                sorted.take(topCount).average()
                            }

                            else -> amplitudeBuffer.maxOrNull() ?: db
                        }

                        Loglog.log("窗口大小: ${amplitudeBuffer.size}, 聚合DB: $aggregatedDb")

                        // 使用映射函数
                        val amplitude = mapDbToAmplitudeOptimized(aggregatedDb)

                        withUI {
                            bind.wave.addAmplitude(amplitude)
                        }
                    }

                    // 清空缓冲区，准备下一个时间窗口
                    amplitudeBuffer.clear()
                }
            }
        }
    }).show()
}

// 优化后的映射函数
private fun mapDbToAmplitudeOptimized(db: Double): Float {
    // 针对人声优化的参数
    val minDb = 40.0  // 安静阈值
    val maxDb = 85.0  // 大声阈值

    // 限制范围
    val clampedDb = db.coerceIn(minDb, maxDb)

    // 非线性映射：让正常音量区域更宽，高低音压缩
    val normalized = ((clampedDb - minDb) / (maxDb - minDb)).toFloat()

    // 使用S曲线映射（中间变化快，两端变化慢）
    return when {
        normalized < 0.3f -> normalized * 0.6f  // 低音部分压缩
        normalized < 0.7f -> 0.18f + (normalized - 0.3f) * 1.1f  // 中音放大
        else -> 0.62f + (normalized - 0.7f) * 0.5f  // 高音压缩
    }.coerceIn(0.1f, 1f)
}

// 改进的dB计算，添加最小阈值
fun ByteArray.calculateDB(read: Int = size): Double {
    val shortArray = ShortArray(read / 2)
    ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)

    if (read <= 0) return MIN_DB

    var sum = 0.0
    var count = 0

    // 使用RMS(均方根)计算
    shortArray.forEach {
        if (abs(it.toInt()) > 100) { // 忽略太小的值，减少噪音
            sum += it * it
            count++
        }
    }

    if (count == 0 || sum == 0.0) return MIN_DB

    val rms = sqrt(sum / count)
    val db = 20 * log10(rms)

    // 确保返回有效值
    return db.coerceAtLeast(MIN_DB)
}

// 定义最小dB值
private const val MIN_DB = 30.0
