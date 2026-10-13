# Physician-Ready Medical JSON Schema & Export Modernization

Restructure the application's medical data export into a clean, physician-oriented JSON specification tailored for direct ingestion by clinical web dashboards, and upgrade the Android app's export pipeline to generate this format natively with clinical triage alerts.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were confirmed during requirements clarification:
> - **JSON Architecture Style**: Clean physician-oriented JSON with structured clinical sections (Patient Profile, Family Members, Medications, Vitals, Labs, Daily Logs) rather than raw internal DB dumps or heavy FHIR wrappers.
> - **Android Integration**: Update the app's native export engine (`HealthViewModel.kt` and export utilities) to output this modernized format in addition to providing the standalone reference JSON file.
> - **Clinical Triage Summary**: Integrate an automated clinical alert and triage header assessing hypertensive crisis, Stage 2 hypertension, hypoglycemia/hyperglycemia, and abnormal pulse rates for immediate physician scanning.
> - **Imaging Exclusion**: Strictly omit any X-ray, radiological, or imaging metadata fields (deferred to v2).

---

### 1. Overview & Core Concept

- **What It Does**: Standardizes exported patient medical records into an organized, physician-legible format. It replaces opaque Unix epoch timestamps with standardized ISO 8601 timestamps (`YYYY-MM-DD HH:mm:ss`), enriches vital sign readings with clinical context (e.g., fasting/post-prandial tags, measurement site, posture), categorizes medication compliance with explicit active/stopped status, and introduces an executive triage summary at the root level.
- **Target Audience**: Attending physicians, clinical nurse practitioners, and web dashboard backend developers consuming patient-generated health data (PGHD).
- **Key Value**: Eliminates the cognitive burden of parsing unstructured logs, highlights urgent physiological anomalies upfront, and ensures seamless backend schema compatibility.

---

### 2. User Experience & Data Hierarchy

- **Physician Dashboard Consumption Flow**:
  1. **Header & Triage Assessment**: The web dashboard first reads `clinical_triage_alerts` to flag urgent patient conditions (e.g., Blood Pressure >= 180/120 mmHg or Fasting Glucose < 70 mg/dL).
  2. **Patient Demographics & Family Context**: The dashboard displays the patient's primary profile alongside secondary family/dependent cards from `family_members`.
  3. **Medications & Treatment Plan**: Displays active prescriptions, dosage schedules, start/discontinuation dates, and prescribing doctors.
  4. **Longitudinal Vitals & Daily Logs**: Chronological streams for Blood Pressure, Blood Glucose, Sleep, Symptoms, and Lab Results with structured context metadata.

- **Data Presentation Strategy**:
  - Clear section separation (`schema_version`, `export_metadata`, `clinical_triage_alerts`, `patient_profile`, `family_members`, `medications`, `vital_signs`, `symptoms`, `laboratory_results`, `lifestyle_logs`).
  - Self-descriptive clinical units (e.g., `mmHg`, `mg/dL`, `bpm`, `hours`, `kg`, `cm`).
  - Standardized string enumerations for meal contexts (`fasting`, `pre_meal`, `post_meal`, `bedtime`, `random`) and medication statuses (`active`, `discontinued`, `paused`).

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Physician-First Structured JSON vs. FHIR R4 Bundle**
  - *Chosen Approach*: Lightweight, semantically structured JSON with intuitive clinical nomenclature.
  - *Why*: FHIR bundles introduce deep nested nesting (`resourceType`, `coding`, `system`, `entry`) that bloats payload size and requires specialized parser tooling. A clean domain model allows physician web dashboards to render charts and tables with zero parsing overhead.
  - *Alternatives Considered*: Pure FHIR JSON (rejected due to excessive complexity for patient exports).

- **Decision 2: Automated Triage Alert Computation**
  - *Chosen Approach*: Evaluate the latest vitals against AHA/ACC hypertension guidelines and ADA glycemic guidelines, injecting a `clinical_triage_alerts` object at the root of the JSON export.
  - *Why*: Provides immediate high-priority callouts for emergency blood pressures (Hypertensive Crisis / Stage 2) or dangerous glycemic excursions before the physician inspects hundreds of line items.

- **Decision 3: Dual-Mode Export Support in Android App**
  - *Chosen Approach*: Update the existing `exportProfileData`, `exportForDoctor`, and JSON generation routines in `HealthViewModel.kt` to emit the new clean schema while preserving existing local DB schemas.
  - *Why*: Guarantees that future exports generated directly from the Android UI match the new refined standard seamlessly.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   Room Database Layer                  │
│ (EmergencyInfo, FamilyMember, BP, Sugar, Meds, Labs)   │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│             Android Export Engine (HealthViewModel)    │
│  - Formats ISO 8601 Timestamps                         │
│  - Evaluates Clinical Triage Rules                     │
│  - Assembles Clean Physician-Oriented JSON Model       │
└───────────────────────────┬────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            ▼                               ▼
┌──────────────────────────────┐ ┌──────────────────────┐
│  doctor_patient_record.json  │ │ Android Native Share │
│  (Standalone clean file)     │ │ (JSON & WhatsApp)    │
└──────────────────────────────┘ └──────────────────────┘
```

#### Refined JSON Structure Outline:
```json
{
  "schema_version": "2.0.0",
  "export_metadata": {
    "generated_at": "YYYY-MM-DD HH:mm:ss",
    "application": "My Medical Journal",
    "export_purpose": "Physician Clinical Review"
  },
  "clinical_triage_alerts": {
    "status": "ATTENTION_REQUIRED | NORMAL",
    "high_priority_findings": [],
    "recent_vitals_summary": { }
  },
  "patient_profile": {
    "full_name": "...",
    "age": 42,
    "sex": "Male",
    "blood_type": "O+",
    "height_cm": 178.0,
    "weight_kg": 82.5,
    "bmi": 26.04,
    "chronic_conditions": [ ],
    "allergies": [ ],
    "emergency_contact": { }
  },
  "family_members": [ ],
  "medications": [ ],
  "vital_signs": {
    "blood_pressure": [ ],
    "blood_glucose": [ ],
    "heart_rate": [ ],
    "weight_logs": [ ]
  },
  "symptoms": [ ],
  "laboratory_results": [ ],
  "lifestyle_logs": {
    "sleep": [ ],
    "mood": [ ]
  }
}
```

#### Implementation Steps:
1. **Create Standalone Refined JSON File**: Generate `doctor_patient_record.json` populated with realistic, comprehensive patient history matching all prompt guidelines and omitting imaging fields.
2. **Update Android Export Functions in `HealthViewModel.kt`**:
   - Refactor `generateJSON()`, `exportProfileData()`, and `generateFullJSON()` to format timestamps via `SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())`.
   - Add clinical triage evaluation helper (`evaluateClinicalTriage()`) checking systolic/diastolic, glucose, and pulse ranges.
   - Restructure JSON output keys into the standardized physician sections.
3. **Compile & Verify**: Run `compile_applet` to verify that Android compilation succeeds with zero regressions.
