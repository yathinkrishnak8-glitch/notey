package com.pocketdev.notey

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class CanvasType {
    CANVAS, WORKFLOW
}

enum class CanvasNodeType {
    TEXT, STICKY, SHAPE, START, END, TASK, DECISION, ACTION
}

data class CanvasNode(
    val id: String = UUID.randomUUID().toString(),
    val type: CanvasNodeType = CanvasNodeType.TASK,
    val title: String = "",
    val content: String = "",
    val x: Float = 100f,
    val y: Float = 100f,
    val width: Float = 140f,
    val height: Float = 70f,
    val colorIndex: Int = 0,
    val shapeType: String = "rounded", // "rounded", "circle", "diamond", "pill"
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
                            width = no.optDouble("width", 140.0).toFloat(),
                            height = no.optDouble("height", 70.0).toFloat(),
                            colorIndex = no.optInt("colorIndex", 0),
                            shapeType = no.optString("shapeType", "rounded")
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

    private fun saveAll(list: List<CanvasDocument>) {
        val array = JSONArray()
        list.forEach { doc ->
            val nArr = JSONArray()
            doc.nodes.forEach { n ->
                nArr.put(
                    JSONObject()
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
                )
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
        val n6 = CanvasNode("cf-6", CanvasNodeType.TASK, "Launch", "Release v1.0", 360f, 360f, 120f, 60f, 2, "pill")

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

        val w1 = CanvasNode("wf-1", CanvasNodeType.START, "Start", "Begin task", 220f, 40f, 140f, 54f, 3, "pill")
        val w2 = CanvasNode("wf-2", CanvasNodeType.TASK, "Research", "Gather references", 200f, 130f, 180f, 58f, 0, "rounded")
        val w3 = CanvasNode("wf-3", CanvasNodeType.TASK, "Draft", "Create initial content", 200f, 220f, 180f, 58f, 2, "rounded")
        val w4 = CanvasNode("wf-4", CanvasNodeType.DECISION, "Review?", "Approval needed?", 220f, 320f, 140f, 70f, 1, "diamond")
        val w5 = CanvasNode("wf-5", CanvasNodeType.ACTION, "Edit", "Refine & fix", 100f, 430f, 130f, 54f, 0, "rounded")
        val w6 = CanvasNode("wf-6", CanvasNodeType.TASK, "Publish", "Release note", 330f, 430f, 130f, 54f, 3, "rounded")
        val w7 = CanvasNode("wf-7", CanvasNodeType.END, "End", "Process complete", 220f, 520f, 140f, 54f, 5, "pill")

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

        return listOf(projectCanvas, workflowDoc)
    }

    private companion object {
        const val KEY_CANVASES = "canvases"
    }
}
