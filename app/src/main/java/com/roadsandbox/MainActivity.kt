package com.roadsandbox

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import com.google.android.material.button.MaterialButton
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var map: CityMapView
    private val activeButtons = mutableMapOf<String, MaterialButton>()
    private lateinit var leftTools: ScrollView
    private lateinit var rightTools: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = 0
        window.statusBarColor = Color.rgb(20, 26, 27)
        window.navigationBarColor = Color.rgb(20, 26, 27)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(24, 30, 31))
            fitsSystemWindows = true
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(8), 0)
            setBackgroundColor(Color.rgb(20, 26, 27))
        }
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        brand.addView(TextView(this).apply {
            text = "POCKET CITY SANDBOX"
            textSize = 13f
            letterSpacing = .08f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        })
        brand.addView(TextView(this).apply {
            text = "WORLD BUILDER  •  2D EDITOR"
            textSize = 8f
            letterSpacing = .12f
            setTextColor(Color.rgb(151, 172, 157))
        })
        header.addView(brand, LinearLayout.LayoutParams(0, dp(48), 1f))
        header.addView(topButton("↶", "Отменить") { map.undo() })
        header.addView(topButton("⌖", "К центру") { map.centerMap() })
        header.addView(topButton("×", "Очистить карту") { map.clearMap() })
        header.addView(topButton("◀", "Скрыть или показать левую панель") { leftTools.visibility = if (leftTools.visibility == View.VISIBLE) View.GONE else View.VISIBLE })
        header.addView(topButton("▶", "Скрыть или показать правую панель") { rightTools.visibility = if (rightTools.visibility == View.VISIBLE) View.GONE else View.VISIBLE })
        root.addView(header, LinearLayout.LayoutParams(-1, dp(52)))

        val workspace = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(29, 35, 35))
        }
        map = CityMapView(this)
        leftTools = toolPanel("СТРОИТЕЛЬСТВО", listOf(
            Triple("↗", "Улица", "street"),
            Triple("▰", "Проспект", "avenue"),
            Triple("▤", "Шоссе", "highway"),
            Triple("⌁", "Съезд", "ramp"),
            Triple("▥", "Мост", "bridge"),
            Triple("✣", "Развязка", "interchange"),
            Triple("◉", "Кольцо", "roundabout"),
            Triple("□", "Фигура", "rect"),
            Triple("○", "Круг", "circle"),
            Triple("△", "Треуг.", "triangle"),
            Triple("⬡", "Полигон", "polygon")
        ))
        workspace.addView(leftTools, LinearLayout.LayoutParams(dp(78), -1))
        workspace.addView(map, LinearLayout.LayoutParams(0, -1, 1f))
        rightTools = toolPanel("МИР И ОБЪЕКТЫ", listOf(
            Triple("⌂", "Дом", "house"),
            Triple("▦", "Квартал", "block"),
            Triple("▥", "Завод", "factory"),
            Triple("▧", "Паркинг", "parking"),
            Triple("▣", "Офис", "office"),
            Triple("▤", "Магазин", "shop"),
            Triple("✚", "Больница", "hospital"),
            Triple("▧", "Школа", "school"),
            Triple("▩", "Склад", "warehouse"),
            Triple("♣", "Дерево", "tree"),
            Triple("♧", "Парк", "park"),
            Triple("▱", "Площадь", "plaza"),
            Triple("⌫", "Ластик", "erase"),
            Triple("砂", "Пустыня", "desert"),
            Triple("♧", "Степь", "steppe"),
            Triple("♠", "Лес", "forest"),
            Triple("▲", "Горы", "rock"),
            Triple("≈", "Вода", "water"),
            Triple("▨", "Поля", "field"),
            Triple("◩", "Спутник", "layer_satellite"),
            Triple("◫", "Рельеф", "layer_terrain"),
            Triple("−", "Уменьшить", "zoom_out"),
            Triple("+", "Увеличить", "zoom_in")
        ))
        workspace.addView(rightTools, LinearLayout.LayoutParams(dp(78), -1))
        root.addView(workspace, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        map.loadWorld()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()

    private fun topButton(label: String, hint: String, action: () -> Unit) =
        MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            contentDescription = hint
            textSize = 16f
            isAllCaps = false
            minWidth = dp(40)
            minimumWidth = dp(40)
            minimumHeight = dp(38)
            insetTop = 0
            insetBottom = 0
            cornerRadius = dp(9)
            setPadding(dp(4), 0, dp(4), 0)
            setOnClickListener { action() }
        }

    private fun toolPanel(title: String, entries: List<Triple<String, String, String>>): ScrollView {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(dp(3), dp(5), dp(3), dp(8))
            setBackgroundColor(Color.rgb(35, 43, 43))
        }
        panel.addView(TextView(this).apply {
            text = title
            textSize = 7f
            letterSpacing = .05f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(156, 178, 162))
            setPadding(0, dp(3), 0, dp(6))
        })
        entries.forEach { (glyph, caption, key) ->
            lateinit var b: MaterialButton
            b = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "$glyph\n$caption"
                contentDescription = caption
                textSize = 8f
                isAllCaps = false
                gravity = Gravity.CENTER
                maxLines = 2
                minWidth = 0
                minimumWidth = 0
                minimumHeight = dp(48)
                insetTop = 0
                insetBottom = 0
                cornerRadius = dp(8)
                setPadding(dp(1), dp(2), dp(1), dp(2))
                setTextColor(Color.rgb(232, 235, 224))
                strokeWidth = dp(1)
                strokeColor = android.content.res.ColorStateList.valueOf(Color.rgb(73, 88, 80))
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(45, 55, 53))
                setOnClickListener {
                    when {
                        key.startsWith("layer_") -> map.toggleLayer(key.removePrefix("layer_"))
                        key == "zoom_in" -> map.zoomBy(1.25f)
                        key == "zoom_out" -> map.zoomBy(.8f)
                        else -> map.setTool(key)
                    }
                    activeButtons.values.forEach {
                        it.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(45, 55, 53))
                    }
                    if (!key.startsWith("layer_") && !key.startsWith("zoom_")) {
                        activeButtons[key] = b
                        b.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(89, 119, 91))
                    }
                }
            }
            panel.addView(b, LinearLayout.LayoutParams(-1, dp(51)).apply { bottomMargin = dp(4) })
        }
        return ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            addView(panel)
        }
    }
}

