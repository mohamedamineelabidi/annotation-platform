# NLP Annotation Platform

Academic mini-project: collaborative annotation platform for supervised NLP classification.

## Requirements

- Java 17
- Docker Desktop
- Maven or the included Maven Wrapper
- Python available as `python` or set `PYTHON_EXECUTABLE`

## Run

```powershell
docker compose up -d
.\mvnw clean package -DskipTests
java -jar target\app.jar
```

Open http://localhost:8080.

The MariaDB container is exposed on local port `3310` to avoid common local MySQL/MariaDB conflicts. Override the connection with `DB_URL`, `DB_USER`, and `DB_PASSWORD` if needed.

## Accounts

- admin / admin
- user1 / user1
- user2 / user2
- user3 / user3

## CSV Import

Supported formats:

```csv
id,text
1,The service was excellent.
```

```csv
id,text1,text2
1,The sky is blue.,The weather is clear.
```
