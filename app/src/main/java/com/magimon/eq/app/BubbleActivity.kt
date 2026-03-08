package com.magimon.eq.app

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magimon.eq.bubble.BubbleAxisOptions
import com.magimon.eq.bubble.BubbleChartView
import com.magimon.eq.bubble.BubbleDatum
import com.magimon.eq.bubble.BubbleLegendItem
import com.magimon.eq.bubble.BubbleLayoutMode
import com.magimon.eq.bubble.BubbleLegendMode
import com.magimon.eq.bubble.BubblePresentationOptions
import java.text.NumberFormat
import java.util.Locale

/**
 * Demo screen showing packed bubbles with title/legend options in [BubbleChartView].
 */
class BubbleActivity : AppCompatActivity() {

    private data class LoanPoint(
        val loanType: String,
        val category: String,
        val loans: Double,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val palette = mapOf(
            "Arts" to Color.parseColor("#4A7FB1"),
            "Goods" to Color.parseColor("#FF9100"),
            "Labor" to Color.parseColor("#F84C5A"),
            "Services" to Color.parseColor("#62B8B4"),
        )
        val numberFormatter = NumberFormat.getIntegerInstance(Locale.US)

        val loans = listOf(
            LoanPoint("Food", "Arts", 129_087.0),
            LoanPoint("Retail", "Goods", 113_576.0),
            LoanPoint("Agriculture", "Labor", 102_304.0),
            LoanPoint("Services", "Services", 38_822.0),
            LoanPoint("Clothing", "Goods", 34_262.0),
            LoanPoint("Housing", "Labor", 13_854.0),
            LoanPoint("Arts", "Arts", 10_915.0),
            LoanPoint("Salon", "Services", 8_900.0),
            LoanPoint("Transport", "Labor", 7_400.0),
            LoanPoint("School", "Services", 6_800.0),
            LoanPoint("Bakery", "Goods", 5_200.0),
            LoanPoint("Tailoring", "Arts", 4_700.0),
            LoanPoint("Craft", "Labor", 3_600.0),
        )

        val bubbleView = BubbleChartView(this).apply {
            setChartBackgroundColor(Color.parseColor("#E6E6E6"))
            setLayoutMode(BubbleLayoutMode.PACKED)
            setPresentationOptions(
                BubblePresentationOptions(
                    title = "Which type of loan is the most popular?",
                    showLegend = true,
                    legendMode = BubbleLegendMode.AUTO_WITH_OVERRIDE,
                ),
            )
            setAxisOptions(
                BubbleAxisOptions(
                    showAxes = false,
                    showGrid = false,
                    showTicks = false,
                ),
            )
            setLegendItems(
                listOf(
                    BubbleLegendItem("Arts", palette.getValue("Arts")),
                    BubbleLegendItem("Goods", palette.getValue("Goods")),
                    BubbleLegendItem("Labor", palette.getValue("Labor")),
                    BubbleLegendItem("Services", palette.getValue("Services")),
                ),
            )

            setData(loans) { loan ->
                BubbleDatum(
                    x = 0.0,
                    y = 0.0,
                    size = loan.loans,
                    color = palette[loan.category] ?: Color.GRAY,
                    label = if (loan.loans >= 10_000.0) {
                        "${loan.loanType}\n${numberFormatter.format(loan.loans.toLong())} Loans"
                    } else {
                        null
                    },
                    legendGroup = loan.category,
                    payload = loan,
                )
            }

            setOnBubbleClickListener { datum ->
                val loan = datum.payload as? LoanPoint
                val message = if (loan != null) {
                    "${loan.loanType} | ${loan.category} | ${numberFormatter.format(loan.loans.toLong())} Loans"
                } else {
                    "${datum.label ?: "Unknown"} | size=${datum.size}"
                }
                Toast.makeText(this@BubbleActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        setContentView(bubbleView)
    }
}
