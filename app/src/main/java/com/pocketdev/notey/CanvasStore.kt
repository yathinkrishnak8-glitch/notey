package com.pocketdev.notey

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class CanvasType {
    CANVAS, WORKFLOW, MIND_MAP, PLANNING
}

enum class CanvasNodeType {
    TEXT, STICKY, SHAPE, NOTE, FOLDER, IMAGE, VIDEO, FILE, LINK, ASSET, START, END, TASK, DECISION, ACTION
}

data class CanvasNode(
    val id: String = UUID.randomUUID().toString(),
    val type: CanvasNodeType = CanvasNodeType.TASK,
    val title: String = "",
    val content: String = "",
    val x: Float = 100f,
    val y: Float = 100f,
    val width: Float = 150f,
    val height: Float = 80f,
    val colorIndex: Int = 0,
    val shapeType: String = "rounded", // "rounded", "circle", "diamond", "pill", "rect"
    val mediaPath: String = "",
    val mediaType: String = "", // "image", "video", "file"
    val zIndex: Int = 0,
    val noteId: Long? = null,
    val folderName: String? = null,
    val assetId: String? = null,
    val linkUrl: String? = null,
)

data class CanvasConnector(
    val id: String = UUID.randomUUID().toString(),
    val fromNodeId: String,
    val toNodeId: String,
    val label: String = "",
    val colorIndex: Int = 0,
)

data class CanvasDocument(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: CanvasType = CanvasType.CANVAS,
    val nodes: List<CanvasNode> = emptyList(),
    val connectors: List<CanvasConnector> = emptyList(),
    val zoom: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val updatedAt: Long = System.currentTimeMillis(),
)

class CanvasStore(context: Context) {
    private val prefs = context.getSharedPreferences("notey_canvases", Context.MODE_PRIVATE)

