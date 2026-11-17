package com.ariane.mobile

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var preview: TextView
    private lateinit var viewport: TextView

    private val IDE = 4101
    private val IPL = 4102
    private val IMG = 4103

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.rgb(17, 24, 39))
        }

        val title = TextView(this).apply {
            text = "ARIANE MOBILE"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }

        val subtitle = TextView(this).apply {
            text = "GTA San Andreas Classic · Standalone Editor"
            textSize = 12f
            setTextColor(Color.LTGRAY)
        }

        root.addView(title)
        root.addView(subtitle)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun addButton(label: String, code: Int) {
            val button = Button(this).apply {
                text = label
                setOnClickListener { openFile(code) }
            }
            buttons.addView(
                button,
                LinearLayout.LayoutParams(0, 54, 1f)
            )
        }

        addButton("OPEN IDE", IDE)
        addButton("OPEN IPL", IPL)
        addButton("OPEN IMG", IMG)

        root.addView(buttons)

        status = TextView(this).apply {
            text = "Belum ada file yang diimport"
            textSize = 13f
            setTextColor(Color.rgb(120, 220, 160))
            setPadding(4, 12, 4, 12)
        }
        root.addView(status)

        val viewportTitle = TextView(this).apply {
            text = "3D VIEWPORT"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(viewportTitle)

        viewport = TextView(this).apply {
            text = "Ariane 3D Editor\n\nBelum ada map dimuat\n\nImport IDE dan IPL untuk memulai"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setBackgroundColor(Color.rgb(31, 41, 55))
        }

        root.addView(
            viewport,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                210
            )
        )

        val previewTitle = TextView(this).apply {
            text = "FILE INSPECTOR"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 12, 0, 6)
        }
        root.addView(previewTitle)

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

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
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

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}

        try {
            val bytes = contentResolver.openInputStream(uri)?.use {
                it.readBytes()
            } ?: throw Exception("File tidak dapat dibaca")

            val filename = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.substringAfterLast(':')
                ?: "Unknown"

            when (requestCode) {
                IDE -> inspectTextFile("IDE", filename, bytes)
                IPL -> inspectTextFile("IPL", filename, bytes)
                IMG -> inspectImg(filename, bytes)
            }
        } catch (e: Exception) {
            status.text = "Gagal membaca file: ${e.message}"
        }
    }

    private fun inspectTextFile(
        type: String,
        filename: String,
        bytes: ByteArray
    ) {
        val content = String(bytes, Charsets.UTF_8)
        val lines = content.lines()

        val nonEmpty = lines.count { it.trim().isNotEmpty() }

        val instCount = lines.count {
            val line = it.trim()
            line.contains(",") &&
            line.firstOrNull()?.isDigit() == true
        }

        status.text = "$type berhasil dibuka: $filename"

        preview.text = buildString {
            appendLine("FORMAT: $type")
            appendLine("FILE: $filename")
            appendLine("SIZE: ${bytes.size} bytes")
            appendLine("TOTAL LINES: ${lines.size}")
            appendLine("NON-EMPTY LINES: $nonEmpty")

            if (type == "IPL") {
                appendLine("POTENTIAL IPL ENTRIES: $instCount")
            }

            appendLine()
            appendLine("----- CONTENT PREVIEW -----")
            appendLine(lines.take(35).joinToString("\n"))
        }

        viewport.text =
            "$type LOADED\n\n$filename\n\nFile berhasil dibaca.\n3D map rendering belum terhubung."
    }

    private fun inspectImg(filename: String, bytes: ByteArray) {
        val magic = if (bytes.size >= 4) {
            String(bytes.copyOfRange(0, 4), Charsets.US_ASCII)
        } else {
            "UNKNOWN"
        }

        var entryCount = -1

        if (magic == "VER2" && bytes.size >= 8) {
            entryCount = ByteBuffer.wrap(bytes, 4, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .int
        }

        status.text = "IMG berhasil dibuka: $filename"

        preview.text = buildString {
            appendLine("FORMAT: IMG ARCHIVE")
            appendLine("FILE: $filename")
            appendLine("SIZE: ${bytes.size} bytes")
            appendLine("HEADER: $magic")

            if (entryCount >= 0) {
                appendLine("INDEX ENTRIES: $entryCount")
            } else {
                appendLine("Index belum dikenali oleh inspector.")
            }
        }

        viewport.text =
            "IMG ARCHIVE LOADED\n\n$filename\n\nHeader: $magic\n\nModel 3D belum dimuat."
    }
}
