package com.roadsandbox

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Typeface
import android.view.Gravity
import android.graphics.Color

class MainActivity : Activity() {
    private lateinit var roadView: RoadView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(238, 235, 225))
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(248, 247, 242))
        }
        val title = TextView(this).apply {
            text = "ROAD SANDBOX"
            textSize = 15f
            setTextColor(Color.rgb(45, 49, 50))
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(4, 0, 12, 0)
        }
        val undo = Button(this).apply { text = "UNDO" }
        val clear = Button(this).apply { text = "CLEAR" }
        bar.addView(title, LinearLayout.LayoutParams(0, 48, 1f))
        bar.addView(undo)
        bar.addView(clear)
        roadView = RoadView(this)
        undo.setOnClickListener { roadView.undo() }
        clear.setOnClickListener { roadView.clear() }
        root.addView(bar, LinearLayout.LayoutParams(-1, 58))
        root.addView(roadView, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }
}

private class RoadView(context: android.content.Context) : View(context) {
    private val roads = mutableListOf<MutableList<PointF>>()
    private var current: MutableList<PointF>? = null
    private val density = resources.displayMetrics.density
    private val ground = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(231, 226, 211) }
    private val speckle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(20, 110, 100, 80) }
    private val shadow = roadPaint(Color.rgb(90, 91, 87), 38f)
    private val border = roadPaint(Color.rgb(190, 190, 180), 34f)
    private val asphalt = roadPaint(Color.rgb(60, 64, 65), 29f)
    private val edge = roadPaint(Color.rgb(235, 233, 219), 1.3f)
    private val center = roadPaint(Color.rgb(244, 207, 91), 2.1f).apply {
        pathEffect = DashPathEffect(floatArrayOf(13f * density, 10f * density), 0f)
    }

    private fun roadPaint(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        this.color = color
        strokeWidth = width * density
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(231, 226, 211))
        val step = (24f * density).coerceAtLeast(12f)
        var y = 10f
        while (y < height) {
            var x = 8f
            while (x < width) {
                canvas.drawCircle(x, y, 0.65f * density, speckle)
                x += step
            }
            y += step
        }
        roads.forEach { drawRoad(canvas, it) }
        current?.let { drawRoad(canvas, it) }
        if (roads.isEmpty() && current == null) {
            val hint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(153, 148, 133)
                textSize = 14f * density
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Draw a road with your finger", width / 2f, height / 2f, hint)
        }
    }

    private fun drawRoad(canvas: Canvas, points: List<PointF>) {
        if (points.isEmpty()) return
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            if (points.size == 1) lineTo(points[0].x + 0.1f, points[0].y + 0.1f)
            else for (i in 1 until points.size) {
                val p = points[i]
                lineTo(p.x, p.y)
            }
        }
        canvas.drawPath(path, shadow)
        canvas.drawPath(path, border)
        canvas.drawPath(path, asphalt)
        val edgePaint = roadPaint(Color.rgb(225, 224, 211), 1.1f)
        val centerPaint = roadPaint(Color.rgb(244, 207, 91), 2f).apply {
            pathEffect = DashPathEffect(floatArrayOf(12f * density, 10f * density), 0f)
        }
        canvas.drawPath(path, centerPaint)
    }

    fun undo() {
        if (roads.isNotEmpty()) roads.removeAt(roads.lastIndex)
        invalidate()
    }
    fun clear() {
        roads.clear()
        current = null
        invalidate()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                current = mutableListOf(PointF(event.x, event.y))
                parent.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                current?.add(PointF(event.x, event.y))
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                current?.let { if (it.isNotEmpty()) roads.add(it) }
                current = null
                performClick()
                invalidate()
                return true
            }
        }
        return true
    }
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
