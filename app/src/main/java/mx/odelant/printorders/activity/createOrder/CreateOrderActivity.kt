package mx.odelant.printorders.activity.createOrder

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.create_order__activity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.odelant.printorders.R
import mx.odelant.printorders.activity.client.ClientDetailDialog
import mx.odelant.printorders.activity.orderDetail.OrderDetailActivity
import mx.odelant.printorders.activity.utils.adapter.*
import mx.odelant.printorders.dataLayer.AppDatabase
import mx.odelant.printorders.dataLayer.CartDL
import mx.odelant.printorders.dataLayer.CartItemDL
import mx.odelant.printorders.dataLayer.CartReturnItemDL
import mx.odelant.printorders.entities.Cart
import mx.odelant.printorders.entities.Client
import mx.odelant.printorders.utils.Formatter

class CreateOrderActivity : AppCompatActivity() {

    private val rCreateOrderActivity = R.layout.create_order__activity
    private val mOrderItemsListViewAdapter = Grid3CellAdapter()
    private var mSelectedClient: Client? = null
    private lateinit var mPendingCart: Cart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(rCreateOrderActivity)

        val db = AppDatabase.getInstance(this)

        setToolbar()
        bindAdapters()
        setDataSources()
        setListeners()
    }

    private fun setToolbar() {
        val rToolbar = create_order_toolbar
        setSupportActionBar(rToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindAdapters() {
        val rClientsListView = create_order_lv_products
        rClientsListView.adapter = mOrderItemsListViewAdapter

        create_order_btn_finalize.visibility = View.GONE
    }

    private fun setDataSources() {
        updateOrderItemsList()
    }

    private fun setListeners() {

        val rCreateClientButton = create_order_btn_create_client
        rCreateClientButton.setOnClickListener {
            val db = AppDatabase.getInstance(applicationContext)
            ClientDetailDialog.makeCreateClientDialog(this, db) { selectedClient ->
                updateSelectedClient(selectedClient)
            }
        }

        val rSelectClientButton = create_order_btn_select_client
        rSelectClientButton.setOnClickListener {
            val db = AppDatabase.getInstance(applicationContext)
            CreateOrderDialog.makeSelectClientDialog(this, db) { selectedClient ->
                updateSelectedClient(selectedClient)
            }
        }

        val rAddProductButton = create_order_btn_add_product
        rAddProductButton.setOnClickListener {
            val db = AppDatabase.getInstance(applicationContext)
            val pendingCart = mPendingCart
            CreateOrderDialog.makeAddProductToCartDialog(this, db, mSelectedClient, pendingCart) {
                updateOrderItemsList()
            }
        }

        val rFinalizeOrderButton = create_order_btn_finalize
        rFinalizeOrderButton.setOnClickListener {
            val db = AppDatabase.getInstance(applicationContext)
            val context = this
            CreateOrderDialog.makeConfirmFinalizeOrderDialog(this) {
                GlobalScope.launch {
                    val finalizedCart = CartDL.finalizePendingCart(db)

                    val orderDetailIntent = Intent(context, OrderDetailActivity::class.java)
                    orderDetailIntent.putExtra(
                        OrderDetailActivity.INTENT_CART_ID_KEY,
                        finalizedCart.cart_id
                    )
                    withContext(Dispatchers.Main) {
                        startActivity(orderDetailIntent)
                        finish()
                    }
                }
            }
        }
    }

    private fun updateSelectedClient(selectedClient: Client?) {
        runOnUiThread {
            mSelectedClient = selectedClient
            create_order_tv_select_client.text = selectedClient?.name ?: "Ninguno"
            val db = AppDatabase.getInstance(this)
            GlobalScope.launch {
                val updateCart = CartDL.getOrCreatePendingCart(db).copy(
                    clientId = selectedClient?.id
                )
                CartDL.update(db, updateCart)
                mPendingCart = updateCart
            }
        }
    }

    private fun clearAllCartItems() {
        val db = AppDatabase.getInstance(this)
        GlobalScope.launch {
            CartDL.clearPendingCart(db)
            updateOrderItemsList()
        }
    }

    private fun updateOrderItemsList() {
        val db = AppDatabase.getInstance(this)
        val context = this

        GlobalScope.launch {
            val data: ArrayList<Grid3CellRow> = ArrayList()
            var totalOrderCostInTenThousandths = 0
            var isOrderEmpty = true
            withContext(Dispatchers.IO) {
                    mPendingCart = CartDL.getOrCreatePendingCart(db)
                val pendingCart = mPendingCart

                data.add(Grid3CellHeader("#", "Producto", "$"){})

                val cartItemsAndProducts =
                    CartItemDL.getCartItemAndProductByCartId(db, pendingCart.cart_id)
                for (cartItemProduct in cartItemsAndProducts) {
                    val cartItem = cartItemProduct.cartItem
                    val product = cartItemProduct.product
                    val totalCostInTenThousandths =
                        cartItem.quantityInHundredths * cartItem.unitPriceInCents

                    totalOrderCostInTenThousandths += totalCostInTenThousandths

                    data.add(
                        Grid3CellContent(
                            Formatter.intInHundredthsToString(cartItem.quantityInHundredths),
                            product.name,
                            "$${Formatter.intInHundredthsToString((totalCostInTenThousandths + 50) / 100)}",
                            View.OnClickListener {
                                CreateOrderDialog.makeEditCartItemDialog(
                                    context,
                                    db,
                                    cartItem,
                                    null,
                                    product,
                                    ::updateOrderItemsList
                                )
                            }, null, null
                        )
                    )
                }
                val cartReturnItems =
                    CartReturnItemDL.getCartReturnItemAndProductByCartId(db, pendingCart.cart_id)
                if (cartReturnItems.isNotEmpty()) {
                    data.add(Grid3CellTitle("Devoluciones"))
                }
                data.addAll(
                    cartReturnItems.map { it ->
                        Grid3CellContent(
                            Formatter.intInHundredthsToString(it.cartReturnItem.quantityInHundredths),
                            it.product.name,
                            "",
                            View.OnClickListener { _ ->
                                CreateOrderDialog.makeEditCartItemDialog(
                                    context,
                                    db,
                                    null,
                                    it.cartReturnItem,
                                    it.product,
                                    ::updateOrderItemsList
                                )
                            }, null, null
                        )
                    }
                )

                val updatedCart = pendingCart.copy(
                    totalPriceInCents = ((totalOrderCostInTenThousandths + 50) / 100)
                )

                CartDL.update(db, updatedCart)
                mPendingCart = updatedCart

                isOrderEmpty = cartItemsAndProducts.isEmpty() && cartReturnItems.isEmpty()
            }

            withContext(Dispatchers.Main) {
                mOrderItemsListViewAdapter.setRowList(data)
                create_order_tv_total.text =
                    "$${Formatter.intInHundredthsToString((totalOrderCostInTenThousandths + 50) / 100)}"
                create_order_btn_finalize.visibility = if (isOrderEmpty) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_create_order, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.create_order_menu_btn_clear_cart) {
            CreateOrderDialog.makeConfirmClearCartDialog(this) {
                clearAllCartItems()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

    }
}
