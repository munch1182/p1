package com.munch1182.feature.bluetooth

import com.munch1182.feature.bluetooth.parse.BTCommand
import com.munch1182.feature.bluetooth.parse.BTDevCfg
import com.munch1182.feature.bluetooth.parse.BTEndpoint
import com.munch1182.feature.bluetooth.parse.BTFrame
import com.munch1182.feature.bluetooth.parse.BTFrameField
import com.munch1182.feature.bluetooth.parse.DescriptorOperation
import com.munch1182.feature.bluetooth.parse.NotifyOperation
import com.munch1182.feature.bluetooth.parse.WriteOperation
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class DataJsonUnitTest {
    @Test
    fun bt_json_test() {
        val cfg = BTDevCfg(
            name = "MockDevice",
            mtu = 256,
            endpoints = listOf(
                BTEndpoint(
                    id = "main_endpoint",
                    writeServiceUuid = "00001111-2222-3333-4444-555566667777",
                    notifyServiceUuid = "88889999-aaaa-bbbb-cccc-ddddeeeeffff",
                    notifyDescriptorUuid = "00002902-0000-1000-8000-00805f9b34fb", // 常见 CCCD
                    writeOp = WriteOperation.WRITE_NO_RESPONSE,
                    notifyOp = NotifyOperation.NOTIFY,
                    descriptorOp = DescriptorOperation.ENABLE,
                    descriptorValue = null
                )
            ),
            frame = BTFrame(
                endian = "little",
                minLen = 10,
                fields = linkedMapOf(
                    "header" to BTFrameField.Const(byteArrayOf(0xAA.toByte(), 0x55)),
                    "length" to BTFrameField.Len(len = 2, offset = 0),
                    "cmd" to BTFrameField.Var(len = 1, default = byteArrayOf(0x00)),
                    "payload" to BTFrameField.Payload(len = 0, default = null)
                )
            ),
            from = null, // 或可指定来源，例如 "config_v1"
            commands = listOf(
                BTCommand(
                    name = "turn_on_led",
                    endpoint = "main_endpoint",
                    values = mapOf(
                        "cmd" to JsonPrimitive(0x01),
                        "payload" to JsonPrimitive("0x01 0x02 0x03") // 仅作示意，实际按需
                    )
                ),
                BTCommand(
                    name = "read_sensor",
                    endpoint = "main_endpoint",
                    values = mapOf(
                        "cmd" to JsonPrimitive(0x02)
                    )
                )
            )
        )
        val str = Json.encodeToString(cfg)
        println("str: $str")
        assert(str.isNotEmpty())
    }
}