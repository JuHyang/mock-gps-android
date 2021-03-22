package com.example.mockgps

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
import android.util.Xml
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    companion object {
        const val PLAN_IMPORT_GPX_REQUEST_CODE = 2000
    }

    var job: Job? = null

    var minSpeed = 2.5f
    var maxSpeed = 3.5f
    var speed = 3f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        aboutView()
    }

    private fun aboutView() {
        btn_mock_start.setOnClickListener {
            try {
                val fileChooser = Intent(Intent.ACTION_OPEN_DOCUMENT)
                fileChooser.type = "*/*"
                startActivityForResult(fileChooser, PLAN_IMPORT_GPX_REQUEST_CODE)
            } catch (e: Exception) {
                Log.d("yejigpx", "PlanFragment Import GPX error: $e")
            }
        }

        btn_mock_stop.setOnClickListener {
            stopMockGps()
            edit_text.setText("Stop")
        }

        apply_button.setOnClickListener {
            try {
                val minSpeed = speed_min.text.toString().toFloat()
                val maxSpeed = speed_max.text.toString().toFloat()

                if (minSpeed < maxSpeed) {
                    this.minSpeed = minSpeed / 3.6f
                    this.maxSpeed = maxSpeed / 3.6f
                } else {
                    this.minSpeed = maxSpeed / 3.6f
                    this.maxSpeed = minSpeed / 3.6f
                }

            } catch (e: Exception) {
                Log.d("hyang@e", "${e.message}")
            }
        }
    }

    private fun startMockGps(lngLatList: ArrayList<LngLat>) {
        println(lngLatList.size)
        var lng = ""
        var lat = ""
        var bearingString = ""
        for (i in 0 until lngLatList.size - 2) {
            if (i % 2 == 0) {
                val lngLat = lngLatList[i]
                lng += "${(lngLat.lng * 10000000.0).toInt()}, \n"
                lat += "${(lngLat.lat * 10000000.0).toInt()}, \n"
                bearingString += "${(getBearing(
                    lngLat.lat,
                    lngLat.lng,
                    lngLatList[i + 2].lat,
                    lngLatList[i + 2].lng
                ).toFloat() * 100000.0).toInt()}, \n"
            }
        }
        println("--")
        println(lng)
        println("--")
        println(lat)
        println("--")
        println(bearingString)
        println("--")
        var bearing = 0f
        var term : Long = 20
        job = GlobalScope.launch {

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                addSetProvider(locationManager, "gps")
                addSetProvider(locationManager, "network")
            } catch (e: java.lang.Exception) {
                Log.d("hyang@e", "${e}")
            }

            for (i in 0 until lngLatList.size) {
                val lngLat = lngLatList[i]


                Log.d("hyang@lat", "${lngLat.lat}")
                Log.d("hyang@lng", "${lngLat.lng}")

                if (i == 0) {
                    for (j in 0 until 200) {
                        setPLocation(locationManager, "gps", lngLat.lat, lngLat.lng, bearing)
                        setPLocation(locationManager, "network", lngLat.lat, lngLat.lng, bearing)
                        delay(50)
                    }
                } else if (i == lngLatList.size - 1) {
                    for (j in 0 until 200) {
                        setPLocation(locationManager, "gps", lngLat.lat, lngLat.lng, bearing)
                        setPLocation(locationManager, "network", lngLat.lat, lngLat.lng, bearing)
                        delay(50)
                    }
                } else {
                    for (j in 0 until 10) {
                        setPLocation(locationManager, "gps", lngLat.lat, lngLat.lng, bearing)
                        setPLocation(locationManager, "network", lngLat.lat, lngLat.lng, bearing)
                        delay(term)
                    }
                }

                val minX = 0.9f
                val maxX = 1.1f

                val rand = Random()
                val finalX = rand.nextFloat() * (maxX - minX) + minX
                speed *= finalX
                if (speed < minSpeed) {
                    speed = minSpeed
                }

                if (speed > maxSpeed) {
                    speed = maxSpeed
                }

                if (i < lngLatList.size - 1) {
                    bearing = getBearing(
                        lngLat.lat,
                        lngLat.lng,
                        lngLatList[i + 1].lat,
                        lngLatList[i + 1].lng
                    ).toFloat()
                }
            }

            delProvider(locationManager, "gps")
            delProvider(locationManager, "network")
        }
    }

    fun stopMockGps() {
        job?.cancel()
    }

    fun setPLocation(
        locationManager: LocationManager,
        provider: String,
        curLat: Double,
        curLng: Double,
        bearing: Float
    ) {
        val location = Location(provider)
        location.time = System.currentTimeMillis()
        location.latitude = curLat
        location.longitude = curLng
        location.bearing = bearing
        location.accuracy = 1.0f
        location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        location.speed = speed
        locationManager.setTestProviderLocation(provider, location)
    }

    private fun addSetProvider(locationManager: LocationManager, provider: String) {
        locationManager.addTestProvider(
            provider,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            1,
            1
        )
        if (!locationManager.isProviderEnabled(provider)) {
            locationManager.setTestProviderEnabled(provider, true)
        }
    }

    private fun delProvider(locationManager: LocationManager, provider: String) {
        if (locationManager.isProviderEnabled(provider)) {
            locationManager.setTestProviderEnabled(provider, false)
        }
        locationManager.removeTestProvider(provider)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) return
        when (requestCode) {
            PLAN_IMPORT_GPX_REQUEST_CODE -> {
                if (data.data != null) {
                    val filename = getFileNameFromUri(this, data.data!!)

                    if (filename?.endsWith(".gpx") == true) {
                        if (saveGPX(this, data.data)) {
                            val lngLatList = loadGpx() ?: return
                            startMockGps(lngLatList)
                            edit_text.setText("Start")
                            return
                        }
                    } else {
                        Log.d("hyang@error", "${"is not gpx"}")
                    }
                }
            }
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var fileName: String? = null
        when (uri.scheme) {
            ContentResolver.SCHEME_FILE -> {
                fileName = File(uri.path ?: return null).name
            }
            ContentResolver.SCHEME_CONTENT -> {
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.apply {
                        if (moveToFirst()) {
                            fileName = getString(getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        }
                        close()
                    }
                } catch (e: Exception) {
                    return null
                }
            }
        }
        return fileName
    }

    private fun loadGpx(): ArrayList<LngLat>? {
        Log.d("yejigpx", "RideFragment loadGpx")

        val path = this.filesDir.path
        val latLngJson = JSONArray()                    // to Core
        val lngLatList: ArrayList<LngLat> = ArrayList() // to Ride Fragment
        try {
            // <trkpt lat="37.017533400" lon="127.919328600">
            val inputStream = FileInputStream(File("$path/loadedGPX.gpx"))
            val gpxParser = Xml.newPullParser()
            gpxParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            gpxParser.setInput(inputStream, null)

            while (gpxParser.next() != XmlPullParser.END_DOCUMENT) {
                if (gpxParser.name == "trkpt" && gpxParser.eventType == XmlPullParser.START_TAG) {
                    try {
                        val latitude = gpxParser.getAttributeValue(null, "lat").toDouble()
                        val longitude = gpxParser.getAttributeValue(null, "lon").toDouble()

                        if (-90.0 <= latitude && latitude <= 90.0 && -180.0 <= longitude && longitude <= 180.0) {
                            val latLng = JSONArray()
                            latLng.put(latitude)
                            latLng.put(longitude)
                            latLngJson.put(latLng)

                            val lngLat = LngLat(longitude, latitude)
                            lngLatList.add(lngLat)
                        }
                    } catch (e: Exception) {
                        Log.d("yejigpx", "gpx 파싱 실패 11111: $e")
                        return null
                    }
                }
            }

            inputStream.close()
        } catch (e: Exception) {
            Log.d("yejigpx", "gpx 파싱 실패 22222: $e")
            return null
        }

        // gpx 파싱 실패한 경우
        return if (latLngJson.length() == 0 || lngLatList.size == 0) {
            Log.d("yejigpx", "gpx 파싱 실패: size 0")
            null
        } else {
            lngLatList
        }

    }

    private fun saveGPX(context: Context?, sourceUri: Uri?): Boolean {
        if (context == null) return false
        if (sourceUri == null) return false

        if (ContentResolver.SCHEME_CONTENT == sourceUri.scheme) {
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return false
                val out = FileOutputStream(File("${context.filesDir.path}/loadedGPX.gpx"))
                val buffer = ByteArray(1024)

                while (true) {
                    val size = inputStream.read(buffer)
                    if (size < 0) break
                    out.write(buffer, 0, size)
                }
                out.close()

                return true // 파일 저장 성공
            } catch (e: Exception) {
            }
        }

        return false    // 파일 저장 실패
    }

    private fun getBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1 = lat1 * Math.PI / 180
        val lat2 = lat2 * Math.PI / 180
        val lon1 = lon1 * Math.PI / 180
        val lon2 = lon2 * Math.PI / 180
        val y = sin(lon2 - lon1) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(lon2 - lon1)
        val aaa = atan2(y, x); // 방위각 (라디안)
        val bearing = (aaa * 180 / Math.PI + 360) % 360 // 방위각 (디그리, 정규화 완료)

        return bearing
    }

}
