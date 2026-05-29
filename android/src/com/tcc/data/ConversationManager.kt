package com.tcc.data

import android.content.Context
import com.tcc.model.Conversation
import com.tcc.model.Message
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// 对话管理器 - 增删改查持久化
class ConversationManager private constructor(context: Context) {

    private val convDir: File = File(context.filesDir, "conversations")
    private val indexFile: File = File(convDir, "index.json")

    init {
        convDir.mkdirs()
    }

    // 列出所有对话摘要（按更新时间倒序）
    fun listConversations(): List<Conversation> {
        val jsonArray = readIndex()
        val result = mutableListOf<Conversation>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i)
            if (obj != null) {
                result.add(Conversation(
                    id = obj.optString("id", ""),
                    title = obj.optString("title", "新对话"),
                    model = obj.optString("model", "deepseek-v4-flash"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                ))
            }
        }
        // Sort by updatedAt descending
        result.sortByDescending { it.updatedAt }
        return result
    }

    // 获取完整对话（含消息）
    fun getConversation(id: String): Conversation? {
        val file = File(convDir, "${id}.json")
        if (!file.exists()) return null
        return try {
            val content = file.readText()
            Conversation.fromJson(JSONObject(content))
        } catch (e: Exception) {
            null
        }
    }

    // 创建新对话
    @Synchronized
    fun createConversation(model: String): Conversation {
        val conv = Conversation(model = model)
        saveConversation(conv)
        return conv
    }

    // 删除对话
    @Synchronized
    fun deleteConversation(id: String) {
        val file = File(convDir, "${id}.json")
        if (file.exists()) file.delete()
        removeFromIndex(id)
    }

    // 删除所有对话
    @Synchronized
    fun deleteAllConversations() {
        val files = convDir.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.name.endsWith(".json")) {
                    f.delete()
                }
            }
        }
        // Recreate empty index
        writeIndex(JSONArray())
    }

    // 保存对话到文件
    @Synchronized
    fun saveConversation(conv: Conversation) {
        val file = File(convDir, "${conv.id}.json")
        try {
            file.writeText(conv.toJson().toString())
        } catch (e: Exception) {
            // Fallback: attempt to create parent dirs
            convDir.mkdirs()
            file.writeText(conv.toJson().toString())
        }
        updateIndex(conv)
    }

    // 添加消息到对话
    @Synchronized
    fun addMessage(convId: String, msg: Message) {
        val conv = getConversation(convId) ?: return
        conv.addMessage(msg)
        saveConversation(conv)
    }

    // 更新对话中的消息
    @Synchronized
    fun updateMessage(convId: String, index: Int, msg: Message) {
        val conv = getConversation(convId) ?: return
        if (index >= 0 && index < conv.messages.size) {
            conv.messages[index] = msg
            conv.updatedAt = System.currentTimeMillis()
            saveConversation(conv)
        }
    }

    // 更新对话索引
    private fun updateIndex(conv: Conversation) {
        val jsonArray = readIndex()
        var found = false
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i)
            if (obj != null && obj.optString("id", "") == conv.id) {
                obj.put("title", conv.title)
                obj.put("model", conv.model)
                obj.put("updatedAt", conv.updatedAt)
                obj.put("createdAt", conv.createdAt)
                obj.put("messageCount", conv.messages.size)
                found = true
                break
            }
        }
        if (!found) {
            jsonArray.put(JSONObject().apply {
                put("id", conv.id)
                put("title", conv.title)
                put("model", conv.model)
                put("createdAt", conv.createdAt)
                put("updatedAt", conv.updatedAt)
                put("messageCount", conv.messages.size)
            })
        }
        writeIndex(jsonArray)
    }

    // 从索引中移除
    private fun removeFromIndex(id: String) {
        val jsonArray = readIndex()
        val updated = JSONArray()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i)
            if (obj != null && obj.optString("id", "") != id) {
                updated.put(obj)
            }
        }
        writeIndex(updated)
    }

    // 读取对话索引文件
    private fun readIndex(): JSONArray {
        if (!indexFile.exists()) return JSONArray()
        return try {
            val content = indexFile.readText().trim()
            if (content.isEmpty()) {
                JSONArray()
            } else {
                JSONArray(content)
            }
        } catch (e: Exception) {
            JSONArray()
        }
    }

    // 写入对话索引文件
    private fun writeIndex(jsonArray: JSONArray) {
        try {
            indexFile.writeText(jsonArray.toString())
        } catch (e: Exception) {
            convDir.mkdirs()
            indexFile.writeText(jsonArray.toString())
        }
    }

    companion object {
        @Volatile
        private var instance: ConversationManager? = null

        // 获取单例实例
        @JvmStatic
        fun getInstance(context: Context): ConversationManager {
            return instance ?: synchronized(this) {
                instance ?: ConversationManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
