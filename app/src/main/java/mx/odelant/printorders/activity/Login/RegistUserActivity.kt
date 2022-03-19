package mx.odelant.printorders.activity.Login

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import mx.odelant.printorders.R
import mx.odelant.printorders.activity.main.MainActivity
import mx.odelant.printorders.databinding.InventoryActivityBinding
import mx.odelant.printorders.databinding.RegistUserActivityBinding


class RegistUserActivity : AppCompatActivity( ) {
    private lateinit var binding: RegistUserActivityBinding
    private val rRegistActivity = R.layout.regist_user_activity
    //private val KEY = "1Hbfh667adfDEJ78";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegistUserActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("SHARED_PREFERENCES", Context.MODE_PRIVATE)

        val btnDispatcher : Button = findViewById(R.id.btnLoginDispatcher)
        val btnLogin : Button = findViewById(R.id.btnLogin)
        val registEt : EditText  = findViewById(R.id.et_registPassword)
        val btnRegist : Button = findViewById(R.id.btnRegistPassUser)
        val textPass : TextView =  findViewById(R.id.tv_na_registPassword)

        val connectUser = sharedPref.getBoolean("connect", false)

        if(connectUser)
            continueToMainActivity()

        val passwordGlobal : String = sharedPref.getString("password", "").toString()

        if (passwordGlobal.isNotEmpty()) {
            btnRegist.visibility = View.GONE

            btnDispatcher.visibility = View.VISIBLE
            btnLogin.visibility = View.VISIBLE

            textPass.setText(R.string.placeholderPassword)
        } else {
            btnRegist.visibility = View.VISIBLE

            btnDispatcher.visibility = View.GONE
            btnLogin.visibility = View.GONE

            textPass.setText(R.string.placeholderResgist)
        }


        /*
        builder?.setMessage(R.string.dialog_message)
            .setTitle(R.string.dialog_title)
        */
        btnRegist.setOnClickListener {

            val alertDialog: AlertDialog? = this?.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setPositiveButton(R.string.positiveButton,
                        DialogInterface.OnClickListener { dialog, id ->
                            //val encrypPass = encrypt(registEt.text.toString())
                            //sharedPref.edit().putString("password", encrypPass).commit()
                            savePassword(registEt.text.toString())
                            sharedPref.edit().putBoolean("isSystemUser", true).commit()
                            sharedPref.edit().putBoolean("connect",true).commit()

                            continueToMainActivity()

                })
                    setNegativeButton(R.string.cancelButton,
                        DialogInterface.OnClickListener { dialog, id ->
                            dialog.cancel()
                        })
                }
                builder.setTitle(R.string.app_name)
                builder.setMessage(R.string.messageDialogRegist)
                builder.create()
                builder.show()
            }


        }

        btnLogin.setOnClickListener {
            var pass=  registEt.text.toString()
            if(pass == "" || pass == null) {
                Toast.makeText(this, "Escriba alguna contraseña", Toast.LENGTH_LONG).show()
            } else if(pass == getPasswordFromPref()/*decrypt(passwordGlobal)*/) {
                sharedPref.edit().putBoolean("isSystemUser", true).apply()
                sharedPref.edit().putBoolean("connect",true).commit()
                continueToMainActivity()
            } else {
                Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_LONG).show()
            }
        }


        btnDispatcher.setOnClickListener {
            sharedPref.edit().putBoolean("isSystemUser", false).apply()
            sharedPref.edit().putBoolean("connect",true).commit()
            continueToMainActivity()
        }


    }

    private fun continueToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        this.finish()
    }

    private fun generateKey()  : MasterKey{
        return MasterKey.Builder(this, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    private fun savePassword(value : String) {
        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            this,
            "secret_shared_prefs",
            generateKey(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val editor = sharedPreferences.edit()
        editor.putString("password",value).commit()

    }

    private fun getPasswordFromPref() : String {
        val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
            this,
            "secret_shared_prefs",
            generateKey(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        return sharedPreferences.getString("password","unknown") ?: "unknown"
    }
    /*fun encrypt(value : String) : String
    {
        val key = generateKey()
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedByteValue = cipher.doFinal(value.toByteArray(Charset.defaultCharset()))
        val encryptedValue64 = android.util.Base64.encodeToString(encryptedByteValue, android.util.Base64.DEFAULT)
        return encryptedValue64

    }

     fun  decrypt(value : String): String
    {
        val key = generateKey()
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, key);
        val decryptedValue64 = android.util.Base64.decode(value,android.util.Base64.DEFAULT)
        val decryptedByteValue = cipher.doFinal(decryptedValue64)
        val decryptedValue =  String(decryptedByteValue, Charset.defaultCharset())

        return decryptedValue

    }

    private fun generateKey() : Key {
        val key =  SecretKeySpec(KEY.toByteArray(),"AES")
        return key
    }*/
}