package mx.odelant.printorders.activity.orderHistory

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.borax12.materialdaterangepicker.date.DatePickerDialog
import kotlinx.android.synthetic.main.order_history__activity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.odelant.printorders.R
import mx.odelant.printorders.activity.orderDetail.OrderDetailActivity
import mx.odelant.printorders.activity.utils.adapter.Grid3CellAdapter
import mx.odelant.printorders.activity.utils.adapter.Grid3CellContent
import mx.odelant.printorders.activity.utils.adapter.Grid3CellRow
import mx.odelant.printorders.dataLayer.AppDatabase
import mx.odelant.printorders.dataLayer.CartDL
import mx.odelant.printorders.entities.CartDao
import mx.odelant.printorders.entities.Client
import mx.odelant.printorders.entities.OrderToExcel
import mx.odelant.printorders.utils.Formatter
import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class OrderHistoryActivity : AppCompatActivity() {

    private val rOrderHistoryActivity = R.layout.order_history__activity
    private val mOrdersListViewAdapter = Grid3CellAdapter()
    private var mSelectedClient: Client? = null
    private val mCalendarStart = initCalendarStart()
    private val mCalendarEnd = initCalendarEnd()
    private var db: AppDatabase? = null
    private val ordersToPrint : ArrayList<CartDao.CartAndClient> = ArrayList()
    private var cartsAndClients : List<CartDao.CartAndClient> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getInstance(this)
        setContentView(rOrderHistoryActivity)
        setToolbar()
        bindAdapters()
        setDataSources()
        setListeners()
    }

    fun checkPermissions(){
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED-> {

                GlobalScope.launch {
                    if (ordersToPrint.size > 0)
                        generateExcel(ordersToPrint)
                    else
                        generateExcel(cartsAndClients)
                }
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    1
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.size > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                checkPermissions()
            }
        }


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
            checkPermissions()
        }
    }

//    private var filePath: File? = null

    private fun generateExcel(cartsAndClients : List<CartDao.CartAndClient>) {

        val nameFile = "/Reporte-"+SimpleDateFormat("dd-MM-yy-hh:mm").format(Date())+".xls"
        val filePath : File = File(Environment.getExternalStorageDirectory().toString() + nameFile )

        val workbook = HSSFWorkbook()

        var cell: Cell


        val cellStyle = workbook.createCellStyle()
        cellStyle.fillForegroundColor = HSSFColor.AQUA.index
        cellStyle.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
        cellStyle.alignment = CellStyle.ALIGN_CENTER

        val sheet = workbook.createSheet("Registro de ventas")


        var row = sheet.createRow(0)

        cell = row.createCell(0)
        cell.setCellValue("Folio de la venta")

        cell = row.createCell(1)
        cell.setCellValue("Fecha de creación")

        cell = row.createCell(2)
        cell.setCellValue("Cliente")


        cell = row.createCell(3)
        cell.setCellValue("Total")

        val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm aa")

        var count = 1
        for (item in cartsAndClients) {

            var row = sheet.createRow(count)
            count++

            cell = row.createCell(0)
            cell.setCellValue(item.cart?.folio.toString())
//            cell.cellStyle = cellStyle

            cell = row.createCell(1)
            cell.setCellValue(dateFormat.format(item.cart.dateCreated))
//            cell.cellStyle = cellStyle

            if (item.client != null){
                cell = row.createCell(2)
                cell.setCellValue(item.client.name)
            }else{
                cell = row.createCell(2)
                cell.setCellValue("Sin Cliente")
            }

            cell = row.createCell(3)
            cell.setCellValue(Formatter.intInHundredthsToString(item.cart.totalPriceInCents))
        }

        try {
            if (!filePath!!.exists()) {
                filePath!!.createNewFile()
            }


            val fileOutputStream = FileOutputStream(filePath)
            workbook.write(fileOutputStream)
            if (fileOutputStream != null) {
                fileOutputStream.flush()
                fileOutputStream.close()
            }

            val photoURI = FileProvider.getUriForFile(
                this,
                this.applicationContext.packageName + ".provider",
                filePath
            )

            val target = Intent(Intent.ACTION_SEND)
            target.setDataAndType(photoURI, "text/plain")
            target.putExtra(Intent.EXTRA_SUBJECT, "Reporte Ventas")
            target .putExtra(Intent.EXTRA_STREAM, photoURI)

            val intent = Intent.createChooser(target, "Open File")

            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Instruct the user to install a PDF reader here, or something
            }

        } catch (e: Exception) {
            e.printStackTrace()
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

                cartsAndClients =
                    CartDL.getCartsWithClient(db, selectedClient, mCalendarStart, mCalendarEnd)

                val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm aa")
                val sharedPref = getSharedPreferences("SHARED_PREFERENCES", Context.MODE_PRIVATE)


                val actualization = sharedPref.getBoolean("firstActualization", false)
                if (!actualization) {
                    val carts = db.cartDao().getAllCart()
                    var iterator = 0
                    if (carts.size != 0) {
                        for (car in carts) {
                            iterator += 1
                            db.cartDao().updateFolio(car.cart_id, iterator)
                        }
                        sharedPref.edit().putBoolean("firstActualization", true).commit()
                    }
                }

                for (cartAndClient in cartsAndClients) {
                    val row = Grid3CellContent(
                        "",
                        "${dateFormat.format(cartAndClient.cart.dateCreated)}\nFolio:  #0${cartAndClient.cart?.folio}\nCliente: ${
                            cartAndClient.client?.name
                                ?: "-"
                        }",
                        "Total: $${Formatter.intInHundredthsToString(cartAndClient.cart.totalPriceInCents)}",
                        {
                            val orderDetailIntent = Intent(context, OrderDetailActivity::class.java)
                            orderDetailIntent.putExtra(
                                OrderDetailActivity.INTENT_CART_ID_KEY,
                                cartAndClient.cart.cart_id
                            )
                            startActivity(orderDetailIntent)
                        },
                        {

                            ordersToPrint.add(cartAndClient)
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

