package com.magimon.eq.app.ui.android

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.app.ui.applySampleToolbar
import com.magimon.eq.bubble3d.Bubble3DAxisOptions
import com.magimon.eq.bubble3d.Bubble3DCameraOptions
import com.magimon.eq.bubble3d.Bubble3DChartView
import com.magimon.eq.bubble3d.Bubble3DDatum
import com.magimon.eq.bubble3d.Bubble3DPresentationOptions
import java.text.NumberFormat
import java.util.Locale

/**
 * Demo screen for the OpenGL-based true 3D bubble chart.
 */
class Bubble3DActivity : AppCompatActivity() {

    private data class InnovationHub(
        val label: String,
        val region: String,
        val talentIndex: Double,
        val fundingMomentum: Double,
        val operatingScale: Double,
        val ventureVolumeM: Double,
    )

    private lateinit var bubble3DChartView: Bubble3DChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val palette = mapOf(
            "North America" to Color.parseColor("#4F46E5"),
            "Europe" to Color.parseColor("#0EA5E9"),
            "Asia" to Color.parseColor("#10B981"),
            "Middle East" to Color.parseColor("#F59E0B"),
        )
        val numberFormatter = NumberFormat.getIntegerInstance(Locale.US)

        val hubs = listOf(
            InnovationHub("SF", "North America", 95.0, 92.0, 90.0, 920.0),
            InnovationHub("NYC", "North America", 88.0, 84.0, 81.0, 710.0),
            InnovationHub("Austin", "North America", 79.0, 74.0, 67.0, 420.0),
            InnovationHub("London", "Europe", 86.0, 80.0, 76.0, 610.0),
            InnovationHub("Berlin", "Europe", 77.0, 72.0, 63.0, 350.0),
            InnovationHub("Paris", "Europe", 80.0, 69.0, 68.0, 330.0),
            InnovationHub("Seoul", "Asia", 83.0, 78.0, 85.0, 520.0),
            InnovationHub("Tokyo", "Asia", 82.0, 66.0, 88.0, 470.0),
            InnovationHub("Singapore", "Asia", 76.0, 82.0, 73.0, 310.0),
            InnovationHub("Bengaluru", "Asia", 74.0, 86.0, 59.0, 340.0),
            InnovationHub("Dubai", "Middle East", 68.0, 71.0, 70.0, 260.0),
            InnovationHub("Riyadh", "Middle East", 64.0, 77.0, 62.0, 210.0),
        )

        bubble3DChartView = Bubble3DChartView(this).apply {
            setPresentationOptions(
                Bubble3DPresentationOptions(),
            )
            setAxisOptions(
                Bubble3DAxisOptions(
                    showAxes = true,
                    showGridPlanes = true,
                    showTicks = true,
                    xAxisTitle = "Talent Index",
                    yAxisTitle = "Funding Momentum",
                    zAxisTitle = "Operating Scale",
                    xLabelFormatter = { "Talent ${it.toInt()}" },
                    yLabelFormatter = { "Funding ${it.toInt()}" },
                    zLabelFormatter = { "Scale ${it.toInt()}" },
                ),
            )
            setCameraOptions(
                Bubble3DCameraOptions(
                    yawDegrees = -32f,
                    pitchDegrees = 18f,
                    distance = 4.8f,
                ),
            )
            setData(
                hubs.map { hub ->
                    Bubble3DDatum(
                        x = hub.talentIndex,
                        y = hub.fundingMomentum,
                        z = hub.operatingScale,
                        size = hub.ventureVolumeM,
                        color = palette[hub.region] ?: Color.GRAY,
                        label = hub.label,
                        legendGroup = hub.region,
                        payload = hub,
                    )
                },
            )
            setOnBubbleClickListener { datum ->
                val hub = datum.payload as? InnovationHub
                val message = if (hub != null) {
                    "${hub.label} | ${hub.region} | Talent ${hub.talentIndex.toInt()} | " +
                        "Funding ${hub.fundingMomentum.toInt()} | Scale ${hub.operatingScale.toInt()} | " +
                        "$${numberFormatter.format(hub.ventureVolumeM.toLong())}M"
                } else {
                    "${datum.label ?: "Unknown"} | size=${datum.size}"
                }
                Toast.makeText(this@Bubble3DActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        applySampleToolbar(
            title = "Bubble 3D",
            content = bubble3DChartView,
        )
    }

    override fun onResume() {
        super.onResume()
        if (::bubble3DChartView.isInitialized) {
            bubble3DChartView.onResume()
        }
    }

    override fun onPause() {
        if (::bubble3DChartView.isInitialized) {
            bubble3DChartView.onPause()
        }
        super.onPause()
    }
}
