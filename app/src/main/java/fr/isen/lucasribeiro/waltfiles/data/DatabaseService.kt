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

    private fun sanitize(s: String): String {
        return s.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
    }

    fun saveUserTag(filmTitle: String, tag: String?, username: String? = null) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val database = FirebaseDatabase.getInstance(DATABASE_URL).reference
        
        val sanitizedTitle = sanitize(filmTitle)
        val userRef = database.child("users").child(userId)
        
        // If username is provided (e.g. from a profile update or during registration/login)
        // we should make sure it's stored.
        if (username != null) {
            userRef.child("username").setValue(username)
        }
        
        val tagRef = userRef.child("tags").child(sanitizedTitle)
        if (tag == null) {
            tagRef.removeValue()
        } else {
            tagRef.setValue(tag)
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

    fun fetchGlobalTagStats(filmTitle: String, onResult: (Map<String, List<String>>) -> Unit) {
        val database = FirebaseDatabase.getInstance(DATABASE_URL).reference
        val sanitizedTitle = sanitize(filmTitle)
        val usersRef = database.child("users")

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val stats = mutableMapOf<String, MutableList<String>>()
                for (userSnapshot in snapshot.children) {
                    val username = userSnapshot.child("username").getValue(String::class.java) 
                        ?: userSnapshot.child("email").getValue(String::class.java) // fallback to email
                        ?: "Unknown User"
                    
                    val userTag = userSnapshot.child("tags").child(sanitizedTitle).getValue(String::class.java)
                    if (userTag != null) {
                        stats.getOrPut(userTag) { mutableListOf() }.add(username)
                    }
                }
                onResult(stats)
            }

            override fun onCancelled(error: DatabaseError) {
                onResult(emptyMap())
            }
        })
    }
}
