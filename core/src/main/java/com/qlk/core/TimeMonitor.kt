package com.qlk.core

/**
 * QQ:1055329812
 * Created by QiLiKing on 2019-08-04.
 */
class TimeMonitor(private val tag: String) {
    private val piece = if (tag.contains("Fragment")) 80 else if (tag.contains("Activity")) 160 else 40
    private var timeMillis: Long = 0
    private val interval: MutableList<Long> = mutableListOf()
    private val sb = StringBuilder()
    private var startTag: String? = null

    fun reset() {
        timeMillis = 0
        interval.clear()
    }

    fun start(startTag: String? = null) {
        this.startTag = startTag
        timeMillis = System.currentTimeMillis()
    }

    fun pause() {
        interval.add(System.currentTimeMillis() - timeMillis)
    }

    fun printAndRestart(stopTag: String? = null, customMessage: String? = null) {
        interval.add(System.currentTimeMillis() - timeMillis)
        val duration = interval.sum()
        println("TimeMonitor#stopAndPrint:26-> $tag ${assembleMessage(startTag, stopTag, customMessage)} use $duration milliseconds${level(duration)}")
        reset()
        start(stopTag)
    }

    fun printAndReset(stopTag: String? = null, customMessage: String? = null) {
        interval.add(System.currentTimeMillis() - timeMillis)
        val duration = interval.sum()
        println("TimeMonitor#stopAndPrint:26-> $tag ${assembleMessage(startTag, stopTag, customMessage)} use $duration milliseconds${level(duration)}")
        reset()
    }

    private fun assembleMessage(startTag: String?, stopTag: String?, customMessage: String?): String =
        sb.apply {
            setLength(0)
            if (customMessage != null) {
                append(" ")
                append(customMessage)
                append(" ")
            }
            if (startTag != null && stopTag != null) {
                append(" ")
                append("from ")
                append(startTag)
                append(" to ")
                append(stopTag)
                append(" ")
            } else if (startTag != null) {
                append(" ")
                append(startTag)
                append(" ")
            } else {
                append(" ")
                append(stopTag)
                append(" ")
            }
        }.toString()

    private fun level(duration: Long): String =
        sb.apply {
            setLength(0)
            repeat((duration / piece).toInt()) {
                append("!")
            }
        }.toString()
}