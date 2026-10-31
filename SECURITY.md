# Sekrety poza repozytorium

Nie commituj:

- `frontend/ssl/*.key`, certyfikatów produkcyjnych z kluczem
- `server/src/main/resources/keystore/*.p12`
- haseł do keystore / bazy / JWT w plikach `.env`

Pliki binarne trzymaj lokalnie lub w menedżerze haseł. Po usunięciu z repo **obróć** skompromitowane klucze, jeśli kiedyś były na GitHubie.
