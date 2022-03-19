package mx.odelant.printorders.activity.inventory

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
import mx.odelant.printorders.dataLayer.ProductDL
import mx.odelant.printorders.databinding.InventoryActivityBinding
import mx.odelant.printorders.entities.Product
import mx.odelant.printorders.utils.Formatter

class InventoryActivity : AppCompatActivity() {
    private lateinit var binding : InventoryActivityBinding
    private val rInventoryActivity = R.layout.inventory__activity
    private val mProductsListViewAdapter = Grid3CellAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = InventoryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setToolbar()
        bindAdapters()
        setDataSources()
        setListeners()
    }

    private fun setToolbar() {
        val rToolbar = binding.inventoryToolbar
        setSupportActionBar(rToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindAdapters() {
        val rProductsListView = binding.inventoryLvProducts
        rProductsListView.adapter = mProductsListViewAdapter
    }

    private fun setDataSources() {
        GlobalScope.launch {
            updateProductsList(mProductsListViewAdapter)
        }
    }

    private fun setListeners() {

        val rFilterProductsEditText = binding.inventoryEtFilterProducts
        rFilterProductsEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                GlobalScope.launch {
                    updateProductsList(mProductsListViewAdapter)
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        val rAddProductButton = binding.inventoryBtnAddProduct
        rAddProductButton.setOnClickListener {
            val db = AppDatabase.getInstance(applicationContext)
            ProductDetailDialog.makeCreateProductDialog(this, db) {
                GlobalScope.launch {
                    updateProductsList(mProductsListViewAdapter)
                }
            }
        }
    }

    private suspend fun updateProductsList(gridAdapter: Grid3CellAdapter) {
        val db = AppDatabase.getInstance(applicationContext)
        val data: ArrayList<Grid3CellRow> = ArrayList()

        val productToRow = { product: Product ->
            val row = Grid3CellContent(
                "",
                product.name,
                Formatter.intInHundredthsToString(product.stockInHundredths),
                View.OnClickListener {
                    ProductDetailDialog.makeEditProductDialog(
                        this,
                        db,
                        product
                    ) { GlobalScope.launch { updateProductsList(gridAdapter) } }
                }, null, null
            )
            row.hideField1 = true
            row
        }

        val header = Grid3CellHeader("", "Producto", "#"){

        }
        header.hideField1 = true
        data.add(header)

        val rFilterProductsEditText = binding.inventoryEtFilterProducts
        val searchString = rFilterProductsEditText.text.toString()

        withContext(Dispatchers.IO) {
            val productsInInventory = ProductDL.getAllProductsLikeName(db, searchString)
            data.addAll(productsInInventory.map(productToRow))
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
