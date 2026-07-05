package ca.uwaterloo.ece452.discoveruwaterloo.ui.map

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ca.uwaterloo.ece452.discoveruwaterloo.AppViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(viewModel: AppViewModel) {
    val filteredEvents by viewModel.filteredEvents.collectAsState()
    val plannerEvents by viewModel.plannerEvents.collectAsState()
    
    var showOnlyMyEvents by remember { mutableStateOf(false) }
    val eventsToDisplay = if (showOnlyMyEvents) plannerEvents else filteredEvents
    
    val context = LocalContext.current

    // Initialize osmdroid configuration
    remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    // Default location: UWaterloo Campus
    val uwaterloo = GeoPoint(43.4723, -80.5448)

    Box(modifier = Modifier.fillMaxSize()) {
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
                
                val geoPoints = mutableListOf<GeoPoint>()
                
                eventsToDisplay.forEach { event ->
                    event.locationCoords?.let { coords ->
                        val point = GeoPoint(coords.lat, coords.lng)
                        geoPoints.add(point)
                        
                        val marker = Marker(mapView)
                        marker.position = point
                        marker.title = event.name
                        marker.snippet = event.date ?: event.location ?: "UWaterloo"
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        
                        marker.infoWindow = object : org.osmdroid.views.overlay.infowindow.MarkerInfoWindow(
                            org.osmdroid.library.R.layout.bonuspack_bubble, mapView
                        ) {
                            override fun onOpen(item: Any?) {
                                super.onOpen(item)
                                val clickListener = android.view.View.OnClickListener {
                                    val uri = android.net.Uri.parse("geo:${coords.lat},${coords.lng}?q=${coords.lat},${coords.lng}(${android.net.Uri.encode(event.name)})")
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                }
                                mView.setOnClickListener(clickListener)
                                mView.findViewById<android.view.View>(org.osmdroid.library.R.id.bubble_title)?.setOnClickListener(clickListener)
                                mView.findViewById<android.view.View>(org.osmdroid.library.R.id.bubble_description)?.setOnClickListener(clickListener)
                            }
                        }
                        
                        mapView.overlays.add(marker)
                    }
                }
                
                mapView.invalidate()
            }
        )
        
        FloatingActionButton(
            onClick = { showOnlyMyEvents = !showOnlyMyEvents },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (showOnlyMyEvents) Icons.Default.FilterList else Icons.Default.FilterListOff,
                contentDescription = "Toggle My Events"
            )
        }
    }
}
