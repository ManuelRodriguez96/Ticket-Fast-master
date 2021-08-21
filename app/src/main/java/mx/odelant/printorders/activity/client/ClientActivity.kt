package mx.odelant.printorders.activity.client

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.client__activity.*
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
import mx.odelant.printorders.entities.Client

class ClientActivity : AppCompatActivity() {

    private val rClientActivity = R.layout.client__activity
    private val mClientsListViewAdapter = Grid3CellAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(rClientActivity)

        setToolbar()
        bindAdapters()
        setDataSources()
        setListeners()
    }

    override fun onResume() {
        super.onResume()
        // this handles client deletion
        setDataSources()
    }

    private fun setToolbar() {
        val rToolbar = client_toolbar
        setSupportActionBar(rToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun bindAdapters() {
        val rClientsListView = client_lv_clients
        rClientsListView.adapter = mClientsListViewAdapter
    }

    private fun setDataSources() {
        GlobalScope.launch {
            updateClientsList(mClientsListViewAdapter)
        }
    }

    private fun setListeners() {

        val rFilterClientsEditText = client_et_filter_clients
        rFilterClientsEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                GlobalScope.launch {
                    updateClientsList(mClientsListViewAdapter)
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        val rAddClientButton = client_btn_add_client
        rAddClientButton.setOnClickListener {
            val db = AppDatabase.getInstance(applicationContext)
            ClientDetailDialog.makeCreateClientDialog(this, db) {
                GlobalScope.launch {
                    updateClientsList(mClientsListViewAdapter)
                }
            }
        }
    }

    private suspend fun updateClientsList(gridAdapter: Grid3CellAdapter) {
        val db = AppDatabase.getInstance(applicationContext)
        val data: ArrayList<Grid3CellRow> = ArrayList()

        val clientToRow = { client: Client ->
            val row = Grid3CellContent(
                "",
                client.name,
                "",
                View.OnClickListener {
                    val clientDetailIntent = Intent(this, ClientDetailActivity::class.java)
                    clientDetailIntent.putExtra(
                        ClientDetailActivity.INTENT_CLIENT_ID_KEY,
                        client.id
                    )
                    startActivity(clientDetailIntent)
                }, null, null
            )
            row.hideField1 = true
            row.hideField3 = true
            row
        }

        val header = Grid3CellHeader("", "Cliente", "", null)
        header.hideField1 = true
        header.hideField3 = true
        data.add(header)

        val rFilterClientsEditText = client_et_filter_clients
        val searchString = rFilterClientsEditText.text.toString()

        withContext(Dispatchers.IO) {
            val clientsList = ClientDL.getAllClientsLikeName(db, searchString)
            data.addAll(clientsList.map(clientToRow))
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
