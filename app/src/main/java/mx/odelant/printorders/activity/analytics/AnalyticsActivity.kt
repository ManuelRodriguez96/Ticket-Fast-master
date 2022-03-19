package mx.odelant.printorders.activity.analytics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.model.GradientColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.odelant.printorders.R
import mx.odelant.printorders.dataLayer.AnalyticsDL
import mx.odelant.printorders.dataLayer.AppDatabase
import mx.odelant.printorders.databinding.AnalyticsActivityBinding
import mx.odelant.printorders.entities.Client
import mx.odelant.printorders.entities.Product
import mx.odelant.printorders.utils.Formatter
import java.util.*

class AnalyticsActivity : AppCompatActivity() {
    private lateinit var binding : AnalyticsActivityBinding
    private var mSelectedDataType: AnalyticsDL.DataType = AnalyticsDL.DataType.Revenue
    private var mSelectedDateGranularity: AnalyticsDL.DateScope = AnalyticsDL.DateScope.Month
    private var mSelectedDate: Calendar = Calendar.getInstance()
    private var mSelectedProduct: Product? = null
    private var mSelectedClient: Client? = null

    private val rAnalyticsActivity = R.layout.analytics__activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AnalyticsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setToolbar()
        bindAdapters()
        updateChart()
        setListeners()
    }

    private fun updateChart() {
        val db = AppDatabase.getInstance(this)
        GlobalScope.launch {

            val dataSourceInt = AnalyticsDL.getBarChart(
                db,
                mSelectedDataType,
                mSelectedDateGranularity,
                mSelectedDate,
                mSelectedProduct,
                mSelectedClient
            )

            val totals =
                AnalyticsDL.getTotals(
                    db,
                    mSelectedDateGranularity,
                    mSelectedDate,
                    mSelectedProduct,
                    mSelectedClient
                )

            val returns =
                AnalyticsDL.getTotalReturns(
                    db,
                    mSelectedDateGranularity,
                    mSelectedDate,
                    mSelectedProduct,
                    mSelectedClient
                )

            withContext(Dispatchers.Main) {
                binding.analyticsTvDateLabel.text =
                    Formatter.toScopedDate(mSelectedDateGranularity, mSelectedDate)
                binding.analyticsTvClientLabel.text = mSelectedClient?.name ?: "-"
                binding.analyticsTvProductLabel.text = mSelectedProduct?.name ?: "-"

                val formattedRevenue = "$${Formatter.intInHundredthsToString(totals.totalRevenue)}"
                binding.analyticsTvTotalRevenue.text = formattedRevenue
                binding.analyticsTvTotalQuantity.text =
                    Formatter.intInHundredthsToString(totals.totalAmount)
                binding.analyticsTvTotalOrders.text = totals.totalOrders.toString()
                binding.analyticsTvTotalReturns.text =
                    Formatter.intInHundredthsToString(returns)
            }

            val dataSource = dataSourceInt.map {
                Pair(it.date, it.amount.toFloat())
            }

            updateBarData(dataSource)
        }
    }

    private fun updateBarData(dataSource: List<Pair<Date, Float>>) {
        val yAxisLabelCount = 6

        val localDate = mSelectedDate
        val localDateGranularity = mSelectedDateGranularity
        val localDataType = mSelectedDataType

        val values = dataSource.map {
            val calendar = Calendar.getInstance()
            calendar.time = it.first

            val xValue = when (localDateGranularity) {
                AnalyticsDL.DateScope.Week ->
                    calendar.get(Calendar.DAY_OF_WEEK).toFloat()
                AnalyticsDL.DateScope.Month ->
                    calendar.get(Calendar.DAY_OF_MONTH).toFloat()
                AnalyticsDL.DateScope.Year ->
                    calendar.get(Calendar.MONTH).toFloat()
            }
            BarEntry(xValue, it.second)
        }

        val chart = findViewById<BarChart>(R.id.analytics_bc_chart)

        chart.setFitBars(true)

        chart.setDrawGridBackground(false)
        chart.setDrawBarShadow(false)
        chart.setDrawValueAboveBar(true)
        chart.setMaxVisibleValueCount(31)
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.setDrawGridBackground(false)

        val xAxis = chart.xAxis
        xAxis.position = XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawLabels(false)
        xAxis.granularity = 1f // only intervals of 1 day
        xAxis.labelCount = 16
        xAxis.setDrawLabels(true)

        when (localDateGranularity) {
            AnalyticsDL.DateScope.Week -> {
                xAxis.axisMinimum = 0f
                xAxis.axisMaximum = 6f
            }
            AnalyticsDL.DateScope.Month -> {
                xAxis.axisMinimum = 1f
                xAxis.axisMaximum = localDate.getActualMaximum(Calendar.DAY_OF_MONTH).toFloat()
            }
            AnalyticsDL.DateScope.Year -> {
                xAxis.axisMinimum = 0f
                xAxis.axisMaximum = 11f
            }
        }

        val weekdays =
            arrayOf("Domingo", "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado")
        val months = arrayOf(
            "Enero",
            "Febrero",
            "Marzo",
            "Abril",
            "Mayo",
            "Junio",
            "Julio",
            "Agosto",
            "Septiembre",
            "Octubre",
            "Noviembre",
            "Diciembre"
        )

        val xAxisFormatter = IAxisValueFormatter { value, _ ->
            val intGranularity = value.toInt()
            when (localDateGranularity) {
                AnalyticsDL.DateScope.Week ->
                    if (intGranularity < 7) weekdays[intGranularity] else ""
                AnalyticsDL.DateScope.Month ->
                    intGranularity.toString()
                AnalyticsDL.DateScope.Year ->
                    if (intGranularity < 12) months[intGranularity] else ""
            }
        }

        xAxis.valueFormatter = xAxisFormatter
        xAxis.labelRotationAngle = 45f

        val yAxisFormatter = IAxisValueFormatter { value, _ ->
            when (localDataType) {
                AnalyticsDL.DataType.Revenue ->
                    "$${Formatter.intInHundredthsToString(value.toInt())}"
                AnalyticsDL.DataType.Quantity ->
                    Formatter.intInHundredthsToString(value.toInt())
            }
        }

        val leftAxis = chart.axisLeft
        leftAxis.setDrawLabels(true)
        leftAxis.granularity = 100f
        leftAxis.labelCount = yAxisLabelCount
        leftAxis.valueFormatter = yAxisFormatter

        val rightAxis = chart.axisRight
        rightAxis.setDrawGridLines(false)
        rightAxis.isEnabled = false

        val l = chart.legend
        l.isEnabled = false
        val desc = chart.description
        desc.isEnabled = false

        val set1: BarDataSet

        if (chart.data != null && chart.data.dataSetCount > 0) {
            set1 = chart.data.getDataSetByIndex(0) as BarDataSet
            set1.values = values

            set1.valueFormatter = IValueFormatter { value, _, _, _ ->
                when (localDataType) {
                    AnalyticsDL.DataType.Revenue ->
                        "$${Formatter.intInHundredthsToString(value.toInt())}"
                    AnalyticsDL.DataType.Quantity ->
                        Formatter.intInHundredthsToString(value.toInt())
                }
            }
            chart.data.notifyDataChanged()
            chart.notifyDataSetChanged()
        } else {
            set1 = BarDataSet(values, "")

            set1.setDrawIcons(false)
            set1.valueFormatter = IValueFormatter { value, _, _, _ ->
                when (localDataType) {
                    AnalyticsDL.DataType.Revenue ->
                        "$${Formatter.intInHundredthsToString(value.toInt())}"
                    AnalyticsDL.DataType.Quantity ->
                        Formatter.intInHundredthsToString(value.toInt())
                }
            }

            val gradientColors = arrayListOf<GradientColor>()
            gradientColors.add(
                GradientColor(
                    ContextCompat.getColor(this, android.R.color.holo_blue_light),
                    ContextCompat.getColor(this, android.R.color.holo_blue_dark)
                )
            )

            set1.gradientColors = gradientColors

            val dataSets = arrayListOf<IBarDataSet>()
            dataSets.add(set1)

            val data = BarData(dataSets)
            data.setValueTextSize(10f)
            data.barWidth = 0.9f

            chart.data = data
        }

        chart.invalidate()
    }

    private fun setToolbar() {
        val rToolbar = binding.analyticsToolbar
        setSupportActionBar(rToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindAdapters() {
    }

    private fun setListeners() {
        setSelectFilterButton()
    }

    private fun setSelectFilterButton() {
        val db = AppDatabase.getInstance(applicationContext)
        binding.analyticsBtnSelectFilters.setOnClickListener {
            AnalyticsDialog.makeSelectFiltersDialog(
                mSelectedDate,
                mSelectedDateGranularity,
                mSelectedDataType,
                mSelectedClient,
                mSelectedProduct,
                this,
                db
            ) { date: Calendar, dateScope: AnalyticsDL.DateScope, dataType: AnalyticsDL.DataType, client: Client?, product: Product? ->
                mSelectedDate = date
                mSelectedDateGranularity = dateScope
                mSelectedDataType = dataType
                mSelectedClient = client
                mSelectedProduct = product
                updateChart()
            }
        }
    }



    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
