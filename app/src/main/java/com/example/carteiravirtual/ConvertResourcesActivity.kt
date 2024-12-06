package com.example.carteiravirtual

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.carteiravirtual.api.RetrofitInstance
import com.example.carteiravirtual.data.entities.UserBalance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConvertResourcesActivity : AppCompatActivity() {

    private val userBalanceDao by lazy { (application as MyApplication).database.userBalanceDao() }

    private lateinit var spinnerFromCurrency: Spinner
    private lateinit var spinnerToCurrency: Spinner
    private lateinit var etConvertAmount: EditText
    private lateinit var tvFromCurrencyBalance: TextView
    private lateinit var tvToCurrencyMaxAmount: TextView
    private lateinit var btnConvert: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_convert_resources)

        spinnerFromCurrency = findViewById(R.id.spinnerFromCurrency)
        spinnerToCurrency = findViewById(R.id.spinnerToCurrency)
        etConvertAmount = findViewById(R.id.etConvertAmount)
        tvFromCurrencyBalance = findViewById(R.id.tvFromCurrencyBalance)
        tvToCurrencyMaxAmount = findViewById(R.id.tvToCurrencyMaxAmount)
        btnConvert = findViewById(R.id.btnConvert)
        progressBar = findViewById(R.id.progressBar)

        setupSpinners()
        setupConvertButton()
    }

    private fun setupSpinners() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val availableCurrencies = userBalanceDao.getAllBalances().map { it.currency }

                withContext(Dispatchers.Main) {
                    val fromAdapter = ArrayAdapter(
                        this@ConvertResourcesActivity,
                        android.R.layout.simple_spinner_item,
                        availableCurrencies
                    )
                    fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerFromCurrency.adapter = fromAdapter

                    val toAdapter = ArrayAdapter(
                        this@ConvertResourcesActivity,
                        android.R.layout.simple_spinner_item,
                        availableCurrencies
                    )
                    toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerToCurrency.adapter = toAdapter

                    spinnerFromCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val selectedCurrency = availableCurrencies[position]
                            updateFromCurrencyBalance(selectedCurrency)
                            updateToCurrencyMaxAmount(selectedCurrency, spinnerToCurrency.selectedItem.toString())
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }

                    spinnerToCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val fromCurrency = spinnerFromCurrency.selectedItem.toString()
                            val selectedCurrency = availableCurrencies[position]
                            updateToCurrencyMaxAmount(fromCurrency, selectedCurrency)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ConvertResourcesActivity,
                        "Erro ao carregar moedas: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateFromCurrencyBalance(currency: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val balance = userBalanceDao.getBalanceOrNull(currency)?.balance ?: 0.0
            withContext(Dispatchers.Main) {
                tvFromCurrencyBalance.text = "Saldo disponível: %.2f %s".format(balance, currency)
            }
        }
    }

    private fun updateToCurrencyMaxAmount(fromCurrency: String, toCurrency: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (fromCurrency == toCurrency) {
                    withContext(Dispatchers.Main) {
                        tvToCurrencyMaxAmount.text = "Selecione uma moeda diferente da moeda de origem."
                    }
                    return@launch
                }

                val conversionKey = if (toCurrency == "BTC" || toCurrency == "ETH") {
                    "$toCurrency-$fromCurrency"
                } else {
                    "$fromCurrency-$toCurrency"
                }

                val response = RetrofitInstance.api.getExchangeRates(conversionKey)

                val key = conversionKey.replace("-", "")

                val conversionRate = response[key]?.bid?.toDoubleOrNull() ?: 0.0

                val fromBalance = userBalanceDao.getBalanceOrNull(fromCurrency)?.balance ?: 0.0
                val maxAmount = if (toCurrency == "BTC" || toCurrency == "ETH") {
                    fromBalance / conversionRate
                } else {
                    fromBalance * conversionRate
                }

                withContext(Dispatchers.Main) {
                    tvToCurrencyMaxAmount.text = "Máximo que pode comprar: %.2f %s".format(maxAmount, toCurrency)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvToCurrencyMaxAmount.text = "Erro ao buscar taxa de conversão."
                }
            }
        }
    }

    private fun setupConvertButton() {
        btnConvert.setOnClickListener {
            val fromCurrency = spinnerFromCurrency.selectedItem.toString()
            val toCurrency = spinnerToCurrency.selectedItem.toString()
            val amountToConvert = etConvertAmount.text.toString().toDoubleOrNull()

            if (amountToConvert == null || amountToConvert <= 0) {
                Toast.makeText(this, "Digite um valor válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (fromCurrency == toCurrency) {
                Toast.makeText(this, "Escolha moedas diferentes para converter", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performConversion(fromCurrency, toCurrency, amountToConvert)
        }
    }

    private fun performConversion(fromCurrency: String, toCurrency: String, amount: Double) {
        if (fromCurrency == toCurrency) {
            Toast.makeText(this, "Selecione moedas diferentes para converter", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnConvert.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fromBalance = userBalanceDao.getBalanceOrNull(fromCurrency) ?: UserBalance(fromCurrency, 0.0)
                val toBalance = userBalanceDao.getBalanceOrNull(toCurrency) ?: UserBalance(toCurrency, 0.0)

                if (fromBalance.balance < amount) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnConvert.isEnabled = true
                        Toast.makeText(this@ConvertResourcesActivity, "Saldo insuficiente em $fromCurrency", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val isDigitalCurrencyConversion = (fromCurrency == "BTC" || toCurrency == "BTC" || fromCurrency == "ETH" || toCurrency == "ETH")

                val conversionKey = if (isDigitalCurrencyConversion) {
                    if (fromCurrency == "BTC") {
                        "BTC-$toCurrency"
                    } else if (toCurrency == "BTC") {
                        "BTC-$fromCurrency"
                    } else if (fromCurrency == "ETH") {
                        "ETH-$toCurrency"
                    } else {
                        "ETH-$fromCurrency"
                    }
                } else {
                    "$fromCurrency-$toCurrency"
                }

                val response = RetrofitInstance.api.getExchangeRates(conversionKey)
                val key = conversionKey.replace("-", "")
                val conversionRate = response[key]?.bid?.toDoubleOrNull()

                if (conversionRate == null || conversionRate <= 0) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnConvert.isEnabled = true
                        Toast.makeText(this@ConvertResourcesActivity, "Erro ao obter taxa de conversão", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val convertedAmount = if (isDigitalCurrencyConversion) {
                    if (fromCurrency == "BTC" || fromCurrency == "ETH") {
                        amount * conversionRate
                    } else {
                        amount / conversionRate
                    }
                } else {
                    amount * conversionRate
                }

                val updatedFromBalance = fromBalance.copy(balance = fromBalance.balance - amount)
                val updatedToBalance = toBalance.copy(balance = toBalance.balance + convertedAmount)

                userBalanceDao.insertOrUpdate(updatedFromBalance)
                userBalanceDao.insertOrUpdate(updatedToBalance)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConvert.isEnabled = true
                    Toast.makeText(
                        this@ConvertResourcesActivity,
                        "Convertido para $toCurrency: %.2f".format(convertedAmount),
                        Toast.LENGTH_LONG
                    ).show()

                    startActivity(Intent(this@ConvertResourcesActivity, ListResourcesActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConvert.isEnabled = true
                    Toast.makeText(this@ConvertResourcesActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
