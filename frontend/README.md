# Frontend

Android app for DiscoverUWaterloo built with Jetpack Compose.

## Prerequisites
- Android Studio
- Android SDK 24+
- JDK 11+

## Setup

1. Open Android Studio → **File → Open** → select `frontend/DiscoverUWaterloo`
2. Wait for Gradle sync to complete (first sync downloads dependencies, takes a few minutes)
3. If you see a `kotlin.sourceSets DSL` warning, it is harmless — it's already suppressed in `gradle.properties`

## Running the app

### Emulator
- **Device Manager** → create a Pixel emulator (API 35) if you don't have one
- Hit the green **Run ▶** button
- The emulator reaches your local backend at `10.0.2.2:8000` automatically

## Connecting to the backend

The app points to `http://10.0.2.2:8000` in debug mode (emulator only).  
See `backend/README.md` for how to run the backend locally.

Test credentials (after running `python seed.py`):
| Email | Password | Role |
|---|---|---|
| student@uwaterloo.ca | student123 | Student |
| organizer@uwaterloo.ca | organizer123 | Organizer |
| admin@uwaterloo.ca | admin123 | Admin |

