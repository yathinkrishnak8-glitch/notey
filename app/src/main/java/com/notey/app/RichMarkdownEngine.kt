package com.notey.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Visual Transformation that hides raw Markdown symbols (**, *, ###, etc.)
 * and renders formatted styles (Bold, Italic, Heading, Bullet, Checkbox) directly.
 */
class RichMarkdownVisualTransformation(
    private val accentColor: Color = Color(0xFF38BDF8)
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder()
        val origToTrans = IntArray(original.length + 1)
        val transToOrigList = ArrayList<Int>()

        var origIndex = 0
        var transIndex = 0

        // Parse line by line
        val lines = original.split("\n")
        for (lineIdx in lines.indices) {
            val line = lines[lineIdx]
            val lineStartOrig = origIndex

            var lineProcessed = line
            var prefixOffsetInOrig = 0

            // Check line prefixes: "### ", "- [ ] ", "- [x] ", "- "
            val trimmedLeading = lineProcessed.trimStart()
            val leadingSpaces = lineProcessed.length - trimmedLeading.length

            if (leadingSpaces > 0) {
                for (s in 0 until leadingSpaces) {
                    origToTrans[origIndex] = transIndex
                    transToOrigList.add(origIndex)
                    builder.append(line[s])
                    origIndex++
                    transIndex++
                }
                lineProcessed = trimmedLeading
            }

            var lineStyle: SpanStyle? = null

            if (lineProcessed.startsWith("### ")) {
                // Heading: skip "### ", format line as bold accent 20sp
                lineStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = accentColor)
                val skipCount = 4
                for (k in 0 until skipCount) {
                    origToTrans[origIndex + k] = transIndex
                }
                origIndex += skipCount
                lineProcessed = lineProcessed.substring(skipCount)
            } else if (lineProcessed.startsWith("## ")) {
                lineStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, color = accentColor)
                val skipCount = 3
                for (k in 0 until skipCount) {
                    origToTrans[origIndex + k] = transIndex
                }
                origIndex += skipCount
                lineProcessed = lineProcessed.substring(skipCount)
            } else if (lineProcessed.startsWith("# ")) {
                lineStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = accentColor)
                val skipCount = 2
                for (k in 0 until skipCount) {
                    origToTrans[origIndex + k] = transIndex
                }
                origIndex += skipCount
                lineProcessed = lineProcessed.substring(skipCount)
            } else if (lineProcessed.startsWith("- [ ] ")) {
                // Open checklist: replace "- [ ] " with visual box "☐ "
                for (k in 0 until 6) {
                    origToTrans[origIndex + k] = transIndex
                }
                origIndex += 6
                origToTrans[origIndex] = transIndex
                transToOrigList.add(origIndex - 6)
                builder.append("☐ ")
                transIndex += 2
                transToOrigList.add(origIndex - 6)
                lineProcessed = lineProcessed.substring(6)
            } else if (lineProcessed.startsWith("- [x] ", ignoreCase = true)) {
                // Completed checklist: replace with "☑ "
                for (k in 0 until 6) {
                    origToTrans[origIndex + k] = transIndex
                }
                origIndex += 6
                origToTrans[origIndex] = transIndex
                transToOrigList.add(origIndex - 6)
                builder.append("☑ ")
                transIndex += 2
                transToOrigList.add(origIndex - 6)
                lineStyle = SpanStyle(color = Color(0xFF71717A), textDecoration = TextDecoration.LineThrough)
                lineProcessed = lineProcessed.substring(6)
            } else if (lineProcessed.startsWith("- ")) {
                // Bullet list: replace "- " with "• "
                for (k in 0 until 2) {
                    origToTrans[origIndex + k] = transIndex
                }
                origIndex += 2
                origToTrans[origIndex] = transIndex
                transToOrigList.add(origIndex - 2)
                builder.append("• ")
                transIndex += 2
                transToOrigList.add(origIndex - 2)
                lineProcessed = lineProcessed.substring(2)
            }

            // Inline formatting within line (bold `**`, italic `*`, inline code `` ` ``)
            var charIdx = 0
            val lineStartTrans = transIndex

            while (charIdx < lineProcessed.length) {
                // Check Bold **text**
                if (charIdx + 1 < lineProcessed.length && lineProcessed[charIdx] == '*' && lineProcessed[charIdx + 1] == '*') {
                    val closingBold = lineProcessed.indexOf("**", charIdx + 2)
                    if (closingBold != -1) {
                        // Skip opening **
                        origToTrans[origIndex] = transIndex
                        origToTrans[origIndex + 1] = transIndex
                        origIndex += 2
                        charIdx += 2

                        val boldStartTrans = transIndex
                        // Emit bold content
                        while (charIdx < closingBold) {
                            origToTrans[origIndex] = transIndex
                            transToOrigList.add(origIndex)
                            builder.append(lineProcessed[charIdx])
                            origIndex++
                            transIndex++
                            charIdx++
                        }
                        builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), boldStartTrans, transIndex)

                        // Skip closing **
                        origToTrans[origIndex] = transIndex
                        origToTrans[origIndex + 1] = transIndex
                        origIndex += 2
                        charIdx += 2
                        continue
                    }
                }

                // Check Italic *text*
                if (lineProcessed[charIdx] == '*' && (charIdx + 1 == lineProcessed.length || lineProcessed[charIdx + 1] != '*')) {
                    val closingItalic = lineProcessed.indexOf('*', charIdx + 1)
                    if (closingItalic != -1 && (closingItalic + 1 == lineProcessed.length || lineProcessed[closingItalic + 1] != '*')) {
                        // Skip opening *
                        origToTrans[origIndex] = transIndex
                        origIndex += 1
                        charIdx += 1

                        val italicStartTrans = transIndex
                        while (charIdx < closingItalic) {
                            origToTrans[origIndex] = transIndex
                            transToOrigList.add(origIndex)
                            builder.append(lineProcessed[charIdx])
                            origIndex++
                            transIndex++
                            charIdx++
                        }
                        builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic), italicStartTrans, transIndex)

                        // Skip closing *
                        origToTrans[origIndex] = transIndex
                        origIndex += 1
                        charIdx += 1
                        continue
                    }
                }

                // Normal character
                origToTrans[origIndex] = transIndex
                transToOrigList.add(origIndex)
                builder.append(lineProcessed[charIdx])
                origIndex++
                transIndex++
                charIdx++
            }

            if (lineStyle != null) {
                builder.addStyle(lineStyle, lineStartTrans, transIndex)
            }

            // Append newline between lines if not last
            if (lineIdx < lines.lastIndex) {
                origToTrans[origIndex] = transIndex
                transToOrigList.add(origIndex)
                builder.append('\n')
                origIndex++
                transIndex++
            }
        }

        // Cap arrays
        origToTrans[original.length] = transIndex
        transToOrigList.add(original.length)

        val transToOrig = transToOrigList.toIntArray()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, origToTrans.size - 1)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToOrig[offset.coerceIn(0, transToOrig.size - 1)]
            }
        }

        return TransformedText(builder.toAnnotatedString(), offsetMapping)
    }
}

