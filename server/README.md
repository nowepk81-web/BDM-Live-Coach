# Serwer BDM Live Coach

Serwer jest granicą bezpieczeństwa między aplikacją a modelem. Domyślnie używa DeepSeek Flash przez endpoint zgodny z OpenAI. Przechowuje klucz API oraz instrukcje, a aplikacja wysyła mu tylko krótkie okno tekstu po lokalnym rozpoznaniu mowy — nigdy klucz API.

1. Zainstaluj Node.js 20+.
2. Skopiuj `.env.example` do `.env` i ustaw `DEEPSEEK_API_KEY` oraz `COACH_SHARED_SECRET` jako zmienne środowiskowe hostingu.
3. Uruchom `npm install`, następnie `npm start`.
4. W produkcji postaw za HTTPS, dodaj uwierzytelnianie użytkowników zamiast współdzielonego sekretu oraz ograniczenia częstotliwości.

Endpoint `POST /v1/coach` wymaga nagłówka `Authorization: Bearer <COACH_SHARED_SECRET>` i obiektu `{ "context": "..." }`.

Do kontroli działania hostingu służy nieautoryzowany endpoint `GET /health`, zwracający `{ "status": "ok" }`. Odpowiedź modelu jest ponownie walidowana przez serwer (maksymalnie trzy krótkie karty), zanim trafi do aplikacji.
