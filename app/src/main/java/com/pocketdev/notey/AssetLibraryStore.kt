package com.pocketdev.notey

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import kotlin.math.*

enum class AssetCategory(val displayName: String, val expectedCount: Int) {
    INTERFACE("Interface Icons", 300),
    LOGOS("Logos & Branding", 100),
    WORKFLOW("Workflow & Diagrams", 100),
    ILLUSTRATIONS("Illustrations", 50),
    SHAPES("Shapes & Elements", 50)
}

data class VisualAsset(
    val id: String,
    val name: String,
    val category: AssetCategory,
    val tags: List<String>,
    val drawType: String
)

class AssetLibraryStore(context: Context) {
    private val prefs = context.getSharedPreferences("notey_assets", Context.MODE_PRIVATE)

    val allAssets: List<VisualAsset> by lazy {
        generateAllAssets()
    }

    fun search(query: String, category: AssetCategory? = null): List<VisualAsset> {
        val q = query.trim().lowercase()
        return allAssets.filter { asset ->
            val matchCat = category == null || asset.category == category
            val matchQuery = q.isEmpty() ||
                    asset.name.lowercase().contains(q) ||
                    asset.id.lowercase().contains(q) ||
                    asset.tags.any { it.lowercase().contains(q) }
            matchCat && matchQuery
        }
    }