/**
 * Controller for toggling Markdown formatting on text selections or current lines.
 */
object RichStyleController {

    /**
     * Check if Bold is active at current selection or cursor
     */
    fun isBoldActive(value: TextFieldValue): Boolean {
        val text = value.text
        val sel = value.selection
        if (sel.min < sel.max) {
            val selected = text.substring(sel.min, sel.max)
            if (selected.startsWith("**") && selected.endsWith("**") && selected.length >= 4) return true
            if (sel.min >= 2 && sel.max + 2 <= text.length) {
                if (text.substring(sel.min - 2, sel.min) == "**" && text.substring(sel.max, sel.max + 2) == "**") {
                    return true
                }
            }
            return false
        } else {
            // Check if cursor is enclosed by **
            val before = text.substring(0, sel.min)
            val after = text.substring(sel.min)
            val openIdx = before.lastIndexOf("**")
            val closeIdx = after.indexOf("**")
            return openIdx != -1 && closeIdx != -1 && !before.substring(openIdx + 2).contains("\n")
        }
    }

    /**
     * Toggle Bold on selected text or cursor
     */
    fun toggleBold(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val sel = value.selection

        if (sel.min < sel.max) {
            val start = sel.min
            val end = sel.max
            val selected = text.substring(start, end)

            // If selected text already has ** surrounded internally
            if (selected.startsWith("**") && selected.endsWith("**") && selected.length >= 4) {
                val unbolded = selected.substring(2, selected.length - 2)
                val newText = text.substring(0, start) + unbolded + text.substring(end)
                return TextFieldValue(newText, TextRange(start, start + unbolded.length))
            }

            // If selection is enclosed externally by **...**
            if (start >= 2 && end + 2 <= text.length &&
                text.substring(start - 2, start) == "**" &&
                text.substring(end, end + 2) == "**"
            ) {
                val newText = text.substring(0, start - 2) + selected + text.substring(end + 2)
                return TextFieldValue(newText, TextRange(start - 2, end - 2))
            }

            // Otherwise, bold the selection
            val bolded = "**$selected**"
            val newText = text.substring(0, start) + bolded + text.substring(end)
            return TextFieldValue(newText, TextRange(start, start + bolded.length))
        } else {
            // No selection: check if inside **
            val pos = sel.min
            val before = text.substring(0, pos)
            val after = text.substring(pos)
            val openIdx = before.lastIndexOf("**")
            val closeIdx = after.indexOf("**")

            if (openIdx != -1 && closeIdx != -1 && !before.substring(openIdx + 2).contains("\n")) {
                // Inside bold: strip **
                val absCloseIdx = pos + closeIdx
                val newText = text.substring(0, openIdx) +
                        text.substring(openIdx + 2, absCloseIdx) +
                        text.substring(absCloseIdx + 2)
                val newCursor = (pos - 2).coerceIn(0, newText.length)
                return TextFieldValue(newText, TextRange(newCursor))
            } else {
                // Insert empty bold token and place cursor in middle
                val newText = text.substring(0, pos) + "****" + text.substring(pos)
                return TextFieldValue(newText, TextRange(pos + 2))
            }
        }
    }

