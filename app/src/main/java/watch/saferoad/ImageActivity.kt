package watch.saferoad

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/** Shows one picture of an event, with zoom. Extras: "path"; "deletable" adds a delete button (for captured stills). */
class ImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val file = intent.getStringExtra("path")?.let { File(it) }
        val bmp = file?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
        if (file == null || bmp == null) {
            Toast.makeText(this, "เปิดรูปไม่ได้", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val stage = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        val pic = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageBitmap(bmp)
        }
        stage.addView(pic, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        Zoomable(stage, pic)
        if (intent.getBooleanExtra("deletable", false)) {
            val del = Button(this).apply {
                text = "ลบรูปนี้"
                isAllCaps = false
                setOnClickListener {
                    AlertDialog.Builder(this@ImageActivity)
                        .setMessage("ลบรูปนี้? กู้คืนไม่ได้")
                        .setPositiveButton("ลบ") { _, _ ->
                            file.delete()
                            finish()
                        }
                        .setNegativeButton("ยกเลิก", null)
                        .show()
                }
            }
            stage.addView(del, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.END).apply { setMargins(16, 16, 16, 16) })
        }
        setContentView(stage)
    }
}