    fun getFavorites(): Set<String> {
        return prefs.getStringSet("favorite_assets", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(assetId: String) {
        val current = getFavorites().toMutableSet()
        if (current.contains(assetId)) {
            current.remove(assetId)
        } else {
            current.add(assetId)
        }
        prefs.edit().putStringSet("favorite_assets", current).apply()
    }

    fun isFavorite(assetId: String): Boolean {
        return getFavorites().contains(assetId)
    }

    fun getRecentAssets(): List<VisualAsset> {
        val raw = prefs.getString("recent_assets", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val ids = mutableListOf<String>()
            for (i in 0 until array.length()) {
                ids.add(array.getString(i))
            }
            ids.mapNotNull { id -> allAssets.firstOrNull { it.id == id } }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun recordRecent(assetId: String) {
        val current = getRecentAssets().map { it.id }.filter { it != assetId }.toMutableList()
        current.add(0, assetId)
        val trimmed = current.take(30)
        val arr = JSONArray()
        trimmed.forEach { arr.put(it) }
        prefs.edit().putString("recent_assets", arr.toString()).apply()
    }

    private fun generateAllAssets(): List<VisualAsset> {
        val list = mutableListOf<VisualAsset>()

        // 1. INTERFACE ICONS (300 items: ui_001 to ui_300)
        val uiDefinitions = listOf(
            // Navigation & Base (30)
            "home" to listOf("main", "dashboard", "house"),
            "search" to listOf("find", "magnifier", "lookup"),
            "settings" to listOf("gear", "preferences", "config"),
            "menu" to listOf("hamburger", "sidebar", "options"),
            "close" to listOf("cancel", "dismiss", "x"),
            "check" to listOf("done", "tick", "success"),
            "back" to listOf("arrow", "previous", "return"),
            "forward" to listOf("arrow", "next", "continue"),
            "expand" to listOf("fullscreen", "open", "arrows"),
            "collapse" to listOf("minimize", "shrink", "arrows"),
            "filter" to listOf("sort", "refine", "funnel"),
            "sort" to listOf("order", "arrange", "sequence"),
            "refresh" to listOf("reload", "sync", "cycle"),
            "more_horiz" to listOf("dots", "actions", "menu"),
            "more_vert" to listOf("dots", "actions", "overflow"),
            "grid" to listOf("layout", "gallery", "tiles"),
            "list" to listOf("layout", "rows", "menu"),
            "dashboard" to listOf("widgets", "panel", "board"),
            "external_link" to listOf("open", "redirect", "url"),
            "link" to listOf("chain", "hyperlink", "url"),
            "lock" to listOf("secure", "password", "closed"),
            "unlock" to listOf("open", "access", "unsecure"),
            "visibility" to listOf("eye", "show", "preview"),
            "visibility_off" to listOf("hide", "hidden", "private"),
            "help" to listOf("question", "info", "support"),
            "info" to listOf("detail", "about", "note"),
            "warning" to listOf("alert", "caution", "triangle"),
            "error" to listOf("danger", "alert", "circle"),
            "star" to listOf("favorite", "rate", "rating"),
            "heart" to listOf("love", "like", "favorite"),

            // Documents & Content (30)
            "file" to listOf("doc", "document", "paper"),
            "file_text" to listOf("notes", "writing", "article"),
            "file_code" to listOf("programming", "developer", "source"),
            "file_image" to listOf("photo", "picture", "asset"),
            "file_video" to listOf("movie", "clip", "media"),
            "file_audio" to listOf("music", "sound", "voice"),
            "file_pdf" to listOf("portable", "document", "reader"),
            "file_zip" to listOf("archive", "compressed", "bundle"),
            "file_check" to listOf("verified", "approved", "done"),
            "file_plus" to listOf("new", "add", "create"),
            "folder" to listOf("directory", "container", "storage"),
            "folder_open" to listOf("browse", "active", "directory"),
            "folder_plus" to listOf("new", "add", "create"),
            "folder_user" to listOf("shared", "profile", "directory"),
            "folder_lock" to listOf("private", "secure", "encrypted"),
            "folder_star" to listOf("favorite", "important", "pinned"),
            "archive" to listOf("box", "backup", "store"),
            "copy" to listOf("duplicate", "clone", "clipboard"),
            "cut" to listOf("scissors", "remove", "snip"),
            "paste" to listOf("clipboard", "insert", "apply"),
            "edit" to listOf("pencil", "modify", "write"),
            "trash" to listOf("delete", "remove", "bin"),
            "save" to listOf("floppy", "store", "persist"),
            "share" to listOf("export", "publish", "distribute"),
            "download" to listOf("receive", "save", "fetch"),
            "upload" to listOf("send", "cloud", "publish"),
            "bookmark" to listOf("saved", "marker", "read_later"),
            "tag" to listOf("label", "category", "badge"),
            "pin" to listOf("sticky", "top", "fasten"),
            "flag" to listOf("mark", "milestone", "priority"),

            // Media & Audio (30)
            "play" to listOf("start", "video", "music"),
            "pause" to listOf("stop", "halt", "video"),
            "stop" to listOf("halt", "end", "square"),
            "skip_next" to listOf("fast_forward", "track", "jump"),
            "skip_previous" to listOf("rewind", "track", "jump"),
            "volume_high" to listOf("audio", "sound", "loud"),
            "volume_low" to listOf("audio", "sound", "quiet"),
            "volume_mute" to listOf("silent", "audio", "off"),
            "mic" to listOf("record", "voice", "audio"),
            "mic_off" to listOf("mute", "voice", "off"),
            "camera" to listOf("photo", "capture", "lens"),
            "video" to listOf("camera", "record", "film"),
            "image" to listOf("photo", "gallery", "landscape"),
            "music" to listOf("note", "sound", "audio"),
            "headphones" to listOf("listen", "audio", "music"),
            "film" to listOf("movie", "reel", "cinema"),
            "aperture" to listOf("lens", "focus", "shutter"),
            "slider" to listOf("controls", "settings", "tuning"),
            "sliders" to listOf("equalizer", "audio", "mixer"),
            "cast" to listOf("screen", "stream", "airplay"),
            "radio" to listOf("broadcast", "stream", "frequency"),
            "disc" to listOf("album", "cd", "record"),
            "speaker" to listOf("audio", "woofer", "sound"),
            "crop" to listOf("resize", "photo", "cut"),
            "layers" to listOf("stack", "levels", "design"),
            "brush" to listOf("paint", "art", "draw"),
            "palette" to listOf("colors", "theme", "art"),
            "sparkles" to listOf("magic", "clean", "ai"),
            "wand" to listOf("magic", "transform", "tool"),
            "dropper" to listOf("eyedropper", "color", "picker"),

            // Communication & Social (30)
            "mail" to listOf("email", "message", "inbox"),
            "mail_open" to listOf("read", "email", "letter"),
            "send" to listOf("paperplane", "post", "deliver"),
            "chat" to listOf("bubble", "message", "conversation"),
            "message_square" to listOf("comment", "dialog", "talk"),
            "messages" to listOf("discussion", "chat", "forum"),
            "bell" to listOf("notification", "alert", "ring"),
            "bell_off" to listOf("mute", "do_not_disturb", "silent"),
            "phone" to listOf("call", "contact", "dial"),
            "phone_call" to listOf("active", "contact", "talking"),
            "user" to listOf("person", "profile", "account"),
            "user_plus" to listOf("add", "invite", "contact"),
            "user_check" to listOf("verified", "profile", "active"),
            "users" to listOf("group", "team", "people"),
            "at_sign" to listOf("mention", "email", "tag"),
            "hash" to listOf("hashtag", "channel", "number"),
            "thumb_up" to listOf("like", "agree", "vote"),
            "thumb_down" to listOf("dislike", "disagree", "vote"),
            "smile" to listOf("emoji", "happy", "face"),
            "gift" to listOf("present", "reward", "bonus"),
            "globe" to listOf("world", "earth", "web"),
            "rss" to listOf("feed", "syndication", "stream"),
            "inbox" to listOf("incoming", "mail", "drawer"),
            "award" to listOf("badge", "trophy", "ribbon"),
            "shield" to listOf("security", "protect", "defense"),
            "shield_check" to listOf("verified", "safe", "secure"),
            "key" to listOf("access", "auth", "token"),
            "credit_card" to listOf("payment", "money", "billing"),
            "dollar" to listOf("currency", "finance", "money"),
            "shopping_cart" to listOf("store", "checkout", "buy"),

            // Devices, System & Network (30)
            "smartphone" to listOf("mobile", "device", "cell"),
            "tablet" to listOf("ipad", "mobile", "slate"),
            "laptop" to listOf("computer", "macbook", "notebook"),
            "monitor" to listOf("screen", "display", "desktop"),
            "cpu" to listOf("processor", "chip", "hardware"),
            "hard_drive" to listOf("hdd", "storage", "disk"),
            "server" to listOf("backend", "rack", "hosting"),
            "cloud" to listOf("storage", "network", "sync"),
            "cloud_rain" to listOf("weather", "download", "precipitation"),
            "cloud_snow" to listOf("weather", "winter", "cold"),
            "wifi" to listOf("wireless", "network", "internet"),
            "bluetooth" to listOf("wireless", "pair", "connect"),
            "battery_full" to listOf("power", "charge", "energy"),
            "battery_charging" to listOf("lightning", "power", "charge"),
            "power" to listOf("turn_on", "switch", "button"),
            "zap" to listOf("lightning", "energy", "speed"),
            "sun" to listOf("light", "day", "brightness"),
            "moon" to listOf("dark", "night", "mode"),
            "compass" to listOf("direction", "safari", "navigation"),
            "map_pin" to listOf("location", "marker", "gps"),
            "navigation" to listOf("direction", "pointer", "gps"),
            "clock" to listOf("time", "schedule", "hour"),
            "watch" to listOf("wearable", "time", "clock"),
            "calendar" to listOf("date", "schedule", "event"),
            "gauge" to listOf("meter", "speed", "speedometer"),
            "activity" to listOf("pulse", "health", "waveform"),
            "thermometer" to listOf("temperature", "weather", "heat"),
            "terminal" to listOf("console", "command_line", "prompt"),
            "code" to listOf("brackets", "developer", "syntax"),
            "git_branch" to listOf("version_control", "fork", "source"),

            // Office, Analytics & Tools (30)
            "briefcase" to listOf("work", "job", "business"),
            "calculator" to listOf("math", "accounting", "finance"),
            "bar_chart" to listOf("analytics", "metrics", "graph"),
            "line_chart" to listOf("trending", "stock", "growth"),
            "pie_chart" to listOf("distribution", "stats", "data"),
            "percent" to listOf("discount", "ratio", "rate"),
            "trending_up" to listOf("growth", "profit", "positive"),
            "trending_down" to listOf("loss", "decrease", "negative"),
            "target" to listOf("goal", "focus", "aim"),
            "crosshair" to listOf("precision", "focus", "target"),
            "printer" to listOf("print", "paper", "hardware"),
            "scissors" to listOf("cut", "tool", "craft"),
            "ruler" to listOf("measure", "scale", "tool"),
            "pen_tool" to listOf("bezier", "vector", "drawing"),
            "tool" to listOf("wrench", "repair", "fix"),
            "settings_sliders" to listOf("adjust", "tune", "controls"),
            "archive_box" to listOf("storage", "history", "records"),
            "database" to listOf("sql", "storage", "server"),
            "table" to listOf("grid", "spreadsheet", "data"),
            "columns" to listOf("layout", "split", "sidebar"),
            "rows" to listOf("layout", "horizontal", "lines"),
            "check_circle" to listOf("success", "passed", "complete"),
            "x_circle" to listOf("failed", "error", "cancelled"),
            "alert_circle" to listOf("notice", "attention", "info"),
            "help_circle" to listOf("question", "faq", "support"),
            "plus" to listOf("add", "new", "increment"),
            "minus" to listOf("subtract", "remove", "decrement"),
            "plus_circle" to listOf("add", "button", "create"),
            "minus_circle" to listOf("remove", "button", "delete"),
            "maximize" to listOf("expand", "window", "scale"),

            // Travel, Nature & General (30)
            "coffee" to listOf("cafe", "break", "drink"),
            "anchor" to listOf("nautical", "stable", "marine"),
            "feather" to listOf("quill", "write", "light"),
            "key_round" to listOf("security", "password", "lock"),
            "life_buoy" to listOf("help", "support", "emergency"),
            "package" to listOf("delivery", "box", "shipping"),
            "truck" to listOf("transport", "delivery", "vehicle"),
            "map" to listOf("geography", "travel", "navigation"),
            "compass_tool" to listOf("drafting", "circle", "geometry"),
            "box_cube" to listOf("3d", "package", "geometry"),
            "umbrella" to listOf("rain", "protect", "weather"),
            "wind" to listOf("air", "breeze", "weather"),
            "droplet" to listOf("water", "liquid", "ink"),
            "sunset" to listOf("dusk", "evening", "sun"),
            "sunrise" to listOf("dawn", "morning", "sun"),
            "mountain" to listOf("peak", "landscape", "adventure"),
            "tree" to listOf("nature", "forest", "green"),
            "sprout" to listOf("growth", "leaf", "plant"),
            "flower" to listOf("blossom", "petal", "nature"),
            "flame" to listOf("fire", "hot", "trending"),
            "eye" to listOf("vision", "sight", "watch"),
            "glasses" to listOf("read", "optics", "vision"),
            "ticket" to listOf("pass", "event", "admission"),
            "flag_triangle" to listOf("banner", "point", "finish"),
            "radio_tower" to listOf("signal", "broadcast", "antenna"),
            "broadcast" to listOf("waves", "stream", "live"),
            "bluetooth_connected" to listOf("paired", "wireless", "device"),
            "usb" to listOf("port", "cable", "hardware"),
            "cast_screen" to listOf("mirror", "tv", "stream"),
            "scan" to listOf("barcode", "qr", "camera"),

            // E-commerce & Everyday (30)
            "shopping_bag" to listOf("retail", "store", "purchase"),
            "store" to listOf("shop", "market", "business"),
            "wallet" to listOf("money", "cards", "finance"),
            "receipt" to listOf("invoice", "bill", "transaction"),
            "tag_price" to listOf("sale", "offer", "discount"),
            "badge_percent" to listOf("promotion", "discount", "deal"),
            "coins" to listOf("currency", "wealth", "cash"),
            "banknote" to listOf("cash", "paper_money", "dollar"),
            "vault" to listOf("bank", "safe", "security"),
            "scan_barcode" to listOf("product", "sku", "scanner"),
            "trophy" to listOf("winner", "first", "achievement"),
            "medal" to listOf("award", "prize", "second"),
            "crown" to listOf("king", "queen", "vip"),
            "lightbulb" to listOf("idea", "innovation", "bright"),
            "puzzle" to listOf("game", "plugin", "piece"),
            "rocket" to listOf("launch", "startup", "fast"),
            "telescope" to listOf("explore", "discover", "astronomy"),
            "microscope" to listOf("science", "lab", "research"),
            "beaker" to listOf("chemistry", "experiment", "lab"),
            "atom" to listOf("science", "physics", "quantum"),
            "dna" to listOf("biology", "genetics", "medical"),
            "heart_pulse" to listOf("cardio", "health", "hospital"),
            "stethoscope" to listOf("doctor", "medicine", "clinic"),
            "syringe" to listOf("vaccine", "medicine", "shot"),
            "pill" to listOf("medicine", "tablet", "pharmacy"),
            "first_aid" to listOf("cross", "emergency", "medical"),
            "shield_cross" to listOf("health_insurance", "security", "safe"),
            "virus" to listOf("biology", "germ", "pathogen"),
            "bacteria" to listOf("microbe", "lab", "science"),
            "brain" to listOf("mind", "intelligence", "ai"),

            // Creative & Interaction (30)
            "shapes" to listOf("geometry", "figures", "design"),
            "drawing" to listOf("canvas", "sketch", "art"),
            "vector_line" to listOf("bezier", "node", "path"),
            "curve" to listOf("bezier", "arc", "vector"),
            "color_filter" to listOf("effects", "photo", "palette"),
            "contrast" to listOf("display", "brightness", "adjust"),
            "scale" to listOf("resize", "transform", "dimension"),
            "rotate_cw" to listOf("clockwise", "turn", "spin"),
            "rotate_ccw" to listOf("counter_clockwise", "turn", "spin"),
            "flip_horiz" to listOf("mirror", "horizontal", "reflect"),
            "flip_vert" to listOf("mirror", "vertical", "reflect"),
            "layers_select" to listOf("active_layer", "design", "canvas"),
            "align_left" to listOf("format", "text", "justify"),
            "align_center" to listOf("format", "text", "middle"),
            "align_right" to listOf("format", "text", "end"),
            "align_justify" to listOf("format", "text", "block"),
            "bold" to listOf("text", "font", "strong"),
            "italic" to listOf("text", "font", "emphasis"),
            "underline" to listOf("text", "font", "line"),
            "strikethrough" to listOf("text", "font", "cancel"),
            "type" to listOf("font", "typography", "letters"),
            "heading" to listOf("h1", "title", "text"),
            "paragraph" to listOf("text", "body", "article"),
            "quote" to listOf("citation", "saying", "dialog"),
            "code_inline" to listOf("backtick", "mono", "script"),
            "list_ordered" to listOf("numbers", "sequence", "steps"),
            "list_bullet" to listOf("points", "items", "tasks"),
            "indent_increase" to listOf("tab", "shift", "format"),
            "indent_decrease" to listOf("tab", "shift", "format"),
            "subscript" to listOf("math", "index", "chemical"),

            // Workflow & Systems (30)
            "workflow_step" to listOf("stage", "phase", "process"),
            "decision_node" to listOf("logic", "branch", "condition"),
            "terminal_node" to listOf("start", "end", "flow"),
            "merge_node" to listOf("combine", "join", "flow"),
            "split_node" to listOf("fork", "branch", "flow"),
            "pipeline" to listOf("ci_cd", "stages", "automation"),
            "webhook" to listOf("event", "api", "integration"),
            "endpoint" to listOf("api", "route", "http"),
            "request" to listOf("fetch", "call", "network"),
            "response" to listOf("payload", "result", "network"),
            "token" to listOf("auth", "jwt", "key"),
            "cookie" to listOf("session", "web", "storage"),
            "fingerprint" to listOf("biometric", "security", "id"),
            "id_badge" to listOf("employee", "credential", "pass"),
            "signature" to listOf("sign", "contract", "auth"),
            "stamp" to listOf("seal", "verified", "official"),
            "certificate" to listOf("degree", "ssl", "award"),
            "license" to listOf("permit", "auth", "legal"),
            "gavel" to listOf("law", "legal", "court"),
            "handcuffs" to listOf("security", "police", "arrest"),
            "siren" to listOf("alarm", "police", "alert"),
            "shield_alert" to listOf("warning", "security", "threat"),
            "bug" to listOf("defect", "issue", "debug"),
            "test_tube" to listOf("qa", "experiment", "verify"),
            "gauge_fast" to listOf("speed", "performance", "turbo"),
            "cpu_burst" to listOf("overclock", "turbo", "boost"),
            "memory_chip" to listOf("ram", "storage", "hardware"),
            "motherboard" to listOf("circuit", "hardware", "system"),
            "binary_code" to listOf("zeros", "ones", "data"),
            "algorithm" to listOf("computation", "logic", "ai"),

            // Social, Web & Utility (30)
            "thumb_up_fill" to listOf("liked", "approved", "positive"),
            "heart_fill" to listOf("loved", "favorite", "like"),
            "star_fill" to listOf("favorited", "bookmarked", "rated"),
            "bookmark_fill" to listOf("saved", "marker", "pinned"),
            "pin_fill" to listOf("fastened", "pinned", "sticky"),
            "share_2" to listOf("nodes", "social", "distribute"),
            "send_fill" to listOf("sent", "posted", "dispatched"),
            "comment_fill" to listOf("feedback", "remark", "response"),
            "bell_ring" to listOf("ringing", "urgent", "alarm"),
            "calendar_event" to listOf("meeting", "appointment", "schedule"),
            "timer" to listOf("countdown", "stopwatch", "seconds"),
            "hourglass" to listOf("waiting", "duration", "sand"),
            "watch_digital" to listOf("clock", "time", "smartwatch"),
            "compass_rose" to listOf("direction", "north", "bearing"),
            "anchor_heavy" to listOf("marine", "fixed", "dock"),
            "ship" to listOf("boat", "transport", "vessel"),
            "airplane" to listOf("flight", "travel", "aviation"),
            "train" to listOf("rail", "transit", "locomotive"),
            "car" to listOf("vehicle", "auto", "drive"),
            "bus" to listOf("public_transit", "travel", "commute"),
            "bicycle" to listOf("bike", "eco", "ride"),
            "scooter" to listOf("electric", "transit", "ride"),
            "fuel" to listOf("gas", "petrol", "station"),
            "charging_station" to listOf("ev", "electric", "battery"),
            "parking" to listOf("lot", "space", "vehicle"),
            "traffic_light" to listOf("signal", "rules", "transit"),
            "signpost" to listOf("directions", "path", "options"),
            "footprints" to listOf("steps", "walking", "track"),
            "paw_print" to listOf("animal", "pet", "dog"),
            "feather_light" to listOf("soft", "writing", "plume")
        )

        uiDefinitions.forEachIndexed { idx, (name, tags) ->
            list += VisualAsset(
                id = "ui_${String.format("%03d", idx + 1)}",
                name = name.replace("_", " ").replaceFirstChar { it.uppercase() },
                category = AssetCategory.INTERFACE,
                tags = tags + listOf("ui", "icon"),
                drawType = name
            )
        }

        // 2. LOGOS & BRANDING (100 items: logo_001 to logo_100)
        val logoNames = listOf(
            "Crest Shield", "Hexagon Emblem", "Orbit Atom", "Infinity Loop", "Diamond Crown",
            "Double Helix", "Wireframe Cube", "Cyber Matrix", "Solar Flare", "Mountain Peak",
            "Falcon Wing", "Origami Bird", "Flame Crest", "Water Droplet", "Sunburst Petal",
            "Pinecone Spiral", "Anchor Marina", "Compass Rose", "Lighthouse Beacon", "Pillar Column",
            "Hourglass Studio", "Bullseye Target", "Triskele Spiral", "Delta Triangle", "Vortex Arc",
            "Wave Ripple", "Galaxy Core", "Acorn Oak", "Feather Plume", "Keyhole Secret",
            "Crown Royal", "Shield Aegis", "Matrix Node", "Quantum Dot", "Binary Ring",
            "Zen Circle", "Lotus Bloom", "Octagon Seal", "Circular Stamp", "Vintage Ribbon",
            "Shield Cross", "Hexa Crest", "Prism Crystal", "Facet Gem", "Nova Star",
            "Cosmic Dust", "Echo Wave", "Pulse Rhythm", "Rune Symbol", "Totem Mark",
            "Monogram Minimal", "Monogram Bold", "Monogram Serif", "Monogram Script", "Monogram Linear",
            "Badge Vintage", "Badge Modern", "Badge Circular", "Badge Shield", "Badge Diamond",
            "Emblem Eagle", "Emblem Lion", "Emblem Wolf", "Emblem Bear", "Emblem Fox",
            "Emblem Phoenix", "Emblem Dragon", "Emblem Stag", "Emblem Owl", "Emblem Hawk",
            "Tech Circuit", "Tech Grid", "Tech Chip", "Tech Pulse", "Tech Cloud",
            "Studio Film", "Studio Sound", "Studio Lens", "Studio Craft", "Studio Code",
            "Nature Leaf", "Nature Tree", "Nature Flower", "Nature Sprout", "Nature Mountain",
            "Water Wave", "Water Drop", "Water Stream", "Water Spring", "Water Ocean",
            "Fire Spark", "Fire Flame", "Fire Ember", "Fire Torch", "Fire Blaze",
            "Earth Stone", "Earth Peak", "Earth Horizon", "Earth Canyon", "Earth Globe"
        )
        logoNames.forEachIndexed { idx, name ->
            list += VisualAsset(
                id = "logo_${String.format("%03d", idx + 1)}",
                name = name,
                category = AssetCategory.LOGOS,
                tags = listOf("logo", "brand", "emblem", "symbol", name.lowercase()),
                drawType = "logo_${idx + 1}"
            )
        }

        // 3. WORKFLOW & DIAGRAM ELEMENTS (100 items: wf_001 to wf_100)
        val workflowNames = listOf(
            "Start Terminal", "End Terminal", "Process Step", "Decision Rhombus", "Input Output Block",
            "Predefined Subroutine", "Manual Operation", "Preparation Hexagon", "Display Node", "Delay Node",
            "Document Output", "Multi-Document", "Database Cylinder", "Internal Storage", "Sequential Data",
            "Direct Data", "Manual Input", "Card Node", "Paper Tape", "Collate Node",
            "Sort Node", "Extract Node", "Merge Node", "Stored Data", "Summing Junction",
            "Or Node", "Branch Split", "Loop Limit", "Off-Page Reference", "On-Page Reference",
            "Cloud Platform", "Server Rack", "Microservice Pod", "API Gateway", "Load Balancer",
            "Firewall Shield", "Message Queue", "Event Bus", "Cache Cluster", "Web Client",
            "Mobile Client", "Edge CDN", "Blob Storage", "Relational SQL", "NoSQL Document",
            "Search Cluster", "Worker Process", "Cron Scheduler", "Auth Provider", "Payment Gateway",
            "Actor User", "Actor System", "Actor Admin", "Use Case Oval", "Boundary Interface",
            "Control Logic", "Entity Storage", "Package Module", "Component Block", "Node Device",
            "State Initial", "State Final", "State Composite", "State History", "Fork Transition",
            "Join Transition", "Choice Pseudo", "Junction Point", "Entry Point", "Exit Point",
            "Mind Central", "Mind Main Branch", "Mind Sub Branch", "Mind Note Pin", "Mind Question",
            "Mind Idea Spark", "Mind Checkpoint", "Mind Risk Warning", "Mind Goal Flag", "Mind Resource",
            "Network Router", "Network Switch", "Network Bridge", "Network Repeater", "Network Gateway",
            "Fiber Backbone", "Wireless AP", "Cell Tower", "Satellite Uplink", "SAN Storage",
            "Docker Container", "Kubernetes Pod", "Git Repository", "Build Pipeline", "Deploy Target",
            "Test Runner", "Log Collector", "Metric Monitor", "Alert Dispatch", "Dashboard View"
        )
        workflowNames.forEachIndexed { idx, name ->
            list += VisualAsset(
                id = "wf_${String.format("%03d", idx + 1)}",
                name = name,
                category = AssetCategory.WORKFLOW,
                tags = listOf("workflow", "diagram", "chart", "node", name.lowercase()),
                drawType = "wf_${idx + 1}"
            )
        }

        // 4. ILLUSTRATIONS & DECORATIVE (50 items: ill_001 to ill_50)
        val illustrationNames = listOf(
            "Rocket Launch", "Trophy Victory", "Medal Gold", "Lightbulb Innovation", "Artist Palette",
            "Magic Wand", "Diamond Jewel", "Coffee Mug", "Books Stack", "Workspace Desk",
            "Mountain Sunrise", "Campfire Flame", "Hot Air Balloon", "Paper Airplane", "Bicycle Cruiser",
            "Compass Adventure", "Camera Classic", "Vinyl Record", "Telescope Explorer", "Brainstorming Mind",
            "Hourglass Focus", "Target Bullseye", "Checklist Completed", "Graduation Cap", "Music Guitar",
            "Music Piano", "Headphones Studio", "Microphone Vintage", "Gamepad Controller", "Dice Pair",
            "Chess Knight", "Shield of Honor", "Sword of Justice", "Castle Tower", "Bridge Crossing",
            "Sailing Boat", "Anchor Maritime", "Lighthouse Shore", "Windmill Classic", "Solar Panels",
            "Wind Turbine", "Sprout Growth", "Bonsai Tree", "Sunflower Bloom", "Cactus Desert",
            "Planet Saturn", "Constellation Stars", "Shooting Star", "Rainbow Arc", "Aurora Lights"
        )
        illustrationNames.forEachIndexed { idx, name ->
            list += VisualAsset(
                id = "ill_${String.format("%02d", idx + 1)}",
                name = name,
                category = AssetCategory.ILLUSTRATIONS,
                tags = listOf("illustration", "art", "decorative", name.lowercase()),
                drawType = "ill_${idx + 1}"
            )
        }

        // 5. SHAPES & DESIGN ELEMENTS (50 items: shp_001 to shp_50)
        val shapeNames = listOf(
            "Rectangle", "Rounded Rectangle", "Square", "Squircle", "Circle",
            "Oval", "Pill Capsule", "Diamond Rhombus", "Triangle Equilateral", "Triangle Right",
            "Pentagon Regular", "Hexagon Flat", "Hexagon Pointy", "Heptagon", "Octagon Stop",
            "Decagon", "Dodecagon", "Trapezoid", "Parallelogram Left", "Parallelogram Right",
            "Star 4-Point", "Star 5-Point", "Star 6-Point", "Star 8-Point", "Star 12-Point",
            "Starburst Burst", "Sparkle Quad", "Sparkle Octo", "Sunburst Ray", "Cross Plus",
            "Cross Maltese", "Chevron Right", "Chevron Double", "Arrow Point Right", "Arrow Bidirectional",
            "Speech Bubble", "Thought Cloud", "Chat Callout", "Banner Ribbon", "Bookmark Ribbon",
            "Badge Circular", "Badge Shield", "Badge Scallop", "Tag Label", "Bracket Left",
            "Bracket Right", "Wave Horizontal", "Corner Frame", "Shield Classic", "Heart Shape"
        )
        shapeNames.forEachIndexed { idx, name ->
            list += VisualAsset(
                id = "shp_${String.format("%02d", idx + 1)}",
                name = name,
                category = AssetCategory.SHAPES,
                tags = listOf("shape", "geometry", "design", name.lowercase()),
                drawType = "shp_${idx + 1}"
            )
        }

        return list
    }
}

/**
 * High-performance, parametric Compose vector renderer for all 600 built-in assets.
 * Renders resolution-independent vectors using native Android Canvas routines.
 */
@Composable
fun AssetVectorRenderer(
    asset: VisualAsset,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = (w * 0.08f).coerceIn(1.5f, 4.0f)
        val strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (asset.category) {
            AssetCategory.SHAPES -> drawShapeAsset(asset.id, w, h, tint, strokeStyle)
            AssetCategory.LOGOS -> drawLogoAsset(asset.id, w, h, tint, strokeStyle)
            AssetCategory.WORKFLOW -> drawWorkflowAsset(asset.id, w, h, tint, strokeStyle)
            AssetCategory.ILLUSTRATIONS -> drawIllustrationAsset(asset.id, w, h, tint, strokeStyle)
            AssetCategory.INTERFACE -> drawInterfaceAsset(asset.drawType, w, h, tint, strokeStyle)
        }
    }
}

private fun DrawScope.drawShapeAsset(id: String, w: Float, h: Float, tint: Color, stroke: Stroke) {
    val num = id.removePrefix("shp_").toIntOrNull() ?: 1
    val cx = w / 2f
    val cy = h / 2f
    val r = min(w, h) * 0.42f

    when (num % 10) {
        1 -> drawRoundRect(tint, Offset(w * 0.1f, h * 0.15f), Size(w * 0.8f, h * 0.7f), CornerRadius(w * 0.08f), style = stroke)
        2 -> drawCircle(tint, r, Offset(cx, cy), style = stroke)
        3 -> {
            val p = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r, cy)
                close()
            }
            drawPath(p, tint, style = stroke)
        }
        4 -> {
            val p = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r * 0.866f, cy + r * 0.5f)
                lineTo(cx - r * 0.866f, cy + r * 0.5f)
                close()
            }
            drawPath(p, tint, style = stroke)
        }
        5 -> {
            // Star
            val p = Path()
            val points = 5
            for (i in 0 until points * 2) {
                val rad = if (i % 2 == 0) r else r * 0.45f
                val angle = i * Math.PI / points - Math.PI / 2
                val px = cx + (rad * cos(angle)).toFloat()
                val py = cy + (rad * sin(angle)).toFloat()
                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
            p.close()
            drawPath(p, tint, style = stroke)
        }
        6 -> {
            // Hexagon
            val p = Path()
            for (i in 0 until 6) {
                val angle = i * Math.PI / 3
                val px = cx + (r * cos(angle)).toFloat()
                val py = cy + (r * sin(angle)).toFloat()
                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
            p.close()
            drawPath(p, tint, style = stroke)
        }
        7 -> {
            // Octagon
            val p = Path()
            for (i in 0 until 8) {
                val angle = i * Math.PI / 4 + Math.PI / 8
                val px = cx + (r * cos(angle)).toFloat()
                val py = cy + (r * sin(angle)).toFloat()
                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
            p.close()
            drawPath(p, tint, style = stroke)
        }
        8 -> {
            // Heart
            val p = Path().apply {
                moveTo(cx, cy + r * 0.6f)
                cubicTo(cx - r, cy, cx - r * 0.8f, cy - r * 0.8f, cx, cy - r * 0.3f)
                cubicTo(cx + r * 0.8f, cy - r * 0.8f, cx + r, cy, cx, cy + r * 0.6f)
            }
            drawPath(p, tint, style = stroke)
        }
        9 -> {
            // Speech bubble
            val p = Path().apply {
                moveTo(w * 0.15f, h * 0.2f)
                lineTo(w * 0.85f, h * 0.2f)
                lineTo(w * 0.85f, h * 0.65f)
                lineTo(w * 0.45f, h * 0.65f)
                lineTo(w * 0.25f, h * 0.85f)
                lineTo(w * 0.25f, h * 0.65f)
                lineTo(w * 0.15f, h * 0.65f)
                close()
            }
            drawPath(p, tint, style = stroke)
        }
        else -> {
            // Pill
            drawRoundRect(tint, Offset(w * 0.1f, h * 0.25f), Size(w * 0.8f, h * 0.5f), CornerRadius(h * 0.25f), style = stroke)
        }
    }
}

private fun DrawScope.drawLogoAsset(id: String, w: Float, h: Float, tint: Color, stroke: Stroke) {
    val num = id.removePrefix("logo_").toIntOrNull() ?: 1
    val cx = w / 2f
    val cy = h / 2f
    val r = min(w, h) * 0.4f

    when (num % 6) {
        0 -> {
            // Shield Crest
            val p = Path().apply {
                moveTo(cx, h * 0.12f)
                lineTo(w * 0.85f, h * 0.22f)
                lineTo(w * 0.85f, h * 0.55f)
                cubicTo(w * 0.85f, h * 0.82f, cx, h * 0.92f, cx, h * 0.92f)
                cubicTo(cx, h * 0.92f, w * 0.15f, h * 0.82f, w * 0.15f, h * 0.55f)
                lineTo(w * 0.15f, h * 0.22f)
                close()
            }
            drawPath(p, tint, style = stroke)
            drawLine(tint, Offset(cx, h * 0.2f), Offset(cx, h * 0.85f), strokeWidth = stroke.width * 0.75f)
        }
        1 -> {
            // Orbit atom
            drawCircle(tint, r * 0.25f, Offset(cx, cy), style = Fill)
            drawOval(tint, Offset(cx - r, cy - r * 0.4f), Size(r * 2f, r * 0.8f), style = stroke)
            drawOval(tint, Offset(cx - r * 0.4f, cy - r), Size(r * 0.8f, r * 2f), style = stroke)
        }
        2 -> {
            // Infinity loop
            val p = Path().apply {
                moveTo(cx, cy)
                cubicTo(cx + r * 0.6f, cy - r * 0.6f, cx + r, cy - r * 0.6f, cx + r, cy)
                cubicTo(cx + r, cy + r * 0.6f, cx + r * 0.6f, cy + r * 0.6f, cx, cy)
                cubicTo(cx - r * 0.6f, cy - r * 0.6f, cx - r, cy - r * 0.6f, cx - r, cy)
                cubicTo(cx - r, cy + r * 0.6f, cx - r * 0.6f, cy + r * 0.6f, cx, cy)
            }
            drawPath(p, tint, style = stroke)
        }
        3 -> {
            // Diamond crown
            val p = Path().apply {
                moveTo(w * 0.18f, h * 0.7f)
                lineTo(w * 0.18f, h * 0.35f)
                lineTo(w * 0.38f, h * 0.5f)
                lineTo(cx, h * 0.22f)
                lineTo(w * 0.62f, h * 0.5f)
                lineTo(w * 0.82f, h * 0.35f)
                lineTo(w * 0.82f, h * 0.7f)
                close()
            }
            drawPath(p, tint, style = stroke)
            drawCircle(tint, w * 0.04f, Offset(cx, h * 0.18f), style = Fill)
        }
        4 -> {
            // Geometric Cube Wireframe
            val p1 = Path().apply {
                moveTo(cx, h * 0.15f)
                lineTo(w * 0.8f, h * 0.32f)
                lineTo(w * 0.8f, h * 0.68f)
                lineTo(cx, h * 0.85f)
                lineTo(w * 0.2f, h * 0.68f)
                lineTo(w * 0.2f, h * 0.32f)
                close()
            }
            drawPath(p1, tint, style = stroke)
            drawLine(tint, Offset(cx, h * 0.15f), Offset(cx, cy), strokeWidth = stroke.width)
            drawLine(tint, Offset(cx, cy), Offset(w * 0.8f, h * 0.68f), strokeWidth = stroke.width)
            drawLine(tint, Offset(cx, cy), Offset(w * 0.2f, h * 0.68f), strokeWidth = stroke.width)
        }
        else -> {
            // Mountain Sunburst
            val p = Path().apply {
                moveTo(w * 0.12f, h * 0.78f)
                lineTo(w * 0.42f, h * 0.32f)
                lineTo(w * 0.62f, h * 0.6f)
                lineTo(w * 0.75f, h * 0.45f)
                lineTo(w * 0.88f, h * 0.78f)
                close()
            }
            drawPath(p, tint, style = stroke)
            drawCircle(tint, r * 0.28f, Offset(w * 0.68f, h * 0.25f), style = stroke)
        }
    }
}

private fun DrawScope.drawWorkflowAsset(id: String, w: Float, h: Float, tint: Color, stroke: Stroke) {
    val num = id.removePrefix("wf_").toIntOrNull() ?: 1
    val cx = w / 2f
    val cy = h / 2f

    when (num % 5) {
        0 -> {
            // Database cylinder
            drawOval(tint, Offset(w * 0.2f, h * 0.15f), Size(w * 0.6f, h * 0.2f), style = stroke)
            drawLine(tint, Offset(w * 0.2f, h * 0.25f), Offset(w * 0.2f, h * 0.75f), strokeWidth = stroke.width)
            drawLine(tint, Offset(w * 0.8f, h * 0.25f), Offset(w * 0.8f, h * 0.75f), strokeWidth = stroke.width)
            drawArc(tint, 0f, 180f, false, Offset(w * 0.2f, h * 0.65f), Size(w * 0.6f, h * 0.2f), style = stroke)
        }
        1 -> {
            // Decision Diamond
            val p = Path().apply {
                moveTo(cx, h * 0.15f)
                lineTo(w * 0.85f, cy)
                lineTo(cx, h * 0.85f)
                lineTo(w * 0.15f, cy)
                close()
            }
            drawPath(p, tint, style = stroke)
        }
        2 -> {
            // Terminal Pill
            drawRoundRect(tint, Offset(w * 0.15f, h * 0.28f), Size(w * 0.7f, h * 0.44f), CornerRadius(h * 0.22f), style = stroke)
        }
        3 -> {
            // Process Step
            drawRoundRect(tint, Offset(w * 0.15f, h * 0.25f), Size(w * 0.7f, h * 0.5f), CornerRadius(w * 0.05f), style = stroke)
            drawLine(tint, Offset(w * 0.25f, cy), Offset(w * 0.75f, cy), strokeWidth = stroke.width * 0.75f)
        }
        else -> {
            // Cloud Node
            val p = Path().apply {
                moveTo(w * 0.3f, h * 0.72f)
                lineTo(w * 0.75f, h * 0.72f)
                cubicTo(w * 0.88f, h * 0.72f, w * 0.88f, h * 0.52f, w * 0.78f, h * 0.52f)
                cubicTo(w * 0.82f, h * 0.32f, w * 0.62f, h * 0.26f, w * 0.52f, h * 0.38f)
                cubicTo(w * 0.45f, h * 0.32f, w * 0.28f, h * 0.38f, w * 0.28f, h * 0.55f)
                cubicTo(w * 0.18f, h * 0.55f, w * 0.18f, h * 0.72f, w * 0.3f, h * 0.72f)
                close()
            }
            drawPath(p, tint, style = stroke)
        }
    }
}

private fun DrawScope.drawIllustrationAsset(id: String, w: Float, h: Float, tint: Color, stroke: Stroke) {
    val num = id.removePrefix("ill_").toIntOrNull() ?: 1
    val cx = w / 2f
    val cy = h / 2f
    val r = min(w, h) * 0.4f

    when (num % 5) {
        0 -> {
            // Rocket Launch
            val p = Path().apply {
                moveTo(cx, h * 0.12f)
                cubicTo(w * 0.72f, h * 0.25f, w * 0.7f, h * 0.62f, w * 0.62f, h * 0.72f)
                lineTo(w * 0.38f, h * 0.72f)
                cubicTo(w * 0.3f, h * 0.62f, w * 0.28f, h * 0.25f, cx, h * 0.12f)
                close()
            }
            drawPath(p, tint, style = stroke)
            drawCircle(tint, w * 0.08f, Offset(cx, h * 0.38f), style = stroke)
            drawLine(tint, Offset(cx - w * 0.1f, h * 0.75f), Offset(cx - w * 0.18f, h * 0.88f), strokeWidth = stroke.width)
            drawLine(tint, Offset(cx + w * 0.1f, h * 0.75f), Offset(cx + w * 0.18f, h * 0.88f), strokeWidth = stroke.width)
        }
        1 -> {
            // Trophy Cup
            val p = Path().apply {
                moveTo(w * 0.28f, h * 0.18f)
                lineTo(w * 0.72f, h * 0.18f)
                lineTo(w * 0.7f, h * 0.52f)
                cubicTo(w * 0.68f, h * 0.68f, w * 0.32f, h * 0.68f, w * 0.3f, h * 0.52f)
                close()
            }
            drawPath(p, tint, style = stroke)
            drawLine(tint, Offset(cx, h * 0.65f), Offset(cx, h * 0.8f), strokeWidth = stroke.width)
            drawLine(tint, Offset(w * 0.32f, h * 0.85f), Offset(w * 0.68f, h * 0.85f), strokeWidth = stroke.width)
        }
        2 -> {
            // Lightbulb
            drawCircle(tint, r * 0.6f, Offset(cx, cy - r * 0.2f), style = stroke)
            drawLine(tint, Offset(cx - w * 0.14f, cy + r * 0.45f), Offset(cx + w * 0.14f, cy + r * 0.45f), strokeWidth = stroke.width)
            drawLine(tint, Offset(cx - w * 0.1f, cy + r * 0.65f), Offset(cx + w * 0.1f, cy + r * 0.65f), strokeWidth = stroke.width)
        }
        3 -> {
            // Coffee Mug
            drawRoundRect(tint, Offset(w * 0.2f, h * 0.3f), Size(w * 0.45f, h * 0.5f), CornerRadius(w * 0.06f), style = stroke)
            drawArc(tint, -90f, 180f, false, Offset(w * 0.62f, h * 0.38f), Size(w * 0.22f, h * 0.32f), style = stroke)
            drawLine(tint, Offset(w * 0.3f, h * 0.18f), Offset(w * 0.3f, h * 0.24f), strokeWidth = stroke.width * 0.75f)
            drawLine(tint, Offset(w * 0.42f, h * 0.15f), Offset(w * 0.42f, h * 0.24f), strokeWidth = stroke.width * 0.75f)
        }
        else -> {
            // Books Stack
            drawRoundRect(tint, Offset(w * 0.18f, h * 0.65f), Size(w * 0.64f, h * 0.18f), CornerRadius(w * 0.04f), style = stroke)
            drawRoundRect(tint, Offset(w * 0.24f, h * 0.45f), Size(w * 0.56f, h * 0.18f), CornerRadius(w * 0.04f), style = stroke)
            drawRoundRect(tint, Offset(w * 0.28f, h * 0.25f), Size(w * 0.48f, h * 0.18f), CornerRadius(w * 0.04f), style = stroke)
        }
    }
}

private fun DrawScope.drawInterfaceAsset(type: String, w: Float, h: Float, tint: Color, stroke: Stroke) {
    val cx = w / 2f
    val cy = h / 2f
    val r = min(w, h) * 0.4f

    when (type) {
        "home" -> {
            val p = Path().apply {
                moveTo(cx, h * 0.18f)
                lineTo(w * 0.85f, h * 0.45f)
                lineTo(w * 0.75f, h * 0.45f)
                lineTo(w * 0.75f, h * 0.85f)
                lineTo(w * 0.25f, h * 0.85f)
                lineTo(w * 0.25f, h * 0.45f)
                lineTo(w * 0.15f, h * 0.45f)
                close()
            }
            drawPath(p, tint, style = stroke)
        }
        "search" -> {
            drawCircle(tint, r * 0.65f, Offset(cx - r * 0.2f, cy - r * 0.2f), style = stroke)
            drawLine(tint, Offset(cx + r * 0.3f, cy + r * 0.3f), Offset(w * 0.85f, h * 0.85f), strokeWidth = stroke.width)
        }
        "settings" -> {
            drawCircle(tint, r * 0.35f, Offset(cx, cy), style = stroke)
            drawCircle(tint, r * 0.85f, Offset(cx, cy), style = stroke)
        }
        "check" -> {
            val p = Path().apply {
                moveTo(w * 0.2f, cy)
                lineTo(w * 0.42f, h * 0.75f)
                lineTo(w * 0.82f, h * 0.25f)
            }
            drawPath(p, tint, style = stroke)
        }
        "close" -> {
            drawLine(tint, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), strokeWidth = stroke.width)
            drawLine(tint, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), strokeWidth = stroke.width)
        }
        "star" -> {
            val p = Path()
            for (i in 0 until 10) {
                val rad = if (i % 2 == 0) r else r * 0.45f
                val angle = i * Math.PI / 5 - Math.PI / 2
                val px = cx + (rad * cos(angle)).toFloat()
                val py = cy + (rad * sin(angle)).toFloat()
                if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
            }
            p.close()
            drawPath(p, tint, style = stroke)
        }
        "heart" -> {
            val p = Path().apply {
                moveTo(cx, cy + r * 0.65f)
                cubicTo(cx - r, cy, cx - r * 0.8f, cy - r * 0.8f, cx, cy - r * 0.3f)
                cubicTo(cx + r * 0.8f, cy - r * 0.8f, cx + r, cy, cx, cy + r * 0.65f)
            }
            drawPath(p, tint, style = stroke)
        }
        "folder" -> {
            val p = Path().apply {
                moveTo(w * 0.15f, h * 0.3f)
                lineTo(w * 0.4f, h * 0.3f)
                lineTo(w * 0.5f, h * 0.4f)
                lineTo(w * 0.85f, h * 0.4f)
                lineTo(w * 0.85f, h * 0.8f)
                lineTo(w * 0.15f, h * 0.8f)
                close()
            }
            drawPath(p, tint, style = stroke)
        }
        "file" -> {
            val p = Path().apply {
                moveTo(w * 0.22f, h * 0.15f)
                lineTo(w * 0.6f, h * 0.15f)
                lineTo(w * 0.78f, h * 0.33f)
                lineTo(w * 0.78f, h * 0.85f)
                lineTo(w * 0.22f, h * 0.85f)
                close()
            }
            drawPath(p, tint, style = stroke)
            drawLine(tint, Offset(w * 0.6f, h * 0.15f), Offset(w * 0.6f, h * 0.33f), strokeWidth = stroke.width)
            drawLine(tint, Offset(w * 0.6f, h * 0.33f), Offset(w * 0.78f, h * 0.33f), strokeWidth = stroke.width)
        }
        "edit" -> {
            val p = Path().apply {
                moveTo(w * 0.2f, h * 0.8f)
                lineTo(w * 0.35f, h * 0.75f)
                lineTo(w * 0.75f, h * 0.35f)
                lineTo(w * 0.65f, h * 0.25f)
                lineTo(w * 0.25f, h * 0.65f)
                close()
            }
            drawPath(p, tint, style = stroke)
        }
        else -> {
            // General geometric badge fallback with clean dot or inner ring
            drawRoundRect(tint, Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.64f), CornerRadius(w * 0.12f), style = stroke)
            drawCircle(tint, r * 0.35f, Offset(cx, cy), style = stroke)
        }
    }
}
