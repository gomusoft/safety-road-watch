package watch.saferoad

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/** The few user choices, kept in SharedPreferences. Everything is off until the user turns it on, except video stabilization. */
object Settings {
    private fun prefs(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Look up road / tambon / amphoe / province from GPS. Sends the coordinate to the phone's map service. */
    fun lookupOn(c: Context) = prefs(c).getBoolean("place_lookup", false)
    fun setLookup(c: Context, on: Boolean) = prefs(c).edit().putBoolean("place_lookup", on).apply()

    /** Open the camera straight away when the app starts, skipping the home screen. */
    fun autoStart(c: Context) = prefs(c).getBoolean("auto_start", false)
    fun setAutoStart(c: Context, on: Boolean) = prefs(c).edit().putBoolean("auto_start", on).apply()

    /** Video stabilization is on unless the user turns it off; phones that cannot do it ignore it. */
    fun stabilize(c: Context) = prefs(c).getBoolean("stabilize", true)
    fun setStabilize(c: Context, on: Boolean) = prefs(c).edit().putBoolean("stabilize", on).apply()

    /** Who is reporting. Stays on the phone; goes only into the text of a report the user sends. */
    fun profileName(c: Context) = prefs(c).getString("profile_name", "") ?: ""
    fun profilePhone(c: Context) = prefs(c).getString("profile_phone", "") ?: ""
    fun profileEmail(c: Context) = prefs(c).getString("profile_email", "") ?: ""
    fun setProfile(c: Context, name: String, phone: String, email: String) =
        prefs(c).edit().putString("profile_name", name).putString("profile_phone", phone).putString("profile_email", email).apply()

    /** The user has read and accepted the rules for sending a report. */
    fun reportAccepted(c: Context) = prefs(c).getBoolean("report_accepted", false)
    fun setReportAccepted(c: Context, on: Boolean) = prefs(c).edit().putBoolean("report_accepted", on).apply()

    /** The user must accept before any coordinate is sent to the phone's map service. */
    fun askLookupConsent(a: AppCompatActivity, onYes: () -> Unit) {
        AlertDialog.Builder(a)
            .setTitle("ดึงที่อยู่จาก GPS")
            .setMessage(
                "แอปจะส่งพิกัดของเหตุการณ์ให้บริการแผนที่ของเครื่อง (เช่น Google) เพื่อหาถนน ตำบล อำเภอ จังหวัด " +
                    "ต้องใช้อินเทอร์เน็ต พิกัดจึงออกจากเครื่องไปที่บริการนั้น\n\n" +
                    "ถ้าไม่ยอมรับ แอปยังใช้งานได้ปกติ และยังพิมพ์ที่อยู่เองได้ ถ้าไม่มีเน็ต แอปก็ยังใช้งานได้ปกติเช่นกัน",
            )
            .setPositiveButton("ยอมรับ") { _, _ ->
                setLookup(a, true)
                onYes()
            }
            .setNegativeButton("ไม่ยอมรับ", null)
            .show()
    }
}
