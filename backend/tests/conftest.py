import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.auth import get_current_user_id, hash_password
from app.database import Base, get_db
from app.main import app
from app.models.user import User, UserRole


@pytest.fixture()
def test_engine():
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(engine)
    yield engine
    Base.metadata.drop_all(engine)


@pytest.fixture()
def db_session(test_engine):
    session = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture()
def test_user(db_session):
    user = User(
        name="Test Organizer",
        email="organizer@test.com",
        password=hash_password("password123"),
        role=UserRole.ORGANIZER,
        is_verified=True,
    )
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)
    return user


@pytest.fixture()
def client(test_engine, test_user):
    TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)

    def override_get_db():
        session = TestingSessionLocal()
        try:
            yield session
        finally:
            session.close()

    def override_get_current_user_id():
        return test_user.id

    app.dependency_overrides[get_db] = override_get_db
    app.dependency_overrides[get_current_user_id] = override_get_current_user_id
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()
