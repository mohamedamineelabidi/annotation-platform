# NLP Annotation Platform

Academic mini-project: collaborative annotation platform for supervised NLP classification.

Developed by **Rida Aderkane** and **Mohamed Amine El Abidi**.

## Requirements

- Java 17
- Maven or the included Maven Wrapper (only needed to build)
- Python available as `python` or set `PYTHON_EXECUTABLE` (optional, for the training feature)

## Run (standalone)

The application ships with an embedded H2 database and initializes all required data
(users, labels, sample dataset) automatically at startup. No external database is required.

```powershell
.\mvnw clean package
java -jar target\app.jar
```

Open http://localhost:8080.

## Run with MariaDB (optional)

To use MariaDB instead of the embedded database, start the bundled container and
override the connection settings:

```powershell
docker compose up -d
$env:DB_URL="jdbc:mariadb://localhost:3310/annotation_nlp"
$env:DB_USER="annotation"; $env:DB_PASSWORD="annotation"; $env:DB_DRIVER="org.mariadb.jdbc.Driver"
java -jar target\app.jar
```

The MariaDB container is exposed on local port `3310` to avoid common local MySQL/MariaDB conflicts.

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
