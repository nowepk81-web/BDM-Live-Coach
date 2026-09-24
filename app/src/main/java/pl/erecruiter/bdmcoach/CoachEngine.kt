package pl.erecruiter.bdmcoach

enum class Priority { HIGH, MEDIUM, LOW }
data class CoachCard(val label: String, val message: String, val reason: String, val priority: Priority) : java.io.Serializable
data class CoachState(val status: String, val quote: String?, val cards: List<CoachCard>) : java.io.Serializable

/** Deliberately conservative: product claims stay out unless confirmed by the sales team. */
class CoachEngine {
    private var lastTopic = "Rozpoznanie potrzeb"

    fun analyse(transcript: String): CoachState {
        val text = transcript.lowercase()
        fun state(topic: String, quote: String?, vararg cards: CoachCard): CoachState {
            lastTopic = topic
            // The UI must support a quick glance; keep the source excerpt short as well.
            return CoachState(topic, quote?.take(280), cards.take(3))
        }
        fun card(label: String, message: String, reason: String, p: Priority) = CoachCard(label, message, reason, p)

        return when {
            text.anyOf("to drogo", "za drogo", "cena", "koszt") -> state("Cena · najpierw zrozum obiekcję", transcript,
                card("🛑 NIE BROŃ CENY", "Z czym porównuje Pani ten koszt?", "Ustal punkt odniesienia, zanim odpowiesz.", Priority.HIGH),
                card("ALTERNATYWA", "Co musiałoby się wydarzyć, żeby ta cena była uzasadniona?", "Prowadzi do kryterium wartości.", Priority.MEDIUM))
            text.anyOf("wdrażamy", "kupujemy", "umowę", "decyzja", "wybieramy") -> state("Decyzja · przejdź do formalności", transcript,
                card("➡️ ZMIEŃ ETAP", "Co musi się wydarzyć po Państwa stronie, żebyśmy mogli przejść dalej?", "Klient sygnalizuje gotowość — nie wracaj do prezentacji.", Priority.HIGH),
                card("ALTERNATYWA", "Kto jeszcze powinien być zaangażowany w kolejny krok?", "Dopina proces decyzyjny.", Priority.MEDIUM))
            text.anyOf("test", "przetestować", "demo") -> state("Test · ustal kryteria sukcesu", transcript,
                card("⭐ ZADAJ TERAZ", "Po czym po dwóch tygodniach pozna Pani, że system rzeczywiście pomaga?", "Test ma potwierdzić konkretną wartość.", Priority.HIGH),
                card("ALTERNATYWA", "Które 2–3 rzeczy koniecznie powinniśmy sprawdzić?", "Zamienia test w plan decyzji.", Priority.MEDIUM))
            text.anyOf("dużo cv", "cv", "ręcznie", "czasochłonne", "chaos", "zabiera czasu") -> state("Selekcja · pogłęb wartość", transcript,
                card("⭐ ZADAJ TERAZ", "Jak dużo czasu to dzisiaj wymaga?", "Pozwala klientowi samemu opisać skalę.", Priority.HIGH),
                card("ALTERNATYWA", "Co byłoby największą różnicą, gdyby ten etap był prostszy?", "Sprawdza znaczenie usprawnienia.", Priority.MEDIUM))
            text.anyOf("świetne", "ważne", "potrzebujemy", "pomogłoby", "brakuje nam") -> state("Wartość · nie przechodź dalej", transcript,
                card("🛑 STOP — ZNALAZŁEŚ WARTOŚĆ", "Co dokładnie byłoby w tym dla Pani najważniejsze?", "Klient sam nazwał wartość; najpierw ustal jej wagę.", Priority.HIGH),
                card("ALTERNATYWA", "Jak często pojawia się taka sytuacja?", "Pomaga zrozumieć kontekst bez liczenia ROI za klienta.", Priority.MEDIUM))
            text.anyOf("nie potrzebujemy", "nie jest problem", "mamy to poukładane") -> state("Ten wątek nie rezonuje", transcript,
                card("↪️ ODPUŚĆ TEN WĄTEK", "W takim razie gdzie widzi Pani większy potencjał usprawnienia?", "Nie twórz sztucznego problemu.", Priority.HIGH))
            else -> state("$lastTopic · słuchaj i doprecyzuj", null,
                card("✅ SŁUCHAJ", "Nie przerywaj — zbieraj opis procesu i język klienta.", "Jeszcze nie ma sygnału, który uzasadnia zmianę kierunku.", Priority.MEDIUM),
                card("GDY ZAPADNIE CISZA", "Jak wygląda to u Państwa krok po kroku?", "Naturalne pytanie o stan obecny.", Priority.LOW))
        }
    }

    private fun String.anyOf(vararg phrases: String) = phrases.any { contains(it) }
}
