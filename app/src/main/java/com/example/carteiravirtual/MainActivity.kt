package com.example.carteiravirtual

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.carteiravirtual.data.entities.UserBalance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvBalance: TextView
    private val userBalanceDao by lazy { (application as MyApplication).database.userBalanceDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBalance = findViewById(R.id.tvBalance)

        // Configuração dos botões
        findViewById<Button>(R.id.btnDeposit).setOnClickListener {
            startActivity(Intent(this, DepositActivity::class.java))
        }

        findViewById<Button>(R.id.btnListResources).setOnClickListener {
            startActivity(Intent(this, ListResourcesActivity::class.java))
        }

        findViewById<Button>(R.id.btnConvertResources).setOnClickListener {
            startActivity(Intent(this, ConvertResourcesActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        // Buscar o saldo atualizado do banco de dados
        CoroutineScope(Dispatchers.IO).launch {
            val balance = userBalanceDao.getBalanceOrNull("BRL") ?: UserBalance("BRL", 0.0)
            val formattedBalance = "Saldo: R$%.2f".format(balance.balance)

            withContext(Dispatchers.Main) {
                tvBalance.text = formattedBalance
            }
        }
    }
}
