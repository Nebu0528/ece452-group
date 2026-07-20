from datetime import datetime, timedelta
import threading

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.database import get_db
from app.email import generate_verification_code, send_verification_email
from app.models.invite import Invite
from app.models.user import User
from app.schemas.user import LoginRequest, Token, UserCreate, UserOut, VerifyEmailRequest
from app.auth import create_access_token, hash_password, verify_password

router = APIRouter(prefix="/users", tags=["users"])

CODE_EXPIRY_MINUTES = 10


def _send_email_background(email: str, code: str):
    try:
        send_verification_email(email, code)
    except Exception as e:
        print(f"[WARN] Failed to send verification email to {email}: {e}")
        print(f"[DEV] Verification code for {email}: {code}")


@router.post("/register", response_model=UserOut)
def register(user: UserCreate, db: Session = Depends(get_db)):
    if db.query(User).filter(User.email == user.email).first():
        raise HTTPException(status_code=400, detail="Email already registered")

    # Resolve role from invite token if provided
    role = UserRole.BASIC
    if user.invite_token:
        invite = db.query(Invite).filter(Invite.token == user.invite_token).first()
        if not invite:
            raise HTTPException(status_code=400, detail="Invalid invite code")
        if invite.used:
            raise HTTPException(status_code=400, detail="Invite code has already been used")
        if invite.expires_at < datetime.utcnow():
            raise HTTPException(status_code=400, detail="Invite code has expired")
        if invite.email != user.email:
            raise HTTPException(status_code=400, detail="Invite code is not for this email address")
        role = UserRole(invite.role)
        invite.used = True

    code = generate_verification_code()
    expires = datetime.utcnow() + timedelta(minutes=CODE_EXPIRY_MINUTES)

    db_user = User(
        name=user.name,
        email=user.email,
        password=hash_password(user.password),
        role=role,
        is_verified=False,
        verification_code=code,
        verification_code_expires_at=expires,
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)

    threading.Thread(target=_send_email_background, args=(db_user.email, code), daemon=True).start()

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

    threading.Thread(target=_send_email_background, args=(user.email, code), daemon=True).start()
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
