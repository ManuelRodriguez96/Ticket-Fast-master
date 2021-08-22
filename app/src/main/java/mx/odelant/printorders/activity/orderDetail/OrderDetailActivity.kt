package mx.odelant.printorders.activity.orderDetail

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.os.Message
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.StyleSpan
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.leerybit.escpos.*
import com.leerybit.escpos.bluetooth.BTService
import kotlinx.android.synthetic.main.order_detail__activity.*
import kotlinx.coroutines.*
import mx.odelant.printorders.R
import mx.odelant.printorders.dataLayer.*
import mx.odelant.printorders.entities.Cart
import mx.odelant.printorders.utils.Formatter
import java.io.BufferedReader
import java.io.IOException
import java.sql.Date
import java.text.DateFormat
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*

class OrderDetailActivity : AppCompatActivity() {
    private val mCartId by lazy {
        intent.getIntExtra(INTENT_CART_ID_KEY, 0)
    }
    private val rOrderDetailActivity = R.layout.order_detail__activity
    private val printer by lazy { PosPrinter60mm(this) }
    private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()

    companion object {
        const val INTENT_CART_ID_KEY = "cartID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(rOrderDetailActivity)

        if (mCartId == 0) {
            finish()
        }

        setToolbar()
        setupPrinter()
        setOnClickListeners()
    }

