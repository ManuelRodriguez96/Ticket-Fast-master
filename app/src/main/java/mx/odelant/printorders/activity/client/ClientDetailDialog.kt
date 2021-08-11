package mx.odelant.printorders.activity.client

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.odelant.printorders.R
import mx.odelant.printorders.activity.utils.dialog.DialogHelpers
import mx.odelant.printorders.dataLayer.AppDatabase
import mx.odelant.printorders.dataLayer.ClientDL
import mx.odelant.printorders.dataLayer.ClientPriceDL
import mx.odelant.printorders.dataLayer.ProductDL
import mx.odelant.printorders.entities.Client
import mx.odelant.printorders.entities.ClientPrice
import mx.odelant.printorders.entities.Product
import mx.odelant.printorders.utils.Formatter

class ClientDetailDialog {

    companion object {
        @SuppressLint("InflateParams")
        fun makeEditClientDialog(
            context: Context,
            db: AppDatabase,
            client: Client,
            onSuccess: () -> Unit
        ): AlertDialog {
            val builder = AlertDialog.Builder(context)
            val dialogLayout =
                LayoutInflater.from(context).inflate(R.layout.client__dialog__client, null, false)
            val editTextName = dialogLayout.findViewById<EditText>(R.id.client_dialog_edittext_name)

            builder.setTitle("Editar cliente")

            editTextName.setText(client.name)

            builder.setView(dialogLayout)
            builder.setPositiveButton("OK", null)
            builder.setNegativeButton("cancelar", null)
            builder.setCancelable(true)
            builder.setNeutralButton("Eliminar", null)

            val dialog = builder.show()

            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)

            neutralButton.setOnClickListener {
                AlertDialog.Builder(context).setMessage("Seguro?")
                    .setCancelable(true)
                    .setPositiveButton("Eliminar") { _, _ ->
                        GlobalScope.launch {
                            ClientDL.deleteClientById(db, client)
                            onSuccess()
                            dialog.dismiss()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                val updateClient = Client(
                    id = client.id,
                    name = editTextName.text.toString(),
                    isDeleted = false
                )

                GlobalScope.launch {
                    val error = ClientDL.tryUpdateClient(db, updateClient)
                    if (error == ClientDL.ClientDLError.NONE) {
                        onSuccess()
                        dialog.dismiss()
                    } else {
                        Snackbar.make(
                            it,
                            "Un cliente con ese nombre ya existe. Seleccionalo para editarlo.",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }

            return dialog
        }

        @SuppressLint("InflateParams")
        fun makeCreateClientDialog(
            context: Context,
            db: AppDatabase,
            onSuccess: () -> Unit
        ): AlertDialog {
            val builder = AlertDialog.Builder(context)
            val dialogLayout =
                LayoutInflater.from(context).inflate(R.layout.client__dialog__client, null, false)
            val editTextName = dialogLayout.findViewById<EditText>(R.id.client_dialog_edittext_name)

            builder.setTitle("Crear cliente")
            builder.setView(dialogLayout)

            builder.setPositiveButton("OK", null)
            builder.setNegativeButton("cancelar", null)
            builder.setCancelable(true)
            val dialog = builder.show()

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            okButton.setOnClickListener {

                val client = Client(
                    0,
                    editTextName.text.toString(),
                    false
                )

                GlobalScope.launch {
                    val error = ClientDL.tryInsertClient(db, client)
                    if (error == ClientDL.ClientDLError.NONE) {
                        onSuccess()
                        dialog.dismiss()
                    } else {
                        Snackbar.make(
                            it,
                            "Un cliente con ese nombre ya existe. Seleccionalo para editarlo.",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }

            return dialog
        }

        fun makeEditClientPriceDialog(
            context: Context,
            db: AppDatabase,
            clientPrice: ClientPrice,
            product: Product,
            onSuccess: () -> Unit
        ): AlertDialog {

            val builder = AlertDialog.Builder(context)
            val dialogLayout =
                LayoutInflater.from(context)
                    .inflate(R.layout.client__dialog__edit_client_price, null, false)
            val productNameTextView =
                dialogLayout.findViewById<TextView>(R.id.edit_client_price_tv_product_name)
            val basePriceTextView =
                dialogLayout.findViewById<TextView>(R.id.edit_client_price_tv_base_price)
            val previousCustomPriceTextView =
                dialogLayout.findViewById<TextView>(R.id.edit_client_price_tv_previous_custom_price)
            val customPriceEditText =
                dialogLayout.findViewById<EditText>(R.id.edit_client_price_et_custom_price)

            builder.setTitle("Editar precio para cliente")

            productNameTextView.text = product.name
            basePriceTextView.text = Formatter.intInHundredthsToString(product.basePriceInCents)
            previousCustomPriceTextView.text =
                Formatter.intInHundredthsToString(clientPrice.priceInCents)
            customPriceEditText.setText(Formatter.intInHundredthsToString(clientPrice.priceInCents))

            builder.setView(dialogLayout)

            customPriceEditText.filters = arrayOf(Formatter.DecimalDigitsInputFilter(7, 2))

            builder.setPositiveButton("OK", null)
            builder.setNegativeButton("cancelar", null)
            builder.setCancelable(true)
            builder.setNeutralButton("Eliminar", null)

            val dialog = builder.show()

            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)

            neutralButton.setOnClickListener {
                val deleteConfirmationDialog = DialogHelpers.makeDeleteConfirmationAlert(context) {
                    GlobalScope.launch {
                        ClientPriceDL.deleteClientPrice(db, clientPrice)
                        onSuccess()
                        dialog.dismiss()
                    }
                }
                deleteConfirmationDialog.show()
            }

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {

                val updatedPriceInCents =
                    Formatter.stringToIntInHundredths(customPriceEditText.text.toString())

                if (updatedPriceInCents == clientPrice.priceInCents) {
                    dialog.dismiss()
                    return@setOnClickListener
                }

                val updateClientPrice =
                    clientPrice.copy(priceInCents = updatedPriceInCents)

                GlobalScope.launch {
                    val error = ClientPriceDL.tryUpdateClientPrice(db, updateClientPrice)
                    if (error == ClientPriceDL.ClientPriceDLError.NONE) {
                        onSuccess()
                        dialog.dismiss()
                    } else {
                        Snackbar.make(
                            it,
                            "Error actualizando precio.",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }

            return dialog
        }

        fun makeAddClientPriceDialog(
            context: Context,
            db: AppDatabase,
            client: Client,
            onSuccess: () -> Unit
        ): AlertDialog {
            val builder = AlertDialog.Builder(context)
            val dialogLayout =
                LayoutInflater.from(context)
                    .inflate(R.layout.client__dialog__create_client_price, null, false)
            val productNameACT =
                dialogLayout.findViewById<AutoCompleteTextView>(R.id.create_client_price_act_select_product)
            val basePriceTextView =
                dialogLayout.findViewById<TextView>(R.id.create_client_price_tv_base_price)
            val customPriceEditText =
                dialogLayout.findViewById<EditText>(R.id.create_client_price_et_custom_price)

            builder.setTitle("Agregar precio para ${client.name}")

            basePriceTextView.text = ""
            customPriceEditText.setText("")
            customPriceEditText.isEnabled = false
            customPriceEditText.filters = arrayOf(Formatter.DecimalDigitsInputFilter(7, 2))

            builder.setView(dialogLayout)
            builder.setPositiveButton("OK", null)
            builder.setNegativeButton("cancelar", null)
            builder.setCancelable(true)

            val dialog = builder.show()

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.isEnabled = false

            fun okButtonOnClickListener(product: Product): View.OnClickListener {
                return View.OnClickListener {
                    val updatedPriceInCents =
                        Formatter.stringToIntInHundredths(customPriceEditText.text.toString())

                    val clientPrice = ClientPrice(0, product.id, client.id, updatedPriceInCents)

                    GlobalScope.launch {
                        val error = ClientPriceDL.tryInsertClientPrice(db, clientPrice)
                        if (error == ClientPriceDL.ClientPriceDLError.NONE) {
                            onSuccess()
                            dialog.dismiss()
                        } else {
                            Snackbar.make(
                                it,
                                "Error creando precio.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }

            val adapter =
                ArrayAdapter<Product>(context, android.R.layout.simple_dropdown_item_1line)
            productNameACT.setAdapter(adapter)
            productNameACT.threshold = 1

            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    val products = ProductDL.getAll(db)
                    adapter.clear()
                    adapter.addAll(products)
                }
            }

            productNameACT.setOnItemClickListener { _, _, pos, _ ->
                val product = adapter.getItem(pos)
                if (product != null) {
                    basePriceTextView.text =
                        Formatter.intInHundredthsToString(product.basePriceInCents)
                    customPriceEditText.isEnabled = true
                    okButton.isEnabled = true
                    okButton.setOnClickListener(okButtonOnClickListener(product))
                } else {
                    basePriceTextView.text = ""
                    okButton.isEnabled = false
                    okButton.setOnClickListener(null)
                }
            }

            return dialog
        }
    }
}