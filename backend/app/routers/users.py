from datetime import datetime, timedelta

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.email import generate_verification_code, send_verification_email
from app.models.user import User
from app.schemas.user import LoginRequest, Token, UserCreate, UserOut, VerifyEmailRequest
from app.auth import create_access_token, hash_password, verify_password

router = APIRouter(prefix="/users", tags=["users"])

CODE_EXPIRY_MINUTES = 10


@router.post("/register", response_model=UserOut)
def register(user: UserCreate, db: Session = Depends(get_db)):
    if db.query(User).filter(User.email == user.email).first():
        raise HTTPException(status_code=400, detail="Email already registered")

    code = generate_verification_code()
    expires = datetime.utcnow() + timedelta(minutes=CODE_EXPIRY_MINUTES)

    db_user = User(
        name=user.name,
        email=user.email,
        password=hash_password(user.password),
        role=user.role,
        is_verified=False,
        verification_code=code,
        verification_code_expires_at=expires,
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)

    send_verification_email(db_user.email, code)

    return db_user


@router.post("/verify-email")
def verify_email(body: VerifyEmailRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == body.email).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if user.is_verified:
        return {"message": "Already verified"}
    if user.verification_code != body.code:
        raise HTTPException(status_code=400, detail="Invalid verification code")
    if user.verification_code_expires_at < datetime.utcnow():
        raise HTTPException(status_code=400, detail="Verification code has expired")

    user.is_verified = True
    user.verification_code = None
    user.verification_code_expires_at = None
    db.commit()

    return {"message": "Email verified successfully"}


@router.post("/resend-code")
def resend_code(body: VerifyEmailRequest, db: Session = Depends(get_db)):
    # body.code is unused here — only email matters
    user = db.query(User).filter(User.email == body.email).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if user.is_verified:
        return {"message": "Already verified"}

    code = generate_verification_code()
    user.verification_code = code
    user.verification_code_expires_at = datetime.utcnow() + timedelta(minutes=CODE_EXPIRY_MINUTES)
    db.commit()

    send_verification_email(user.email, code)
    return {"message": "Code resent"}


@router.post("/login", response_model=Token)
def login(credentials: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.email == credentials.email).first()
    if not user or not verify_password(credentials.password, user.password):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    if not user.is_verified:
        raise HTTPException(status_code=403, detail="Please verify your email before logging in")
    return {"access_token": create_access_token({"sub": str(user.id), "role": user.role})}


@router.get("/{user_id}", response_model=UserOut)
def get_user(user_id: int, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user
