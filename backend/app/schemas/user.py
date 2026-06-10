from pydantic import BaseModel
from app.models.user import UserRole


class UserCreate(BaseModel):
    name: str
    password: str
    role: UserRole = UserRole.BASIC


class UserOut(BaseModel):
    id: int
    name: str
    role: UserRole

    model_config = {"from_attributes": True}


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"


class LoginRequest(BaseModel):
    name: str
    password: str
