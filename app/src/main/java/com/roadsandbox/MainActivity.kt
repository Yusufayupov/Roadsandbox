package com.roadsandbox

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class MainActivity : Activity() {
    private lateinit var map: CityMapView
    private val buttons = mutableMapOf<String, Button>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(35, 39, 39))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 4, 8, 4)
            setBackgroundColor(Color.rgb(35, 39, 39))
        }
        val title = TextView(this).apply {
            text = "CITY SANDBOX"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            letterSpacing = 0.08f
        }
        header.addView(title, LinearLayout.LayoutParams(0, 48, 1f))
        header.addView(makeButton("↶", "Отменить") { map.undo() })
        header.addView(makeButton("Очистить", "Очистить карту") { map.clearMap() })
        root.addView(header)

        map = CityMapView(this)
        root.addView(map, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(toolbarRow(listOf(
            "Пустыня" to { map.setTerrain("desert") },
            "Степь" to { map.setTerrain("steppe") },
            "Лес" to { map.setTerrain("forest") },
            "Горы" to { map.setTerrain("rock") },
            "Вода" to { map.setTerrain("water") },
            "Поля" to { map.setTerrain("field") }
        )))
        root.addView(toolbarRow(listOf(
            "Панорама" to { map.setTool("pan") },
            "Кисть" to { map.setTool("terrain") },
            "Дорога" to { map.setTool("road") },
            "Шоссе" to { map.setTool("highway") },
            "Дом" to { map.setTool("house") },
            "Многоэтажка" to { map.setTool("block") },
            "Завод" to { map.setTool("factory") },
            "Дерево" to { map.setTool("tree") },
            "Ластик" to { map.setTool("erase") }
        )))
        root.addView(toolbarRow(listOf(
            "Спутник" to { map.toggleLayer("satellite") },
            "Рельеф" to { map.toggleLayer("terrain") },
            "Дороги" to { map.toggleLayer("roads") },
            "Здания" to { map.toggleLayer("buildings") },
            "Природа" to { map.toggleLayer("nature") },
            "−" to { map.zoomBy(0.8f) },
            "+" to { map.zoomBy(1.25f) }
        )))
        setContentView(root)
    }

    private fun makeButton(label: String, description: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            contentDescription = description
            textSize = 11f
            isAllCaps = false
            setOnClickListener { action() }
        }
    }

    private fun toolbarRow(items: List<Pair<String, () -> Unit>>): HorizontalScrollView {
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(5, 2, 5, 2)
            setBackgroundColor(Color.rgb(48, 53, 52))
        }
        items.forEach { item ->
            val b = makeButton(item.first, item.first, item.second)
            strip.addView(b, LinearLayout.LayoutParams(-2, 43).apply {
                setMargins(3, 2, 3, 2)
            })
        }
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(strip)
        }
    }
}

private data class MapStroke(val points: MutableList<PointF>, val kind: String)
private data class MapObject(val x: Float, val y: Float, val kind: String)

