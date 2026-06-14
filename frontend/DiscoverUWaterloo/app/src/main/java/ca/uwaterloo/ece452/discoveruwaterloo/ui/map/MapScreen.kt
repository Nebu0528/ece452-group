package ca.uwaterloo.ece452.discoveruwaterloo.ui.map

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import ca.uwaterloo.ece452.discoveruwaterloo.AppViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(viewModel: AppViewModel) {
    val events by viewModel.filteredEvents.collectAsState()
    val context = LocalContext.current

    // Initialize osmdroid configuration
    remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    // Default location: UWaterloo Campus
    val uwaterloo = GeoPoint(43.4723, -80.5448)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setMultiTouchControls(true)
                controller.setZoom(16.0)
                controller.setCenter(uwaterloo)
            }
        },
        update = { mapView ->
            mapView.overlays.clear()
            
            events.forEach { event ->
                event.locationCoords?.let { coords ->
                    val marker = Marker(mapView)
                    marker.position = GeoPoint(coords.lat, coords.lng)
                    marker.title = event.name
                    marker.snippet = event.date ?: event.location ?: "UWaterloo"
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    mapView.overlays.add(marker)
                }
            }
            mapView.invalidate()
        }
    )
}
