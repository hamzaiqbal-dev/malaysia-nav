package app.organicmaps.my

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import app.organicmaps.sdk.my.MyTolls
import java.util.concurrent.Executors

/**
 * Toll total of the current route: booth names come from the map data (JNI), prices from
 * data/my_tolls.csv shipped as an asset. The map scan runs off the main thread.
 */
object RouteTolls {
    /** A toll-booth node sits on the road, so a tight radius keeps the other carriageway's booths out. */
    private const val MAX_DISTANCE_M = 40.0
    private const val ASSET_NAME = "my_tolls.csv"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var table: TollTable? = null
    private var requestId = 0

    /**
     * Prices the route currently built; [onResult] gets null when there is no route. A newer call
     * cancels the delivery of an older one, so a rerouted trip never shows the previous total.
     */
    @MainThread
    fun compute(context: Context, onResult: (RouteTollTotal?) -> Unit) {
        val polyline = MyTolls.nativeGetRoutePolyline()
        if (polyline == null) {
            onResult(null)
            return
        }
        val id = ++requestId
        val appContext = context.applicationContext
        executor.execute {
            val tollTable = table ?: loadTable(appContext).also { table = it }
            val booths = MyTolls.nativeFindTollBooths(polyline, MAX_DISTANCE_M)
                .map { RouteTollBooth(it.mName, it.mDistanceAlongRouteM) }
            val total = tollTable.priceRoute(booths)
            mainHandler.post { if (id == requestId) onResult(total) }
        }
    }

    private fun loadTable(context: Context): TollTable =
        context.assets.open(ASSET_NAME).bufferedReader().useLines { TollTable.parse(it) }
}
