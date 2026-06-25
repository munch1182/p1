package com.munch1182.feature.bluetooth.parse

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BTDevCfg(
    val name: String,
    val mtu: Int = 512,
    val endpoints: List<BTEndpoint>,
    val frame: BTFrame,
    val from: String? = null,
    val commands: List<BTCommand> = emptyList()
)

/**
 * 对读取/写入能力进行组合
 */
@Serializable
data class BTEndpoint(
    val id: String,
    val writeServiceUuid: String? = null, // 指定特定的写入服务uuid, 不设置则只可读
    val notifyServiceUuid: String? = null, // 指定特定的读取服务uuid, 不这种则只可写
    val notifyDescriptorUuid: String? = null, // 读取的描述符uuid
    val writeOp: WriteOperation = WriteOperation.WRITE_NO_RESPONSE, // 设置写入动作, 如果是WriteOperation.NONE则无视写入动作
    val notifyOp: NotifyOperation = NotifyOperation.NONE, // 设置读取动作, 如果是NotifyOperation.NONE则无视读取动作
    val descriptorOp: DescriptorOperation = DescriptorOperation.ENABLE, // 设置描述符动作, 如果是NotifyOperation.NONE或者DescriptorOperation.NONE则无视描述符动作
    val descriptorValue: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BTEndpoint

        if (id != other.id) return false
        if (writeServiceUuid != other.writeServiceUuid) return false
        if (notifyServiceUuid != other.notifyServiceUuid) return false
        if (notifyDescriptorUuid != other.notifyDescriptorUuid) return false
        if (writeOp != other.writeOp) return false
        if (notifyOp != other.notifyOp) return false
        if (descriptorOp != other.descriptorOp) return false
        if (!descriptorValue.contentEquals(other.descriptorValue)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (writeServiceUuid?.hashCode() ?: 0)
        result = 31 * result + (notifyServiceUuid?.hashCode() ?: 0)
        result = 31 * result + (notifyDescriptorUuid?.hashCode() ?: 0)
        result = 31 * result + writeOp.hashCode()
        result = 31 * result + notifyOp.hashCode()
        result = 31 * result + descriptorOp.hashCode()
        result = 31 * result + (descriptorValue?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * 数据帧模版
 *
 * 通过命令构建数据时, 会根据模版按照顺序构建, 如果模板中某些值不是固定的, 则会向实际命令中去查找同名的值配置
 */
@Serializable
data class BTFrame(
    val endian: String = "little",
    val minLen: Int = 18,
    val fields: LinkedHashMap<String, BTFrameField>  // 有序
)

@Serializable
@SerialName("Field")
sealed class BTFrameField {
    abstract val len: Int

    @SerialName("Const")
    @Serializable
    data class Const(
        val bytes: ByteArray,
        override val len: Int = bytes.size
    ) : BTFrameField() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Const
            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int = bytes.contentHashCode()
    }

    @SerialName("Var")
    @Serializable
    data class Var(
        override val len: Int = 2,
        val default: ByteArray? = null
    ) : BTFrameField() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Var

            if (len != other.len) return false
            if (!default.contentEquals(other.default)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = len
            result = 31 * result + (default?.contentHashCode() ?: 0)
            return result
        }
    }

    @SerialName("Len")
    @Serializable
    data class Len(
        override val len: Int = 2,
        val offset: Int = 0
    ) : BTFrameField()

    @SerialName("Payload")
    @Serializable
    data class Payload(
        override val len: Int = 0,   // 0 表示动态长度
        val default: ByteArray? = null
    ) : BTFrameField() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Payload

            if (len != other.len) return false
            if (!default.contentEquals(other.default)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = len
            result = 31 * result + (default?.contentHashCode() ?: 0)
            return result
        }
    }
}

@Serializable
data class BTCommand(
    val name: String,
    val endpoint: String,
    val values: Map<String, JsonElement> = emptyMap()   // 支持数字、数组、字符串
)


@Serializable
enum class WriteOperation {
    NONE, WRITE_RESPONSE, WRITE_NO_RESPONSE
}

@Serializable
enum class NotifyOperation {
    NONE, NOTIFY, INDICATE
}

@Serializable
enum class DescriptorOperation {
    NONE, ENABLE, DISABLE, CUSTOM
}