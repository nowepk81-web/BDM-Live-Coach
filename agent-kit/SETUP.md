# Uruchomienie BDM Live Coach w ChatGPT

## Zalecana konfiguracja: Projekt ChatGPT

To właściwa opcja, jeśli agent ma być prywatny i używany przez jedną osobę na komputerze oraz telefonie.

1. W przeglądarce uruchom skrypt `build-knowledge.ps1` w PowerShell:
   `./agent-kit/build-knowledge.ps1`
2. W ChatGPT utwórz projekt o nazwie `BDM Live Coach — eRecruiter`.
3. Wklej pełną treść `BDM_LIVE_COACH_INSTRUCTIONS.md` do instrukcji projektu.
4. Dodaj jako wiedzę pliki: `agent-kit/BDM_REFERENCE_TRANSCRIPTS.md`, `oferta cała.pdf` oraz — jeżeli zawiera aktualne informacje produktowe — `eRecruiter _ IPSUPPORT sp. z o.o._ spotkanie online.pdf`.
5. Przetestuj go trzema starterami z poniższej listy. Potem otwórz ten sam projekt w aplikacji ChatGPT na Androidzie/iOS i prowadź spotkanie w tej samej rozmowie.

## Opcja dla zespołu: zarządzany GPT

Jeżeli masz uprawnienia w ChatGPT Business, Enterprise lub Edu, na stronie GPT buildera ustaw:

- **Nazwa:** BDM Live Coach — eRecruiter
- **Opis:** Dyskretny coach BDM na spotkaniach o ATS eRecruiter. Analizuje kontekst, rozpoznaje etap sprzedaży i proponuje jeden najlepszy następny ruch.
- **Instructions:** pełna zawartość `BDM_LIVE_COACH_INSTRUCTIONS.md`.
- **Knowledge:** `BDM_REFERENCE_TRANSCRIPTS.md` plus zatwierdzone, aktualne materiały produktowe.
- **Web search:** wyłączone domyślnie, aby agent nie mieszał źródeł produktowych; włączaj tylko do konkretnej, świadomej weryfikacji.
- **Code Interpreter / Data Analysis:** wyłączone, chyba że chcesz analizować liczby w osobnym trybie.
- **Udostępnienie:** tylko wskazanym osobom lub obszarowi roboczemu; nie publikuj publicznie, ponieważ wiedza może zawierać dane ze spotkań.

Na telefonie GPT można używać, ale tworzenie i edycja odbywa się w przeglądarce.

## Startery rozmowy

- `LIVE: Klient mówi: „Mamy około 150 CV na stanowisko, a sensownych jest może dziesięć.”`
- `cena: Klient mówi, że oferta jest za droga.`
- `test: Klient chce przetestować system na jednej rekrutacji.`
- `Przygotuj 5 pytań o współpracę HR z hiring managerami.`

## Ważne ograniczenie

Agent chatowy nie słucha pasywnie rozmowy w tle i nie aktualizuje sugestii bez wejścia użytkownika. Używaj trybu głosowego ChatGPT albo wysyłaj krótkie fragmenty rozmowy jako kolejne wiadomości. Przed wprowadzeniem wypowiedzi osób trzecich ustal zgodę i politykę danych swojej organizacji.
