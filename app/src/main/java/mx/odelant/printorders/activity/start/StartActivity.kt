package mx.odelant.printorders.activity.start

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import mx.odelant.printorders.R
import mx.odelant.printorders.activity.Login.RegistUserActivity
import java.net.URLEncoder
import android.content.Intent as Intent

class StartActivity : AppCompatActivity() {

    private val rStartActivity = R.layout.start__activity

    private lateinit var startView: StartView
    private lateinit var startPresenter: StartPresenter
    private lateinit var textEmail: EditText
    private lateinit var sharedPref: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(rStartActivity)

        sharedPref = getSharedPreferences("SHARED_PREFERENCES", Context.MODE_PRIVATE)
        startView = StartView(this)
        startPresenter = StartPresenter(this, startView)

        textEmail = findViewById(R.id.start_et_email)

        //continueTRegisterActivity()

        if (startPresenter.checkRegistration()) {
            continueTRegisterActivity()
        } else {
            startView.registrationButton.setOnClickListener {
                if (startPresenter.attemptRegistration()) {

                    sharedPref.edit().putString("EMAIL", textEmail.text.toString()).commit()
                    continueTRegisterActivity()
                } else {
                    Toast.makeText(this, "Error registrando aplicación", Toast.LENGTH_LONG).show()
                }
            }
            startView.whatsappButton.setOnClickListener {
                val intent = Intent ("android.intent.action.MAIN")
                val url = "https://api.whatsapp.com/send?phone= 81 1610 8660 &text=" + URLEncoder.encode("", "UTF-8")

                intent.setComponent(ComponentName("com.whatsapp","com.whatsapp.Conversation"))
                intent.putExtra("jid", PhoneNumberUtils.stripSeparators("5218116108680" ) +"@s.whatsapp.net")

                startActivity(intent)
            }

        }
    }

    private fun continueTRegisterActivity() {
        val intent = Intent(this, RegistUserActivity::class.java)
        startActivity(intent)
        this.finish()
    }
}
