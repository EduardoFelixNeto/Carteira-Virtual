package com.example.carteiravirtual

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.carteiravirtual.data.entities.UserBalance
import com.example.carteiravirtual.databinding.ActivityListResourcesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListResourcesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListResourcesBinding
    private val userBalanceDao by lazy { (application as MyApplication).database.userBalanceDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListResourcesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val defaultCurrencies = listOf("BRL", "USD", "EUR", "BTC", "ETH")

        binding.rvResources.layoutManager = LinearLayoutManager(this)

        CoroutineScope(Dispatchers.IO).launch {
            val balances = defaultCurrencies.map { currency ->
                userBalanceDao.getBalanceOrNull(currency) ?: UserBalance(currency, 0.0)
            }

            withContext(Dispatchers.Main) {
                val resourceItems = balances.map { ResourceItem(it.currency, it.balance) }
                binding.rvResources.adapter = ResourcesAdapter(resourceItems)
            }
        }
    }
}
