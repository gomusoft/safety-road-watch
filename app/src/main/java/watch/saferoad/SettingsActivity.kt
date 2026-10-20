package watch.saferoad

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(s: String, sp: Float, bold: Boolean = false) = TextView(this).apply {
        text = s
        setTextColor(Color.BLACK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun gray(s: String) = label(s, 13f).apply { setTextColor(0xFF555555.toInt()) }

    private fun tint(on: Int, off: Int) = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
        intArrayOf(on, off),
    )

    /**
     * One setting: the whole card is the button, with a clear on/off switch at the right.
     * [onTap] gets the switch and its new wanted state, and decides whether to apply it.
     */
    private fun setting(title: String, desc: String, checked: Boolean, onTap: (SwitchCompat, Boolean) -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(0xFFEEEEEE.toInt())
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val ripple = TypedValue()
            if (context.theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)) {
                foreground = getDrawable(ripple.resourceId)
            }
            val texts = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(label(title, 16f, true))
                addView(gray(desc).apply { setPadding(0, dp(2), 0, 0) })
            }
            addView(texts, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            val sw = SwitchCompat(context).apply {
                isChecked = checked
                isClickable = false // the card handles the tap
                thumbTintList = tint(0xFF2E7D32.toInt(), 0xFF9E9E9E.toInt())
                trackTintList = tint(0xFF81C784.toInt(), 0xFFCCCCCC.toInt())
            }
            addView(sw, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginStart = dp(12) })
            setOnClickListener { onTap(sw, !sw.isChecked) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        fun add(v: android.view.View) = box.addView(v, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(12) })

        add(label("ตั้งค่า", 24f, true))
        add(
            setting(
                "ดึงที่อยู่จาก GPS อัตโนมัติ (ใช้เน็ต)",
                "ส่งพิกัดให้บริการแผนที่ของเครื่องเพื่อหาถนน ตำบล อำเภอ จังหวัด ปิดไว้ก่อนจนกว่าคุณจะยอมรับ ไม่มีเน็ตก็ใช้งานอย่างอื่นได้ตามปกติ",
                Settings.lookupOn(this),
            ) { sw, want ->
                if (want) {
                    Settings.askLookupConsent(this) { sw.isChecked = true } // on only after the user accepts
                } else {
                    Settings.setLookup(this, false)
                    sw.isChecked = false
                }
            },
        )
        add(
            setting(
                "เปิดกล้องทันทีเมื่อเปิดแอป",
                "ข้ามหน้าแรก เหมาะกับตอนใช้ในรถ กดย้อนกลับจากกล้องเพื่อมาหน้าแรก",
                Settings.autoStart(this),
            ) { sw, want ->
                Settings.setAutoStart(this, want)
                sw.isChecked = want
            },
        )

        val version = packageManager.getPackageInfo(packageName, 0).versionName
        add(gray("เวอร์ชัน $version\nสัญญาอนุญาต CC BY-NC-SA 4.0 · ข้อมูลทั้งหมดอยู่ในเครื่องของคุณ"))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(box)
        })
    }
}
