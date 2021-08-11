package mx.odelant.printorders.activity.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mx.odelant.printorders.R
import mx.odelant.printorders.dataLayer.*
import java.nio.charset.Charset
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class MainDialog {

    companion object {
        @SuppressLint("InflateParams")
        fun makeEditUsernameDialog(
            context: Context,
            sharedPreferences: SharedPreferences,
            onSuccess: () -> Unit
        ): AlertDialog {
            val builder = AlertDialog.Builder(context)
            val dialogLayout =
                LayoutInflater.from(context).inflate(R.layout.main__dialog__username, null, false)
            val editTextName = dialogLayout.findViewById<EditText>(R.id.main_dialog_edittext_username)

            builder.setTitle("Editar empresa")

            val username = sharedPreferences.getString(context.getString(R.string.username), "")
            editTextName.setText(username)

            builder.setView(dialogLayout)
            builder.setPositiveButton("OK", null)
            builder.setNegativeButton("cancelar", null)
            builder.setCancelable(true)

            val dialog = builder.show()

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {

                val usernameFromEditText = editTextName.text.toString()

                val sharedPreferencesEditor = sharedPreferences.edit()

                if(!usernameFromEditText.isBlank()) {
                    sharedPreferencesEditor.putString(
                        context.getString(R.string.username),
                        usernameFromEditText
                    )
                    sharedPreferencesEditor.apply()
                } else {
                    sharedPreferencesEditor.remove(context.getString(R.string.username))
                    sharedPreferencesEditor.apply()
                }
                onSuccess()
                dialog.dismiss()
            }

            return dialog
        }

        @SuppressLint("InflateParams")
        fun makeReStarDialog(
            context: Context,
            db: AppDatabase,
            sharedPreferences: SharedPreferences,
            onSuccess: () -> Unit
        ): AlertDialog {
            val builder = AlertDialog.Builder(context)
            val dialogLayout =
                LayoutInflater.from(context).inflate(R.layout.main__dialog__restart, null, false)
            val editTextName = dialogLayout.findViewById<EditText>(R.id.main_dialog_edittext_username)

            builder.setTitle("Borrar todos los datos de la app")

            editTextName.inputType =InputType.TYPE_TEXT_VARIATION_PASSWORD

            builder.setView(dialogLayout)
            builder.setPositiveButton("OK", null)
            builder.setNegativeButton("cancelar", null)
            builder.setCancelable(true)

            val dialog = builder.show()

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {


                var pass=  editTextName.text.toString()
                val passwordGlobal : String = sharedPreferences.getString("password", "").toString()

                val KEY = "1Hbfh667adfDEJ78";
                val key =  SecretKeySpec(KEY.toByteArray(),"AES")
                val cipher = Cipher.getInstance("AES")
                cipher.init(Cipher.DECRYPT_MODE, key);
                val decryptedValue64 = android.util.Base64.decode(passwordGlobal,android.util.Base64.DEFAULT)
                val decryptedByteValue = cipher.doFinal(decryptedValue64)
                val decryptedValue =  String(decryptedByteValue, Charset.defaultCharset())

                if(pass == "" || pass == null) {
                    Toast.makeText(context, "Escriba alguna contraseña", Toast.LENGTH_LONG).show()
                } else if(pass == decryptedValue) {
                    Toast.makeText(context, "Base de datos limpia", Toast.LENGTH_LONG).show()

                    GlobalScope.launch {
                        ProductDL.deleteAll(db)
                        CartDL.deleteAll(db)
                        CartItemDL.deleteAll(db)
                        CartReturnItemDL.deleteAll(db)
                        ClientDL.deleteAll(db)
                        ClientPriceDL.deleteAll(db)
                        ProductDL.deleteAll(db)
                    }
                } else {
                    Toast.makeText(context, "Contraseña incorrecta", Toast.LENGTH_LONG).show()
                }

                onSuccess()
                dialog.dismiss()
            }

            return dialog
        }

    }
}