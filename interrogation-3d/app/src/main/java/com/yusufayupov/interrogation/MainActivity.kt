package com.yusufayupov.interrogation

import android.app.Activity
import android.os.Bundle
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.media.ToneGenerator
import android.media.AudioManager
import kotlin.math.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.rgb(13, 17, 23)
        window.navigationBarColor = Color.rgb(13, 17, 23)
        setContentView(InterrogationView(this))
    }
}

private class InterrogationView : View {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
    private var started = System.currentTimeMillis()
    private var actionAt = 0L
    private var action = 0
    private var line = 0
    private var pressure = 0
    private var trust = 0
    private var showEvidence = false
    private val dialogues = arrayOf(
        "Men hech narsani bilmayman... Iltimos, menga ishoning.",
        "Siz aytgan vaqt bilan kamera yozuvi mos kelmayapti.",
        "Mayli... men u yerda edim. Lekin yolg'iz emasdim.",
        "Uning ismi... men buni aytishim kerakmi?"
    )
    private val buttons = arrayOf("SAVOL BER", "DALIL KO'RSAT", "QAT'IY GAPIR", "QO'L URISH")
    private val colors = intArrayOf(Color.rgb(55, 126, 170), Color.rgb(62, 132, 105), Color.rgb(173, 112, 54), Color.rgb(145, 65, 62))

    constructor(context: android.content.Context) : super(context) {
        isFocusable = true
    }

