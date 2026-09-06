# NutriSnap 🥑📸

NutriSnap is an Android macro & meal tracking application powered by Gemini AI and Jetpack Compose. It offers real-time nutrition estimation from text descriptions and food photos, customizable daily macro targets, detailed analytics, PIN lock privacy, and local backup/export functionality.

---

## Features

- 🤖 **Gemini AI Meal Estimation**: Instant macro estimation (calories, protein, carbs, fats, fiber, sugar, meal type, and confidence notes) powered by Gemini 2.5 Flash via text or photo input.
- 🥗 **Smart Local Fallback**: Seamless offline heuristics for Asian, Filipino (e.g., Adobo, Sinigang, Silog plates), and Western everyday meals with clear AI vs. local provenance indicators.
- 📊 **Macro Dashboard & Weekly/Monthly Trends**: Track progress against customizable daily calorie and macronutrient targets.
- 🔒 **PIN Lock Privacy**: Optional 4-digit PIN lock with background inactivity timeout to keep health data private.
- 📁 **Data Backup & Restore**: Full JSON backup export and import preview to easily back up or transfer meal logs across devices.
- 🗄️ **Room Database Storage**: Robust SQLite persistence (`nutrisnap_database` version 2) with offline support.

---

## Setup & Gemini API Key Configuration

NutriSnap uses the Gradle Secrets plugin to inject the Gemini API key into `BuildConfig.GEMINI_API_KEY`.

1. Copy `.env.example` to `.env` in the repository root:
   ```bash
   cp .env.example .env
   ```
2. Open `.env` and insert your Gemini API Key from Google AI Studio:
   ```env
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
3. Custom API keys can also be saved directly inside the app under **Settings > Custom Gemini API Key** without committing keys to source control.

---

## Build & Run Instructions

### Prerequisites
- JDK 17 or JDK 21
- Android Studio Ladybug (or newer) / Android SDK 36

### Command Line Build
- **Run Unit & Robolectric Tests**:
  ```bash
  ./gradlew test
  ```
- **Build Debug APK**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Build Minified Release APK**:
  ```bash
  ./gradlew assembleRelease
  ```

---

## Architecture & Data Persistence Notes

- **Package**: `com.janreins.nutrisnap`
- **Application ID**: `com.janreins.nutrisnap`
- **Database**: Room database `nutrisnap_database` (version 2) stores `meals` and `macro_goals`.
- **Database Migrations**: Version 1 to Version 2 migration (`MIGRATION_1_2`) adds `fiberGrams`, `sugarGrams`, `mealType`, and `isAiEstimated` columns. Existing local meal logs are preserved across namespace refactoring.
