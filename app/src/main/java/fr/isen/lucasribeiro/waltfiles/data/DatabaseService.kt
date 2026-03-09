package fr.isen.lucasribeiro.waltfiles.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log

object DatabaseService {
    // Correct Database URL from Logcat:
    private const val DATABASE_URL = "https://waltfiles-default-rtdb.europe-west1.firebasedatabase.app"
    
    fun fetchCategories(onResult: (List<Category>) -> Unit) {
        try {
            val database = FirebaseDatabase.getInstance(DATABASE_URL).reference
            Log.d("DatabaseService", "Fetching categories from $DATABASE_URL")
            
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val categoriesList = mutableListOf<Category>()
                    
                    // The root might contain the "categories" list directly or as a child
                    val categoriesNode = snapshot.child("categories")
                    val source = if (categoriesNode.exists()) categoriesNode else snapshot

                    for (child in source.children) {
                        try {
                            val category = child.getValue(Category::class.java)
                            if (category != null) {
                                // Basic validation: at least the name or franchises should be there
                                if (category.categorie != null || category.franchises != null) {
                                    categoriesList.add(category)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("DatabaseService", "Error parsing child ${child.key}", e)
                        }
                    }
                    
                    Log.d("DatabaseService", "Fetched ${categoriesList.size} categories")
                    onResult(categoriesList)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DatabaseService", "Database error: ${error.message} (Code: ${error.code})")
                    onResult(emptyList())
                }
            })
        } catch (e: Exception) {
            Log.e("DatabaseService", "Initialization error", e)
            onResult(emptyList())
        }
    }
}
