package com.example.simpleexpensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Insert
    suspend fun addCategory(category: Category)

    @Insert
    suspend fun addExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Transaction
    @Query("SELECT * FROM categories ORDER BY name")
    fun categoriesWithExpenses(): Flow<List<CategoryWithExpenses>>
}