private data class Stroke(val points: MutableList<PointF>, val kind: String)
private data class Item(val x: Float, val y: Float, val kind: String, val w: Float = 0f, val h: Float = 0f)

private class CityMapView(context: android.content.Context) : View(context) {
    private val strokes = mutableListOf<Stroke>()
    private val items = mutableListOf<Item>()
    private var active: Stroke? = null
    private var preview: RectF? = null
    private var tool = "street"
    private var terrain = "desert"
    private var zoom = 1f
    private var ox = 0f
    private var oy = 0f
    private var ready = false
    private var lastX = 0f
    private var lastY = 0f
    private var pinchX = 0f
    private var pinchY = 0f
    private var showSatellite = true
    private var showTerrain = true
    private var showRoads = true
    private var showBuildings = true
    private var showNature = true
    private var shapeStart: PointF? = null
    private val scaler = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            val old = zoom
            zoom = (zoom * d.scaleFactor).coerceIn(.18f, 9f)
            val r = zoom / old
            ox = d.focusX - (d.focusX - ox) * r
            oy = d.focusY - (d.focusY - oy) * r
            invalidate()
            return true
        }
    })

    private fun p(color: Int, style: Paint.Style = Paint.Style.FILL) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; this.style = style }
    init { isFocusable = true; setLayerType(LAYER_TYPE_SOFTWARE, null) }

    fun setTool(value: String) {
        tool = value
        if (value in listOf("desert","steppe","forest","rock","water","field")) { terrain = value; tool = "terrain" }
        invalidate()
    }
    fun toggleLayer(v: String) {
        when(v) {
            "satellite" -> showSatellite = !showSatellite
            "terrain" -> showTerrain = !showTerrain
            "roads" -> showRoads = !showRoads
            "buildings" -> showBuildings = !showBuildings
            "nature" -> showNature = !showNature
        }
        invalidate()
    }
    fun centerMap() { ox=width/2f; oy=height/2f; zoom=1f; invalidate() }
    fun zoomBy(f: Float) {
        val old = zoom; zoom = (zoom*f).coerceIn(.18f,9f)
        val r = zoom/old; ox = width/2f-(width/2f-ox)*r; oy = height/2f-(height/2f-oy)*r
        invalidate()
    }
    fun undo() { if (active != null) active=null else if(items.isNotEmpty()) items.removeAt(items.lastIndex) else if(strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex); saveWorld(); invalidate() }
    fun clearMap() { strokes.clear(); items.clear(); active=null; saveWorld(); invalidate() }

    fun saveWorld() {
        try {
            val root = JSONObject()
            val strokeArray = JSONArray()
            strokes.forEach { stroke ->
                val obj = JSONObject().put("kind", stroke.kind)
                val pts = JSONArray()
                stroke.points.forEach { pts.put(JSONObject().put("x", it.x.toDouble()).put("y", it.y.toDouble())) }
                obj.put("points", pts); strokeArray.put(obj)
            }
            val itemArray = JSONArray()
            items.forEach { itemArray.put(JSONObject().put("x", item.x.toDouble()).put("y", item.y.toDouble()).put("kind", item.kind).put("w", item.w.toDouble()).put("h", item.h.toDouble())) }
            root.put("strokes", strokeArray).put("items", itemArray)
            context.getSharedPreferences("city_sandbox_world", android.content.Context.MODE_PRIVATE).edit().putString("world", root.toString()).apply()
        } catch (_: Exception) { }
    }

    fun loadWorld() {
        try {
            val raw = context.getSharedPreferences("city_sandbox_world", android.content.Context.MODE_PRIVATE).getString("world", null) ?: return
            val root = JSONObject(raw)
            strokes.clear(); items.clear()
            val sa = root.optJSONArray("strokes") ?: JSONArray()
            for (i in 0 until sa.length()) {
                val obj = sa.getJSONObject(i); val pts = obj.optJSONArray("points") ?: JSONArray()
                val list = mutableListOf<PointF>()
                for (j in 0 until pts.length()) { val q=pts.getJSONObject(j); list.add(PointF(q.optDouble("x").toFloat(), q.optDouble("y").toFloat())) }
                strokes.add(Stroke(list, obj.optString("kind", "street")))
            }
            val ia = root.optJSONArray("items") ?: JSONArray()
            for (i in 0 until ia.length()) { val q=ia.getJSONObject(i); items.add(Item(q.optDouble("x").toFloat(), q.optDouble("y").toFloat(), q.optString("kind", "house"), q.optDouble("w").toFloat(), q.optDouble("h").toFloat())) }
            invalidate()
        } catch (_: Exception) { }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if(!ready) { ox=width/2f; oy=height/2f; ready=true }
        c.drawColor(Color.rgb(112,120,91))
        c.save(); c.translate(ox,oy); c.scale(zoom,zoom)
        val l=-ox/zoom-120; val t=-oy/zoom-120; val r=(width-ox)/zoom+120; val b=(height-oy)/zoom+120
        if(showSatellite) drawSatellite(c,l,t,r,b)
        if(showTerrain) strokes.filter { it.kind in TERRAIN }.forEach { drawTerrain(c,it) }
        if(showRoads) {
            strokes.filter { it.kind in ROADS }.forEach { drawRoad(c,it) }
            active?.takeIf { it.kind in ROADS }?.let { drawRoad(c,it) }
        }
        items.forEach { item ->
            if(item.kind=="tree" && showNature) drawItem(c,item)
            else if(item.kind!="tree" && showBuildings) drawItem(c,item)
        }
        active?.takeIf { it.kind !in ROADS && it.kind !in TERRAIN }?.let { drawShape(c,it) }
        preview?.let { drawGeometry(c,it,tool) }
        c.restore()
        hud(c)
    }

    private fun hash(x:Int,y:Int):Float { val n=sin(x*127.1+y*311.7)*43758.5453; return (n-floor(n)).toFloat() }
    private fun drawSatellite(c:Canvas,l:Float,t:Float,r:Float,b:Float) {
        c.drawColor(Color.rgb(145,148,111))
        val cell=128f
        for(y in floor(t/cell).toInt()..floor(b/cell).toInt()) for(x in floor(l/cell).toInt()..floor(r/cell).toInt()) {
            val h=hash(x,y); val col=when { h<.18f->0xff777d54.toInt(); h<.38f->0xffa49b70.toInt(); h<.61f->0xff858f62.toInt(); h<.8f->0xffb1a77b.toInt(); else->0xff6d8058.toInt() }
            val xx=x*cell; val yy=y*cell
            c.drawRect(xx,yy,xx+cell+1,yy+cell+1,p(col))
            for(i in 0..8) {
                val px=xx+hash(x*17+i,y*13)*cell; val py=yy+hash(x*7,y*19+i)*cell
                c.drawOval(px,py,px+12+hash(i,x)*34,py+8+hash(y,i)*25,p(Color.argb(35,47,55,35)))
            }
            if(h>.72f) {
                val field=p(Color.argb(35,224,211,159),Paint.Style.STROKE).apply { strokeWidth=1.4f }
                c.drawRect(xx+12,yy+12,xx+cell-10,yy+cell-10,field)
            }
        }
        val fine=48f
        for(y in floor(t/fine).toInt()..floor(b/fine).toInt()) for(x in floor(l/fine).toInt()..floor(r/fine).toInt()) {
            val h=hash(x+331,y-91)
            if(h>.74f) c.drawCircle(x*fine+20,y*fine+18,3+h*7,p(Color.argb(48,48,65,40)))
        }
    }

    private fun drawTerrain(c:Canvas,s:Stroke) {
        if(s.points.isEmpty()) return
        val col=when(s.kind) { "desert"->0xffd1ad70.toInt(); "steppe"->0xff9ca36a.toInt(); "forest"->0xff456d45.toInt(); "rock"->0xff88877d.toInt(); "water"->0xff477e9c.toInt(); else->0xffb8a95d.toInt() }
        val path=path(s.points)
        val w=if(s.kind=="water")190f else 155f
        c.drawPath(path,p(col,Paint.Style.STROKE).apply { strokeWidth=w; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND; alpha=225 })
        val texture=p(Color.argb(45,25,35,28),Paint.Style.STROKE).apply { strokeWidth=w*.32f; strokeCap=Paint.Cap.ROUND; pathEffect=DashPathEffect(floatArrayOf(10f,16f),0f) }
        c.drawPath(path,texture)
        if(s.kind=="water") c.drawPath(path,p(Color.argb(75,210,228,220),Paint.Style.STROKE).apply { strokeWidth=3f; pathEffect=DashPathEffect(floatArrayOf(18f,22f),0f) })
    }

    private fun drawRoad(c:Canvas,s:Stroke) {
        if(s.points.isEmpty()) return
        val path=path(s.points)
        val highway=s.kind=="highway"
        val ramp=s.kind=="ramp"
        val bridge=s.kind=="bridge"
        val w=when(s.kind) { "highway"->78f; "avenue"->52f; "ramp"->37f; "bridge"->42f; else->28f }
        // Raised curb, shoulder and asphalt layers create a miniature city-builder look.
        c.drawPath(path,p(Color.argb(125,18,22,20),Paint.Style.STROKE).apply { strokeWidth=w+18; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND })
        c.drawPath(path,p(if(bridge) 0xffc9c1ae.toInt() else 0xff9b9d91.toInt(),Paint.Style.STROKE).apply { strokeWidth=w+10; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND })
        c.drawPath(path,p(if(bridge) 0xff747b7b.toInt() else 0xff3d4242.toInt(),Paint.Style.STROKE).apply { strokeWidth=w; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND })
        // Fine asphalt grain and worn wheel tracks.
        c.drawPath(path,p(Color.argb(35,205,205,190),Paint.Style.STROKE).apply {
            strokeWidth=w*.62f; strokeCap=Paint.Cap.ROUND; pathEffect=DashPathEffect(floatArrayOf(2f,7f),2f)
        })
        if(highway) {
            // Two carriageways divided by a raised, pale concrete median.
            c.drawPath(path,p(0xffd2c8a8.toInt(),Paint.Style.STROKE).apply { strokeWidth=8f; strokeCap=Paint.Cap.ROUND })
            c.drawPath(path,p(0xfff0e9d4.toInt(),Paint.Style.STROKE).apply { strokeWidth=2f; pathEffect=DashPathEffect(floatArrayOf(12f,9f),0f) })
            c.drawPath(path,p(0xffbdb7a5.toInt(),Paint.Style.STROKE).apply { strokeWidth=1f })
        } else {
            c.drawPath(path,p(0xffd8d1bd.toInt(),Paint.Style.STROKE).apply { strokeWidth=1.6f })
            c.drawPath(path,p(if(ramp) 0xffeee1b6.toInt() else 0xffffd16b.toInt(),Paint.Style.STROKE).apply {
                strokeWidth=if(ramp) 1.8f else 2.2f
                pathEffect=DashPathEffect(floatArrayOf(if(ramp)7f else 13f,if(ramp)7f else 12f),0f)
            })
            if(s.kind=="avenue") {
                c.drawPath(path,p(0xffd5d0bd.toInt(),Paint.Style.STROKE).apply { strokeWidth=1f; pathEffect=DashPathEffect(floatArrayOf(3f,8f),0f) })
            }
        }
        if(bridge) {
            val bounds=RectF(); path.computeBounds(bounds,true)
            val rails=p(0xffd7d0bd.toInt(),Paint.Style.STROKE).apply { strokeWidth=2.5f; pathEffect=DashPathEffect(floatArrayOf(5f,7f),0f) }
            c.drawPath(path,rails)
        }
    }

    private fun path(points:List<PointF>)=Path().apply {
        if(points.isEmpty()) return@apply
        moveTo(points[0].x,points[0].y)
        for(i in 1 until points.size) {
            val prev=points[i-1]; val cur=points[i]
            val mx=(prev.x+cur.x)/2; val my=(prev.y+cur.y)/2
            if(i==1) lineTo(mx,my)
            quadTo(prev.x,prev.y,mx,my)
            if(i==points.lastIndex) lineTo(cur.x,cur.y)
        }
    }

    private fun drawShape(c:Canvas,s:Stroke) {
        if(s.points.size<2) return
        val a=s.points.first(); val z=s.points.last()
        val rect=RectF(min(a.x,z.x),min(a.y,z.y),max(a.x,z.x),max(a.y,z.y))
        drawGeometry(c,rect,s.kind)
    }

    private fun drawGeometry(c:Canvas,r:RectF,kind:String) {
        val fill=p(0x557f9e9c,Paint.Style.FILL)
        val edge=p(0xffe8e4d6.toInt(),Paint.Style.STROKE).apply { strokeWidth=3f }
        when(kind) {
            "rect" -> { c.drawRect(r,fill); c.drawRect(r,edge) }
            "circle" -> { val rad=min(r.width(),r.height())/2; c.drawCircle(r.centerX(),r.centerY(),rad,fill); c.drawCircle(r.centerX(),r.centerY(),rad,edge) }
            "triangle" -> {
                val q=Path().apply { moveTo(r.centerX(),r.top); lineTo(r.right,r.bottom); lineTo(r.left,r.bottom); close() }
                c.drawPath(q,fill); c.drawPath(q,edge)
            }
            "polygon" -> {
                val q=Path().apply { for(i in 0..5) { val an=-Math.PI/2+i*Math.PI/3; val x=r.centerX()+cos(an).toFloat()*r.width()/2; val y=r.centerY()+sin(an).toFloat()*r.height()/2; if(i==0) moveTo(x,y) else lineTo(x,y) }; close() }
                c.drawPath(q,fill); c.drawPath(q,edge)
            }
        }
    }

    private fun drawItem(c:Canvas,o:Item) {
        val x=o.x; val y=o.y
        if(o.kind=="park" || o.kind=="plaza") {
            val ground = if(o.kind=="park") 0xff668451.toInt() else 0xffb8b19b.toInt()
            c.drawRoundRect(x-50,y-44,x+50,y+44,8f,8f,p(0xff4b5546.toInt()))
            c.drawRoundRect(x-46,y-40,x+46,y+40,6f,6f,p(ground))
            if(o.kind=="park") { for(dx in listOf(-27f,27f)) for(dy in listOf(-18f,18f)) { c.drawCircle(x+dx,y+dy,12f,p(0xff315d38.toInt())); c.drawCircle(x+dx-3,y+dy-4,5f,p(0xff6f9258.toInt())) } }
            else { c.drawCircle(x,y,12f,p(0xff7b9a9b.toInt())); c.drawCircle(x,y,6f,p(0xffcbd4c6.toInt())) }
            return
        }
        if(o.kind=="tree") {
            // Layered canopy with trunk, highlights and soft contact shadow.
            c.drawOval(x-25,y+5,x+28,y+26,p(0x66302b20))
            c.drawRect(x-3,y-3,x+3,y+20,p(0xff76563a.toInt()))
            c.drawCircle(x,y-8,22f,p(0xff244d31.toInt()))
            c.drawCircle(x-9,y-14,14f,p(0xff477849.toInt()))
            c.drawCircle(x+9,y-5,13f,p(0xff356b3d.toInt()))
            c.drawCircle(x-4,y-19,6f,p(0xff71915a.toInt()))
            c.drawCircle(x+11,y-12,4f,p(0x667fa56a))
            return
        }
        val w=when(o.kind) { "block"->82f; "factory"->100f; "parking"->92f; else->58f }
        val h=when(o.kind) { "block"->70f; "factory"->78f; "parking"->64f; else->52f }
        val wall=when(o.kind) { "factory"->0xff777b74.toInt(); "block"->0xff8c877b.toInt(); else->0xffa18a70.toInt() }
        val roof=when(o.kind) { "factory"->0xffa9aaa1.toInt(); "block"->0xffb9b3a5.toInt(); "parking"->0xff7e8888.toInt(); else->0xffc9a783.toInt() }
        // Isometric-style extruded walls and roof, similar to miniature city builders.
        val side=Path().apply { moveTo(x-w/2,y-h/2); lineTo(x-w/2+9,y-h/2+7); lineTo(x-w/2+9,y+h/2+7); lineTo(x-w/2,y+h/2); close() }
        c.drawPath(side,p(0xff555951.toInt()))
        val front=Path().apply { moveTo(x-w/2,y+h/2); lineTo(x-w/2+9,y+h/2+7); lineTo(x+w/2+9,y+h/2+7); lineTo(x+w/2,y+h/2); close() }
        c.drawPath(front,p(wall))
        c.drawRect(x-w/2,y-h/2,x+w/2,y+h/2,p(wall))
        c.drawRect(x-w/2+4,y-h/2+4,x+w/2-4,y+h/2-4,p(roof))
        c.drawRect(x-w/2+4,y-h/2+4,x+w/2-4,y-h/2+8,p(Color.argb(60,255,255,255)))
        if(o.kind=="parking") {
            val stripe=p(0xffe8e3d1.toInt(),Paint.Style.STROKE).apply { strokeWidth=2f }
            for(i in -3..3) c.drawLine(x+i*10,y-h/2+10,x+i*10,y+h/2-9,stripe)
            c.drawRect(x-17,y-9,x+17,y+9,p(0xff5f6869.toInt()))
        } else if(o.kind=="hospital" || o.kind=="school") {
            val sign = if(o.kind=="hospital") "+" else "S"
            c.drawRect(x-12,y-18,x+12,y-12,p(0xfff4f1e5.toInt()))
            c.drawRect(x-4,y-22,x+4,y-8,p(0xffb94f48.toInt()))
            c.drawText(sign,x+22,y-18,p(Color.WHITE).apply { textSize=13f; typeface=Typeface.DEFAULT_BOLD })
        } else if(o.kind=="factory" || o.kind=="warehouse") {
            c.drawRect(x-17,y-14,x+18,y+14,p(0xff777c76.toInt()))
            c.drawRect(x+26,y-47,x+36,y-10,p(0xff77736a.toInt()))
            c.drawRect(x-37,y-34,x-27,y-10,p(0xff77736a.toInt()))
            c.drawOval(x+24,y-54,x+39,y-42,p(0x447c8580))
        } else {
            val win=p(0xff526b6b.toInt())
            for(i in -1..1) for(j in -1..1) {
                c.drawRect(x+i*14-4,y+j*12-4,x+i*14+4,y+j*12+4,win)
                c.drawRect(x+i*14-4,y+j*12-4,x+i*14+4,y+j*12-2,p(0xffa8b6a8.toInt()))
            }
            if(o.kind=="house") {
                val roofPath=Path().apply { moveTo(x-w/2+3,y-h/2+3); lineTo(x,y-h/2-12); lineTo(x+w/2-3,y-h/2+3); close() }
                c.drawPath(roofPath,p(0xff985f49.toInt()))
            }
        }
        c.drawRect(x-w/2+4,y+h/2-3,x+w/2-4,y+h/2,p(0x55302b22))
    }

    private fun drawInterchange(x:Float,y:Float) {
        // Four loop ramps around a grade-separated motorway crossing.
        val radius=76f
        for(sx in listOf(-1f,1f)) for(sy in listOf(-1f,1f)) {
            val q=Path().apply {
                moveTo(x+sx*18,y+sy*18)
                cubicTo(x+sx*105,y+sy*18,x+sx*105,y+sy*105,x+sx*18,y+sy*105)
                cubicTo(x+sx*2,y+sy*105,x+sx*2,y+sy*88,x+sx*18,y+sy*88)
            }
            strokes.add(Stroke(mutableListOf(PointF(x+sx*18,y+sy*18),PointF(x+sx*radius,y+sy*18),PointF(x+sx*radius,y+sy*radius),PointF(x+sx*18,y+sy*radius)), "ramp"))
        }
        strokes.add(Stroke(mutableListOf(PointF(x-190,y),PointF(x+190,y)),"highway"))
        strokes.add(Stroke(mutableListOf(PointF(x,y-190),PointF(x,y+190)),"highway"))
        items.add(Item(x,y,"block",100f,100f))
    }

    private fun hud(c:Canvas) {
        c.drawRoundRect(12f,12f,220f,49f,12f,12f,p(0xdd1c2423.toInt()))
        c.drawText("МАСШТАБ  ${(zoom*100).toInt()}%",25f,36f,p(Color.WHITE).apply { textSize=12f*resources.displayMetrics.density; typeface=Typeface.DEFAULT_BOLD })
        c.drawText("Проведи пальцем • два пальца — обзор",14f,height-14f,p(Color.WHITE).apply { textSize=10f*resources.displayMetrics.density; alpha=220 })
    }

    override fun onTouchEvent(e:MotionEvent):Boolean {
        scaler.onTouchEvent(e)
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX=e.x; lastY=e.y
                if(tool=="pan") return true
                val w=world(e.x,e.y)
                if(tool=="interchange") { drawInterchange(w.x,w.y); saveWorld(); invalidate(); return true }
                if(tool=="roundabout") {
                    strokes.add(Stroke(mutableListOf(PointF(w.x-130,w.y),PointF(w.x-80,w.y-45),PointF(w.x+80,w.y-45),PointF(w.x+130,w.y),PointF(w.x+80,w.y+45),PointF(w.x-80,w.y+45),PointF(w.x-130,w.y)),"avenue"))
                    items.add(Item(w.x,w.y,"tree")); saveWorld(); invalidate(); return true
                }
                if(tool in listOf("house","block","factory","parking","office","shop","hospital","school","warehouse","tree","park","plaza")) { items.add(Item(w.x,w.y,tool)); saveWorld(); invalidate(); return true }
                if(tool=="erase") { erase(w.x,w.y); saveWorld(); return true }
                if(tool in listOf("rect","circle","triangle","polygon")) { shapeStart=w; preview=RectF(w.x,w.y,w.x,w.y); return true }
                if(tool in ROADS || tool=="terrain") { active=Stroke(mutableListOf(w),if(tool=="terrain") terrain else tool); invalidate() }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> { active=null; shapeStart=null; preview=null; pinchX=(e.getX(0)+e.getX(1))/2; pinchY=(e.getY(0)+e.getY(1))/2; return true }
            MotionEvent.ACTION_MOVE -> {
                if(e.pointerCount>=2) {
                    val cx=(e.getX(0)+e.getX(1))/2; val cy=(e.getY(0)+e.getY(1))/2
                    ox+=cx-pinchX; oy+=cy-pinchY; pinchX=cx; pinchY=cy
                } else if(tool=="pan") { ox+=e.x-lastX; oy+=e.y-lastY }
                else if(shapeStart!=null) { val q=world(e.x,e.y); preview=RectF(shapeStart!!.x,shapeStart!!.y,q.x,q.y) }
                else active?.points?.add(world(e.x,e.y))
                lastX=e.x; lastY=e.y; invalidate(); return true
            }
            MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {
                active?.let { if(it.points.size>0) strokes.add(it) }; active=null
                val start=shapeStart; val pr=preview
                if(start!=null && pr!=null) {
                    val q=world(e.x,e.y); val points=mutableListOf(start, q)
                    strokes.add(Stroke(points,tool))
                }
                shapeStart=null; preview=null; saveWorld(); performClick(); invalidate(); return true
            }
        }
        return true
    }

    private fun world(x:Float,y:Float)=PointF((x-ox)/zoom,(y-oy)/zoom)
    private fun erase(x:Float,y:Float) {
        val i=items.indexOfLast { abs(it.x-x)<70 && abs(it.y-y)<70 }
        if(i>=0) items.removeAt(i) else if(strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
        invalidate()
    }
    override fun performClick():Boolean { super.performClick(); return true }

    companion object {
        val ROADS=setOf("street","avenue","highway","ramp","bridge")
        val TERRAIN=setOf("desert","steppe","forest","rock","water","field")
    }
}