    private fun setToolbar() {
        val rToolbar = order_detail_toolbar
        setSupportActionBar(rToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setOnClickListeners() {
        order_detail_btn_connect_printer.setOnClickListener {
            if (!printer.isConnected) {
                printer.connect()
            }
        }
        val db = AppDatabase.getInstance(this)

        GlobalScope.launch {
            val ticket = generateTicketAsync(db, mCartId, printer).await()
            runOnUiThread {
                order_detail_ticket_preview.setTicket(ticket)
                order_detail_btn_finalize.setOnClickListener {
                    try {
                        printer.send(ticket)
                    } catch (e: IOException) {
                        Snackbar.make(
                            order_detail_main_view,
                            "Hubo un problema imprimiendo.",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }


        }
    }

    private fun generateTicketAsync(
        db: AppDatabase,
        cartId: Int,
        printer: PosPrinter
    ): Deferred<Ticket> {
        return GlobalScope.async {
            val cartItemsAsync =
                GlobalScope.async(Dispatchers.IO) {
                    CartItemDL.getCartItemAndProductByCartId(
                        db,
                        cartId
                    )
                }
            val cartReturnItemsAsync =
                GlobalScope.async(Dispatchers.IO) {
                    CartReturnItemDL.getCartReturnItemAndProductByCartId(
                        db,
                        cartId
                    )
                }
            val cartAsync =
                GlobalScope.async(Dispatchers.IO) { CartDL.getById(db, cartId) }
            val clientAsync =
                GlobalScope.async(Dispatchers.IO) {
                    cartAsync.await()?.clientId?.let {
                        ClientDL.getById(db, it)
                    }
                }

            val dateformat = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT,
                Locale.getDefault()
            )

            val ticketBuilder: TicketBuilder = TicketBuilder(printer)
                .isCyrillic(true)
                .feedLine()

            val sharedPref = getSharedPreferences("SHARED_PREFERENCES", Context.MODE_PRIVATE)
            val username = sharedPref.getString(getString(R.string.username), "")

            if (!username.isNullOrBlank()) {
                ticketBuilder.header(username)
            }

            var date = Calendar.getInstance()
            var day = date.get(Calendar.DAY_OF_MONTH)
            var mont = date.get(Calendar.MONTH)
            var year = date.get(Calendar.YEAR)
            var client: CharSequence? = if (TextUtils.isEmpty(clientAsync.await()?.name)) {
                ""
            } else {
                clientAsync.await()?.name
            }
            var email = sharedPref.getString("EMAIL", "")
            var prefijClient = "00"
            if (!TextUtils.isEmpty(client))
                prefijClient = client.toString().get(0) + "" + client.toString().get(1)

            val folio = db.cartDao().getFolio(cartId)

            ticketBuilder
                .right("No folio : #" + prefijClient.toUpperCase() + "-" + day + (mont + 1) + year + "- 0" + folio)
                .dividerDouble()
                .header("Nota de expedicion")
                .dividerDouble()
                .text(
                    "Cliente: " + REGEX_UNACCENT.replace(
                        Normalizer.normalize(
                            client,
                            Normalizer.Form.NFD
                        ), ""
                    )
                )
                .text("Expedida: ${dateformat.format(cartAsync.await()!!.dateCreated)}")
                .dividerDouble()
                .feedLine()

            val cartItems = cartItemsAsync.await()
            if (cartItems.isNotEmpty()) {
                ticketBuilder.subHeader("Orden")
                cartItems.map {
                    val productNormalize =
                        Normalizer.normalize(it.product.name, Normalizer.Form.NFD)
                    val productName = REGEX_UNACCENT.replace(productNormalize, "")
                    val productNameAndUnitPriceString =
                        "${
                            Formatter.intInHundredthsToString(it.cartItem.quantityInHundredths)
                                .padStart(
                                    5,
                                    ' '
                                )
                        } ${productName} ($${Formatter.intInHundredthsToString(it.cartItem.unitPriceInCents)})"
                    val productTotalString =
                        "$${Formatter.intInHundredthsToString((it.cartItem.unitPriceInCents * it.cartItem.quantityInHundredths + 50) / 100)}"

                    if (productNameAndUnitPriceString.length + productTotalString.length > 31) {
                        ticketBuilder
                            .menuLine(
                                productNameAndUnitPriceString.substring(
                                    0,
                                    32 - productTotalString.length - 2
                                ), productTotalString
                            )
                            .text("    ${productNameAndUnitPriceString.substring(32 - productTotalString.length - 2)}")
                    } else {
                        ticketBuilder
                            .menuLine(
                                productNameAndUnitPriceString, productTotalString
                            )
                    }
                }
            }
            val cartReturnItems = cartReturnItemsAsync.await()
            if (cartReturnItems.isNotEmpty()) {
                ticketBuilder
                    .feedLine()
                    .subHeader("Devoluciones")
                cartReturnItems.map {
                    ticketBuilder.text(
                        "${
                            Formatter.intInHundredthsToString(it.cartReturnItem.quantityInHundredths)
                                .padStart(
                                    5,
                                    ' '
                                )
                        } ${it.product.name}"
                    )
                }
            }

            val total = (cartAsync.await()?.totalPriceInCents ?: 0).toString()
            ticketBuilder.feedLine()
                .divider()
                .feedLine()
                .menuLine(
                    "Total:",
                    "$${
                        SpannableString(total)/*.setSpan(
                            StyleSpan(Typeface.BOLD),
                            0,
                            total.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )*/
                    }"
                )
                .divider()
                .feedLine(2)
                .build()
        }
    }

    /*Formatter.intInHundredthsToString(
                            cartAsync.await()?.totalPriceInCents ?: 0
                        )
    * */
    private fun setupPrinter() {
        val onStateChange: (state: Int) -> Unit = { state ->
            when (state) {
                BTService.STATE_NONE -> setState(getString(R.string.no_hay_conexion), R.color.text)
                BTService.STATE_CONNECTED -> setState(getString(R.string.conectado), R.color.green)
                BTService.STATE_CONNECTING -> setState(getString(R.string.conectado), R.color.blue)
                BTService.STATE_LISTENING -> setState(
                    getString(R.string.buscando_impresora),
                    R.color.amber
                )
            }
        }

        printer.setCharsetName("UTF-8")
        printer.setDeviceCallbacks(object : DeviceCallbacks {
            override fun onConnected() {
                onStateChange(BTService.STATE_CONNECTED)
            }

            override fun onFailure() {
                Snackbar.make(
                    order_detail_main_view,
                    "Hubo un problema conectandose a la impresora",
                    Snackbar.LENGTH_LONG
                ).show()
            }

            override fun onDisconnected() {
                onStateChange(BTService.STATE_NONE)
            }
        })

        printer.setStateChangedListener { state: Int, _: Message -> onStateChange(state) }
    }

    @SuppressLint("SetTextI18n")
    private fun setState(value: String, color: Int) {
        order_detail_tv_connection_status.text = value
        order_detail_tv_connection_status.setTextColor(ContextCompat.getColor(this, color))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
