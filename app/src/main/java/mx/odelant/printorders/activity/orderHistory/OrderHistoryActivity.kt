package mx.odelant.printorders.activity.orderHistory

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.borax12.materialdaterangepicker.date.DatePickerDialog
import kotlinx.android.synthetic.main.order_history__activity.*
import kotlinx.coroutines.*
import mx.odelant.printorders.R
import mx.odelant.printorders.activity.orderDetail.OrderDetailActivity
import mx.odelant.printorders.activity.utils.adapter.Grid3CellAdapter
import mx.odelant.printorders.activity.utils.adapter.Grid3CellContent
import mx.odelant.printorders.activity.utils.adapter.Grid3CellRow
import mx.odelant.printorders.dataLayer.AppDatabase
import mx.odelant.printorders.dataLayer.CartDL
import mx.odelant.printorders.dataLayer.CartItemDL
import mx.odelant.printorders.entities.CartDao
import mx.odelant.printorders.entities.Client
import mx.odelant.printorders.entities.OrderToExcel
import mx.odelant.printorders.utils.Formatter
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Workbook
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class  OrderHistoryActivity : AppCompatActivity() {

    private val rOrderHistoryActivity = R.layout.order_history__activity
    private val mOrdersListViewAdapter = Grid3CellAdapter()
    private var mSelectedClient: Client? = null
    private val ordersToPrint  = ArrayList<OrderToExcel>()
    private val mCalendarStart = initCalendarStart()
    private val mCalendarEnd = initCalendarEnd()
    private var  db: AppDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getInstance(this)
        setContentView(rOrderHistoryActivity)
        setToolbar()
        bindAdapters()
        setDataSources()
        setListeners()
    }

    private fun initCalendarStart(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar
    }

    private fun initCalendarEnd(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        return calendar
    }

    private fun setToolbar() {
        val rToolbar = order_history_toolbar
        setSupportActionBar(rToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindAdapters() {
        val rOrdersListView = order_history_lv_orders
        rOrdersListView.adapter = mOrdersListViewAdapter
    }

    private fun setDataSources() {
        updateOrdersList()
    }

    private fun setListeners() {
        val yearStart = mCalendarStart.get(Calendar.YEAR)
        val monthStart = mCalendarStart.get(Calendar.MONTH)
        val dayStart = mCalendarStart.get(Calendar.DAY_OF_MONTH)
        val yearEnd = mCalendarEnd.get(Calendar.YEAR)
        val monthEnd = mCalendarEnd.get(Calendar.MONTH)
        val dayEnd = mCalendarEnd.get(Calendar.DAY_OF_MONTH)

        val dateSelectString = "$dayStart/$monthStart/$yearStart a $dayEnd/$monthEnd/$yearEnd"
        order_history_tv_select_date.text = dateSelectString

        val rSelectClientButton = order_history_btn_select_client
        rSelectClientButton.setOnClickListener {
            val db = AppDatabase.getInstance(applicationContext)
            OrderHistoryDialog.makeSelectClientDialog(this, db) { selectedClient ->
                updateSelectedClient(selectedClient)
            }
        }

        val datePickerListener =
            DatePickerDialog.OnDateSetListener { _, dpYearStart, dpMonthStart, dpDayStart, dpYearEnd, dpMonthEnd, dpDayEnd ->
                mCalendarStart.set(Calendar.YEAR, dpYearStart)
                mCalendarStart.set(Calendar.MONTH, dpMonthStart)
                mCalendarStart.set(Calendar.DAY_OF_MONTH, dpDayStart)
                mCalendarEnd.set(Calendar.YEAR, dpYearEnd)
                mCalendarEnd.set(Calendar.MONTH, dpMonthEnd)
                mCalendarEnd.set(Calendar.DAY_OF_MONTH, dpDayEnd)

                val dpDateSelectString =
                    "$dpDayStart/$dpMonthStart/$dpYearStart a $dpDayEnd/$dpMonthEnd/$dpYearEnd"
                order_history_tv_select_date.text = dpDateSelectString

                updateOrdersList()
            }

        val rSelectDateRangeButton = order_history_btn_select_date_range
        rSelectDateRangeButton.setOnClickListener {
            val dpd = DatePickerDialog.newInstance(
                datePickerListener,
                mCalendarStart.get(Calendar.YEAR),
                mCalendarStart.get(Calendar.MONTH),
                mCalendarStart.get(Calendar.DAY_OF_MONTH),
                mCalendarEnd.get(Calendar.YEAR),
                mCalendarEnd.get(Calendar.MONTH),
                mCalendarEnd.get(Calendar.DAY_OF_MONTH)
            )
            dpd.show(fragmentManager, "Datepickerdialog")
        }

        val downloadButtonOrders = order_history_imbtn_donwloadFile
        downloadButtonOrders.setOnClickListener {
            if (ordersToPrint.size > 0)
                //TODO
            else
                Toast.makeText(this,"Primero debes seleccionar ordenes para descargar", Toast.LENGTH_LONG)
        }
    }

    private fun generateExcel(orders : ArrayList<OrderToExcel>){
        var workbook: Workbook = HSSFWorkbook()
        var cell: Cell
        var cellStyle = workbook.createCellStyle()
        cellStyle.fillForegroundColor = IndexedColors.BLACK.index
        var sheet = workbook.createSheet("Registro de ventas")

        var count = 0
        for (item in orders) {
            var row = sheet.createRow(count)
            cell = row.createCell(count)
            cell.setCellValue("Folio de la venta:")
            cell.cellStyle = cellStyle

            sheet.createRow(count + 1)
            cell = row.createCell(count + 1)
            cell.setCellValue(item.folio.toString())


            sheet.createRow(count + 1)
            cell = row.createCell(count + 1)
            cell.setCellValue("Fecha de creación:")
            cell.cellStyle = cellStyle

            sheet.createRow(count + 1)
            cell = row.createCell(count + 1)
            cell.setCellValue(item.dataCreated.toString())

            sheet.createRow(4)
            cell = row.createCell(4)
            cell.setCellValue("Cliente:")
            cell.cellStyle = cellStyle

            sheet.createRow(5)
            cell = row.createCell(5)
            cell.setCellValue(item.clientName)

            count++

            var productLines = db!!.cartItemDao().getCartItems(item.folio)

            for (line in  productLines){
                
            }

            var file = File(getExternalFilesDir(null), "Reporte de ordenes del (Fecha)" + ".xls")
            var outputStream: FileOutputStream

            try {
                outputStream = FileOutputStream(file)
                workbook.write(outputStream)

            } catch (e : IOException){
                e.printStackTrace()
            }

        }
    }

    private fun updateSelectedClient(selectedClient: Client?) {
        mSelectedClient = selectedClient
        order_history_tv_select_client.text = selectedClient?.name ?: "Ninguno"
        updateOrdersList()
    }

    private fun updateOrdersList() {
        val db = AppDatabase.getInstance(this)
        val context = this
        GlobalScope.launch {
            val data: ArrayList<Grid3CellRow> = ArrayList()
            withContext(Dispatchers.IO) {
                val selectedClient = mSelectedClient

                val cartsAndClients =
                    CartDL.getCartsWithClient(db, selectedClient, mCalendarStart, mCalendarEnd)

                val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm aa")
                val sharedPref = getSharedPreferences("SHARED_PREFERENCES", Context.MODE_PRIVATE)


                    val actualization = sharedPref.getBoolean("firstActualization",false)
                    if (!actualization) {
                        val carts = db.cartDao().getAllCart()
                        var iterator = 0
                        if (carts.size != 0) {
                            for (car in carts){
                                iterator += 1
                                db.cartDao().updateFolio(car.cart_id, iterator)
                            }
                            sharedPref.edit().putBoolean("firstActualization",true).commit()
                        }
                    }

                for (cartAndClient in cartsAndClients) {
                    val row = Grid3CellContent(
                        "",
                        "${dateFormat.format(cartAndClient.cart.dateCreated)}\nFolio:  #0${cartAndClient.cart?.folio}\nCliente: ${cartAndClient.client?.name
                            ?: "-"}",
                        "Total: $${Formatter.intInHundredthsToString(cartAndClient.cart.totalPriceInCents)}",
                        View.OnClickListener {
                            val orderDetailIntent = Intent(context, OrderDetailActivity::class.java)
                            orderDetailIntent.putExtra(
                                OrderDetailActivity.INTENT_CART_ID_KEY,
                                cartAndClient.cart.cart_id
                            )
                            startActivity(orderDetailIntent)
                        },
                        View.OnClickListener {
                            var orderSent = OrderToExcel(cartAndClient.client!!.name, cartAndClient.cart.dateCreated, 22.20, cartAndClient.cart.folio, true)
                            ordersToPrint.add(orderSent)
                       }
                    )
                    row.hideField1 = true
                    data.add(row)
                }
            }
            withContext(Dispatchers.Main) {

                mOrdersListViewAdapter.setRowList(data)

            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
    }
}

