package com.ariane.mobile

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.hypot

class MainActivity : Activity() {

    companion object {
        init { System.loadLibrary("ariane_mobile") }
    }

    private lateinit var status: TextView
    private lateinit var preview: TextView
    private lateinit var glView: GLSurfaceView
    private val appLogs = mutableListOf<String>()
    private val LOG_EXPORT = 4104

    private fun addLog(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        synchronized(appLogs) {
            appLogs.add("[$time] $message")
            if (appLogs.size > 2000) appLogs.removeAt(0)
        }
    }

    private fun exportLogs() {
        addLog("Meminta ekspor log")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "SA_Map_Editor_Log.txt")
        }
        startActivityForResult(intent, LOG_EXPORT)
    }

    private val IDE = 4101
    private val IPL = 4102
    private val IMG = 4103
    private external fun nativeGetLogs(): String


    private external fun nativeCreate()
    private external fun nativeDraw(width: Int, height: Int)
    private external fun nativeDestroy()
    private external fun nativeSetPoints(points: FloatArray)
    private external fun nativeSetCamera(x: Float, y: Float, zoom: Float)

    private var cameraX = 0.65f
    private var cameraY = 0.55f
    private var cameraZoom = 1.0f
    private var lastX = 0f
    private var lastY = 0f
    private var lastDistance = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addLog("Aplikasi dimulai")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(17, 24, 39))
        }

        root.addView(TextView(this).apply {
            text = "ARIANE MOBILE"
            textSize = 24f
            setTextColor(Color.WHITE)
        })

        root.addView(TextView(this).apply {
            text = "GTA San Andreas Classic · 3D Map Editor"
            textSize = 12f
            setTextColor(Color.LTGRAY)
        })

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun addButton(label: String, code: Int) {
            val button = Button(this).apply {
                text = label
                setOnClickListener { openFile(code) }
            }
            buttons.addView(button, LinearLayout.LayoutParams(0, 50, 1f))
        }

        addButton("OPEN IDE", IDE)
        addButton("OPEN IPL", IPL)
        addButton("OPEN IMG", IMG)
        root.addView(buttons)
        root.addView(Button(this).apply {
            text = "EKSPOR LOG (.TXT)"
            setOnClickListener { exportLogs() }
        })

        status = TextView(this).apply {
            text = "Belum ada file yang diimport"
            textSize = 13f
            setTextColor(Color.rgb(120, 220, 160))
            setPadding(4, 8, 4, 8)
        }
        root.addView(status)

        root.addView(TextView(this).apply {
            text = "3D VIEWPORT · Drag untuk rotasi · Pinch untuk zoom"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 4, 0, 6)
        })

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            setRenderer(object : GLSurfaceView.Renderer {
                override fun onSurfaceCreated(
                    gl: javax.microedition.khronos.opengles.GL10?,
                    config: javax.microedition.khronos.egl.EGLConfig?
                ) {
                    addLog("OpenGL surface dibuat")
                    nativeCreate()
                    addLog("nativeCreate dipanggil")
                    nativeSetCamera(cameraX, cameraY, cameraZoom)
                }

                override fun onSurfaceChanged(
                    gl: javax.microedition.khronos.opengles.GL10?,
                    width: Int,
                    height: Int
                ) {
                    addLog("Viewport: ${width}x${height}")
                    nativeDraw(width, height)
                }

                override fun onDrawFrame(
                    gl: javax.microedition.khronos.opengles.GL10?
                ) {
                    nativeDraw(width, height)
                }
            })
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            setBackgroundColor(Color.rgb(14, 20, 32))
        }

        glView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount >= 2) {
                        lastDistance = distance(event)
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (event.pointerCount >= 2) {
                        val d = distance(event)
                        if (lastDistance > 0f && d > 0f) {
                            cameraZoom *= d / lastDistance
                            cameraZoom = cameraZoom.coerceIn(0.25f, 5f)
                            lastDistance = d
                            updateCamera()
                        }
                    } else {
                        cameraY += (event.x - lastX) * 0.008f
                        cameraX += (event.y - lastY) * 0.008f
                        cameraX = cameraX.coerceIn(-1.5f, 1.5f)
                        lastX = event.x
                        lastY = event.y
                        updateCamera()
                    }
                    true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    lastDistance = 0f
                    true
                }
                else -> true
            }
        }

        root.addView(glView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 260
        ))

        root.addView(TextView(this).apply {
            text = "FILE INSPECTOR"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 5)
        })

        preview = TextView(this).apply {
            text = "Hasil import file akan muncul di sini."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(10, 10, 10, 10)
        }

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(31, 41, 55))
            addView(preview)
        }

        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        setContentView(root)
    }

    private fun distance(e: MotionEvent): Float {
        if (e.pointerCount < 2) return 0f
        return hypot(
            (e.getX(0) - e.getX(1)).toDouble(),
            (e.getY(0) - e.getY(1)).toDouble()
        ).toFloat()
    }

    private fun updateCamera() {
        glView.queueEvent {
            nativeSetCamera(cameraX, cameraY, cameraZoom)
        }
    }

    private fun openFile(code: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, code)
    }

    @Deprecated("Activity Result API migration planned")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return

        if (requestCode == LOG_EXPORT) {
            try {
                val nativeLogs = try {
                    nativeGetLogs()
                } catch (e: Exception) {
                    "Gagal mengambil native log: ${e.message}"
                }

                val content = synchronized(appLogs) {
                    appLogs.joinToString("\n") + "\n"
                } + "\n===== NATIVE OPENGL LOG =====\n" + nativeLogs + "\n"
                contentResolver.openOutputStream(uri)?.use {
                    it.write(content.toByteArray(Charsets.UTF_8))
                } ?: throw Exception("File tidak dapat ditulis")
                Toast.makeText(this, "Log berhasil disimpan", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal ekspor: ${e.message}", Toast.LENGTH_LONG).show()
            }
            return
        }

        try {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}

        try {
            val bytes = contentResolver.openInputStream(uri)?.use {
                it.readBytes()
            } ?: throw Exception("File tidak dapat dibaca")

            val filename = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.substringAfterLast(':') ?: "Unknown"

            when (requestCode) {
                IDE -> inspectTextFile("IDE", filename, bytes)
                IPL -> inspectTextFile("IPL", filename, bytes)
                IMG -> inspectImg(filename, bytes)
            }
        } catch (e: Exception) {
            addLog("ERROR membaca file: ${e.message}")
            status.text = "Gagal membaca file: ${e.message}"
        }
    }

    private fun parseInstances(content: String): FloatArray {
        val result = ArrayList<Float>()
        var inInst = false

        content.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEach

            when (line.lowercase()) {
                "inst" -> {
                    inInst = true
                    return@forEach
                }
                "end" -> {
                    inInst = false
                    return@forEach
                }
            }

            if (!inInst) return@forEach

            val fields = line.split(',')
            if (fields.size < 11) return@forEach

            try {
                result.add(fields[3].trim().toFloat())
                result.add(fields[4].trim().toFloat())
                result.add(fields[5].trim().toFloat())
            } catch (_: Exception) {
                // Abaikan baris IPL yang tidak valid.
            }
        }

        return result.toFloatArray()
    }

    private fun inspectTextFile(
        type: String,
        filename: String,
        bytes: ByteArray
    ) {
        val content = String(bytes, Charsets.UTF_8)
        val lines = content.lines()
        val nonEmpty = lines.count { it.trim().isNotEmpty() }

        if (type == "IPL") {
            val points = parseInstances(content)
            glView.queueEvent {
                nativeSetPoints(points)
            }

            val count = points.size / 3
            addLog("IPL $filename: $count objek, ${bytes.size} bytes")
            status.text = "IPL berhasil dibuka: $filename · $count objek"

            glView.requestRender()
            preview.text = buildString {
                appendLine("FORMAT: IPL")
                appendLine("FILE: $filename")
                appendLine("SIZE: ${bytes.size} bytes")
                appendLine("TOTAL LINES: ${lines.size}")
                appendLine("INSTANCE OBJECTS: $count")
                if (count == 0) {
                    appendLine()
                    appendLine("Tidak ditemukan entri pada section inst.")
                    appendLine("File ini mungkin berisi path, occl, atau section lain.")
                }
                appendLine()
                appendLine("----- CONTENT PREVIEW -----")
                appendLine(lines.take(35).joinToString("\n"))
            }
            return
        }

        status.text = "$type berhasil dibuka: $filename"
        preview.text = buildString {
            appendLine("FORMAT: $type")
            appendLine("FILE: $filename")
            appendLine("SIZE: ${bytes.size} bytes")
            appendLine("TOTAL LINES: ${lines.size}")
            appendLine("NON-EMPTY LINES: $nonEmpty")
            appendLine()
            appendLine("----- CONTENT PREVIEW -----")
            appendLine(lines.take(35).joinToString("\n"))
        }
    }

    private fun inspectImg(filename: String, bytes: ByteArray) {
        val magic = if (bytes.size >= 4) {
            String(bytes.copyOfRange(0, 4), Charsets.US_ASCII)
        } else "UNKNOWN"

        var entryCount = -1
        if (magic == "VER2" && bytes.size >= 8) {
            entryCount = ByteBuffer.wrap(bytes, 4, 4)
                .order(ByteOrder.LITTLE_ENDIAN).int
        }

        status.text = "IMG berhasil dibuka: $filename"
        preview.text = buildString {
            appendLine("FORMAT: IMG ARCHIVE")
            appendLine("FILE: $filename")
            appendLine("SIZE: ${bytes.size} bytes")
            appendLine("HEADER: $magic")
            if (entryCount >= 0) appendLine("INDEX ENTRIES: $entryCount")
            else appendLine("Index belum dikenali oleh inspector.")
        }
    }

    override fun onPause() {
        glView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        glView.onResume()
    }
}
