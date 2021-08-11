package mx.odelant.printorders.activity.createOrder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.odelant.printorders.R
import mx.odelant.printorders.dataLayer.*
import mx.odelant.printorders.entities.*
import mx.odelant.printorders.utils.Formatter

class CreateOrderDialog {
    companion object {
        fun makeSelectClientDialog(
            context: Context,
            db: AppDatabase,
            onSelect: (Client?) -> Unit
        ): AlertDialog {
            val builder = AlertDialog.Builder(context)
            val dialogLayout =
                LayoutInflater.from(context)
                    .inflate(R.layout.create_order__dialog__select_client, null, false)
            val selectClientACT =
                dialogLayout.findViewById<AutoCompleteTextView>(R.id.select_client_act_select_client)

            builder.setTitle("Selecciona cliente")
            builder.setView(dialogLayout)

            builder.setPositiveButton("OK", null)
            builder.setNegativeButton("sin cliente", null)
            builder.setNeutralButton("cancelar", null)

            builder.setCancelable(true)
            val dialog = builder.show()

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val cancelButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            val clearClientButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            clearClientButton.setOnClickListener {
                onSelect(null)
                dialog.dismiss()
            }

            val adapter = ArrayAdapter<Client>(context, android.R.layout.simple_dropdown_item_1line)
            selectClientACT.setAdapter(adapter)
            selectClientACT.threshold = 1

            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    val clients = ClientDL.getAll(db)
                    adapter.clear()
                    adapter.addAll(clients)
                }
            }

            selectClientACT.setOnItemClickListener { _, _, pos, _ ->
                val client = adapter.getItem(pos)
                if (client != null) {
                    okButton.isEnabled = true
                    okButton.setOnClickListener {
                        onSelect(client)
                        dialog.dismiss()
                    }
                }
            }

