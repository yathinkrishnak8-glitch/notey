package com.pocketdev.notey

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LogoProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "New Brand Identity",
    val symbolAssetId: String = "logo_001",
    val brandText: String = "Brand",
    val taglineText: String = "Studio & Craft",
    val layoutMode: String = "VERTICAL", // "VERTICAL", "HORIZONTAL", "SYMBOL_ONLY", "BADGE"
    val symbolColorHex: String = "#4F46E5",
    val textColorHex: String = "#0F172A",
    val taglineColorHex: String = "#64748B",
    val bgShape: String = "ROUNDED", // "NONE", "ROUNDED", "CIRCLE", "SHIELD", "HEXAGON"
    val bgColorHex: String = "#F8FAFC",
    val symbolSize: Int = 72,
    val fontSize: Int = 24,
    val spacing: Int = 12,
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

class LogoProjectStore(context: Context) {
    private val prefs = context.getSharedPreferences("notey_logos", Context.MODE_PRIVATE)

    fun loadAll(): List<LogoProject> {
        val raw = prefs.getString("logo_projects", null)
        if (raw.isNullOrBlank()) {
            val defaults = listOf(
                LogoProject(
                    id = "sample-logo-1",
                    name = "Nexus Creative",
                    symbolAssetId = "logo_001",
                    brandText = "Nexus",
                    taglineText = "Design Studio",
                    layoutMode = "VERTICAL",
                    symbolColorHex = "#6366F1",
                    textColorHex = "#0F172A",
                    taglineColorHex = "#64748B",
                    bgShape = "ROUNDED",
                    bgColorHex = "#F1F5F9",
                    symbolSize = 72,
                    fontSize = 24,
                    spacing = 10,
                    isFavorite = true
                ),
                LogoProject(
                    id = "sample-logo-2",
                    name = "Orbit Tech",
                    symbolAssetId = "logo_003",
                    brandText = "Orbit",
                    taglineText = "Cloud Intelligence",
                    layoutMode = "HORIZONTAL",
                    symbolColorHex = "#06B6D4",
                    textColorHex = "#0F172A",
                    taglineColorHex = "#64748B",
                    bgShape = "NONE",
                    bgColorHex = "#FFFFFF",
                    symbolSize = 56,
                    fontSize = 22,
                    spacing = 12
                )
            )
            saveAll(defaults)
            return defaults
        }

        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<LogoProject>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list += LogoProject(
                    id = o.getString("id"),
                    name = o.optString("name", "Untitled Logo"),
                    symbolAssetId = o.optString("symbolAssetId", "logo_001"),
                    brandText = o.optString("brandText", "Brand"),
                    taglineText = o.optString("taglineText", ""),
                    layoutMode = o.optString("layoutMode", "VERTICAL"),
                    symbolColorHex = o.optString("symbolColorHex", "#4F46E5"),
                    textColorHex = o.optString("textColorHex", "#0F172A"),
                    taglineColorHex = o.optString("taglineColorHex", "#64748B"),
                    bgShape = o.optString("bgShape", "ROUNDED"),
                    bgColorHex = o.optString("bgColorHex", "#F8FAFC"),
                    symbolSize = o.optInt("symbolSize", 72),
                    fontSize = o.optInt("fontSize", 24),
                    spacing = o.optInt("spacing", 12),
                    isFavorite = o.optBoolean("isFavorite", false),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            }
            list.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun save(project: LogoProject) {
        val current = loadAll().filter { it.id != project.id }
        val updated = listOf(project.copy(updatedAt = System.currentTimeMillis())) + current
        saveAll(updated)
    }

    fun delete(id: String) {
        val filtered = loadAll().filter { it.id != id }
        saveAll(filtered)
    }

    fun duplicate(id: String): LogoProject? {
        val original = loadAll().firstOrNull { it.id == id } ?: return null
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            updatedAt = System.currentTimeMillis()
        )
        save(copy)
        return copy
    }

    fun rename(id: String, newName: String): LogoProject? {
        val proj = loadAll().firstOrNull { it.id == id } ?: return null
        val updated = proj.copy(name = newName.trim(), updatedAt = System.currentTimeMillis())
        save(updated)
        return updated
    }

    fun toggleFavorite(id: String) {
        val proj = loadAll().firstOrNull { it.id == id } ?: return
        save(proj.copy(isFavorite = !proj.isFavorite))
    }

    private fun saveAll(list: List<LogoProject>) {
        val array = JSONArray()
        list.forEach { p ->
            array.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("symbolAssetId", p.symbolAssetId)
                    .put("brandText", p.brandText)
                    .put("taglineText", p.taglineText)
                    .put("layoutMode", p.layoutMode)
                    .put("symbolColorHex", p.symbolColorHex)
                    .put("textColorHex", p.textColorHex)
                    .put("taglineColorHex", p.taglineColorHex)
                    .put("bgShape", p.bgShape)
                    .put("bgColorHex", p.bgColorHex)
                    .put("symbolSize", p.symbolSize)
                    .put("fontSize", p.fontSize)
                    .put("spacing", p.spacing)
                    .put("isFavorite", p.isFavorite)
                    .put("updatedAt", p.updatedAt)
            )
        }
        prefs.edit().putString("logo_projects", array.toString()).apply()
    }
}