    /**
     * Check if Italic is active
     */
    fun isItalicActive(value: TextFieldValue): Boolean {
        val text = value.text
        val sel = value.selection
        if (sel.min < sel.max) {
            val selected = text.substring(sel.min, sel.max)
            if (selected.startsWith("*") && !selected.startsWith("**") && selected.endsWith("*") && !selected.endsWith("**") && selected.length >= 2) return true
            if (sel.min >= 1 && sel.max + 1 <= text.length) {
                val beforeChar = text[sel.min - 1]
                val afterChar = text[sel.max]
                if (beforeChar == '*' && afterChar == '*') {
                    val doubleBefore = sel.min >= 2 && text[sel.min - 2] == '*'
                    val doubleAfter = sel.max + 1 < text.length && text[sel.max + 1] == '*'
                    if (!doubleBefore && !doubleAfter) return true
                }
            }
            return false
        } else {
            val pos = sel.min
            val before = text.substring(0, pos)
            val after = text.substring(pos)
            val openIdx = before.lastIndexOf('*')
            val closeIdx = after.indexOf('*')
            return openIdx != -1 && closeIdx != -1 && !before.substring(openIdx + 1).contains("\n")
        }
    }

    /**
     * Toggle Italic on selected text or cursor
     */
    fun toggleItalic(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val sel = value.selection

        if (sel.min < sel.max) {
            val start = sel.min
            val end = sel.max
            val selected = text.substring(start, end)

            if (selected.startsWith("*") && !selected.startsWith("**") && selected.endsWith("*") && !selected.endsWith("**") && selected.length >= 2) {
                val unitalic = selected.substring(1, selected.length - 1)
                val newText = text.substring(0, start) + unitalic + text.substring(end)
                return TextFieldValue(newText, TextRange(start, start + unitalic.length))
            }

            if (start >= 1 && end + 1 <= text.length && text[start - 1] == '*' && text[end] == '*') {
                val doubleBefore = start >= 2 && text[start - 2] == '*'
                val doubleAfter = end + 1 < text.length && text[end + 1] == '*'
                if (!doubleBefore && !doubleAfter) {
                    val newText = text.substring(0, start - 1) + selected + text.substring(end + 1)
                    return TextFieldValue(newText, TextRange(start - 1, end - 1))
                }
            }

            val italicized = "*$selected*"
            val newText = text.substring(0, start) + italicized + text.substring(end)
            return TextFieldValue(newText, TextRange(start, start + italicized.length))
        } else {
            val pos = sel.min
            val newText = text.substring(0, pos) + "**" + text.substring(pos)
            return TextFieldValue(newText, TextRange(pos + 1))
        }
    }

    /**
     * Check if Heading is active on current line
     */
    fun isHeadingActive(value: TextFieldValue): Boolean {
        val line = getCurrentLine(value)
        return line.trimStart().startsWith("#")
    }

