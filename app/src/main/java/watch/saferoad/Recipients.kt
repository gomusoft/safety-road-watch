package watch.saferoad

import android.content.Context
import org.json.JSONObject

/**
 * One way to send a report. The list lives in assets/recipients.json so a contributor can add or fix an agency
 * with a normal commit; nothing here is fetched from the internet.
 */
class Recipient(
    val id: String,
    val name: String,
    val how: String, // "web", "mail" or "phone"
    val url: String?,
    val email: String?,
    val phone: String?,
    val scope: String,
    val verified: String,
    val checked: String,
    val warn: String?,
    val steps: List<String>,
    val images: List<String>, // optional screenshot files under assets/guide/, one per step ("" = none)
)

object Recipients {
    fun load(c: Context): List<Recipient> = try {
        val root = JSONObject(c.assets.open("recipients.json").bufferedReader().use { it.readText() })
        val arr = root.getJSONArray("recipients")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val steps = o.getJSONArray("steps").let { a -> (0 until a.length()).map { a.getString(it) } }
            val images = o.optJSONArray("images")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
            Recipient(
                o.getString("id"), o.getString("name"), o.getString("how"),
                o.optString("url").ifEmpty { null }, o.optString("email").ifEmpty { null }, o.optString("phone").ifEmpty { null },
                o.optString("scope"), o.optString("verified"), o.optString("checked"),
                o.optString("warn").ifEmpty { null }, steps, images,
            )
        }
    } catch (ex: Exception) {
        emptyList()
    }
}
