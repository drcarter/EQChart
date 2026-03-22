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
import com.magimon.eq.compose.pointcloud3d.PointCloud3DChart
import com.magimon.eq.point3d.Point3DAxisOptions
import com.magimon.eq.point3d.Point3DCameraOptions
import com.magimon.eq.pointcloud3d.PointCloud3DDatum
import com.magimon.eq.pointcloud3d.PointCloud3DPresentationOptions

/**
 * Compose sample screen for the true 3D Point Cloud chart.
 */
class ComposePointCloud3DActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EQChartTheme {
                ComposePointCloud3DSampleScreen()
            }
        }
    }
}

private data class SignalNode(
    val label: String,
    val cluster: String,
    val computeScore: Double,
    val marketReadiness: Double,
    val dataDensity: Double,
    val nodeWeight: Double,
)

@Composable
private fun ComposePointCloud3DSampleScreen() {
    val context = LocalContext.current
    val palette = remember {
        mapOf(
            "Core AI" to Color.parseColor("#38BDF8"),
            "Robotics" to Color.parseColor("#F97316"),
            "Climate" to Color.parseColor("#10B981"),
            "Fintech" to Color.parseColor("#A855F7"),
        )
    }
    val data = remember(palette) {
        listOf(
            SignalNode("Aquila", "Core AI", 92.0, 86.0, 90.0, 42.0),
            SignalNode("Atlas", "Core AI", 88.0, 79.0, 84.0, 36.0),
            SignalNode("Nova", "Core AI", 84.0, 74.0, 79.0, 31.0),
            SignalNode("Prism", "Core AI", 90.0, 82.0, 88.0, 39.0),
            SignalNode("Pulse", "Core AI", 81.0, 70.0, 75.0, 28.0),
            SignalNode("Forge", "Robotics", 74.0, 77.0, 65.0, 26.0),
            SignalNode("Crane", "Robotics", 69.0, 72.0, 61.0, 24.0),
            SignalNode("Helix", "Robotics", 78.0, 81.0, 69.0, 33.0),
            SignalNode("Torque", "Robotics", 72.0, 68.0, 58.0, 21.0),
            SignalNode("Foundry", "Robotics", 80.0, 76.0, 72.0, 29.0),
            SignalNode("Bloom", "Climate", 61.0, 85.0, 74.0, 23.0),
            SignalNode("Drift", "Climate", 58.0, 79.0, 70.0, 20.0),
            SignalNode("Aurora", "Climate", 66.0, 88.0, 81.0, 34.0),
            SignalNode("Harbor", "Climate", 63.0, 82.0, 76.0, 27.0),
            SignalNode("Leaf", "Climate", 56.0, 74.0, 68.0, 18.0),
            SignalNode("Ledger", "Fintech", 79.0, 91.0, 64.0, 25.0),
            SignalNode("Vault", "Fintech", 83.0, 94.0, 67.0, 30.0),
            SignalNode("Beacon", "Fintech", 76.0, 88.0, 62.0, 22.0),
            SignalNode("Lattice", "Fintech", 81.0, 86.0, 66.0, 24.0),
            SignalNode("Quill", "Fintech", 73.0, 84.0, 59.0, 19.0),
            SignalNode("Mesh", "Core AI", 86.0, 76.0, 82.0, 32.0),
            SignalNode("Circuit", "Robotics", 71.0, 73.0, 63.0, 22.0),
            SignalNode("Current", "Climate", 60.0, 81.0, 72.0, 21.0),
            SignalNode("Mint", "Fintech", 77.0, 89.0, 61.0, 20.0),
        ).map { node ->
            PointCloud3DDatum(
                x = node.computeScore,
                y = node.marketReadiness,
                z = node.dataDensity,
                size = node.nodeWeight,
                color = palette[node.cluster] ?: Color.LTGRAY,
                label = node.label,
                payload = node,
            )
        }
    }

    ComposeSamplePage(title = "Compose Point Cloud 3D") {
        PointCloud3DChart(
            data = data,
            modifier = Modifier.matchParentSize(),
            axisOptions = Point3DAxisOptions(
                showAxes = true,
                showGridPlanes = true,
                showTicks = true,
                xAxisTitle = "Compute Score",
                yAxisTitle = "Market Readiness",
                zAxisTitle = "Data Density",
                xLabelFormatter = { "${it.toInt()}" },
                yLabelFormatter = { "${it.toInt()}" },
                zLabelFormatter = { "${it.toInt()}" },
            ),
            presentationOptions = PointCloud3DPresentationOptions(),
            cameraOptions = Point3DCameraOptions(
                yawDegrees = -34f,
                pitchDegrees = 18f,
                distance = 5f,
            ),
            onPointClick = { datum ->
                val node = datum.payload as? SignalNode
                val message = if (node != null) {
                    "${node.label} | ${node.cluster} | " +
                        "Compute ${node.computeScore.toInt()} | " +
                        "Market ${node.marketReadiness.toInt()} | " +
                        "Data ${node.dataDensity.toInt()} | " +
                        "Weight ${node.nodeWeight.toInt()}"
                } else {
                    datum.label ?: "Point"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        )
    }
}
