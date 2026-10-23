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

/** Which agencies take which kind of violation. [labels] are matched against the violations found in a voice note. */
class ViolationGroup(
    val id: String,
    val title: String,
    val items: String,
    val labels: List<String>,
    val channels: List<String>,
    val note: String,
) {
    fun matches(violations: List<String>) = violations.any { v -> labels.any { l -> v.contains(l) || l.contains(v) } }
}

class Guide(val note: String, val groups: List<ViolationGroup>)

object Recipients {
    /** The violation to agency table. Empty when the file cannot be read. */
    fun guide(c: Context): Guide = try {
        val root = JSONObject(c.assets.open("recipients.json").bufferedReader().use { it.readText() })
        val arr = root.optJSONArray("violations")
        val groups = if (arr == null) emptyList() else (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            fun list(k: String) = o.getJSONArray(k).let { a -> (0 until a.length()).map { a.getString(it) } }
            ViolationGroup(o.getString("id"), o.getString("title"), o.optString("items"), list("labels"), list("channels"), o.optString("note"))
        }
        Guide(root.optString("violationsNote"), groups)
    } catch (ex: Exception) {
        Guide("", emptyList())
    }

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
