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

                Log.d("FIRESTORE", "snapshot.exists = ${snapshot.exists()}, data = ${snapshot.data}") // Log: check if the document exists in Firestore and inspect the raw data returned
                if (!snapshot.exists()) {

                    onResult(null)

                    return@addOnSuccessListener
                }

                try {
                    val evaluation = snapshot.toObject(ProductEvaluation::class.java)

                    Log.d("FIRESTORE", "mapped evaluation = $evaluation") // Log: confirm that the Firestore document was successfully converted into a ProductEvaluation object

                    onResult(evaluation)
                } catch (e: Exception) {

                    Log.e("FIRESTORE", "Exception in toObject", e) // Log (error): something went wrong while converting the Firestore document into a ProductEvaluation object

                    onError(e)
                }
            }
            .addOnFailureListener { e ->

                Log.e("FIRESTORE", "Error loading document from Firestore", e) // Log (error): the request to Firestore failed (network issues, permissions)
                onError(e)
            }
    }
}