package com.example.simpleexpensetracker

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.simpleexpensetracker.data.AppDatabase
import com.example.simpleexpensetracker.data.Category
import com.example.simpleexpensetracker.data.Expense
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val dao by lazy { AppDatabase.get(this).expenseDao() }
    private var categories: List<Category> = emptyList()
    private var expenses: List<Pair<Expense, String>> = emptyList()
    private var editingExpense: Expense? = null

    private val rupiah = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))

    private lateinit var inputCategory: EditText
    private lateinit var inputNote: EditText
    private lateinit var inputAmount: EditText
    private lateinit var spinner: Spinner
    private lateinit var textTotal: TextView
    private lateinit var btnSave: MaterialButton
    private lateinit var btnCancel: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val pad = (16 * resources.displayMetrics.density).toInt()
            v.setPadding(pad + systemBars.left, pad + systemBars.top, pad + systemBars.right, pad + systemBars.bottom)
            insets
        }

        inputCategory = findViewById(R.id.inputCategory)
        inputNote = findViewById(R.id.inputNote)
        inputAmount = findViewById(R.id.inputAmount)
        spinner = findViewById(R.id.spinnerCategory)
        textTotal = findViewById(R.id.textTotal)
        btnSave = findViewById(R.id.btnSaveExpense)
        btnCancel = findViewById(R.id.btnCancelEdit)
        val list = findViewById<ListView>(R.id.listExpenses)

        val categoryAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mutableListOf())
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = categoryAdapter

        val expenseAdapter = ExpenseAdapter()
        list.adapter = expenseAdapter

        findViewById<MaterialButton>(R.id.btnAddCategory).setOnClickListener {
            val name = inputCategory.text.toString().trim()
            if (name.isEmpty()) return@setOnClickListener toast("Nama kategori wajib diisi")
            lifecycleScope.launch {
                dao.addCategory(Category(name = name))
                inputCategory.text.clear()
            }
        }

        btnSave.setOnClickListener {
            val note = inputNote.text.toString().trim()
            val amount = inputAmount.text.toString().trim().toLongOrNull()
            val category = categories.getOrNull(spinner.selectedItemPosition)
            if (note.isEmpty()) return@setOnClickListener toast("Catatan wajib diisi")
            if (amount == null || amount <= 0) return@setOnClickListener toast("Nominal harus lebih dari 0")
            if (category == null) return@setOnClickListener toast("Tambahkan kategori dulu")

            val editing = editingExpense
            lifecycleScope.launch {
                if (editing == null) {
                    dao.addExpense(
                        Expense(note = note, amount = amount, date = System.currentTimeMillis(), categoryId = category.id)
                    )
                } else {
                    dao.updateExpense(editing.copy(note = note, amount = amount, categoryId = category.id))
                }
                clearForm()
            }
        }

        btnCancel.setOnClickListener { clearForm() }

        list.setOnItemClickListener { _, _, position, _ ->
            val (expense, _) = expenses.getOrNull(position) ?: return@setOnItemClickListener
            editingExpense = expense
            inputNote.setText(expense.note)
            inputAmount.setText(expense.amount.toString())
            spinner.setSelection(categories.indexOfFirst { it.id == expense.categoryId }.coerceAtLeast(0))
            btnSave.text = "Update Pengeluaran"
            btnCancel.visibility = View.VISIBLE
        }

        list.setOnItemLongClickListener { _, _, position, _ ->
            val expense = expenses.getOrNull(position)?.first ?: return@setOnItemLongClickListener false
            confirmDelete(expense)
            true
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dao.categoriesWithExpenses().collect { groups ->
                    categories = groups.map { it.category }
                    categoryAdapter.clear()
                    categoryAdapter.addAll(categories.map { it.name })
                    categoryAdapter.notifyDataSetChanged()

                    expenses = groups.flatMap { group ->
                        group.expenses.map { it to group.category.name }
                    }.sortedByDescending { it.first.date }

                    textTotal.text = "Total: ${formatRupiah(expenses.sumOf { it.first.amount })}"
                    expenseAdapter.clear()
                    expenseAdapter.addAll(expenses)
                    expenseAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private inner class ExpenseAdapter :
        ArrayAdapter<Pair<Expense, String>>(this, 0) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_expense, parent, false)
            val (expense, categoryName) = getItem(position) ?: return view
            view.findViewById<TextView>(R.id.textItem).text =
                "${formatRupiah(expense.amount)} · ${expense.note} · $categoryName · ${dateFormat.format(Date(expense.date))}"
            view.findViewById<ImageButton>(R.id.btnDeleteItem).setOnClickListener {
                confirmDelete(expense)
            }
            return view
        }
    }

    private fun confirmDelete(expense: Expense) {
        AlertDialog.Builder(this)
            .setMessage("Hapus \"${expense.note}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                lifecycleScope.launch {
                    dao.deleteExpense(expense)
                    if (editingExpense?.id == expense.id) clearForm()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun clearForm() {
        editingExpense = null
        inputNote.text.clear()
        inputAmount.text.clear()
        btnSave.text = "Simpan Pengeluaran"
        btnCancel.visibility = View.GONE
    }

    private fun formatRupiah(value: Long) = "Rp ${rupiah.format(value)}"

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
