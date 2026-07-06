import os
import random
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

SMTP_HOST = os.getenv("SMTP_HOST", "smtp.gmail.com")
SMTP_PORT = int(os.getenv("SMTP_PORT", "587"))
SMTP_USER = os.getenv("SMTP_USER", "")
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD", "")
EMAIL_FROM = os.getenv("EMAIL_FROM", SMTP_USER)


def generate_verification_code() -> str:
    return f"{random.randint(0, 999999):06d}"


def send_verification_email(to_email: str, code: str) -> None:
    if not SMTP_USER or not SMTP_PASSWORD:
        print(f"[DEV] Verification code for {to_email}: {code}")
        return

    msg = MIMEMultipart()
    msg["From"] = EMAIL_FROM
    msg["To"] = to_email
    msg["Subject"] = "DiscoverUW – verify your email"
    msg.attach(MIMEText(
        f"Your verification code is: {code}\n\nIt expires in 10 minutes.",
        "plain"
    ))

    with smtplib.SMTP(SMTP_HOST, SMTP_PORT) as server:
        server.starttls()
        server.login(SMTP_USER, SMTP_PASSWORD)
        server.sendmail(EMAIL_FROM, to_email, msg.as_string())
