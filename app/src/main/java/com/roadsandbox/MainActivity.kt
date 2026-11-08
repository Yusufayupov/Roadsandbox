package com.roadsandbox

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.*
import com.google.android.material.button.MaterialButton
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var map: CityMapView
    private val selected = mutableMapOf<String, MaterialButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(24, 29, 30)
        window.navigationBarColor = Color.rgb(24, 29, 30)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(28, 33, 34))
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 3, 8, 3)
            setBackgroundColor(Color.rgb(24, 29, 30))
        }
        val title = TextView(this).apply {
            text = "ROAD SANDBOX  •  CITY BUILDER"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            letterSpacing = .06f
        }
        header.addView(title, LinearLayout.LayoutParams(0, 48, 1f))
        header.addView(button("↶", "Отменить") { map.undo() })
        header.addView(button("Сброс", "Очистить проект") { map.clearMap() })
        root.addView(header)
        map = CityMapView(this)
        root.addView(map, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(section("ДОРОЖНАЯ СЕТЬ", listOf(
            "Улица" to "street", "Проспект" to "avenue", "Автомагистраль" to "highway",
            "Съезд" to "ramp", "Мост" to "bridge", "Развязка" to "interchange", "Кольцо" to "roundabout"
        )))
        root.addView(section("ФИГУРЫ И ОБЪЕКТЫ", listOf(
            "Прямоуг." to "rect", "Круг" to "circle", "Треуг." to "triangle", "Многоуг." to "polygon",
            "Дом" to "house", "Квартал" to "block", "Завод" to "factory", "Парковка" to "parking",
            "Дерево" to "tree", "Ластик" to "erase", "Панорама" to "pan"
        )))
        root.addView(section("ЛАНДШАФТ", listOf(
            "Пустыня" to "desert", "Степь" to "steppe", "Лес" to "forest",
            "Горы" to "rock", "Вода" to "water", "Поля" to "field"
        )))
        root.addView(section("СЛОИ И МАСШТАБ", listOf(
            "Спутник" to "layer_satellite", "Рельеф" to "layer_terrain",
            "Дороги" to "layer_roads", "Здания" to "layer_buildings",
            "Природа" to "layer_nature", "−" to "zoom_out", "+" to "zoom_in"
        )))
        setContentView(root)
    }

    private fun button(label: String, desc: String, action: () -> Unit): MaterialButton {
        return MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            contentDescription = desc
            textSize = 10f
            isAllCaps = false
            insetTop = 1
            insetBottom = 1
            cornerRadius = 10
            minHeight = 38
            setPadding(10, 0, 10, 0)
            setOnClickListener { action() }
        }
    }

    private fun section(name: String, tools: List<Pair<String, String>>): HorizontalScrollView {
        val wrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(39, 45, 45)) }
        val label = TextView(this).apply {
            text = name
            textSize = 9f
            letterSpacing = .12f
            setTextColor(Color.rgb(166, 184, 173))
            setPadding(10, 5, 5, 0)
        }
        wrap.addView(label)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 0, 4, 3)
        }
        tools.forEach { (caption, key) ->
            lateinit var b: MaterialButton
            b = button(caption, caption) {
                when {
                    key.startsWith("layer_") -> map.toggleLayer(key.removePrefix("layer_"))
                    key == "zoom_in" -> map.zoomBy(1.25f)
                    key == "zoom_out" -> map.zoomBy(.8f)
                    else -> map.setTool(key)
                }
                selected.values.forEach { it.alpha = .78f }
                if (!key.startsWith("layer_") && !key.startsWith("zoom_")) {
                    selected[key] = b
                    b.alpha = 1f
                }
            }
            row.addView(b, LinearLayout.LayoutParams(-2, 39).apply { setMargins(2, 2, 2, 1) })
        }
        wrap.addView(row)
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(wrap)
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
    fun zoomBy(f: Float) {
        val old = zoom; zoom = (zoom*f).coerceIn(.18f,9f)
        val r = zoom/old; ox = width/2f-(width/2f-ox)*r; oy = height/2f-(height/2f-oy)*r
        invalidate()
    }
    fun undo() { if (active != null) active=null else if(items.isNotEmpty()) items.removeAt(items.lastIndex) else if(strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex); invalidate() }
    fun clearMap() { strokes.clear(); items.clear(); active=null; invalidate() }

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
        c.drawPath(path,p(Color.argb(110,20,23,22),Paint.Style.STROKE).apply { strokeWidth=w+13; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND })
        c.drawPath(path,p(if(bridge) 0xffc4bca7.toInt() else 0xffb8b5a6.toInt(),Paint.Style.STROKE).apply { strokeWidth=w+7; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND })
        c.drawPath(path,p(if(bridge) 0xff777d7c.toInt() else 0xff454a4a.toInt(),Paint.Style.STROKE).apply { strokeWidth=w; strokeCap=Paint.Cap.ROUND; strokeJoin=Paint.Join.ROUND })
        if(highway) {
            c.drawPath(path,p(0xffded9c6.toInt(),Paint.Style.STROKE).apply { strokeWidth=2.2f; pathEffect=DashPathEffect(floatArrayOf(11f,8f),0f) })
            val median=p(0xffd3c9a5.toInt(),Paint.Style.STROKE).apply { strokeWidth=7f }
            c.drawPath(path,median)
            c.drawPath(path,p(0xffffdf9a.toInt(),Paint.Style.STROKE).apply { strokeWidth=1.4f })
        } else {
            c.drawPath(path,p(0xffeee8d5.toInt(),Paint.Style.STROKE).apply { strokeWidth=1.7f })
            c.drawPath(path,p(if(ramp) 0xffe8d6a1.toInt() else 0xffffd16b.toInt(),Paint.Style.STROKE).apply {
                strokeWidth=if(ramp) 2f else 2.4f
                pathEffect=DashPathEffect(floatArrayOf(if(ramp)8f else 14f,if(ramp)8f else 13f),0f)
            })
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
        if(o.kind=="tree") {
            c.drawOval(x-22,y-10,x+24,y+21,p(0x55302c20))
            c.drawCircle(x,y,20f,p(0xff315d38.toInt()))
            c.drawCircle(x-8,y-6,12f,p(0xff56824b.toInt()))
            c.drawCircle(x+8,y+4,10f,p(0xff244c31.toInt()))
            c.drawCircle(x-3,y-11,4f,p(0x669dbb78))
            return
        }
        val w=when(o.kind) { "block"->76f; "factory"->94f; "parking"->88f; else->52f }
        val h=when(o.kind) { "block"->66f; "factory"->72f; "parking"->60f; else->46f }
        c.drawRect(x-w/2+6,y-h/2+7,x+w/2+6,y+h/2+7,p(0x77302c25))
        c.drawRect(x-w/2,y-h/2,x+w/2,y+h/2,p(0xff55554f.toInt()))
        val roof=when(o.kind) { "factory"->0xff92958d.toInt(); "block"->0xffa9a69c.toInt(); "parking"->0xff777f80.toInt(); else->0xffc1a17d.toInt() }
        c.drawRect(x-w/2+4,y-h/2+4,x+w/2-4,y+h/2-4,p(roof))
        if(o.kind=="parking") {
            val stripe=p(0xffe4dfc9.toInt(),Paint.Style.STROKE).apply { strokeWidth=2f }
            for(i in -3..3) c.drawLine(x+i*10,y-h/2+8,x+i*10,y+h/2-8,stripe)
        } else if(o.kind=="factory") {
            c.drawRect(x-12,y-13,x+16,y+13,p(0xff737873.toInt()))
            c.drawRect(x+25,y-43,x+34,y-10,p(0xff77736a.toInt()))
            c.drawRect(x-35,y-30,x-27,y-10,p(0xff77736a.toInt()))
        } else {
            val win=p(0xff526b6b.toInt())
            for(i in -1..1) for(j in -1..1) c.drawRect(x+i*13-3,y+j*11-3,x+i*13+3,y+j*11+3,win)
        }
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
                if(tool=="interchange") { drawInterchange(w.x,w.y); invalidate(); return true }
                if(tool=="roundabout") {
                    strokes.add(Stroke(mutableListOf(PointF(w.x-130,w.y),PointF(w.x-80,w.y-45),PointF(w.x+80,w.y-45),PointF(w.x+130,w.y),PointF(w.x+80,w.y+45),PointF(w.x-80,w.y+45),PointF(w.x-130,w.y)),"avenue"))
                    items.add(Item(w.x,w.y,"tree")); invalidate(); return true
                }
                if(tool in listOf("house","block","factory","parking","tree")) { items.add(Item(w.x,w.y,tool)); invalidate(); return true }
                if(tool=="erase") { erase(w.x,w.y); return true }
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
                shapeStart=null; preview=null; performClick(); invalidate(); return true
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
