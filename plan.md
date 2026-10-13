# KMH Health App - Feature Expansion Plan

This document outlines the strategic roadmap for expanding the KMH Health app, categorizing new features into Free and Premium (Subscription) tiers to maximize user acquisition while building a sustainable revenue model.

## 🟢 Free Tier (Core Value & User Acquisition)
The free tier focuses on manual tracking, basic integrations, and essential tools to keep users engaged daily.

*   **Android Health Connect API (Basic):** Upgrade from the deprecated Google Fit API to Android's unified Health Connect API for seamless background syncing of basic metrics (steps, active minutes, and sleep sessions) from various wearables.
*   **Basic 30-Day Trend Analytics:** Visual charts showing 30-day trends for blood pressure, heart rate, and activity to help users see their basic progress.
*   **Smart Pill Reminders:** Simple push notifications reminding users to take their daily medications on time.
*   **Children & Baby Records:** Basic scheduling and logging for child vaccinations, growth metrics, and baby records.
*   **Manual Data Logging:** Continue offering robust manual entry for blood pressure, weight, sleep, and lifestyle habits.

## 🟡 Premium Tier (Advanced Insights, AI, & Caregiver Tools)
The premium tier targets power users, chronic condition managers, and families who need automation, deep insights, and medical-grade reporting.

*   **Doctor-Ready PDF & CSV Reports:** 
    *   One-click generation of professional PDF and CSV reports containing health trends and logs, formatted specifically for physician review.
*   **Caregiver & Family Alerts (Cloud Backup & FCM):** 
    *   Real-time Firebase Cloud Messaging (FCM) alerts sent to designated family members or a doctor's companion app if a patient logs a dangerously high/low blood pressure reading or misses critical medication. Includes secure cloud backup of all medical records.
*   **AI Conversational Health Assistant & Coach (Gemini AI):** 
    *   An interactive chat interface powered by Gemini 3.0 Flash. 
    *   Provides personalized daily nutrition, recovery, and lifestyle recommendations based on the user's specific health data.
*   **Automated Correlation Analysis:** 
    *   Advanced AI analysis that connects the dots (e.g., "Your blood pressure is 15% higher on days you get less than 6 hours of sleep").
*   **Lab Report OCR Scanner:** 
    *   Use device camera and AI OCR to scan printed blood test results or lab reports, automatically extracting and logging the biomarkers into the app's database.
*   **Advanced Wearable Sync (Direct BLE for Medical Devices):** 
    *   Direct Bluetooth Low Energy (BLE) integrations with medical-grade devices (e.g., Omron Blood Pressure cuffs) using IEEE 11073 standard profiles, bypassing the need for third-party brand apps.
*   **Smart Medication Inventory Tracker:** 
    *   Tracks how many pills are left in the bottle and sends automated alerts when it's time to request a refill from the pharmacy.

## 🛠️ Implementation Strategy & Architecture
*   **AI Integration:** Utilize the Gemini API for all conversational and analytical AI features, ensuring data is passed securely and without PII where possible.
*   **Alerting System:** Implement Firebase Cloud Messaging (FCM) to handle push notifications between the patient app and the caregiver/doctor receiving endpoints.
*   **Medical Data Import/Export:** Implement standard JSON/CSV import/export schemas and explore FHIR (Fast Healthcare Interoperability Resources) standards to allow doctors to securely push or pull complete patient histories.
