package ca.uwaterloo.ece452.discoveruwaterloo.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ca.uwaterloo.ece452.discoveruwaterloo.AppViewModel
import ca.uwaterloo.ece452.discoveruwaterloo.data.Event
import ca.uwaterloo.ece452.discoveruwaterloo.data.routing.Milestone
import ca.uwaterloo.ece452.discoveruwaterloo.data.routing.MilestoneRouter
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.events.MapEventsReceiver

@Composable
fun MapScreen(viewModel: AppViewModel) {
    val mapEvents by viewModel.mapEvents.collectAsState()
    val plannerEvents by viewModel.plannerEvents.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshEvents()
    }

    var showOnlyMyEvents by remember { mutableStateOf(false) }
    val eventsToDisplay = if (showOnlyMyEvents) plannerEvents else mapEvents
    
    val context = LocalContext.current

    // Routing states
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    var userLocation by remember { mutableStateOf(GeoPoint(43.4718, -80.5456)) } // default to SLC
    var isRouting by remember { mutableStateOf(false) }
    var routeMilestones by remember { mutableStateOf<List<Milestone>>(emptyList()) }
    var currentMilestoneIndex by remember { mutableStateOf(0) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

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
                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapView.overlays.clear()
                
                // Map tap receiver to update user location and recalculate route if active
                // Added first so it is at the bottom of the touch event stack and does not intercept marker taps
                val mapEventsReceiver = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        userLocation = p
                        if (isRouting && selectedEvent != null) {
                            selectedEvent?.locationCoords?.let { coords ->
                                routeMilestones = MilestoneRouter.findRoute(userLocation, GeoPoint(coords.lat, coords.lng))
                                currentMilestoneIndex = 0
                            }
                        }
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint) = false
                }
                mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))
                
                // 1. Draw route polyline if routing is active
                if (isRouting && selectedEvent != null && routeMilestones.isNotEmpty()) {
                    val polylinePoints = mutableListOf<GeoPoint>()
                    
                    // From user location to first milestone
                    polylinePoints.add(userLocation)
                    
                    // Add all milestones in route
                    routeMilestones.forEach { polylinePoints.add(it.location) }
                    
                    // Add destination event location
                    selectedEvent?.locationCoords?.let {
                        polylinePoints.add(GeoPoint(it.lat, it.lng))
                    }
                    
                    val routePolyline = Polyline(mapView).apply {
                        setPoints(polylinePoints)
                        outlinePaint.color = Color.parseColor("#1A73E8") // Sleek blue
                        outlinePaint.strokeWidth = 10f
                    }
                    mapView.overlays.add(routePolyline)
                    
                    // Draw milestone dot markers
                    routeMilestones.forEachIndexed { index, milestone ->
                        if (index >= currentMilestoneIndex) {
                            val msMarker = Marker(mapView).apply {
                                position = milestone.location
                                title = milestone.name
                                snippet = "Milestone ${index + 1} of ${routeMilestones.size}"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                
                                val size = 32
                                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bitmap)
                                val paint = Paint().apply {
                                    color = Color.parseColor("#FF9800") // Orange for upcoming milestones
                                    style = Paint.Style.FILL
                                }
                                canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 3).toFloat(), paint)
                                paint.color = Color.WHITE
                                canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 6).toFloat(), paint)
                                icon = BitmapDrawable(context.resources, bitmap)
                            }
                            mapView.overlays.add(msMarker)
                        }
                    }
                }
                
                // 2. Draw user location blue dot marker
                val userMarker = Marker(mapView).apply {
                    position = userLocation
                    title = "Your Location"
                    snippet = "Tap anywhere on the map to set starting position."
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    
                    val size = 48
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    val paint = Paint().apply {
                        color = Color.parseColor("#4285F4") // Sleek GPS blue dot
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 3).toFloat(), paint)
                    paint.color = Color.WHITE
                    canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 6).toFloat(), paint)
                    icon = BitmapDrawable(context.resources, bitmap)
                }
                mapView.overlays.add(userMarker)

                // 3. Draw standard event markers
                eventsToDisplay.forEach { event ->
                    event.locationCoords?.let { coords ->
                        val point = GeoPoint(coords.lat, coords.lng)
                        
                        val marker = Marker(mapView).apply {
                            position = point
                            title = event.name
                            snippet = event.date ?: event.location ?: "UWaterloo"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        
                        // Handle marker click in Compose
                        marker.setOnMarkerClickListener { clickedMarker, mv ->
                            selectedEvent = event
                            // Clear route when switching events
                            isRouting = false
                            routeMilestones = emptyList()
                            mapView.controller.animateTo(point)
                            true
                        }
                        
                        mapView.overlays.add(marker)
                    }
                }
                
                
                mapView.invalidate()
            }
        )
        
        // Floating filter button
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

        // Bottom card overlay
        selectedEvent?.let { event ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { selectedEvent = null; isRouting = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close details")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Location: ${event.location ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    event.date?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Date/Time: $it", style = MaterialTheme.typography.bodyMedium)
                    }
                    
                    event.description?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = it, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    }
                    
                    // Render tags
                    if (event.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            event.tags.forEach { tag ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (!isRouting) {
                        Button(
                            onClick = {
                                event.locationCoords?.let { coords ->
                                    routeMilestones = MilestoneRouter.findRoute(userLocation, GeoPoint(coords.lat, coords.lng))
                                    currentMilestoneIndex = 0
                                    isRouting = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Navigate via Milestones")
                        }
                    } else {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Milestone Routing Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val navTargets = remember(routeMilestones, event) {
                            val targets = routeMilestones.map { it.location }.toMutableList()
                            event.locationCoords?.let { targets.add(GeoPoint(it.lat, it.lng)) }
                            targets
                        }
                        
                        val navNames = remember(routeMilestones, event) {
                            val names = routeMilestones.map { it.name }.toMutableList()
                            names.add("Destination (${event.name})")
                            names
                        }
                        
                        val nextLocation = if (currentMilestoneIndex < navTargets.size) {
                            navTargets[currentMilestoneIndex]
                        } else null
                        
                        val nextName = if (currentMilestoneIndex < navNames.size) {
                            navNames[currentMilestoneIndex]
                        } else null
                        
                        if (nextLocation != null && nextName != null) {
                            Text(
                                text = "Next: Walk to $nextName",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Use indoor tunnels/bridges when possible to save energy.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "You have reached the final location near ${event.name}!",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Milestone progress list
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "[Start]", style = MaterialTheme.typography.bodySmall)
                            navNames.forEachIndexed { idx, name ->
                                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(12.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (idx == currentMilestoneIndex) FontWeight.Bold else FontWeight.Normal,
                                    color = if (idx < currentMilestoneIndex) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            else if (idx == currentMilestoneIndex) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (nextLocation != null) {
                                Button(
                                    onClick = {
                                        // Simulate low-frequency milestone update
                                        userLocation = nextLocation
                                        currentMilestoneIndex++
                                        mapViewRef?.controller?.animateTo(nextLocation)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simulate GPS Update", maxLines = 1)
                                }
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    isRouting = false
                                    routeMilestones = emptyList()
                                },
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("End Route")
                            }
                        }
                    }
                }
            }
        }
    }
}
