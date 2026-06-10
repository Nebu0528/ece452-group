from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import users, events, tags

app = FastAPI(title="DiscoverUWaterloo API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(users.router)
app.include_router(events.router)
app.include_router(tags.router)