    private fun paint(color: Int, style: Paint.Style = Paint.Style.FILL, width: Float = 1f): Paint {
        p.color = color; p.style = style; p.strokeWidth = width; p.shader = null
        p.strokeCap = Paint.Cap.ROUND; p.strokeJoin = Paint.Join.ROUND
        return p
    }
    private fun rect(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int, radius: Float = 0f) {
        c.drawRoundRect(l,t,r,b,radius,radius,paint(color))
    }
    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false) {
        p.typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
        p.textSize = size; p.color = color; p.style = Paint.Style.FILL; p.shader = null
        c.drawText(s,x,y,p)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sx = width / 420f
        val sy = height / 840f
        val save = canvas.save()
        canvas.scale(sx, sy)
        val now = System.currentTimeMillis()
        val elapsed = (now - started) / 1000f
        val hit = if (action == 4) ((now-actionAt)/650f).coerceIn(0f,1f) else 1f
        val shake = if (action == 4 && hit < 1f) sin(hit*PI*8).toFloat()*(1f-hit)*5f else 0f
        canvas.translate(shake,0f)

        // Dim interrogation room
        val bg = LinearGradient(0f,0f,420f,840f, intArrayOf(Color.rgb(16,23,30),Color.rgb(37,43,48),Color.rgb(12,16,20)), null, Shader.TileMode.CLAMP)
        p.shader=bg; canvas.drawRect(0f,0f,420f,840f,p); p.shader=null
        rect(canvas,22f,68f,398f,510f,Color.rgb(30,38,43),12f)
        rect(canvas,38f,85f,382f,470f,Color.rgb(39,48,53),8f)
        // overhead lamp and light cone
        p.shader=LinearGradient(210f,65f,210f,490f,0x44e6d6a4.toInt(),0x00101010.toInt(),Shader.TileMode.CLAMP)
        canvas.drawRect(90f,80f,330f,500f,p); p.shader=null
        rect(canvas,175f,36f,245f,53f,Color.rgb(83,91,92),5f)
        rect(canvas,183f,51f,237f,61f,Color.rgb(232,220,177),4f)
        // wall details
        rect(canvas,52f,112f,105f,116f,0xff596267.toInt(),2f)
        rect(canvas,315f,112f,368f,116f,0xff596267.toInt(),2f)
        for (i in 0..3) rect(canvas,58f+i*12,133f,63f+i*12,138f,0xff657077.toInt(),2f)

        // chair
        rect(canvas,105f,310f,315f,555f,0xff20272b.toInt(),28f)
        rect(canvas,119f,328f,301f,526f,0xff343d42.toInt(),22f)
        rect(canvas,92f,490f,328f,535f,0xff20272b.toInt(),12f)

        // body and jacket
        val breathe = sin(elapsed*1.7f)*2f
        val lean = if (action==4 && hit<1f) sin(hit*PI*3).toFloat()*7f else 0f
        canvas.save()
        canvas.translate(lean,breathe)
        val torso = Path().apply {
            moveTo(147f,340f); cubicTo(126f,365f,115f,418f,113f,505f)
            cubicTo(155f,540f,267f,540f,307f,505f)
            cubicTo(303f,416f,291f,365f,269f,340f)
            cubicTo(235f,324f,181f,324f,147f,340f); close()
        }
        p.shader=LinearGradient(120f,340f,300f,530f, intArrayOf(0xff46525b.toInt(),0xff252f38.toInt(),0xff111a21.toInt()),null,Shader.TileMode.CLAMP)
        canvas.drawPath(torso,p); p.shader=null
        // shirt collar
        val collar=Path().apply { moveTo(169f,339f); lineTo(210f,379f); lineTo(251f,339f); lineTo(238f,326f); lineTo(182f,326f); close() }
        canvas.drawPath(collar,paint(0xffc4b9a7.toInt()))
        // neck
        rect(canvas,190f,294f,230f,355f,0xffb77f67.toInt(),12f)
        // hair behind head
        p.shader=LinearGradient(125f,175f,290f,350f, intArrayOf(0xff171417.toInt(),0xff30211f.toInt(),0xff0c0c10.toInt()),null,Shader.TileMode.CLAMP)
        canvas.drawOval(129f,144f,292f,351f,p); p.shader=null
        // face
        p.shader=LinearGradient(145f,176f,270f,326f,intArrayOf(0xffe0b39a.toInt(),0xffc88f78.toInt(),0xffa96e60.toInt()),null,Shader.TileMode.CLAMP)
        canvas.drawOval(148f,165f,272f,326f,p); p.shader=null
        // ears
        canvas.drawOval(139f,235f,158f,270f,paint(0xffbb806e.toInt()))
        canvas.drawOval(264f,235f,281f,270f,paint(0xffbb806e.toInt()))
        // hair cap + side locks
        val hair=Path().apply { moveTo(146f,239f); cubicTo(133f,174f,159f,137f,207f,139f); cubicTo(265f,132f,293f,183f,276f,247f); cubicTo(262f,210f,257f,188f,237f,176f); cubicTo(214f,203f,183f,205f,154f,196f); close() }
        canvas.drawPath(hair,paint(0xff241b1c.toInt()))
        canvas.drawOval(140f,205f,159f,310f,paint(0xff261c1d.toInt()))
        canvas.drawOval(261f,204f,281f,310f,paint(0xff20191b.toInt()))
        // eyebrows and eyes
        val brow = paint(0xff51332e.toInt(),Paint.Style.STROKE,3f)
        canvas.drawArc(166f,220f,194f,237f,190f,155f,false,brow)
        canvas.drawArc(224f,220f,252f,237f,195f,150f,false,brow)
        canvas.drawOval(168f,235f,195f,247f,paint(0xfff1e7df.toInt()))
        canvas.drawOval(222f,235f,249f,247f,paint(0xfff1e7df.toInt()))
        canvas.drawCircle(182f,241f,5f,paint(0xff342522.toInt()))
        canvas.drawCircle(235f,241f,5f,paint(0xff342522.toInt()))
        canvas.drawCircle(183f,239f,1.6f,paint(Color.WHITE))
        canvas.drawCircle(236f,239f,1.6f,paint(Color.WHITE))
        // nose and lips
        canvas.drawLine(209f,244f,204f,270f,paint(0xff9d665a.toInt(),Paint.Style.STROKE,2f))
        canvas.drawArc(195f,264f,220f,278f,10f,160f,false,paint(0xff9b625d.toInt(),Paint.Style.STROKE,2f))
        canvas.drawOval(192f,288f,226f,299f,paint(0xff884b4c.toInt()))
        canvas.drawLine(197f,293f,221f,293f,paint(0xffe4a29a.toInt(),Paint.Style.STROKE,1f))
        // subtle cheek shadows and expression marks
        canvas.drawArc(157f,260f,184f,282f,20f,110f,false,paint(0x33a14c4c.toInt(),Paint.Style.STROKE,4f))
        canvas.drawArc(235f,260f,262f,282f,50f,110f,false,paint(0x33a14c4c.toInt(),Paint.Style.STROKE,4f))
        if (action==4 && hit<1f) {
            rect(canvas,245f,269f,258f,276f,0xffa65f59.toInt(),3f)
        }
        // hair strands
        val strand=paint(0xff59403a.toInt(),Paint.Style.STROKE,1.4f)
        canvas.drawArc(147f,145f,272f,220f,190f,135f,false,strand)
        canvas.drawArc(141f,159f,280f,245f,205f,90f,false,strand)
        // hands on table
        canvas.drawOval(126f,478f,184f,510f,paint(0xffc58d78.toInt()))
        canvas.drawOval(238f,478f,294f,510f,paint(0xffc58d78.toInt()))
        canvas.restore()

        // foreground desk
        p.shader=LinearGradient(0f,520f,0f,605f,0xff4b3b30.toInt(),0xff211b18.toInt(),Shader.TileMode.CLAMP)
        canvas.drawRect(0f,520f,420f,605f,p); p.shader=null
        canvas.drawLine(0f,525f,420f,525f,paint(0xff8b7159.toInt(),Paint.Style.STROKE,3f))
        rect(canvas,28f,540f,130f,548f,0xff9b846d.toInt(),2f)
        rect(canvas,292f,539f,389f,547f,0xff9b846d.toInt(),2f)
        // evidence folder
        rect(canvas,160f,535f,260f,574f,0xff9a784d.toInt(),4f)
        text(canvas,"DALIL",184f,559f,13f,0xfff1dfbd.toInt(),true)

        // HUD
        rect(canvas,14f,12f,406f,61f,0xdd10171d.toInt(),12f)
        text(canvas,"INTERROGATION",28f,34f,13f,0xffe9edf0.toInt(),true)
        text(canvas,"XONA 01  •  21:43",28f,51f,10f,0xff9ba8b1.toInt())
        text(canvas,"SAVOL-JAVOB",310f,40f,10f,0xffd5b98a.toInt(),true)

        // dialogue panel
        rect(canvas,16f,615f,404f,700f,0xf01b252d.toInt(),12f)
        text(canvas,"MAHKUMA  •  NODIRA",30f,640f,11f,0xffd9b77f.toInt(),true)
        val current = dialogues[line % dialogues.size]
        val words=current.split(" ")
        var row=""; var yy=665f
        for (w in words) {
            val next=if(row.isEmpty()) w else "$row $w"
            if (next.length>43) { text(canvas,row,30f,yy,14f,0xfff1f3f4.toInt()); yy+=19f; row=w } else row=next
        }
        if(row.isNotEmpty()) text(canvas,row,30f,yy,14f,0xfff1f3f4.toInt())
        // status bars
        text(canvas,"BOSIM",24f,724f,9f,0xffb7c0c7.toInt(),true)
        rect(canvas,77f,714f,190f,722f,0xff35434b.toInt(),4f)
        rect(canvas,77f,714f,77f+pressure*1.13f,722f,0xffc28a54.toInt(),4f)
        text(canvas,"ISHONCH",228f,724f,9f,0xffb7c0c7.toInt(),true)
        rect(canvas,293f,714f,395f,722f,0xff35434b.toInt(),4f)
        rect(canvas,293f,714f,293f+trust*1.02f,722f,0xff65a58a.toInt(),4f)

        // action buttons
        for (i in buttons.indices) {
            val x=14f+(i%2)*199f; val y=742f+(i/2)*43f
            rect(canvas,x,y,x+190f,y+35f,colors[i],8f)
            text(canvas,buttons[i],x+12f,y+23f,11f,Color.WHITE,true)
        }
        text(canvas,"Harakatni tanlang  •  Dalillarni tekshiring",66f,836f,9f,0xff87939b.toInt())
        canvas.restoreToCount(save)
        postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val x=event.x/(width/420f); val y=event.y/(height/840f)
        if (y in 742f..820f) {
            val col=if(x<210f) 0 else 1
            val row=if(y<785f) 0 else 1
            val index=row*2+col
            action=index+1
            actionAt=System.currentTimeMillis()
            when(index) {
                0 -> { line=(line+1)%dialogues.size; trust=(trust+8).coerceAtMost(100); tone.startTone(ToneGenerator.TONE_PROP_BEEP,90) }
                1 -> { showEvidence=!showEvidence; line=if(line<1) 1 else (line+1)%dialogues.size; pressure=(pressure+12).coerceAtMost(100); tone.startTone(ToneGenerator.TONE_PROP_ACK,110) }
                2 -> { pressure=(pressure+18).coerceAtMost(100); line=(line+1)%dialogues.size; tone.startTone(ToneGenerator.TONE_PROP_BEEP2,100) }
                3 -> { pressure=(pressure+25).coerceAtMost(100); line=(line+1)%dialogues.size; tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,130) }
            }
            invalidate()
        }
        return true
    }
}
