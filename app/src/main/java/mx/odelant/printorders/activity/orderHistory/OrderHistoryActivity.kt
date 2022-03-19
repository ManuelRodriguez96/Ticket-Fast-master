package mx.odelant.printorders.activity.orderHistory

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.borax12.materialdaterangepicker.date.DatePickerDialog
import kotlinx.coroutines.*
import mx.odelant.printorders.R
import mx.odelant.printorders.activity.orderDetail.OrderDetailActivity
import mx.odelant.printorders.activity.utils.adapter.Grid3CellAdapter
import mx.odelant.printorders.activity.utils.adapter.Grid3CellContent
import mx.odelant.printorders.activity.utils.adapter.Grid3CellHeader
import mx.odelant.printorders.activity.utils.adapter.Grid3CellRow
import mx.odelant.printorders.dataLayer.AppDatabase
import mx.odelant.printorders.dataLayer.CartDL
import mx.odelant.printorders.dataLayer.CartItemDL
import mx.odelant.printorders.dataLayer.CartReturnItemDL
import mx.odelant.printorders.databinding.OrderHistoryActivityBinding
import mx.odelant.printorders.entities.CartDao
import mx.odelant.printorders.entities.CartItemDao
import mx.odelant.printorders.entities.Client
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


class OrderHistoryActivity : AppCompatActivity() {
    private lateinit var binding : OrderHistoryActivityBinding
    private val permissions_storage = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )
    private val rOrderHistoryActivity = R.layout.order_history__activity
    private val mOrdersListViewAdapter = Grid3CellAdapter()
    private var mSelectedClient: Client? = null
    private val mCalendarStart = initCalendarStart()
    private val mCalendarEnd = initCalendarEnd()
    private var db: AppDatabase? = null
    private val ordersToPrint: ArrayList<CartDao.CartAndClient> = ArrayList()
    private var cartsAndClients: List<CartDao.CartAndClient> = ArrayList()
    private var check: Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = AppDatabase.getInstance(this)
        binding = OrderHistoryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setToolbar()
        bindAdapters()
        setDataSources()
        setListeners()


    }
/*
    fun checkPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {

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
    }*/
/*
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


    }*/

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
        val rToolbar = binding.orderHistoryToolbar
        setSupportActionBar(rToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindAdapters() {
        val rOrdersListView = binding.orderHistoryLvOrders
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
        binding.orderHistoryTvSelectDate.text = dateSelectString

        val rSelectClientButton = binding.orderHistoryBtnSelectClient
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
                binding.orderHistoryTvSelectDate.text = dpDateSelectString

                updateOrdersList()
            }

        val rSelectDateRangeButton = binding.orderHistoryBtnSelectDateRange
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

        val downloadButtonOrders = binding.orderHistoryImbtnDonwloadFile
        downloadButtonOrders.setOnClickListener {
            getStorePermission.launch(permissions_storage)
        }
    }

