package watch.saferoad

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(s: String, sp: Float, bold: Boolean = false) = TextView(this).apply {
        text = s
        setTextColor(Color.BLACK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun hint(s: String) = label(s, 13f).apply {
        setTextColor(0xFF555555.toInt())
        setPadding(dp(32), 0, 0, dp(16))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        box.addView(label("ตั้งค่า", 24f, true), LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(16) })

        val lookup = CheckBox(this).apply {
            text = "ดึงที่อยู่จาก GPS อัตโนมัติ (ใช้เน็ต)"
            setTextColor(Color.BLACK)
            isChecked = Settings.lookupOn(this@SettingsActivity)
            setOnCheckedChangeListener { v, on ->
                if (on == Settings.lookupOn(this@SettingsActivity)) return@setOnCheckedChangeListener
                if (on) {
                    v.isChecked = false // becomes checked only after the user accepts
                    Settings.askLookupConsent(this@SettingsActivity) { v.isChecked = true }
                } else {
                    Settings.setLookup(this@SettingsActivity, false)
                }
            }
        }
        box.addView(lookup)
        box.addView(hint("ส่งพิกัดให้บริการแผนที่ของเครื่องเพื่อหาถนน ตำบล อำเภอ จังหวัด ปิดไว้ก่อนจนกว่าคุณจะยอมรับ ไม่มีเน็ตก็ใช้งานอย่างอื่นได้ตามปกติ"))

        val auto = CheckBox(this).apply {
            text = "เปิดกล้องทันทีเมื่อเปิดแอป"
            setTextColor(Color.BLACK)
            isChecked = Settings.autoStart(this@SettingsActivity)
            setOnCheckedChangeListener { _, on -> Settings.setAutoStart(this@SettingsActivity, on) }
        }
        box.addView(auto)
        box.addView(hint("ข้ามหน้าแรก เหมาะกับตอนใช้ในรถ กดย้อนกลับจากกล้องเพื่อมาหน้าแรก"))

        val version = packageManager.getPackageInfo(packageName, 0).versionName
        box.addView(label("เวอร์ชัน $version", 13f).apply { setTextColor(0xFF555555.toInt()) })
        box.addView(label("สัญญาอนุญาต CC BY-NC-SA 4.0 · ข้อมูลทั้งหมดอยู่ในเครื่องของคุณ", 13f).apply { setTextColor(0xFF555555.toInt()) })

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(box)
        })
    }
}
