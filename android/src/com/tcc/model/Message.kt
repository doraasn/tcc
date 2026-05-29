package com.tcc.model

import org.json.JSONObject
import java.util.UUID

// 消息数据模型
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    var content: String,
    val timestamp: Long = System.currentTimeMillis(),
    var isStreaming: Boolean = false
) {
    // 转为 JSON 对象
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("role", role)
            put("content", content)
            put("timestamp", timestamp)
            put("isStreaming", isStreaming)
        }
    }

    companion object {
        // 从 JSON 解析消息
        @JvmStatic
        fun fromJson(json: JSONObject): Message {
            return Message(
                id = json.optString("id", UUID.randomUUID().toString()),
                role = json.optString("role", "user"),
                content = json.optString("content", ""),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                isStreaming = json.optBoolean("isStreaming", false)
            )
        }
    }
}
