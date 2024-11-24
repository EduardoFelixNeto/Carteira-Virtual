package com.example.carteiravirtual

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.carteiravirtual.data.entities.UserBalance
import com.example.carteiravirtual.databinding.ActivityDepositBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DepositActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDepositBinding
    private val userBalanceDao by lazy { (application as MyApplication).database.userBalanceDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDepositBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDepositConfirm.setOnClickListener {
            val amount = binding.etDepositAmount.text.toString().toDoubleOrNull()
            if (amount != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val balance = userBalanceDao.getBalanceOrNull("BRL") ?: UserBalance("BRL", 0.0)
                    val updatedBalance = balance.copy(balance = balance.balance + amount)
                    userBalanceDao.insertOrUpdate(updatedBalance)

                    // Redirecionar para MainActivity
                    startActivity(Intent(this@DepositActivity, MainActivity::class.java))
                    finish()
                }
            } else {
                Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
