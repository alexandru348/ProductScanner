package com.example.productscanner

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.common.Barcode

object OnlineProductRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun evaluate (
        barcode: String,

        onResult: (ProductEvaluation?) -> Unit,

        onError: (Exception) -> Unit = {}
    ) {
        db.collection("Products")

            .document(barcode)

            .get()

            .addOnSuccessListener { snapshot ->

                Log.d("FIRESTORE", "snapshot.exists = ${snapshot.exists()}, data = ${snapshot.data}")
                if (!snapshot.exists()) {

                    onResult(null)

                    return@addOnSuccessListener
                }

                try {
                    val evaluation = snapshot.toObject(ProductEvaluation::class.java)

                    Log.d("FIRESTORE", "mapped evaluation = $evaluation")

                    onResult(evaluation)
                } catch (e: Exception) {

                    Log.e("FIRESTORE", "Exception in toObject")

                    onError(e)
                }
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}