//    private var filePath: File? = null

    private fun sharedExcel(
        cartsAndClients: List<CartDao.CartAndClient>,
        db: AppDatabase
    ): Deferred<HSSFWorkbook> {

        return GlobalScope.async {

            val workbook = HSSFWorkbook()

            var cell: Cell


            val cellStyle = workbook.createCellStyle()
            cellStyle.fillForegroundColor = HSSFColor.AQUA.index
            cellStyle.fillPattern = HSSFCellStyle.SOLID_FOREGROUND
            cellStyle.alignment = CellStyle.ALIGN_CENTER

            val sheet = workbook.createSheet("Registro de ventas")


            var row = sheet.createRow(0)
            cell = row.createCell(0)
            cell.setCellValue("Ventas")

            cell = row.createCell(4)
            cell.setCellValue("Productos")

            cell = row.createCell(8)
            cell.setCellValue("Devoluciones")

            row = sheet.createRow(1)

            cell = row.createCell(0)
            cell.setCellValue("Folio de la venta")

            cell = row.createCell(1)
            cell.setCellValue("Fecha de creación")

            cell = row.createCell(2)
            cell.setCellValue("Cliente")

            cell = row.createCell(3)
            cell.setCellValue("Total")

            cell = row.createCell(4)
            cell.setCellValue("Unidad")

            cell = row.createCell(5)
            cell.setCellValue("Concepto")

            cell = row.createCell(6)
            cell.setCellValue("Precio")

            cell = row.createCell(7)
            cell.setCellValue("Total x producto")

            cell = row.createCell(8)
            cell.setCellValue("Unidad")

            cell = row.createCell(9)
            cell.setCellValue("Concepto")


            val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm aa")

            var count = 2
            for (item in cartsAndClients) {

                var row = sheet.createRow(count)
                count++

                cell = row.createCell(0)
                cell.setCellValue(item.cart?.folio.toString())

                cell = row.createCell(1)
                cell.setCellValue(dateFormat.format(item.cart.dateCreated))

                if (item.client != null) {
                    cell = row.createCell(2)
                    cell.setCellValue(item.client.name)
                } else {
                    cell = row.createCell(2)
                    cell.setCellValue("Sin Cliente")
                }

                cell = row.createCell(3)
                cell.setCellValue(Formatter.intInHundredthsToString(item.cart.totalPriceInCents))


                val cartItems : List<CartItemDao.CartItemAndProduct> =GlobalScope.async(Dispatchers.IO) {
                    CartItemDL.getCartItemAndProductByCartId(
                        db,
                        item.cart.cart_id
                    )
                }.await()


                for (subItem in cartItems){

                    var row = sheet.createRow(count)
                    count++

                    cell = row.createCell(4)
                    cell.setCellValue(Formatter.intInHundredthsToString(subItem.cartItem.quantityInHundredths))

                    cell = row.createCell(5)
                    cell.setCellValue(subItem.product.name)

                    cell = row.createCell(6)
                    cell.setCellValue(Formatter.intInHundredthsToString(subItem.product.basePriceInCents))

                    cell = row.createCell(7)
                    cell.setCellValue(Formatter.intInHundredthsToString((subItem.cartItem.unitPriceInCents * subItem.cartItem.quantityInHundredths + 50) / 100))
                }

                val cartReturnItemsAsync =
                    GlobalScope.async(Dispatchers.IO) {
                        CartReturnItemDL.getCartReturnItemAndProductByCartId(
                            db,
                            item.cart.cart_id
                        )
                    }.await()

                for (subItem in cartReturnItemsAsync){

                    var row = sheet.createRow(count)
                    count++

                    cell = row.createCell(8)
                    cell.setCellValue(Formatter.intInHundredthsToString(subItem.cartReturnItem.quantityInHundredths))

                    cell = row.createCell(9)
                    cell.setCellValue(subItem.product.name)

                }

            }

            workbook
        }


    }

    private fun generateExcel(context: Context) {

        GlobalScope.launch {

            db = AppDatabase.getInstance(context)
            val workbook = sharedExcel(ordersToPrint, db!!).await()

            val nameFile = "/Reporte" + SimpleDateFormat("ddMMyyhhmm").format(Date()) + ".xls"
            val filePath: File =
                File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + nameFile)


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
                    context,
                    context.applicationContext.packageName + ".provider",
                    filePath
                )

                val target = Intent(Intent.ACTION_SEND)
                target.setDataAndType(photoURI, "text/plain")
                target.putExtra(Intent.EXTRA_SUBJECT, "Reporte Ventas")
                target.putExtra(Intent.EXTRA_STREAM, photoURI)

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


    }

    private fun updateSelectedClient(selectedClient: Client?) {
        mSelectedClient = selectedClient
        binding.orderHistoryTvSelectClient.text = selectedClient?.name ?: "Ninguno"
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


                if (cartsAndClients.isNotEmpty())
                    runOnUiThread {
                        binding.orderHistoryTvSelectDateEnd.visibility = View.GONE
                    }
                else
                    runOnUiThread {
                        binding.orderHistoryTvSelectDateEnd.visibility = View.VISIBLE
                    }


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

                val header = Grid3CellHeader(
                    "", "Ventas", ""
                ) {
                    check = !check
                    mOrdersListViewAdapter.setCheck(check)
                    if (check)
                        ordersToPrint.addAll(cartsAndClients)
                    else
                        ordersToPrint.clear()
                }

                header.hideField1 = true
                header.hideField3 = true
                data.add(header)

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
                        }, {
                            ordersToPrint.remove(cartAndClient)

                        }
                    )
                    row.hideField1 = true
                    data.add(row)
                }
            }
            withContext(Dispatchers.Main) {

                mOrdersListViewAdapter.setRowList(data, true)

            }
        }
    }

    private val getStorePermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        var flag = true
        permissions.entries.forEach{if(!it.value) flag = false}
        when{
            flag -> {
                GlobalScope.launch {
                    if (ordersToPrint.size > 0) {
                        generateExcel(this@OrderHistoryActivity)
                    } else
                        runOnUiThread{
                            Toast.makeText(
                                this@OrderHistoryActivity,
                                "Seleccione elementos",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                }
            }
            else -> {
                Toast.makeText(this,"Para el funcionamiento correcto de la aplicación se necesitan los permisos requeridos.", Toast.LENGTH_LONG).show()
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

