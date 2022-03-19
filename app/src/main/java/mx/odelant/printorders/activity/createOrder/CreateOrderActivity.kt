package mx.odelant.printorders.activity.createOrder

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
import mx.odelant.printorders.databinding.CreateOrderActivityBinding
import mx.odelant.printorders.entities.Cart
import mx.odelant.printorders.entities.Client
import mx.odelant.printorders.utils.Formatter

class CreateOrderActivity : AppCompatActivity() {
    private lateinit var binding : CreateOrderActivityBinding
    private val rCreateOrderActivity = R.layout.create_order__activity
    private val mOrderItemsListViewAdapter = Grid3CellAdapter()
    private var mSelectedClient: Client? = null
    private lateinit var mPendingCart: Cart

    companion object {
        var CLIENTE_PASO = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CreateOrderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getInstance(this)

        setToolbar()
        bindAdapters()
        setDataSources()
        setListeners()
    }

    private fun setToolbar() {
        val rToolbar = binding.createOrderToolbar
        setSupportActionBar(rToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindAdapters() {
        val rClientsListView = binding.createOrderLvProducts
        rClientsListView.adapter = mOrderItemsListViewAdapter

        binding.createOrderBtnFinalize.visibility = View.GONE
    }

    private fun setDataSources() {
        updateOrderItemsList()
    }

    private fun setListeners() {

        val rCreateClientButton = binding.createOrderBtnCreateClient
        rCreateClientButton.setOnClickListener {
            val db = AppDatabase.getInstance(applicationContext)
            ClientDetailDialog.makeCreateClientDialog(this, db) { selectedClient ->
                updateSelectedClient(selectedClient)
            }
        }

        val rSelectClientButton = binding.createOrderBtnSelectClient
        rSelectClientButton.setOnClickListener {
            val db = AppDatabase.getInstance(applicationContext)
            CreateOrderDialog.makeSelectClientDialog(this, db) { selectedClient ->
                updateSelectedClient(selectedClient)
            }
        }

        val rAddProductButton = binding.createOrderBtnAddProduct
        rAddProductButton.setOnClickListener {
            val db = AppDatabase.getInstance(applicationContext)
            val pendingCart = mPendingCart
            CreateOrderDialog.makeAddProductToCartDialog(this, db, mSelectedClient, pendingCart) {
                updateOrderItemsList()
            }
        }

        val rFinalizeOrderButton = binding.createOrderBtnFinalize
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
            binding.createOrderTvSelectClient.text = selectedClient?.name ?: "Ninguno"
            if (selectedClient?.id == 0){
                CLIENTE_PASO = selectedClient?.name

            }else{
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
                binding.createOrderTvTotal.text =
                    "$${Formatter.intInHundredthsToString((totalOrderCostInTenThousandths + 50) / 100)}"
                binding.createOrderBtnFinalize.visibility = if (isOrderEmpty) View.GONE else View.VISIBLE
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
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