            return dialog
        }

        fun makeAddProductToCartDialog(
            context: Context,
            db: AppDatabase,
            selectedClient: Client?,
            cart: Cart,
            onCreate: () -> Unit
        ): AlertDialog {
            val builder = AlertDialog.Builder(context)

            val dialogLayout =
                LayoutInflater.from(context)
                    .inflate(R.layout.create_order__dialog__add_product, null, false)
            val productACT =
                dialogLayout.findViewById<AutoCompleteTextView>(R.id.create_order_act_select_product)
            val productQuantityEditText =
                dialogLayout.findViewById<EditText>(R.id.create_order_et_quantity)
            val productPriceEditText =
                dialogLayout.findViewById<EditText>(R.id.create_order_et_price)

            val sharedPref = context.getSharedPreferences("SHARED_PREFERENCES", Context.MODE_PRIVATE)
            if (!sharedPref.getBoolean("isSystemUser", true))
                productPriceEditText.isEnabled = false
            val returnItemCheckbox =
                dialogLayout.findViewById<CheckBox>(R.id.create_order_checkbox_is_return)
            val productPriceLinearLayout =
                dialogLayout.findViewById<LinearLayout>(R.id.create_order_ll_price_container)

            val currentStockContainer =
                dialogLayout.findViewById<LinearLayout>(R.id.create_order_ll_stock_container)
            currentStockContainer.visibility = View.GONE

            val currentStockTextView =
                dialogLayout.findViewById<TextView>(R.id.create_order_tv_stock)

            builder.setTitle("Agregar producto a carrito")

            productQuantityEditText.filters = arrayOf(Formatter.DecimalDigitsInputFilter(7, 2))
            productPriceEditText.filters = arrayOf(Formatter.DecimalDigitsInputFilter(7, 2))

            builder.setView(dialogLayout)
            builder.setPositiveButton("OK", null)
            builder.setNegativeButton("cancelar", null)
            builder.setCancelable(true)

            val dialog = builder.show()

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.isEnabled = false

            returnItemCheckbox.setOnCheckedChangeListener { _, isChecked ->
                productPriceLinearLayout.visibility = if (isChecked) View.GONE else View.VISIBLE
            }

            fun okButtonOnClickListener(product: Product): View.OnClickListener {
                return View.OnClickListener {

                    val productQuantityInHundredths =
                        Formatter.stringToIntInHundredths(productQuantityEditText.text.toString())

                    GlobalScope.launch {
                        if (returnItemCheckbox.isChecked) {
                            val cartReturnItem = CartReturnItem(
                                0,
                                cart.cart_id,
                                product.id,
                                productQuantityInHundredths
                            )
                            CartReturnItemDL.addCartReturnItem(db, cartReturnItem, product, cart)
                        } else {
                            val productPriceInCents =
                                Formatter.stringToIntInHundredths(productPriceEditText.text.toString())
                            val cartItem = CartItem(
                                0,
                                cart.cart_id,
                                product.id,
                                productQuantityInHundredths,
                                productPriceInCents
                            )
                            CartItemDL.addCartItem(db, cartItem, product, cart)
                        }
                        onCreate()
                        dialog.dismiss()
                    }
                }
            }

            val adapter =
                ArrayAdapter<Product>(context, android.R.layout.simple_dropdown_item_1line)
            productACT.setAdapter(adapter)
            productACT.threshold = 1

            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    val products = ProductDL.getAll(db)
                    adapter.clear()
                    adapter.addAll(products)
                }
            }

            productACT.setOnItemClickListener { _, _, pos, _ ->
                val product = adapter.getItem(pos)
                if (product != null) {
                    productPriceEditText.setText(Formatter.intInHundredthsToString(product.basePriceInCents))

                    currentStockContainer.visibility = View.VISIBLE
                    val currentStockInHundredthsString =
                        Formatter.intInHundredthsToString(product.stockInHundredths)
                    currentStockTextView.text = currentStockInHundredthsString

                    if (selectedClient != null) {
                        GlobalScope.launch {
                            withContext(Dispatchers.IO) {
                                val clientPrice =
                                    ClientPriceDL.getByClientAndProduct(db, selectedClient, product)
                                if (clientPrice != null) {
                                    productPriceEditText.setText(
                                        Formatter.intInHundredthsToString(
                                            clientPrice.priceInCents
                                        )
                                    )
                                }
                            }
                        }
                    }
                    okButton.isEnabled = true
                    okButton.setOnClickListener(okButtonOnClickListener(product))
                } else {
                    okButton.isEnabled = false
                    okButton.setOnClickListener(null)
                }
            }

            return dialog
        }

        fun makeEditCartItemDialog(
            context: Context,
            db: AppDatabase,
            cartItem: CartItem?,
            cartReturnItem: CartReturnItem?,
            product: Product,
            onUpdate: () -> Unit
        ): AlertDialog {
            val builder = AlertDialog.Builder(context)
            val dialogLayout =
                LayoutInflater.from(context)
                    .inflate(R.layout.create_order__dialog__edit_cart_item, null, false)
            val productTextView =
                dialogLayout.findViewById<TextView>(R.id.edit_cart_item_tv_product_name)
            val productQuantityEditText =
                dialogLayout.findViewById<EditText>(R.id.edit_cart_item_et_quantity)
            val productPriceEditText =
                dialogLayout.findViewById<EditText>(R.id.edit_cart_item_et_price)
            val productPriceContainerLinearLayout =
                dialogLayout.findViewById<LinearLayout>(R.id.edit_cart_item_ll_price_container)

            val currentStockTextView =
                dialogLayout.findViewById<TextView>(R.id.edit_cart_item_tv_stock)

            builder.setTitle("Editar producto en carrito")

            val currentStockInHundredthsString =
                Formatter.intInHundredthsToString(product.stockInHundredths)
            currentStockTextView.text = currentStockInHundredthsString

            productQuantityEditText.filters = arrayOf(Formatter.DecimalDigitsInputFilter(7, 2))
            productPriceEditText.filters = arrayOf(Formatter.DecimalDigitsInputFilter(7, 2))

            builder.setView(dialogLayout)
            builder.setPositiveButton("OK", null)
            builder.setNegativeButton("cancelar", null)
            builder.setNeutralButton("eliminar", null)
            builder.setCancelable(true)

            val dialog = builder.show()

            var title = product.name

            if (cartReturnItem != null) {
                productPriceContainerLinearLayout.visibility = View.GONE
                title = "Devolución: ${product.name}"
            }

            productTextView.text = title

            productQuantityEditText.setText(
                Formatter.intInHundredthsToString(
                    cartItem?.quantityInHundredths ?: cartReturnItem?.quantityInHundredths ?: 0
                )
            )
            productPriceEditText.setText(
                Formatter.intInHundredthsToString(
                    cartItem?.unitPriceInCents ?: 0
                )
            )

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val deleteButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)

            fun okButtonOnClickListener(): View.OnClickListener {
                return View.OnClickListener {
                    val productQuantityInHundredths =
                        Formatter.stringToIntInHundredths(productQuantityEditText.text.toString())
                    GlobalScope.launch {
                        if (cartReturnItem != null) {
                            val updatedCartReturnItem = cartReturnItem.copy(
                                quantityInHundredths = productQuantityInHundredths
                            )
                            CartReturnItemDL.update(db, updatedCartReturnItem)
                        } else if (cartItem != null) {
                            val productPriceInCents =
                                Formatter.stringToIntInHundredths(productPriceEditText.text.toString())
                            val updatedCartItem = cartItem.copy(
                                unitPriceInCents = productPriceInCents,
                                quantityInHundredths = productQuantityInHundredths
                            )
                            CartItemDL.update(db, updatedCartItem)
                        }
                        onUpdate()
                        dialog.dismiss()
                    }
                }
            }

            fun deleteButtonOnClickListener(): View.OnClickListener {
                return View.OnClickListener {
                    GlobalScope.launch {
                        if (cartReturnItem != null) {
                            CartReturnItemDL.delete(db, cartReturnItem)
                        } else if (cartItem != null) {
                            CartItemDL.delete(db, cartItem)
                        }
                        onUpdate()
                        dialog.dismiss()
                    }
                }
            }

            okButton.setOnClickListener(okButtonOnClickListener())
            deleteButton.setOnClickListener(deleteButtonOnClickListener())

            return dialog
        }

        fun makeConfirmFinalizeOrderDialog(
            context: Context,
            onConfirm: () -> Unit
        ) {
            AlertDialog.Builder(context).setMessage("Deseas finalizar la orden?")
                .setCancelable(true)
                .setPositiveButton("Finalizar orden") { _, _ ->
                    onConfirm()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        fun makeConfirmClearCartDialog(
            context: Context,
            onConfirm: () -> Unit
        ) {
            AlertDialog.Builder(context).setMessage("Deseas limpiar el carrito?")
                .setCancelable(true)
                .setPositiveButton("Limpiar carrito") { _, _ ->
                    onConfirm()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}