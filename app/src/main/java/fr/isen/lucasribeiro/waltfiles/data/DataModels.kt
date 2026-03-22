package fr.isen.lucasribeiro.waltfiles.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Category(
    val categorie: String? = null,
    val franchises: List<Franchise>? = null
)

@IgnoreExtraProperties
data class Franchise(
    val nom: String? = null,
    val sous_sagas: List<SousSaga>? = null,
    val films: List<Film>? = null
)

@IgnoreExtraProperties
data class SousSaga(
    val nom: String? = null,
    val films: List<Film>? = null
)

@IgnoreExtraProperties
data class Film(
    val titre: String? = null,
    val annee: Int? = null,
    val genre: String? = null,
    val numero: Int? = null,
    val image: String? = null
)
