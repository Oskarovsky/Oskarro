# SSL (lokalnie)

Nie commituj kluczy prywatnych. Wygeneruj na maszynie dev:

```bash
openssl req -x509 -newkey rsa:2048 -keyout server.key -out server.crt -days 365 -nodes
```

`server.key` jest w `.gitignore`.
