package com.mcc.ui
import android.view.ViewGroup.LayoutParams

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.mcc.model.Message

class ChatListView(context: Context) : ScrollView(context) {

    companion object {
        private const val BG = 0xFF0A0A0B.toInt()
        private const val SURFACE = 0xFF141416.toInt()
        private const val SURFACE_ELEVATED = 0xFF1C1C1F.toInt()
        private const val ACCENT = 0xFF6C5CE7.toInt()
        private const val TEXT_PRIMARY = 0xFFFFFFFF.toInt()
        private const val TEXT_SECONDARY = 0xFF8B8B93.toInt()
        private const val TEXT_TERTIARY = 0xFF5E5E66.toInt()
        private const val BORDER = 0xFF2A2A2E.toInt()
    }

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private val messages = mutableListOf<Message>()
    private var onSuggestionClick: ((String) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cursorVisible = true
    private val cursorRunnable = object : Runnable {
        override fun run() {
            cursorVisible = !cursorVisible
            val lastChild = container.getChildAt(container.childCount - 1)
            if (lastChild is MessageBubble && lastChild.isStreaming) {
                lastChild.invalidate()
            }
            mainHandler.postDelayed(this, 500)
        }
    }

    init {
        setBackgroundColor(BG)
        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        isVerticalScrollBarEnabled = false
        showEmptyState()
    }

    fun setMessages(msgs: List<Message>) {
        messages.clear()
        messages.addAll(msgs)
        rebuildMessageViews()
    }

    fun addMessage(msg: Message) {
        messages.add(msg)
        removeEmptyState()
        addMessageView(msg)
        scrollToBottom()
        if (msg.isStreaming) {
            mainHandler.post(cursorRunnable)
        }
    }

    fun updateLastMessage(msg: Message) {
        if (messages.isNotEmpty()) {
            messages[messages.size - 1] = msg
            val lastIndex = container.childCount - 1
            val lastChild = container.getChildAt(lastIndex)
            if (lastChild is MessageBubble) {
                lastChild.updateContent(renderContent(msg.content, msg.isStreaming), msg.isStreaming)
            }
            if (!msg.isStreaming) {
                mainHandler.removeCallbacks(cursorRunnable)
                val bubble = container.getChildAt(lastIndex) as? MessageBubble
                bubble?.invalidate()
            }
        }
    }

    fun scrollToBottom() {
        post { fullScroll(View.FOCUS_DOWN) }
    }

    fun setOnSuggestionClick(callback: (String) -> Unit) {
        onSuggestionClick = callback
    }

    private fun showEmptyState() {
        container.removeAllViews()
        messages.clear()

        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                topMargin = dp(120)
                leftMargin = dp(32)
                rightMargin = dp(32)
            }
        }

        // MCC logo text
        val logoText = TextView(context).apply {
            text = "MCC"
            setTextColor(ACCENT)
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        wrapper.addView(logoText)

        val subtitle = TextView(context).apply {
            text = "Claude Code for Android"
            setTextColor(TEXT_TERTIARY)
            textSize = 16f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(48)
            }
        }
        wrapper.addView(subtitle)

        // Suggestion cards
        val suggestions = listOf(
            "帮我写代码" to "帮我写代码",
            "总结这篇文章" to "总结这篇文章",
            "写一个脚本" to "写一个脚本",
            "翻译这段文字" to "翻译这段文字"
        )

        val cardContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        for ((_, label) in suggestions) {
            val card = createSuggestionCard(label)
            cardContainer.addView(card)
        }

