package watch.saferoad

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.content.Intent
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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

    /** A card that opens something, with an arrow instead of a switch. */
    private fun link(title: String, desc: String, onTap: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(0xFFEEEEEE.toInt())
        setPadding(dp(16), dp(14), dp(16), dp(14))
        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(title, 16f, true))
            addView(gray(desc).apply { setPadding(0, dp(2), 0, 0) })
        }
        addView(texts, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        addView(label("›", 24f, true), LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { marginStart = dp(12) })
        setOnClickListener { onTap() }
    }

    private fun editProfile() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        fun field(hint: String, v: String, type: Int) = EditText(this).apply {
            this.hint = hint
            setText(v)
            setSingleLine()
            inputType = type
            box.addView(this)
        }
        val name = field("ชื่อ-นามสกุล", Settings.profileName(this), android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val phone = field("เบอร์โทร", Settings.profilePhone(this), android.text.InputType.TYPE_CLASS_PHONE)
        val email = field("อีเมล", Settings.profileEmail(this), android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        AlertDialog.Builder(this)
            .setTitle("โปรไฟล์ผู้ส่งรายงาน")
            .setMessage("ว่างไว้ได้ ถ้าไม่อยากใส่ ข้อมูลนี้ออกจากเครื่องก็ต่อเมื่อคุณกดส่งรายงานเอง")
            .setView(box)
            .setPositiveButton("บันทึก") { _, _ ->
                Settings.setProfile(this, name.text.toString().trim(), phone.text.toString().trim(), email.text.toString().trim())
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        fun add(v: android.view.View) = box.addView(v, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(12) })

        add(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val logo = ImageView(context).apply {
                    setImageResource(R.drawable.ic_logo)
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
                addView(logo, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) })
                addView(label("ตั้งค่า", 24f, true))
            },
        )
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
        val stab = setting(
            "กันภาพสั่น",
            "ช่วยให้ภาพนิ่งขึ้นเมื่อใช้ขาตั้งหรือขับบนถนนขรุขระ ภาพอาจถูกตัดขอบเล็กน้อย มีผลเมื่อเปิดกล้องครั้งถัดไป",
            Settings.stabilize(this),
        ) { sw, want ->
            Settings.setStabilize(this, want)
            sw.isChecked = want
        }
        add(stab)
        Stabilization.check(this) { ok ->
            if (!ok && !isFinishing) {
                stab.alpha = 0.5f
                stab.isClickable = false
                stab.setOnClickListener(null)
                ((stab.getChildAt(0) as LinearLayout).getChildAt(1) as TextView).text =
                    "เครื่องนี้ไม่รองรับ: กล้องหลักของเครื่องไม่มีโหมดกันภาพสั่นสำหรับวิดีโอ (พบบ่อยในมือถือราคาประหยัด) จึงปิดไว้"
                (stab.getChildAt(1) as SwitchCompat).isChecked = false
            }
        }

        add(link("โปรไฟล์ผู้ส่งรายงาน", "ชื่อ เบอร์โทร อีเมลของคุณ ใส่ในข้อความรายงานที่คุณส่ง เก็บในเครื่องนี้เท่านั้น") { editProfile() })
        add(link("คู่มือการส่งรายงาน", "ส่งให้หน่วยงานไหน ทางเว็บ อีเมล หรือโทรศัพท์ พร้อมขั้นตอนและที่อยู่เว็บที่ตรวจแล้ว") {
            startActivity(Intent(this, GuideActivity::class.java))
        })

        val version = packageManager.getPackageInfo(packageName, 0).versionName
        add(gray("เวอร์ชัน $version\nสัญญาอนุญาต CC BY-NC-SA 4.0 · ข้อมูลทั้งหมดอยู่ในเครื่องของคุณ"))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(box)
        })
    }
}
