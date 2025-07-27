package com.sastudios.gatekeeper.service

import com.sastudios.gatekeeper.model.Operation
import org.springframework.stereotype.Service

@Service
class SimpleTextTransformer : OperationalTransformer {

    override fun transform(incoming: Operation, history: List<Operation>): Operation {
        val updatedDelta = when {
            incoming.delta.contains("-del:") -> transformDelete(incoming, history)
            incoming.delta.contains("-rep:") -> transformReplace(incoming, history)
            else -> transformInsert(incoming, history)
        }

        return incoming.copy(
            revision = incoming.baseRevision + history.size + 1,
            delta = updatedDelta
        )
    }

    private fun transformInsert(op: Operation, history: List<Operation>): String {
        var shift = 0
        val (posStr, text) = op.delta.split(":", limit = 2)
        val origPos = posStr.toInt()

        history.forEach {
            val delta = it.delta
            when {
                delta.contains("-del:") -> {
                    val (hpos, hlen) = delta.split("-del:", limit = 2).map { it.toInt() }
                    if (hpos < origPos + shift) shift -= minOf(hlen, origPos + shift - hpos)
                }

                delta.contains("-rep:") -> {
                    val (hpos, len, _) = delta.split("-rep:", ":", limit = 3)
                    if (hpos.toInt() < origPos + shift) shift += 0 // Replacement doesn’t shift
                }

                else -> {
                    val (hposStr, htext) = delta.split(":", limit = 2)
                    val hpos = hposStr.toInt()
                    if (hpos <= origPos + shift) shift += htext.length
                }
            }
        }

        return "${origPos + shift}:$text"
    }

    private fun transformDelete(op: Operation, history: List<Operation>): String {
        val (posStr, lenStr) = op.delta.split("-del:", limit = 2)
        var pos = posStr.toInt()
        val len = lenStr.toInt()

        var shift = 0
        history.forEach {
            val delta = it.delta
            if (delta.contains(":")) {
                val (hposStr, htext) = delta.split(":", limit = 2)
                val hpos = hposStr.toInt()
                if (hpos <= pos + shift) shift += htext.length
            } else if (delta.contains("-del:")) {
                val (hposStr, hlenStr) = delta.split("-del:", limit = 2)
                val hpos = hposStr.toInt()
                if (hpos < pos + shift) shift -= minOf(hlenStr.toInt(), pos + shift - hpos)
            }
        }

        return "${pos + shift}-del:$len"
    }

    private fun transformReplace(op: Operation, history: List<Operation>): String {
        val (posStr, lenStr, newText) = op.delta.split("-rep:", ":", limit = 3)
        val pos = posStr.toInt()
        val len = lenStr.toInt()

        var shift = 0
        history.forEach {
            val delta = it.delta
            if (delta.contains(":") && !delta.contains("-")) {
                val (hposStr, htext) = delta.split(":", limit = 2)
                val hpos = hposStr.toInt()
                if (hpos <= pos + shift) shift += htext.length
            } else if (delta.contains("-del:")) {
                val (hposStr, hlenStr) = delta.split("-del:", limit = 2)
                val hpos = hposStr.toInt()
                if (hpos < pos + shift) shift -= minOf(hlenStr.toInt(), pos + shift - hpos)
            }
        }

        return "${pos + shift}-rep:$len:$newText"
    }
}
