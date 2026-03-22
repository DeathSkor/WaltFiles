package fr.isen.lucasribeiro.waltfiles.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

object DatabaseService {
    private const val DATABASE_URL = "https://waltfiles-default-rtdb.europe-west1.firebasedatabase.app"
    
    fun fetchCategories(onResult: (List<Category>) -> Unit) {
        try {
            val database = FirebaseDatabase.getInstance(DATABASE_URL).reference
            database.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val categoriesList = mutableListOf<Category>()
                    val categoriesNode = snapshot.child("categories")
                    val source = if (categoriesNode.exists()) categoriesNode else snapshot

                    for (child in source.children) {
                        try {
                            val category = child.getValue(Category::class.java)
                            if (category != null) {
                                categoriesList.add(category)
                            }
                        } catch (e: Exception) {
                            Log.e("DatabaseService", "Error parsing child ${child.key}", e)
                        }
                    }
                    onResult(categoriesList)
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(emptyList())
                }
            })
        } catch (e: Exception) {
            onResult(emptyList())
        }
    }

    fun saveUserTag(filmTitle: String, tag: String?) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance(DATABASE_URL).reference
        val userTagsRef = database.child("users").child(userId).child("tags")
        
        val sanitizedTitle = filmTitle.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
        
        if (tag == null) {
            userTagsRef.child(sanitizedTitle).removeValue()
        } else {
            userTagsRef.child(sanitizedTitle).setValue(tag)
        }
    }

    fun fetchUserTags(onResult: (Map<String, String>) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(emptyMap())
        val database = FirebaseDatabase.getInstance(DATABASE_URL).reference
        val userTagsRef = database.child("users").child(userId).child("tags")

        userTagsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tagsMap = mutableMapOf<String, String>()
                for (child in snapshot.children) {
                    val tag = child.getValue(String::class.java)
                    if (tag != null) {
                        tagsMap[child.key ?: ""] = tag
                    }
                }
                onResult(tagsMap)
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(emptyMap())
            }
        })
    }

    fun saveUsername(username: String, onComplete: (Boolean) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return onComplete(false)
        val database = FirebaseDatabase.getInstance(DATABASE_URL).reference
        database.child("users").child(userId).child("username").setValue(username)
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    fun fetchUsername(onResult: (String?) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return onResult(null)
        val database = FirebaseDatabase.getInstance(DATABASE_URL).reference
        database.child("users").child(userId).child("username").get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.getValue(String::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun fetchGlobalTagStats(filmTitle: String, onResult: (Map<String, List<String>>) -> Unit) {
        val database = FirebaseDatabase.getInstance(DATABASE_URL).reference
        val usersRef = database.child("users")
        val sanitizedTitle = filmTitle.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statsMap = mutableMapOf<String, MutableList<String>>()
                for (userSnapshot in snapshot.children) {
                    val username = userSnapshot.child("username").getValue(String::class.java) ?: "Anonymous"
                    val tag = userSnapshot.child("tags").child(sanitizedTitle).getValue(String::class.java)
                    if (tag != null) {
                        if (!statsMap.containsKey(tag)) {
                            statsMap[tag] = mutableListOf()
                        }
                        statsMap[tag]?.add(username)
                    }
                }
                onResult(statsMap)
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(emptyMap())
            }
        })
    }
}