    /**
     * Toggle Heading (###) on current line
     */
    fun toggleHeading(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val lineRange = getCurrentLineRange(value)
        val line = text.substring(lineRange.first, lineRange.second)

        val newLine = if (line.trimStart().startsWith("### ")) {
            line.replaceFirst("### ", "")
        } else if (line.trimStart().startsWith("## ")) {
            line.replaceFirst("## ", "")
        } else if (line.trimStart().startsWith("# ")) {
            line.replaceFirst("# ", "")
        } else {
            "### $line"
        }

        val newText = text.substring(0, lineRange.first) + newLine + text.substring(lineRange.second)
        val delta = newLine.length - line.length
        val newCursor = (value.selection.min + delta).coerceIn(0, newText.length)
        return TextFieldValue(newText, TextRange(newCursor))
    }

    /**
     * Check if Bullet List is active on current line
     */
    fun isListActive(value: TextFieldValue): Boolean {
        val line = getCurrentLine(value).trimStart()
        return line.startsWith("- ") && !line.startsWith("- [")
    }

    /**
     * Toggle Bullet List on current line
     */
    fun toggleList(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val lineRange = getCurrentLineRange(value)
        val line = text.substring(lineRange.first, lineRange.second)
        val trimmed = line.trimStart()

        val newLine = if (trimmed.startsWith("- ") && !trimmed.startsWith("- [")) {
            line.replaceFirst("- ", "")
        } else if (trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ")) {
            line.replaceFirst(Regex("^-\\s*\\[[ xX]\\]\\s*"), "- ")
        } else {
            "- $line"
        }

        val newText = text.substring(0, lineRange.first) + newLine + text.substring(lineRange.second)
        val delta = newLine.length - line.length
        val newCursor = (value.selection.min + delta).coerceIn(0, newText.length)
        return TextFieldValue(newText, TextRange(newCursor))
    }

    /**
     * Check if Checklist Task is active on current line
     */
    fun isChecklistActive(value: TextFieldValue): Boolean {
        val line = getCurrentLine(value).trimStart()
        return line.startsWith("- [ ]") || line.startsWith("- [x]", ignoreCase = true)
    }

    /**
     * Toggle Checklist Task on current line
     */
    fun toggleChecklist(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val lineRange = getCurrentLineRange(value)
        val line = text.substring(lineRange.first, lineRange.second)
        val trimmed = line.trimStart()

        val newLine = if (trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ", ignoreCase = true)) {
            line.replaceFirst(Regex("^-\\s*\\[[ xX]\\]\\s*"), "")
        } else if (trimmed.startsWith("- ")) {
            line.replaceFirst("- ", "- [ ] ")
        } else {
            "- [ ] $line"
        }

        val newText = text.substring(0, lineRange.first) + newLine + text.substring(lineRange.second)
        val delta = newLine.length - line.length
        val newCursor = (value.selection.min + delta).coerceIn(0, newText.length)
        return TextFieldValue(newText, TextRange(newCursor))
    }

    /**
     * Check if Code is active
     */
    fun isCodeActive(value: TextFieldValue): Boolean {
        val text = value.text
        val pos = value.selection.min
        val beforeCount = text.substring(0, pos).split("```").size - 1
        return beforeCount % 2 == 1
    }

    /**
     * Toggle Code Block on selection or cursor
     */
    fun toggleCode(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val sel = value.selection

        if (sel.min < sel.max) {
            val start = sel.min
            val end = sel.max
            val selected = text.substring(start, end)
            if (selected.startsWith("```") && selected.endsWith("```") && selected.length >= 6) {
                val uncode = selected.removePrefix("```").removeSuffix("```").trim('\n')
                val newText = text.substring(0, start) + uncode + text.substring(end)
                return TextFieldValue(newText, TextRange(start, start + uncode.length))
            } else {
                val coded = "\n```\n$selected\n```\n"
                val newText = text.substring(0, start) + coded + text.substring(end)
                return TextFieldValue(newText, TextRange(start, start + coded.length))
            }
        } else {
            val pos = sel.min
            val coded = "\n```\n\n```\n"
            val newText = text.substring(0, pos) + coded + text.substring(pos)
            return TextFieldValue(newText, TextRange(pos + 5))
        }
    }

    private fun getCurrentLine(value: TextFieldValue): String {
        val text = value.text
        val pos = value.selection.min.coerceIn(0, text.length)
        val start = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val end = text.indexOf('\n', pos).let { if (it == -1) text.length else it }
        return text.substring(start, end)
    }

    private fun getCurrentLineRange(value: TextFieldValue): Pair<Int, Int> {
        val text = value.text
        val pos = value.selection.min.coerceIn(0, text.length)
        val start = text.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val end = text.indexOf('\n', pos).let { if (it == -1) text.length else it }
        return Pair(start, end)
    }
}
