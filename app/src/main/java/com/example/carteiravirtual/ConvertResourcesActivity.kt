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

        // Inicialização dos componentes
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
                // Buscar todas as moedas disponíveis no banco de dados
                val availableCurrencies = userBalanceDao.getAllBalances().map { it.currency }

                withContext(Dispatchers.Main) {
                    // Configurar o Spinner de moeda de origem
                    val fromAdapter = ArrayAdapter(
                        this@ConvertResourcesActivity,
                        android.R.layout.simple_spinner_item,
                        availableCurrencies
                    )
                    fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerFromCurrency.adapter = fromAdapter

                    // Configurar o Spinner de moeda de destino
                    val toAdapter = ArrayAdapter(
                        this@ConvertResourcesActivity,
                        android.R.layout.simple_spinner_item,
                        availableCurrencies
                    )
                    toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerToCurrency.adapter = toAdapter

                    // Adicionar listeners para os Spinners
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
                val response = RetrofitInstance.api.getExchangeRates("$fromCurrency-$toCurrency")
                val key = "$fromCurrency$toCurrency"
                val conversionRate = response[key]?.bid?.toDoubleOrNull() ?: 0.0

                val fromBalance = userBalanceDao.getBalanceOrNull(fromCurrency)?.balance ?: 0.0
                val maxAmount = fromBalance * conversionRate

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
        // Verificar se as moedas de origem e destino são iguais
        if (fromCurrency == toCurrency) {
            Toast.makeText(this, "Selecione moedas diferentes para converter", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnConvert.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obter os saldos das moedas de origem e destino
                val fromBalance = userBalanceDao.getBalanceOrNull(fromCurrency) ?: UserBalance(fromCurrency, 0.0)
                val toBalance = userBalanceDao.getBalanceOrNull(toCurrency) ?: UserBalance(toCurrency, 0.0)

                // Verificar se há saldo suficiente na moeda de origem
                if (fromBalance.balance < amount) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnConvert.isEnabled = true
                        Toast.makeText(this@ConvertResourcesActivity, "Saldo insuficiente em $fromCurrency", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Consultar a taxa de conversão da API
                val response = RetrofitInstance.api.getExchangeRates("$fromCurrency-$toCurrency")
                val key = "$fromCurrency$toCurrency"
                val conversionRate = response[key]?.bid?.toDoubleOrNull()

                // Validar se a taxa de conversão foi obtida com sucesso
                if (conversionRate == null || conversionRate <= 0) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        btnConvert.isEnabled = true
                        Toast.makeText(this@ConvertResourcesActivity, "Erro ao obter taxa de conversão", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Realizar a conversão
                val convertedAmount = amount * conversionRate

                // Atualizar os saldos das moedas no banco de dados
                val updatedFromBalance = fromBalance.copy(balance = fromBalance.balance - amount)
                val updatedToBalance = toBalance.copy(balance = toBalance.balance + convertedAmount)

                userBalanceDao.insertOrUpdate(updatedFromBalance)
                userBalanceDao.insertOrUpdate(updatedToBalance)

                // Atualizar a interface do usuário e redirecionar para a listagem de recursos
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConvert.isEnabled = true
                    Toast.makeText(
                        this@ConvertResourcesActivity,
                        "Convertido para $toCurrency: %.2f".format(convertedAmount),
                        Toast.LENGTH_LONG
                    ).show()

                    // Redirecionar para ListResourcesActivity
                    startActivity(Intent(this@ConvertResourcesActivity, ListResourcesActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                // Tratar erros durante a conversão
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnConvert.isEnabled = true
                    Toast.makeText(this@ConvertResourcesActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
