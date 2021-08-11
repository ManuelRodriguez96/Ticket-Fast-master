package mx.odelant.printorders.activity.client

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.client__activity__detail.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.odelant.printorders.R
import mx.odelant.printorders.activity.utils.adapter.Grid3CellAdapter
import mx.odelant.printorders.activity.utils.adapter.Grid3CellContent
import mx.odelant.printorders.activity.utils.adapter.Grid3CellHeader
import mx.odelant.printorders.activity.utils.adapter.Grid3CellRow
import mx.odelant.printorders.dataLayer.AppDatabase
import mx.odelant.printorders.dataLayer.ClientDL
import mx.odelant.printorders.dataLayer.ClientPriceDL
import mx.odelant.printorders.dataLayer.ProductDL
import mx.odelant.printorders.entities.ClientPrice
import mx.odelant.printorders.utils.Formatter

class ClientDetailActivity : AppCompatActivity() {

    private val rClientDetailActivity: Int = R.layout.client__activity__detail
    private val mClientPricesListViewAdapter = Grid3CellAdapter()
    private val mClientId by lazy {
        intent.getIntExtra(INTENT_CLIENT_ID_KEY, 0)
    }

    companion object {
        const val INTENT_CLIENT_ID_KEY = "clientID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(rClientDetailActivity)

        if (mClientId == 0) {
            finish()
        }

        setToolbar()
        bindAdapters()
        setDataSources()
        setListeners()
    }

    private fun setToolbar() {
        val rToolbar = client_detail_toolbar
        setSupportActionBar(rToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindAdapters() {
        val rClientPricesListView = client_detail_lv_client_prices
        rClientPricesListView.adapter = mClientPricesListViewAdapter
    }

    private fun setDataSources() {
        GlobalScope.launch {
            handleClientEdit()
            updateClientPricesList(mClientPricesListViewAdapter)
        }
    }

    private fun setListeners() {

        val rFilterClientPricesEditText = client_detail_et_filter_client_prices
        rFilterClientPricesEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                GlobalScope.launch {
                    updateClientPricesList(mClientPricesListViewAdapter)
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        val db = AppDatabase.getInstance(applicationContext)
        val rAddClientPriceButton = client_detail_btn_add_client_price

        val rEditClientNameButton = client_detail_btn_edit_client_name

        val context = this

        GlobalScope.launch {
            val client = ClientDL.getById(db, mClientId)

            if (client != null) {
                rAddClientPriceButton.setOnClickListener {
                    ClientDetailDialog.makeAddClientPriceDialog(context, db, client) {
                        GlobalScope.launch {
                            updateClientPricesList(mClientPricesListViewAdapter)
                        }
                    }
                }
                rEditClientNameButton.setOnClickListener {
                    ClientDetailDialog.makeEditClientDialog(context, db, client) {
                        GlobalScope.launch {
                            handleClientEdit()
                        }
                    }
                }

                client_detail_toolbar.title = "Precios para ${client.name}"
            } else {
                rAddClientPriceButton.isEnabled = false
            }
        }
    }

    private suspend fun handleClientEdit() {
        val db = AppDatabase.getInstance(applicationContext)
        val rClientNameTextView = client_detail_tv_client_name

        val client = ClientDL.getById(db, mClientId)

        if (client == null || client.isDeleted) {
            this.finish()
        }

        withContext(Dispatchers.Main) {
            if (client != null) {
                rClientNameTextView.text = client.name
            }
        }
    }

    private suspend fun updateClientPricesList(gridAdapter: Grid3CellAdapter) {
        val db = AppDatabase.getInstance(applicationContext)
        val data: ArrayList<Grid3CellRow> = ArrayList()

        val header = Grid3CellHeader("", "Producto (Precio base)", "Precio cliente")
        header.hideField1 = true
        data.add(header)

        val rFilterClientPricesEditText = client_detail_et_filter_client_prices
        val searchString = rFilterClientPricesEditText.text.toString()

        val context = this

        withContext(Dispatchers.IO) {
            suspend fun clientPriceToRow(clientPrice: ClientPrice): Grid3CellContent {

                val product =
                    ProductDL.getProductById(db, clientPrice.productId)
                        ?: return Grid3CellContent.empty()

                val row = Grid3CellContent(
                    "",
                    "${product.name} (${Formatter.intInHundredthsToString(product.basePriceInCents)})",
                    Formatter.intInHundredthsToString(clientPrice.priceInCents),
                    View.OnClickListener {
                        ClientDetailDialog.makeEditClientPriceDialog(
                            context,
                            db,
                            clientPrice,
                            product
                        ) { GlobalScope.launch { updateClientPricesList(gridAdapter) } }
                    }, null
                )
                row.hideField1 = true
                return row
            }

            val clientPrices =
                ClientPriceDL.getAllClientPricesLikeProductName(db, mClientId, searchString)
            data.addAll(clientPrices.map { clientPriceToRow(it) })
        }

        withContext(Dispatchers.Main) {
            gridAdapter.setRowList(data)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
