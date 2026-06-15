package ca.uwaterloo.ece452.discoveruwaterloo.ui.organizer

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ca.uwaterloo.ece452.discoveruwaterloo.AppViewModel
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

private val UWATERLOO = GeoPoint(43.4723, -80.5448)

@Composable
fun CreateEventScreen(viewModel: AppViewModel) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationLabel by remember { mutableStateOf("") }
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var nameError by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }

    val availableTags by viewModel.availableTags.collectAsState()
    var selectedTagIds by remember { mutableStateOf(setOf<Int>()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
        true
    }

    if (showMapPicker) {
        LocationPickerDialog(
            initialPoint = selectedPoint,
            onConfirm = { point ->
                selectedPoint = point
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = "Create Event",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (nameError && it.isNotBlank()) nameError = false
                },
                label = { Text("Event Name *") },
                isError = nameError,
                supportingText = if (nameError) ({ Text("Event name is required") }) else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = locationLabel,
                onValueChange = {
                    locationLabel = it
                    if (locationError && it.isNotBlank()) locationError = false
                },
                label = { Text("Location *") },
                placeholder = { Text("e.g. E7 Building") },
                isError = locationError,
                supportingText = if (locationError) ({ Text("Location is required") }) else null,
                trailingIcon = {
                    IconButton(onClick = { showMapPicker = true }) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Pick on map",
                            tint = if (selectedPoint != null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (selectedPoint != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Pin: %.4f, %.4f".format(selectedPoint!!.latitude, selectedPoint!!.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { selectedPoint = null },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            if (availableTags.isNotEmpty()) {
                Text("Tags", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableTags) { tag ->
                        FilterChip(
                            selected = selectedTagIds.contains(tag.id),
                            onClick = {
                                selectedTagIds = if (selectedTagIds.contains(tag.id))
                                    selectedTagIds - tag.id
                                else
                                    selectedTagIds + tag.id
                            },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val validName = name.isNotBlank()
                    val validLocation = locationLabel.isNotBlank()
                    nameError = !validName
                    locationError = !validLocation
                    if (!validName || !validLocation) return@Button

                    viewModel.createEvent(
                        name = name.trim(),
                        description = description.trim().ifBlank { null },
                        location = locationLabel.trim(),
                        lat = selectedPoint?.latitude,
                        lng = selectedPoint?.longitude,
                        tagIds = selectedTagIds.toList(),
                        onError = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                    )
                    scope.launch { snackbarHostState.showSnackbar("Event submitted for review!") }
                    name = ""
                    description = ""
                    locationLabel = ""
                    selectedPoint = null
                    selectedTagIds = emptySet()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit for Review")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LocationPickerDialog(
    initialPoint: GeoPoint?,
    onConfirm: (GeoPoint?) -> Unit,
    onDismiss: () -> Unit
) {
    var tempPoint by remember { mutableStateOf(initialPoint) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.82f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                        "Pick Location",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (tempPoint != null)
                            "Pin: %.4f, %.4f — tap to move".format(tempPoint!!.latitude, tempPoint!!.longitude)
                        else
                            "Tap the map to drop a pin",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tempPoint != null) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                HorizontalDivider()

                // Map
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setMultiTouchControls(true)
                                controller.setZoom(16.0)
                                controller.setCenter(initialPoint ?: UWATERLOO)

                                val tapReceiver = object : MapEventsReceiver {
                                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                                        tempPoint = p
                                        return true
                                    }
                                    override fun longPressHelper(p: GeoPoint) = false
                                }
                                overlays.add(0, MapEventsOverlay(tapReceiver))
                            }
                        },
                        update = { mapView ->
                            mapView.overlays.removeIf { it is Marker }
                            tempPoint?.let { point ->
                                val marker = Marker(mapView)
                                marker.position = point
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                mapView.overlays.add(marker)
                                mapView.invalidate()
                            }
                        }
                    )
                }

                HorizontalDivider()

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onConfirm(tempPoint) }) { Text("Confirm") }
                }
            }
        }
    }
}
