package watch.saferoad

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Place(
    val road: String? = null,
    val km: String? = null,
    val tambon: String? = null,
    val amphoe: String? = null,
    val changwat: String? = null,
    /** Where each field came from: "gps", "voice" or "manual". */
    val src: Map<String, String> = emptyMap(),
) {
    fun value(k: String) = when (k) {
        "road" -> road
        "km" -> km
        "tambon" -> tambon
        "amphoe" -> amphoe
        else -> changwat
    }

    /** One line for lists and the report, e.g. "ถนนมิตรภาพ กม.45 ต.ในเมือง อ.เมือง จ.ขอนแก่น". */
    fun text(): String = listOfNotNull(
        road, km?.let { "กม.$it" }, tambon?.let { "ต.$it" }, amphoe?.let { "อ.$it" }, changwat?.let { "จ.$it" },
    ).joinToString(" ")
}

class EventInfo(
    val dir: File,
    val time: Long,
    val iso: String,
    val lat: Double?,
    val lon: Double?,
    val transcript: String,
    val plate: String?,
    val province: String?,
    val violations: List<String>,
    val note: String,
    val place: Place,
)

class EventStore(context: Context) {
    private val ctx = context.applicationContext
    private val root = File(ctx.filesDir, "events").apply { mkdirs() }
    private val lm = ctx.getSystemService(LocationManager::class.java)

    @Volatile
    private var last: Location? = null
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            last = location
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    private fun hasLocation() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startLocation() {
        if (!hasLocation()) return
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            try {
                lm.requestLocationUpdates(p, 5000L, 0f, listener)
            } catch (e: Exception) {
                // provider missing or permission too coarse: ignore
            }
        }
    }

    fun stopLocation() {
        try {
            lm.removeUpdates(listener)
        } catch (e: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastLocation(): Location? {
        if (!hasLocation()) return null
        last?.let { return it }
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).mapNotNull {
            try {
                lm.getLastKnownLocation(it)
            } catch (e: Exception) {
                null
            }
        }.maxByOrNull { it.time }
    }

    /** Creates events/<yyyyMMdd_HHmmss>/ with an initial event.json. */
    fun create(time: Long): File {
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(time))
        var dir = File(root, name)
        var i = 1
        while (dir.exists()) dir = File(root, "${name}_${i++}")
        dir.mkdirs()
        val loc = lastLocation()
        val j = JSONObject()
            .put("time", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date(time)))
            .put("lat", loc?.latitude ?: JSONObject.NULL)
            .put("lon", loc?.longitude ?: JSONObject.NULL)
            .put("transcript", "")
            .put("plate", JSONObject.NULL)
            .put("province", JSONObject.NULL)
            .put("violations", JSONArray())
            .put("note", "")
        File(dir, "event.json").writeText(j.toString(2))
        return dir
    }

    fun update(dir: File, change: JSONObject.() -> Unit) {
        val f = File(dir, "event.json")
        val j = try {
            JSONObject(f.readText())
        } catch (e: Exception) {
            JSONObject()
        }
        j.change()
        f.writeText(j.toString(2))
    }

    private fun JSONObject.str(k: String): String? = if (isNull(k)) null else getString(k)

    private fun read(dir: File): EventInfo {
        val j = try {
            JSONObject(File(dir, "event.json").readText())
        } catch (e: Exception) {
            JSONObject()
        }
        val time = try {
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).parse(dir.name.take(15))?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
        val v = j.optJSONArray("violations")
        return EventInfo(
            dir, time, j.str("time") ?: dir.name,
            if (j.isNull("lat")) null else j.getDouble("lat"),
            if (j.isNull("lon")) null else j.getDouble("lon"),
            j.str("transcript") ?: "", j.str("plate"), j.str("province"),
            if (v == null) emptyList() else (0 until v.length()).map { v.getString(it) },
            j.str("note") ?: "",
            Place(
                j.str("road"), j.str("km"), j.str("tambon"), j.str("amphoe"), j.str("changwat"),
                j.optJSONObject("place_src")?.let { o -> o.keys().asSequence().associateWith { o.getString(it) } } ?: emptyMap(),
            ),
        )
    }

    fun savePlace(dir: File, p: Place) = update(dir) {
        for ((k, v) in listOf("road" to p.road, "km" to p.km, "tambon" to p.tambon, "amphoe" to p.amphoe, "changwat" to p.changwat))
            put(k, v?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
        put("place_src", JSONObject(p.src.filterKeys { p.value(it) != null }))
    }

    /**
     * Road, tambon, amphoe and province for a coordinate, through the phone's geocoder.
     * Needs internet and sends the coordinate to the phone's map service, so only call it when the user asks.
     * Blocking: call off the main thread. Null when nothing was found.
     */
    @Suppress("DEPRECATION")
    fun lookupPlace(lat: Double, lon: Double): Place? {
        if (!Geocoder.isPresent()) return null
        val a = Geocoder(ctx, Locale("th", "TH")).getFromLocation(lat, lon, 1)?.firstOrNull() ?: return null
        val clean = { s: String? ->
            s?.replace(Regex("^(ตำบล|อำเภอ|จังหวัด|แขวง|เขต|ต\\.|อ\\.|จ\\.)\\s*"), "")?.trim()?.takeIf { it.isNotEmpty() }
        }
        return Place(
            road = a.thoroughfare?.trim()?.takeIf { it.isNotEmpty() },
            tambon = clean(a.subLocality ?: a.locality),
            amphoe = clean(a.subAdminArea),
            changwat = clean(a.adminArea),
            src = mapOf("road" to "gps", "tambon" to "gps", "amphoe" to "gps", "changwat" to "gps"),
        )
    }

    /** Newest first. */
    fun list(): List<EventInfo> =
        (root.listFiles { f -> f.isDirectory } ?: emptyArray()).sortedByDescending { it.name }.map { read(it) }
}