private class CityMapView(context: android.content.Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val strokes = mutableListOf<MapStroke>()
    private val objects = mutableListOf<MapObject>()
    private var activeStroke: MapStroke? = null
    private var tool = "road"
    private var terrain = "desert"
    private var zoom = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var cameraReady = false
    private var lastX = 0f
    private var lastY = 0f
    private var lastPinchX = 0f
    private var lastPinchY = 0f
    private var showSatellite = true
    private var showTerrain = true
    private var showRoads = true
    private var showBuildings = true
    private var showNature = true
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val old = zoom
            zoom = (zoom * detector.scaleFactor).coerceIn(0.25f, 6f)
            val ratio = zoom / old
            offsetX = detector.focusX - (detector.focusX - offsetX) * ratio
            offsetY = detector.focusY - (detector.focusY - offsetY) * ratio
            invalidate()
            return true
        }
    })

    private fun paint(color: Int, style: Paint.Style = Paint.Style.FILL): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; this.style = style }

    init {
        isFocusable = true
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    fun setTool(value: String) { tool = value; invalidate() }
    fun setTerrain(value: String) { terrain = value; tool = "terrain"; invalidate() }

    fun toggleLayer(layer: String) {
        when (layer) {
            "satellite" -> showSatellite = !showSatellite
            "terrain" -> showTerrain = !showTerrain
            "roads" -> showRoads = !showRoads
            "buildings" -> showBuildings = !showBuildings
            "nature" -> showNature = !showNature
        }
        invalidate()
    }

    fun zoomBy(factor: Float) {
        val old = zoom
        zoom = (zoom * factor).coerceIn(0.25f, 6f)
        val cx = width / 2f
        val cy = height / 2f
        val ratio = zoom / old
        offsetX = cx - (cx - offsetX) * ratio
        offsetY = cy - (cy - offsetY) * ratio
        invalidate()
    }

    fun undo() {
        if (activeStroke != null) activeStroke = null
        else if (objects.isNotEmpty()) objects.removeAt(objects.lastIndex)
        else if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
        invalidate()
    }

    fun clearMap() {
        strokes.clear()
        objects.clear()
        activeStroke = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!cameraReady) {
            offsetX = width / 2f
            offsetY = height / 2f
            cameraReady = true
        }
        canvas.drawColor(Color.rgb(177, 169, 143))
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(zoom, zoom)
        val left = -offsetX / zoom - 100f
        val top = -offsetY / zoom - 100f
        val right = (width - offsetX) / zoom + 100f
        val bottom = (height - offsetY) / zoom + 100f
        if (showSatellite) drawSatelliteBase(canvas, left, top, right, bottom)
        if (showTerrain) {
            strokes.filter { it.kind in listOf("desert", "steppe", "forest", "rock", "water", "field") }.forEach { drawTerrainStroke(canvas, it) }
        }
        if (showRoads) {
            strokes.filter { it.kind == "road" || it.kind == "highway" }.forEach { drawRoad(canvas, it) }
            activeStroke?.let {
                if (it.kind == "road" || it.kind == "highway") drawRoad(canvas, it)
            }
        }
        if (showBuildings || showNature) {
            objects.forEach { obj ->
                if ((obj.kind == "tree" && showNature) || (obj.kind != "tree" && showBuildings)) drawObject(canvas, obj)
            }
        }
        activeStroke?.let {
            if (it.kind !in listOf("road", "highway") && showTerrain) drawTerrainStroke(canvas, it)
        }
        canvas.restore()
        drawHud(canvas)
    }

    private fun drawSatelliteBase(c: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        c.drawColor(Color.rgb(151, 151, 112))
        val cell = 150f
        val x0 = floor(left / cell).toInt()
        val x1 = floor(right / cell).toInt()
        val y0 = floor(top / cell).toInt()
        val y1 = floor(bottom / cell).toInt()
        for (gy in y0..y1) for (gx in x0..x1) {
            val h = hash(gx, gy)
            val color = when {
                h < 0.17f -> Color.rgb(126, 133, 91)
                h < 0.34f -> Color.rgb(163, 157, 117)
                h < 0.53f -> Color.rgb(139, 145, 99)
                h < 0.70f -> Color.rgb(174, 166, 126)
                else -> Color.rgb(119, 132, 91)
            }
            val p = paint(color)
            val x = gx * cell
            val y = gy * cell
            c.drawRect(x, y, x + cell + 1, y + cell + 1, p)
            val detail = paint(Color.argb(35, 64, 72, 45))
            for (i in 0..5) {
                val hx = x + hash(gx * 13 + i, gy * 7) * cell
                val hy = y + hash(gx * 5, gy * 11 + i) * cell
                c.drawCircle(hx, hy, 7f + hash(gx + i, gy - i) * 13f, detail)
            }
        }
        // Fine aerial-photo grain and irregular field boundaries.
        val grid = 75f
        for (gy in floor(top / grid).toInt()..floor(bottom / grid).toInt()) {
            for (gx in floor(left / grid).toInt()..floor(right / grid).toInt()) {
                val h = hash(gx + 301, gy - 117)
                if (h > 0.66f) {
                    val p = paint(Color.argb(42, 54, 76, 43))
                    val x = gx * grid + 12f
                    val y = gy * grid + 8f
                    c.drawOval(x, y, x + 26f + h * 28f, y + 18f + h * 22f, p)
                }
            }
        }
    }

    private fun hash(x: Int, y: Int): Float {
        val v = sin(x * 127.1 + y * 311.7) * 43758.5453
        return (v - floor(v)).toFloat()
    }

    private fun drawTerrainStroke(c: Canvas, stroke: MapStroke) {
        if (stroke.points.isEmpty()) return
        val color = when (stroke.kind) {
            "desert" -> Color.rgb(205, 174, 112)
            "steppe" -> Color.rgb(150, 157, 91)
            "forest" -> Color.rgb(58, 103, 61)
            "rock" -> Color.rgb(127, 125, 112)
            "water" -> Color.rgb(67, 123, 155)
            "field" -> Color.rgb(174, 164, 92)
            else -> Color.GRAY
        }
        val path = makePath(stroke.points)
        val radius = if (stroke.kind == "water") 100f else 86f
        val p = paint(color, Paint.Style.STROKE).apply {
            strokeWidth = radius * 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            alpha = 225
        }
        c.drawPath(path, p)
        val texture = paint(Color.argb(45, 35, 42, 30), Paint.Style.STROKE).apply {
            strokeWidth = radius * 0.45f
            strokeCap = Paint.Cap.ROUND
            pathEffect = if (stroke.kind == "field") DashPathEffect(floatArrayOf(14f, 10f), 0f) else null
        }
        c.drawPath(path, texture)
    }

    private fun drawRoad(c: Canvas, stroke: MapStroke) {
        if (stroke.points.isEmpty()) return
        val path = makePath(stroke.points)
        val width = if (stroke.kind == "highway") 54f else 31f
        val shadow = paint(Color.argb(110, 35, 37, 35), Paint.Style.STROKE).apply {
            strokeWidth = width + 9f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val shoulder = paint(Color.rgb(178, 174, 157), Paint.Style.STROKE).apply {
            strokeWidth = width + 4f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val asphalt = paint(Color.rgb(66, 69, 68), Paint.Style.STROKE).apply {
            strokeWidth = width; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        c.drawPath(path, shadow)
        c.drawPath(path, shoulder)
        c.drawPath(path, asphalt)
        val edge = paint(Color.rgb(218, 213, 190), Paint.Style.STROKE).apply {
            strokeWidth = 1.6f; pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
        }
        c.drawPath(path, edge)
        if (stroke.kind == "highway") {
            val line = paint(Color.rgb(237, 222, 161), Paint.Style.STROKE).apply { strokeWidth = 2.2f }
            c.drawPath(path, line)
        } else {
            val line = paint(Color.rgb(241, 207, 103), Paint.Style.STROKE).apply {
                strokeWidth = 2.4f
                pathEffect = DashPathEffect(floatArrayOf(13f, 12f), 0f)
            }
            c.drawPath(path, line)
        }
    }

    private fun makePath(points: List<PointF>): Path = Path().apply {
        if (points.isEmpty()) return@apply
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        if (points.size == 1) lineTo(points[0].x + 0.1f, points[0].y + 0.1f)
    }

    private fun drawObject(c: Canvas, obj: MapObject) {
        val x = obj.x
        val y = obj.y
        when (obj.kind) {
            "tree" -> {
                c.drawOval(x - 19, y - 12, x + 21, y + 18, paint(Color.argb(85, 25, 34, 22)))
                c.drawCircle(x, y, 17f, paint(Color.rgb(48, 82, 43)))
                c.drawCircle(x - 6, y - 5, 10f, paint(Color.rgb(72, 112, 57)))
                c.drawCircle(x + 7, y + 3, 8f, paint(Color.rgb(39, 73, 39)))
                c.drawCircle(x - 3, y - 9, 4f, paint(Color.argb(90, 165, 177, 112)))
            }
            else -> {
                val w = if (obj.kind == "block") 58f else if (obj.kind == "factory") 68f else 42f
                val h = if (obj.kind == "block") 48f else if (obj.kind == "factory") 58f else 36f
                c.drawRoundRect(x - w/2 + 5, y - h/2 + 6, x + w/2 + 5, y + h/2 + 6, 3f, 3f, paint(Color.argb(90, 35, 35, 30)))
                val roof = when (obj.kind) {
                    "block" -> Color.rgb(174, 169, 157)
                    "factory" -> Color.rgb(123, 129, 125)
                    else -> Color.rgb(188, 153, 116)
                }
                c.drawRect(x - w/2, y - h/2, x + w/2, y + h/2, paint(Color.rgb(79, 78, 70)))
                c.drawRect(x - w/2 + 3, y - h/2 + 3, x + w/2 - 3, y + h/2 - 3, paint(roof))
                c.drawLine(x - w/2 + 5, y - h/2 + 7, x + w/2 - 5, y - h/2 + 7, paint(Color.argb(100, 255, 255, 245), Paint.Style.STROKE).apply { strokeWidth = 2f })
                if (obj.kind == "factory") {
                    c.drawRect(x - 8, y - 9, x + 9, y + 10, paint(Color.rgb(95, 101, 98)))
                    c.drawRect(x + 18, y - 30, x + 25, y - 9, paint(Color.rgb(116, 112, 103)))
                } else {
                    val windows = paint(Color.rgb(91, 111, 111))
                    c.drawRect(x - w/2 + 7, y - 5, x - w/2 + 13, y + 3, windows)
                    c.drawRect(x + w/2 - 13, y - 5, x + w/2 - 7, y + 3, windows)
                }
            }
        }
    }

    private fun drawHud(c: Canvas) {
        val p = paint(Color.argb(190, 28, 32, 31))
        c.drawRoundRect(12f, 12f, 210f, 48f, 12f, 12f, p)
        val t = paint(Color.WHITE).apply { textSize = 12f * density; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) }
        c.drawText("МАСШТАБ  " + (zoom * 100).toInt() + "%", 25f, 35f, t)
        val hint = paint(Color.WHITE).apply { textSize = 10f * density; alpha = 210 }
        c.drawText("Два пальца — масштаб и перемещение", 14f, height - 14f, hint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                if (tool == "pan") return true
                val world = toWorld(event.x, event.y)
                when (tool) {
                    "house", "block", "factory", "tree" -> {
                        objects.add(MapObject(world.x, world.y, tool))
                        invalidate()
                    }
                    "erase" -> eraseAt(world.x, world.y)
                    "road", "highway", "terrain" -> {
                        val kind = if (tool == "terrain") terrain else tool
                        activeStroke = MapStroke(mutableListOf(world), kind)
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                activeStroke?.let { activeStroke = null }
                lastPinchX = (event.getX(0) + event.getX(1)) / 2f
                lastPinchY = (event.getY(0) + event.getY(1)) / 2f
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val cx = (event.getX(0) + event.getX(1)) / 2f
                    val cy = (event.getY(0) + event.getY(1)) / 2f
                    offsetX += cx - lastPinchX
                    offsetY += cy - lastPinchY
                    lastPinchX = cx
                    lastPinchY = cy
                } else if (tool == "pan") {
                    offsetX += event.x - lastX
                    offsetY += event.y - lastY
                } else {
                    activeStroke?.points?.add(toWorld(event.x, event.y))
                }
                lastX = event.x
                lastY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val index = if (event.actionIndex == 0) 1 else 0
                if (index < event.pointerCount) {
                    lastX = event.getX(index)
                    lastY = event.getY(index)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeStroke?.let { if (it.points.isNotEmpty()) strokes.add(it) }
                activeStroke = null
                performClick()
                invalidate()
                return true
            }
        }
        return true
    }

    private fun toWorld(x: Float, y: Float) = PointF((x - offsetX) / zoom, (y - offsetY) / zoom)

    private fun eraseAt(x: Float, y: Float) {
        val obj = objects.indexOfLast { kotlin.math.abs(it.x - x) < 55f && kotlin.math.abs(it.y - y) < 55f }
        if (obj >= 0) objects.removeAt(obj)
        else if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
        invalidate()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
