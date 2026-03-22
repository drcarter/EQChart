package com.magimon.eq.app.ui.compose

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.magimon.eq.app.ui.theme.EQChartTheme
import com.magimon.eq.compose.pointline3d.PointLine3DChart
import com.magimon.eq.point3d.Point3DAxisOptions
import com.magimon.eq.point3d.Point3DCameraOptions
import com.magimon.eq.pointline3d.PointLine3DDatum
import com.magimon.eq.pointline3d.PointLine3DPresentationOptions
import com.magimon.eq.pointline3d.PointLine3DSeries

/**
 * Compose sample screen for the true 3D Point Line chart.
 */
class ComposePointLine3DActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposePointLine3DSampleScreen()
            }
        }
    }
}

private data class MissionWaypoint(
    val phase: String,
    val distanceKm: Double,
    val altitude: Double,
    val thermalLoad: Double,
)

@Composable
private fun ComposePointLine3DSampleScreen() {
    val context = LocalContext.current
    val series = remember {
        listOf(
            createSeries(
                name = "Launch Arc",
                color = Color.parseColor("#38BDF8"),
                pointColor = Color.parseColor("#7DD3FC"),
                waypoints = listOf(
                    MissionWaypoint("Launch", 0.0, 0.0, 12.0),
                    MissionWaypoint("Booster", 12.0, 18.0, 38.0),
                    MissionWaypoint("Separation", 28.0, 45.0, 64.0),
                    MissionWaypoint("Orbit Insert", 46.0, 72.0, 78.0),
                    MissionWaypoint("Transfer", 70.0, 88.0, 66.0),
                ),
            ),
            createSeries(
                name = "Return Arc",
                color = Color.parseColor("#F97316"),
                pointColor = Color.parseColor("#FDBA74"),
                waypoints = listOf(
                    MissionWaypoint("Return Burn", 76.0, 86.0, 61.0),
                    MissionWaypoint("Atmosphere", 62.0, 58.0, 52.0),
                    MissionWaypoint("Glide", 39.0, 31.0, 43.0),
                    MissionWaypoint("Approach", 18.0, 12.0, 25.0),
                    MissionWaypoint("Landing", 0.0, 0.0, 14.0),
                ),
            ),
        )
    }

    ComposeSamplePage(title = "Compose Point Line 3D") {
        PointLine3DChart(
            series = series,
            modifier = Modifier.matchParentSize(),
            axisOptions = Point3DAxisOptions(
                showAxes = true,
                showGridPlanes = true,
                showTicks = true,
                xAxisTitle = "Distance (km)",
                yAxisTitle = "Altitude (km)",
                zAxisTitle = "Energy Index",
                xLabelFormatter = { "${it.toInt()} km" },
                yLabelFormatter = { "${it.toInt()} km" },
                zLabelFormatter = { "${it.toInt()}" },
            ),
            presentationOptions = PointLine3DPresentationOptions(
                lineWidth = 4.6f,
                pointSize = 12f,
            ),
            cameraOptions = Point3DCameraOptions(
                yawDegrees = -38f,
                pitchDegrees = 22f,
                distance = 5.2f,
            ),
            onPointClick = { selectedSeries, selectedPoint ->
                val waypoint = selectedPoint.payload as? MissionWaypoint
                val message = if (waypoint != null) {
                    "${selectedSeries.name} | ${waypoint.phase} | " +
                        "Distance ${waypoint.distanceKm.toInt()} km | " +
                        "Altitude ${waypoint.altitude.toInt()} km | " +
                        "Energy ${waypoint.thermalLoad.toInt()}"
                } else {
                    "${selectedSeries.name} | ${selectedPoint.label ?: "Point"}"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        )
    }
}

private fun createSeries(
    name: String,
    color: Int,
    pointColor: Int,
    waypoints: List<MissionWaypoint>,
): PointLine3DSeries {
    return PointLine3DSeries(
        name = name,
        lineColor = color,
        pointColor = pointColor,
        points = waypoints.map { waypoint ->
            PointLine3DDatum(
                x = waypoint.distanceKm,
                y = waypoint.altitude,
                z = waypoint.thermalLoad,
                label = waypoint.phase,
                payload = waypoint,
            )
        },
    )
}