    fun loadAll(): List<CanvasDocument> {
        val raw = prefs.getString(KEY_CANVASES, null)
        if (raw.isNullOrBlank()) {
            val defaults = createSampleDocuments()
            saveAll(defaults)
            return defaults
        }

        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<CanvasDocument>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val typeStr = o.optString("type", CanvasType.CANVAS.name)
                val type = try { CanvasType.valueOf(typeStr) } catch (_: Exception) { CanvasType.CANVAS }

                val nodesList = mutableListOf<CanvasNode>()
                if (o.has("nodes")) {
                    val nArr = o.getJSONArray("nodes")
                    for (j in 0 until nArr.length()) {
                        val no = nArr.getJSONObject(j)
                        val nTypeStr = no.optString("type", CanvasNodeType.TASK.name)
                        val nType = try { CanvasNodeType.valueOf(nTypeStr) } catch (_: Exception) { CanvasNodeType.TASK }
                        nodesList += CanvasNode(
                            id = no.getString("id"),
                            type = nType,
                            title = no.optString("title", ""),
                            content = no.optString("content", ""),
                            x = no.optDouble("x", 100.0).toFloat(),
                            y = no.optDouble("y", 100.0).toFloat(),
                            width = no.optDouble("width", 150.0).toFloat(),
                            height = no.optDouble("height", 80.0).toFloat(),
                            colorIndex = no.optInt("colorIndex", 0),
                            shapeType = no.optString("shapeType", "rounded"),
                            mediaPath = no.optString("mediaPath", ""),
                            mediaType = no.optString("mediaType", ""),
                            zIndex = no.optInt("zIndex", 0),
                            noteId = if (no.has("noteId") && !no.isNull("noteId")) no.getLong("noteId") else null,
                            folderName = if (no.has("folderName") && !no.isNull("folderName")) no.getString("folderName") else null,
                            assetId = if (no.has("assetId") && !no.isNull("assetId")) no.getString("assetId") else null,
                            linkUrl = if (no.has("linkUrl") && !no.isNull("linkUrl")) no.getString("linkUrl") else null,
                        )
                    }
                }

                val connList = mutableListOf<CanvasConnector>()
                if (o.has("connectors")) {
                    val cArr = o.getJSONArray("connectors")
                    for (k in 0 until cArr.length()) {
                        val co = cArr.getJSONObject(k)
                        connList += CanvasConnector(
                            id = co.getString("id"),
                            fromNodeId = co.getString("fromNodeId"),
                            toNodeId = co.getString("toNodeId"),
                            label = co.optString("label", ""),
                            colorIndex = co.optInt("colorIndex", 0)
                        )
                    }
                }

                list += CanvasDocument(
                    id = o.getString("id"),
                    name = o.optString("name", "Untitled"),
                    type = type,
                    nodes = nodesList,
                    connectors = connList,
                    zoom = o.optDouble("zoom", 1.0).toFloat(),
                    panX = o.optDouble("panX", 0.0).toFloat(),
                    panY = o.optDouble("panY", 0.0).toFloat(),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                )
            }
            list.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            e.printStackTrace()
            createSampleDocuments()
        }
    }

    fun save(doc: CanvasDocument) {
        val current = loadAll().filter { it.id != doc.id }
        val updated = listOf(doc.copy(updatedAt = System.currentTimeMillis())) + current
        saveAll(updated)
    }

    fun delete(id: String) {
        val filtered = loadAll().filter { it.id != id }
        saveAll(filtered)
    }

    fun duplicate(id: String): CanvasDocument? {
        val original = loadAll().firstOrNull { it.id == id } ?: return null
        val newDocId = UUID.randomUUID().toString()
        val nodeMap = mutableMapOf<String, String>()
        val duplicatedNodes = original.nodes.map { node ->
            val newId = UUID.randomUUID().toString()
            nodeMap[node.id] = newId
            node.copy(id = newId)
        }
        val duplicatedConnectors = original.connectors.mapNotNull { conn ->
            val newFrom = nodeMap[conn.fromNodeId]
            val newTo = nodeMap[conn.toNodeId]
            if (newFrom != null && newTo != null) {
                conn.copy(id = UUID.randomUUID().toString(), fromNodeId = newFrom, toNodeId = newTo)
            } else null
        }
        val duplicatedDoc = original.copy(
            id = newDocId,
            name = "${original.name} (Copy)",
            nodes = duplicatedNodes,
            connectors = duplicatedConnectors,
            updatedAt = System.currentTimeMillis()
        )
        save(duplicatedDoc)
        return duplicatedDoc
    }

    fun rename(id: String, newName: String): CanvasDocument? {
        val doc = loadAll().firstOrNull { it.id == id } ?: return null
        val updated = doc.copy(name = newName.trim(), updatedAt = System.currentTimeMillis())
        save(updated)
        return updated
    }

    private fun saveAll(list: List<CanvasDocument>) {
        val array = JSONArray()
        list.forEach { doc ->
            val nArr = JSONArray()
            doc.nodes.forEach { n ->
                val no = JSONObject()
                    .put("id", n.id)
                    .put("type", n.type.name)
                    .put("title", n.title)
                    .put("content", n.content)
                    .put("x", n.x.toDouble())
                    .put("y", n.y.toDouble())
                    .put("width", n.width.toDouble())
                    .put("height", n.height.toDouble())
                    .put("colorIndex", n.colorIndex)
                    .put("shapeType", n.shapeType)
                    .put("mediaPath", n.mediaPath)
                    .put("mediaType", n.mediaType)
                    .put("zIndex", n.zIndex)

                if (n.noteId != null) no.put("noteId", n.noteId)
                if (n.folderName != null) no.put("folderName", n.folderName)
                if (n.assetId != null) no.put("assetId", n.assetId)
                if (n.linkUrl != null) no.put("linkUrl", n.linkUrl)

                nArr.put(no)
            }

            val cArr = JSONArray()
            doc.connectors.forEach { c ->
                cArr.put(
                    JSONObject()
                        .put("id", c.id)
                        .put("fromNodeId", c.fromNodeId)
                        .put("toNodeId", c.toNodeId)
                        .put("label", c.label)
                        .put("colorIndex", c.colorIndex)
                )
            }

            array.put(
                JSONObject()
                    .put("id", doc.id)
                    .put("name", doc.name)
                    .put("type", doc.type.name)
                    .put("nodes", nArr)
                    .put("connectors", cArr)
                    .put("zoom", doc.zoom.toDouble())
                    .put("panX", doc.panX.toDouble())
                    .put("panY", doc.panY.toDouble())
                    .put("updatedAt", doc.updatedAt)
            )
        }
        prefs.edit().putString(KEY_CANVASES, array.toString()).apply()
    }

    private fun createSampleDocuments(): List<CanvasDocument> {
        val n1 = CanvasNode("cf-1", CanvasNodeType.STICKY, "Idea", "Project brainstorming", 240f, 60f, 130f, 60f, 4, "pill")
        val n2 = CanvasNode("cf-2", CanvasNodeType.SHAPE, "Plan", "Architectural map", 120f, 160f, 120f, 60f, 3, "rounded")
        val n3 = CanvasNode("cf-3", CanvasNodeType.SHAPE, "Design", "M3 Glass System", 360f, 160f, 120f, 60f, 1, "rounded")
        val n4 = CanvasNode("cf-4", CanvasNodeType.TASK, "Development", "Jetpack Compose", 220f, 260f, 160f, 60f, 4, "rounded")
        val n5 = CanvasNode("cf-5", CanvasNodeType.TASK, "Testing", "Unit & UI tests", 120f, 360f, 120f, 60f, 3, "pill")
        val n6 = CanvasNode("cf-6", CanvasNodeType.TASK, "Launch", "Release v1.2", 360f, 360f, 120f, 60f, 2, "pill")

        val c1 = CanvasConnector("cc-1", "cf-1", "cf-2", "", 0)
        val c2 = CanvasConnector("cc-2", "cf-1", "cf-3", "", 0)
        val c3 = CanvasConnector("cc-3", "cf-2", "cf-4", "", 0)
        val c4 = CanvasConnector("cc-4", "cf-3", "cf-4", "", 0)
        val c5 = CanvasConnector("cc-5", "cf-4", "cf-5", "", 0)
        val c6 = CanvasConnector("cc-6", "cf-4", "cf-6", "", 0)

        val projectCanvas = CanvasDocument(
            id = "sample-canvas-1",
            name = "Project Flow",
            type = CanvasType.CANVAS,
            nodes = listOf(n1, n2, n3, n4, n5, n6),
            connectors = listOf(c1, c2, c3, c4, c5, c6),
            updatedAt = System.currentTimeMillis()
        )

        val w1 = CanvasNode("wf-1", CanvasNodeType.START, "Start", "Begin task", 220f, 80f, 140f, 54f, 3, "pill")
        val w2 = CanvasNode("wf-2", CanvasNodeType.TASK, "Research", "Gather references", 200f, 180f, 180f, 58f, 0, "rounded")
        val w3 = CanvasNode("wf-3", CanvasNodeType.TASK, "Draft", "Create initial content", 200f, 280f, 180f, 58f, 2, "rounded")
        val w4 = CanvasNode("wf-4", CanvasNodeType.DECISION, "Review?", "Approval needed?", 220f, 380f, 140f, 70f, 1, "diamond")
        val w5 = CanvasNode("wf-5", CanvasNodeType.ACTION, "Revise", "Refine & fix", 100f, 500f, 130f, 54f, 0, "rounded")
        val w6 = CanvasNode("wf-6", CanvasNodeType.TASK, "Publish", "Release note", 330f, 500f, 130f, 54f, 3, "rounded")
        val w7 = CanvasNode("wf-7", CanvasNodeType.END, "End", "Process complete", 220f, 610f, 140f, 54f, 5, "pill")

        val wc1 = CanvasConnector("wfc-1", "wf-1", "wf-2", "", 0)
        val wc2 = CanvasConnector("wfc-2", "wf-2", "wf-3", "", 0)
        val wc3 = CanvasConnector("wfc-3", "wf-3", "wf-4", "", 0)
        val wc4 = CanvasConnector("wfc-4", "wf-4", "wf-6", "Yes", 3)
        val wc5 = CanvasConnector("wfc-5", "wf-4", "wf-5", "No", 5)
        val wc6 = CanvasConnector("wfc-6", "wf-5", "wf-4", "", 0)
        val wc7 = CanvasConnector("wfc-7", "wf-6", "wf-7", "", 0)

        val workflowDoc = CanvasDocument(
            id = "sample-workflow-1",
            name = "Content Creation Workflow",
            type = CanvasType.WORKFLOW,
            nodes = listOf(w1, w2, w3, w4, w5, w6, w7),
            connectors = listOf(wc1, wc2, wc3, wc4, wc5, wc6, wc7),
            updatedAt = System.currentTimeMillis() - 3600000L
        )

        val m1 = CanvasNode("mm-1", CanvasNodeType.STICKY, "Central Concept", "Core initiative", 240f, 180f, 160f, 70f, 1, "rounded")
        val m2 = CanvasNode("mm-2", CanvasNodeType.TASK, "Market Discovery", "Customer interviews", 80f, 60f, 140f, 56f, 0, "rounded")
        val m3 = CanvasNode("mm-3", CanvasNodeType.TASK, "Tech Stack", "Compose & Kotlin", 400f, 60f, 140f, 56f, 2, "rounded")
        val m4 = CanvasNode("mm-4", CanvasNodeType.TASK, "UX Prototype", "Figma & Micro-flows", 80f, 300f, 140f, 56f, 3, "rounded")
        val m5 = CanvasNode("mm-5", CanvasNodeType.TASK, "Release Road", "App release milestones", 400f, 300f, 140f, 56f, 4, "rounded")

        val mc1 = CanvasConnector("mmc-1", "mm-1", "mm-2", "Phase 1", 0)
        val mc2 = CanvasConnector("mmc-2", "mm-1", "mm-3", "Architecture", 2)
        val mc3 = CanvasConnector("mmc-3", "mm-1", "mm-4", "Design", 3)
        val mc4 = CanvasConnector("mmc-4", "mm-1", "mm-5", "Delivery", 4)

        val mindMapDoc = CanvasDocument(
            id = "sample-mindmap-1",
            name = "Product Vision Map",
            type = CanvasType.MIND_MAP,
            nodes = listOf(m1, m2, m3, m4, m5),
            connectors = listOf(mc1, mc2, mc3, mc4),
            updatedAt = System.currentTimeMillis() - 7200000L
        )

        return listOf(projectCanvas, workflowDoc, mindMapDoc)
    }

    private companion object {
        const val KEY_CANVASES = "canvases"
    }
}
