package com.ariane.mobile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.opengl.GLSurfaceView
import android.widget.Toast
import android.content.ContentResolver
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : Activity() {
    private external fun nativeCreate()
    private external fun nativeDraw(width: Int, height: Int)
    private external fun nativeDestroy()
    private lateinit var status: TextView
    private val pickGameFiles = 4101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("ariane_mobile")

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 18, 18, 18) }
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        status = TextView(this).apply { text = "Ariane Mobile · GTA San Andreas · Engine bootstrap"; textSize = 14f }
        val open = Button(this).apply { text = "Pilih folder game"; setOnClickListener { openGameFolder() } }
        toolbar.addView(status, LinearLayout.LayoutParams(0, -2, 1f)); toolbar.addView(open)
        val view = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            var viewportWidth = 1
            var viewportHeight = 1
            setRenderer(object : GLSurfaceView.Renderer {
                override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) { nativeCreate() }
                override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                    viewportWidth = width.coerceAtLeast(1)
                    viewportHeight = height.coerceAtLeast(1)
                    nativeDraw(viewportWidth, viewportHeight)
                }
                override fun onDrawFrame(gl: GL10?) { nativeDraw(viewportWidth, viewportHeight) }
            })
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        root.addView(toolbar)
        root.addView(view, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun openGameFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, pickGameFiles)
    }

    @Deprecated("Use Activity Result API in production")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickGameFiles && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                status.text = "Folder game dipilih · ${uri.lastPathSegment ?: "SA data"}"
                Toast.makeText(this, "Akses folder tersimpan. Parser game belum dihubungkan.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() { super.onDestroy() }
    companion object { init { System.loadLibrary("ariane_mobile") } }
}
