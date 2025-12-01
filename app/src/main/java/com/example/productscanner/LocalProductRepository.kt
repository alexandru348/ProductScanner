package com.example.productscanner

object LocalProductRepository {

    private val products: Map<String, ProductEvaluation> = mapOf(
      "5942326400155" to ProductEvaluation(
          barcode = "5942326400155",
          productName = "Aqua Carpatica - apa plata",
          healthScore = 80,
          level = HealthLevel.HEALTHY,
          explanation = "Apa plata, fara calorii",
          compareHint = "Mai sanatoasa decat sucurile carbogazoase cu zahar"
      ),
      "3800020493946" to ProductEvaluation(
          barcode = "3800020493946",
          productName = "KitKat",
          healthScore = 25,
          level = HealthLevel.UNHEALTHY,
          explanation = "Baton cu ciocolata alba",
          compareHint = "Mai putin sanatos decat ciocolata amara cu 90% cacao"
      ),
      "5000159558020" to ProductEvaluation(
          barcode = "5000159558020",
          productName = "Bounty",
          healthScore = 20,
          level = HealthLevel.UNHEALTHY,
          explanation = "Baton de ciocolata cu nuca de cocos, bogat in zahar si grasimi saturate",
          compareHint = "Mai putin sanatos decat o ciocolata neagra simpla cu continut ridicat de cacao, fara umplutura de cocos"
      ),
      "5905477003880" to ProductEvaluation(
          barcode = "5905477003880",
          productName = "Pilos - unt clarificat",
          healthScore = 50,
          level = HealthLevel.MODERATE,
          explanation = "Unt clarificat aproape in totalitate din grasime, foarte bogat in grasimi saturate si calorii, nu contine apa sau lactoza, dar consumat frecvent sau in cantitati mari poate afecta sanatatea cardiovasculara",
          compareHint = "Mai putin sanatos decat uleiul de masline extravirgin sau alte uleiuri vegetale bogate in grasimi nesaturate"
      ),
      "5941486004142" to ProductEvaluation(
          barcode = "5941486004142",
          productName = "LaMinut - mustar de masa",
          healthScore = 60,
          level = HealthLevel.MODERATE,
          explanation = "Mustar de masa pe baza de seminte de mustar, apa, otet, sare si zahar. Are putine calorii per portie, dar poate fi bogat in sodiu si poate contine aditivi (conservanti sau coloranti)",
          compareHint = "Mai putin sanatos decat sosul de usturoi facut in casa, fara aditivi (conservanti sau coloranti)"
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
