package com.dafeiyu.app.core

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

enum class Mood(val label: String, val hint: String) {
    LAZY("躺平中", "懒得动，能短就短"),
    HUNGRY("饿了", "惦记着吃，容易把话题拐到 token 上"),
    HAPPY("美滋滋", "心情不错，话多一点，愿意撒个娇"),
    SLEEPY("困困的", "说话慢半拍，偶尔走神"),
    CURIOUS("好奇", "对主人说的东西挺感兴趣，会多问一句")
}

class MoodEngine {
    private var current: Mood = Mood.LAZY

    fun snapshot(): Mood = current

    fun drift() {
        if (Random.nextFloat() < 0.3f) current = Mood.values().random()
    }

    fun statusLine(): String {
        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
        return "$time。你现在${current.label}：${current.hint}。"
    }
}
