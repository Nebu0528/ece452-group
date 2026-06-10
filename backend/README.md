# Backend

FastAPI backend for DiscoverUWaterloo.

## Local Development

### Prerequisites
- Python 3.11+
- Docker (for local PostgreSQL)

### Setup

Create a `.env` file with the credentials shared by the team, then:

```bash
python -m venv venv
venv\Scripts\activate
docker compose up -d
pip install -r requirements.txt
alembic upgrade head
python seed.py
uvicorn app.main:app --reload
```

API available at `http://localhost:8000`.

### Schema Changes

After modifying a model, generate and apply a migration:

```bash
alembic revision --autogenerate -m "describe your change"
alembic upgrade head
```
