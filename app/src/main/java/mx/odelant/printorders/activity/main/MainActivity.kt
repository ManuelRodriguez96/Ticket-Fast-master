package mx.odelant.printorders.activity.main

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.main__activity.*
import mx.odelant.printorders.activity.analytics.AnalyticsActivity
import mx.odelant.printorders.activity.client.ClientActivity
import mx.odelant.printorders.activity.createOrder.CreateOrderActivity
import mx.odelant.printorders.activity.inventory.InventoryActivity
import mx.odelant.printorders.activity.orderHistory.OrderHistoryActivity
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import mx.odelant.printorders.R
import mx.odelant.printorders.activity.Login.RegistUserActivity
import mx.odelant.printorders.dataLayer.AppDatabase
import java.nio.charset.Charset
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() {

    private val rMainActivity = R.layout.main__activity
    private lateinit var sharedPref:SharedPreferences

    private lateinit var imageUtility: MainImageUtility
    private val KEY = "1Hbfh667adfDEJ78";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(rMainActivity)

        sharedPref = getSharedPreferences("SHARED_PREFERENCES", Context.MODE_PRIVATE)
        imageUtility = MainImageUtility(this)

        val systemUser = sharedPref.getBoolean("isSystemUser", false)

        val rCreateOrderCardView = create_order_card_view
        val rAnalyticsCardView = analytics_card_view
        val rClientsAdminCardView = clients_admin_card_view
        val rInventoryCardView = inventory_admin_card_view
        val rOrderHistoryCardView = order_history_card_view
        val rEditUsernameButton = edit_username_btn
        val rProfilePictureImageView = profile_picture_iv
        val rChangePass = changePass_card_view
        val rLogOutBtn = logOut_card_view
        val rReStarBtn = btn_restar_db

        if(systemUser == false){
            rAnalyticsCardView.visibility  = View.GONE
            rInventoryCardView.visibility  = View.GONE
            rOrderHistoryCardView.visibility  = View.GONE
            rEditUsernameButton.visibility  = View.GONE
            rChangePass.visibility = View.GONE
        }

        updateUsername()
        setProfilePicture()

        rEditUsernameButton.setOnClickListener {
            MainDialog.makeEditUsernameDialog(
                this,
                sharedPref
            ) {
                updateUsername()
            }
        }

        imageUtility.setPickerListener(rProfilePictureImageView)

        rCreateOrderCardView.setOnClickListener { startActivityFromClass(CreateOrderActivity::class.java) }
        rAnalyticsCardView.setOnClickListener { startActivityFromClass(AnalyticsActivity::class.java) }
        rClientsAdminCardView.setOnClickListener { startActivityFromClass(ClientActivity::class.java) }
        rInventoryCardView.setOnClickListener { startActivityFromClass(InventoryActivity::class.java) }
        rOrderHistoryCardView.setOnClickListener { startActivityFromClass(OrderHistoryActivity::class.java) }

        rLogOutBtn.setOnClickListener { LogOutSessión() }

        rChangePass.setOnClickListener { ChangePassword() }

        val db = AppDatabase.getInstance(applicationContext)

        rReStarBtn.setOnClickListener {
            MainDialog.makeReStarDialog(
                this,
                db,
                sharedPref
            ) {
            }
        }

    }

    private fun setProfilePicture() {
        val imageFile = imageUtility.getProfilePictureFile()
        if(imageFile.exists()) {
            val myBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            profile_picture_iv.setImageBitmap(myBitmap)
        }
    }

    private fun updateUsername() {
        val username = sharedPref.getString(getString(R.string.username), "")
        if(!username.isNullOrBlank()) {
            edit_username_tv.text = username
            edit_username_tv.setTextColor(ResourcesCompat.getColor(resources, R.color.range_light_gray, null))
        } else {
            edit_username_tv.text = getString(R.string.sin_nombre)
            edit_username_tv.setTextColor(ResourcesCompat.getColor(resources, R.color.red, null))
        }
    }

    private fun <Activity : AppCompatActivity> startActivityFromClass(activityClass: Class<Activity>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        imageUtility.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        imageUtility.onActivityResult(requestCode, resultCode, data, profile_picture_iv)
    }

    private fun LogOutSessión () {
        sharedPref = getSharedPreferences("SHARED_PREFERENCES", Context.MODE_PRIVATE)
        sharedPref.edit().remove("isSystemUser").apply()
        sharedPref.edit().remove("connect").apply()
        val intent = Intent(this, RegistUserActivity::class.java )
        startActivity(intent)
        this.finish()
    }


    private fun ChangePassword():AlertDialog{
        val builder = AlertDialog.Builder(this)
        val dialogLayout = LayoutInflater.from(this).inflate(R.layout.main_dialog_changepass, null, false )
        val editTextPass = dialogLayout.findViewById<EditText>(R.id.PasswordTx)

        builder.setTitle("Editar contraseña")

        builder.setView(dialogLayout)
        builder.setPositiveButton("Aceptar",null)
        builder.setNegativeButton("Cancelar", null)
        builder.setCancelable(true)

        val dialog = builder.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val password = editTextPass.text.toString()
            val sharedPreferencesEditor = sharedPref.edit()
            if(!password.isBlank()) {
                sharedPreferencesEditor.putString("password",encrypt(password)).apply()
            } else {
                val alertDialog: AlertDialog? = this?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {
                        setNegativeButton(R.string.cancelButton,
                            DialogInterface.OnClickListener { dialog, id ->
                                dialog.cancel()
                            })
                    }
                    builder.setTitle(R.string.app_name)
                    builder.setMessage("Se necesita ingresar una contraseña")
                    builder.create()
                    builder.show()
                }
            }
            dialog.dismiss()
        }

        return dialog
    }

    private fun generateKey() : Key {
        val key =  SecretKeySpec(KEY.toByteArray(),"AES")
        return key
    }

    fun encrypt(value : String) : String
    {
        val key = generateKey()
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedByteValue = cipher.doFinal(value.toByteArray(Charset.defaultCharset()))
        val encryptedValue64 = android.util.Base64.encodeToString(encryptedByteValue, android.util.Base64.DEFAULT)
        return encryptedValue64

    }
}
