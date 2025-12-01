package com.example.productscanner

object LocalProductRepository {

    private val products: Map<String, ProductEvaluation> = mapOf(
        "5901234123457" to ProductEvaluation(
            barcode = "5901234123457",
            productName = "Bautura racoritoare cu zahar",
            healthScore = 25,
            level = HealthLevel.UNHEALTHY,
            explanation = "Contine mult zahar adaugat si aditivi. Consumata frecvent, poate afecta negativ sanatatea.",
            compareHint = "Mai nesanatos decat apa, ceaiul neindulcit sau o apa minerala cu lamaie."
        ),
        "7130451546260" to ProductEvaluation(
            barcode = "7130451546260",
            productName = "Cereale integrale fara zahar adaugat",
            healthScore = 80,
            level = HealthLevel.HEALTHY,
            explanation = "Contin fibre, putin zahar, grasimi reduse. Potrivite pentru consum zilnic, in cantitati moderate.",
            compareHint = "Mai sanatoase decat cerealele cu ciocolata si zahar adaugat."
        )
    )
    fun evaluate(barcode: String): ProductEvaluation {
        return products[barcode] ?: ProductEvaluation(
            barcode = barcode,
            productName = "Produs necunoscut",
            healthScore = 50,
            level = HealthLevel.MODERATE,
            explanation = "Produsul nu exista inca in baza de date a aplicatiei. Fara informatii despre ingrediente, este considerat neutru.",
            compareHint = "Pentru o evaluare corecta, este nevoie de o baza de date cu ingrediente preluata dintr-o sursa online."
        )
    }
}