        wrapper.addView(cardContainer)
        container.addView(wrapper)
    }

    private fun createSuggestionCard(label: String): TextView {
        return TextView(context).apply {
            text = label
            setTextColor(TEXT_PRIMARY)
            textSize = 15f
            gravity = Gravity.CENTER
            setBackgroundResource(android.R.color.transparent)
            setPadding(dp(20), dp(14), dp(20), dp(14))
            layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            setOnClickListener {
                onSuggestionClick?.invoke(label)
            }
            // Draw rounded border background programmatically
            background = createRoundedDrawable(SURFACE_ELEVATED, dp(12))
        }
    }

    private fun removeEmptyState() {
        if (messages.isEmpty() && container.childCount == 1) {
            container.removeAllViews()
        }
    }

    private fun rebuildMessageViews() {
        container.removeAllViews()
        if (messages.isEmpty()) {
            showEmptyState()
            return
        }
        for (msg in messages) {
            addMessageView(msg)
        }
        post { fullScroll(View.FOCUS_DOWN) }
    }

    private fun addMessageView(msg: Message) {
        val bubble = MessageBubble(context)
        val content = renderContent(msg.content, msg.isStreaming)
        bubble.setContent(content, msg.role == "user", msg.isStreaming)

        val params = LinearLayout.LayoutParams(
            (container.measuredWidth * 0.8).toInt().coerceAtLeast(dp(120)),
            LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(4)
            bottomMargin = dp(4)
            if (msg.role == "user") {
                leftMargin = container.measuredWidth - (container.measuredWidth * 0.8).toInt()
                if (leftMargin < dp(40)) leftMargin = dp(40)
                gravity = Gravity.END
            } else {
                leftMargin = dp(8)
                rightMargin = dp(40)
                gravity = Gravity.START
            }
        }
        container.addView(bubble, params)
    }

    private fun renderContent(text: String, streaming: Boolean): SpannableStringBuilder {
        val sb = SpannableStringBuilder()
        var i = 0
        val len = text.length

        while (i < len) {
            // Code block (```)
            if (i + 2 < len && text[i] == '`' && text[i + 1] == '`' && text[i + 2] == '`') {
                val end = text.indexOf("```", i + 3)
                if (end == -1) {
                    // Unclosed code block
                    val codeContent = text.substring(i + 3)
                    val start = sb.length
                    sb.append(codeContent)
                    sb.setSpan(TypefaceSpan("monospace"), start, sb.length, 0)
                    sb.setSpan(BackgroundColorSpan(SURFACE_ELEVATED), start, sb.length, 0)
                    sb.setSpan(ForegroundColorSpan(0xFFE6DB74.toInt()), start, sb.length, 0)
                    break
                } else {
                    val codeContent = text.substring(i + 3, end)
                    val start = sb.length
                    sb.append(codeContent)
                    sb.setSpan(TypefaceSpan("monospace"), start, sb.length, 0)
                    sb.setSpan(BackgroundColorSpan(SURFACE_ELEVATED), start, sb.length, 0)
                    sb.setSpan(ForegroundColorSpan(0xFFE6DB74.toInt()), start, sb.length, 0)
                    i = end + 3
                    if (i < len && text[i] == '\n') i++
                    continue
                }
            }

            // Inline code (`)
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end == -1) {
                    val codeContent = text.substring(i + 1)
                    val start = sb.length
                    sb.append(codeContent)
                    sb.setSpan(TypefaceSpan("monospace"), start, sb.length, 0)
                    sb.setSpan(BackgroundColorSpan(SURFACE_ELEVATED), start, sb.length, 0)
                    break
                } else {
                    val codeContent = text.substring(i + 1, end)
                    val start = sb.length
                    sb.append(codeContent)
                    sb.setSpan(TypefaceSpan("monospace"), start, sb.length, 0)
                    sb.setSpan(BackgroundColorSpan(0xFF2D2D30.toInt()), start, sb.length, 0)
                    sb.setSpan(ForegroundColorSpan(0xFFCE9178.toInt()), start, sb.length, 0)
                    i = end + 1
                    continue
                }
            }

            // Bold (**)
            if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end == -1) {
                    sb.append(text[i])
                    i++
                    continue
                } else {
                    val boldContent = text.substring(i + 2, end)
                    val start = sb.length
                    sb.append(boldContent)
                    sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, 0)
                    i = end + 2
                    continue
                }
            }

            // Newline
            if (text[i] == '\n') {
                sb.append('\n')
                i++
                continue
            }

            sb.append(text[i])
            i++
        }

        // Add streaming cursor if needed
        if (streaming) {
            val cursorPos = sb.length
            sb.append('▊')  // ▊
            sb.setSpan(object : android.text.style.ForegroundColorSpan(ACCENT) {
                // cursor visible simplified
            }, cursorPos, sb.length, 0)
        }

        return sb
    }

    private fun createRoundedDrawable(color: Int, radius: Int): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.style = android.graphics.Paint.Style.FILL
            }

            override fun draw(canvas: android.graphics.Canvas) {
                val r = radius.toFloat()
                canvas.drawRoundRect(
                    bounds.left.toFloat(), bounds.top.toFloat(),
                    bounds.right.toFloat(), bounds.bottom.toFloat(),
                    r, r, paint
                )
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(cf: android.graphics.ColorFilter?) { paint.colorFilter = cf }
            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    // Custom MessageBubble view
    private inner class MessageBubble(context: Context) : LinearLayout(context) {
        private val contentText = TextView(context)
        var isStreaming = false
        private var isUser = false

        init {
            orientation = VERTICAL
            val padding = dp(12)
            setPadding(padding, padding, padding, padding)
            contentText.setTextColor(TEXT_PRIMARY)
            contentText.textSize = 15f
            contentText.setLineSpacing(4f, 1.4f)
            addView(contentText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }

        fun setContent(text: SpannableStringBuilder, user: Boolean, streaming: Boolean) {
            isUser = user
            isStreaming = streaming
            contentText.text = text
            background = createRoundedDrawable(
                if (user) ACCENT else SURFACE,
                dp(12)
            )
            if (!user) {
                (background as? android.graphics.drawable.Drawable)?.let {
                    // Add border for assistant messages
                }
            }
        }

        fun updateContent(text: SpannableStringBuilder, streaming: Boolean) {
            isStreaming = streaming
            contentText.text = text
        }
    }
}
