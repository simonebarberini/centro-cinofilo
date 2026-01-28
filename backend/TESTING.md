# Testing Multi-Tenant Setup

## Avviare il backend con profilo DEV

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev
```

Oppure:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Cosa succede all'avvio (profilo dev)

1. Flyway esegue le migration V1 e V2
2. Vengono create le tabelle `tenant` e `app_user`
3. Il CommandLineRunner seed crea:
   - 1 tenant "Demo Centro" (tipo PENSIONE, 10 box)
   - 1 utente owner (username: `owner`, password: `owner123!`, role: TENANT_OWNER)

## Verifica nel database

```bash
# Connettersi al database
docker exec -it centro-cinofilo-postgres psql -U cinofilo -d cinofilo

# Query di verifica
SELECT * FROM tenant;
SELECT id, username, role, enabled, tenant_id FROM app_user;
```

## Credenziali create

- **Username**: `owner`
- **Password**: `owner123!`
- **Role**: `TENANT_OWNER`
- **Tenant**: `Demo Centro`

## Log attesi

```
🌱 Starting DEV data seeding...
✅ Created demo tenant: Demo Centro (ID: <uuid>)
✅ Created owner user: owner (Role: TENANT_OWNER, Tenant: Demo Centro)
🎉 DEV data seeding completed successfully!
📝 Login credentials - username: owner, password: owner123!
```
