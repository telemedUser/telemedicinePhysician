/*
 * Dummy data for review/demo (Indian locale). Used to populate the screens when a backend
 * section is not connected.
 */
import type {
  AvailabilityWindow,
  ProviderDetail,
  ProviderSummary,
  WorklistEntry,
} from '../api/types';

export const CURRENT_PHYSICIAN = {
  id: 'prov-aks',
  name: 'Dr. Arjun Khanna',
  initials: 'AK',
  specialty: 'General Medicine',
  clinic: 'Apollo Clinic, Bengaluru',
};

export const MOCK_WORKLIST: WorklistEntry[] = [
  {
    appointmentId: 'AP-20931',
    patientRef: 'Priya Sharma',
    mode: 'video',
    enteredAt: new Date(Date.now() - 9 * 60_000).toISOString(),
    waitingSeconds: 9 * 60 + 12,
    readiness: 'READY',
    consultationStatus: 'CHECKED_IN',
    technicalStatus: 'OK',
    telemetryAvailable: true,
    flags: ['FOLLOW_UP'],
    redFlagAcknowledged: false,
  },
  {
    appointmentId: 'AP-20944',
    patientRef: 'Rohan Verma',
    mode: 'video',
    enteredAt: new Date(Date.now() - 6 * 60_000).toISOString(),
    waitingSeconds: 6 * 60 + 40,
    readiness: 'READY',
    consultationStatus: 'CHECKED_IN',
    technicalStatus: 'OK',
    telemetryAvailable: false,
    flags: ['FIRST_VISIT', 'RED_FLAG'],
    redFlagAcknowledged: false,
  },
  {
    appointmentId: 'AP-20950',
    patientRef: 'Lakshmi Iyer',
    mode: 'audio',
    enteredAt: new Date(Date.now() - 3 * 60_000).toISOString(),
    waitingSeconds: 3 * 60 + 5,
    readiness: 'READY',
    consultationStatus: 'CHECKED_IN',
    technicalStatus: 'OK',
    telemetryAvailable: false,
    flags: ['GUEST'],
    redFlagAcknowledged: false,
  },
  {
    appointmentId: 'AP-20962',
    patientRef: 'Aarav Nair (minor)',
    mode: 'video',
    enteredAt: new Date(Date.now() - 2 * 60_000).toISOString(),
    waitingSeconds: 2 * 60 + 18,
    readiness: 'IN_PROGRESS',
    consultationStatus: 'CHECKED_IN',
    technicalStatus: 'OK',
    telemetryAvailable: false,
    flags: ['FIRST_VISIT'],
    redFlagAcknowledged: false,
  },
  {
    appointmentId: 'AP-20977',
    patientRef: 'Vikram Reddy',
    mode: 'video',
    enteredAt: new Date(Date.now() - 1 * 60_000).toISOString(),
    waitingSeconds: 48,
    readiness: 'NOT_READY',
    consultationStatus: 'CHECKED_IN',
    technicalStatus: 'IMPAIRED',
    telemetryAvailable: false,
    flags: ['FIRST_VISIT'],
    redFlagAcknowledged: false,
  },
];

export interface MockPatientContext {
  name: string;
  rut: string; // ABHA / national health id
  age: number;
  sex: string;
  insurance: string;
  reason: string;
  readiness: string;
  medications: string[];
  allergies: string[];
  conditions: string[];
  lastVisit?: string;
  redFlag?: string;
  /** Dependant journeys: who holds consent / acts for the patient. */
  actsFor?: string;
  /** Follow-up journeys: linked prior encounter. */
  linkedEncounter?: string;
  noCharge?: boolean;
}

export const MOCK_CONTEXTS: Record<string, MockPatientContext> = {
  'AP-20931': {
    name: 'Priya Sharma',
    rut: 'ABHA 14-2847-7312-0045',
    age: 34,
    sex: 'Female',
    insurance: 'Star Health · Family Floater',
    reason: 'Follow-up check — throat infection treated on 04 Sep',
    readiness: 'Ready · device check passed 16:34',
    medications: ['Amoxicillin 500mg q8h', 'Paracetamol 650mg PRN'],
    allergies: ['No known allergies'],
    conditions: ['Allergic rhinitis'],
    lastVisit: '04 Sep 2026 · teleconsultation',
    linkedEncounter: 'Linked to the 04 Sep consultation · same physician · no charge',
    noCharge: true,
  },
  'AP-20944': {
    name: 'Rohan Verma',
    rut: 'ABHA 19-7720-1990-0231',
    age: 41,
    sex: 'Male',
    insurance: 'HDFC ERGO · Optima Secure',
    reason: 'Oppressive chest pain for 2 h, radiating to the left arm',
    readiness: 'Ready · red flag must be acknowledged before joining',
    medications: ['Losartan 50mg/day', 'Aspirin 75mg/day'],
    allergies: ['Penicillin'],
    conditions: ['Hypertension', 'Dyslipidemia'],
    lastVisit: '12 Jun 2026 · in person',
    redFlag: 'Possible acute coronary syndrome — emergency guidance shown to the patient',
  },
  'AP-20950': {
    name: 'Lakshmi Iyer',
    rut: 'ABHA 13-0095-5140-0098',
    age: 52,
    sex: 'Female',
    insurance: 'Self-pay',
    reason: 'Call-centre booking — no online intake',
    readiness: 'Ready 16:56 · join by audio',
    medications: ['Metformin 850mg q12h'],
    allergies: ['No known allergies'],
    conditions: ['Type 2 diabetes mellitus'],
  },
  'AP-20962': {
    name: 'Aarav Nair (minor, 7 yrs)',
    rut: 'ABHA 26-5519-0810-0176',
    age: 7,
    sex: 'Male',
    insurance: 'Niva Bupa · ReAssure',
    reason: 'Cough and fever 38.4°C for 2 days — reported by the mother',
    readiness: 'Preparing · consents held by Priya Nair (mother)',
    medications: ['None'],
    allergies: ['No known allergies'],
    conditions: ['Healthy'],
    actsFor: 'Priya Nair (mother) · holds the consents and verified the OTP',
  },
  'AP-20977': {
    name: 'Vikram Reddy',
    rut: 'ABHA 20-1183-3050-0310',
    age: 29,
    sex: 'Male',
    insurance: 'ICICI Lombard · Complete Health',
    reason: 'Lower-back pain after physical exertion',
    readiness: 'Not ready · camera denied — device help sent',
    medications: ['None'],
    allergies: ['No known allergies'],
    conditions: ['No prior history'],
  },
};

export const MOCK_TRANSCRIPT: { from: 'physician' | 'patient'; text: string; at: string }[] = [
  { from: 'patient', text: 'Good afternoon doctor, I feel better than last week.', at: '16:41' },
  { from: 'physician', text: 'Hi Priya. Glad to hear it. Has the fever fully gone?', at: '16:41' },
  { from: 'patient', text: 'Yes, three days without fever and my throat barely bothers me now.', at: '16:42' },
  { from: 'physician', text: 'Great. Let’s review the vital-signs measurement you just took.', at: '16:43' },
];

export const MOCK_VITALS = {
  heartRate: 72,
  spo2: 98,
  respiratoryRate: 15,
  stress: 'Low',
  measuredAt: '16:44',
};

export const MOCK_PROVIDERS: ProviderSummary[] = [
  {
    providerId: 'prov-aks',
    displayName: 'Dr. Arjun Khanna',
    status: 'ACTIVE',
    specialties: ['General Medicine'],
    languages: ['en', 'hi'],
    modes: ['video', 'audio', 'chat'],
  },
  {
    providerId: 'prov-mnp',
    displayName: 'Dr. Meera Nair',
    status: 'ACTIVE',
    specialties: ['Pediatrics'],
    languages: ['en', 'ml'],
    modes: ['video', 'audio'],
  },
  {
    providerId: 'prov-sgp',
    displayName: 'Dr. Sanjay Gupta',
    status: 'ON_LEAVE',
    specialties: ['Cardiology'],
    languages: ['en', 'hi'],
    modes: ['video'],
  },
  {
    providerId: 'prov-ard',
    displayName: 'Dr. Ananya Rao',
    status: 'ACTIVE',
    specialties: ['Dermatology'],
    languages: ['en', 'kn'],
    modes: ['video', 'chat'],
  },
  {
    providerId: 'prov-ism',
    displayName: 'Dr. Imran Shaikh',
    status: 'SUSPENDED',
    specialties: ['Internal Medicine'],
    languages: ['en', 'hi'],
    modes: ['video', 'audio'],
  },
];

const WEEK_AVAIL: AvailabilityWindow[] = [1, 2, 3, 4, 5].map((d) => ({
  dayOfWeek: d,
  startTime: '09:00',
  endTime: '18:00',
  mode: 'video',
  maxConcurrent: 1,
}));

export const MOCK_PROVIDER_DETAILS: Record<string, ProviderDetail> = {
  'prov-aks': {
    providerId: 'prov-aks',
    displayName: 'Dr. Arjun Khanna',
    status: 'ACTIVE',
    specialties: ['General Medicine'],
    languages: ['en', 'hi'],
    modes: ['video', 'audio', 'chat'],
    credentials: [
      { type: 'NMC registration', number: 'NMC-KA-884217', validUntil: '2028-03-31', verified: true },
      { type: 'Specialty certificate (MD)', number: 'MD-GM-5521', validUntil: '2027-11-30', verified: true },
    ],
    availability: WEEK_AVAIL,
    metrics: {
      utilization: 0.78,
      avgDurationSeconds: 17 * 60,
      avgDocumentationSeconds: 4 * 60,
      noShowRate: 0.06,
      technicalFailureRate: 0.02,
    },
    sourceRef: 'his://practitioner/884217',
    lastRefreshedAt: new Date(Date.now() - 4 * 60_000).toISOString(),
    stale: false,
  },
  'prov-mnp': {
    providerId: 'prov-mnp',
    displayName: 'Dr. Meera Nair',
    status: 'ACTIVE',
    specialties: ['Pediatrics'],
    languages: ['en', 'ml'],
    modes: ['video', 'audio'],
    credentials: [
      { type: 'NMC registration', number: 'NMC-KL-771004', validUntil: '2029-06-30', verified: true },
    ],
    availability: WEEK_AVAIL.map((w) => ({ ...w, maxConcurrent: 2 })),
    metrics: {
      utilization: 0.84,
      avgDurationSeconds: 21 * 60,
      avgDocumentationSeconds: 5 * 60,
      noShowRate: 0.04,
      technicalFailureRate: 0.03,
    },
    sourceRef: 'his://practitioner/771004',
    lastRefreshedAt: new Date(Date.now() - 11 * 60_000).toISOString(),
    stale: false,
  },
  'prov-sgp': {
    providerId: 'prov-sgp',
    displayName: 'Dr. Sanjay Gupta',
    status: 'ON_LEAVE',
    specialties: ['Cardiology'],
    languages: ['en', 'hi'],
    modes: ['video'],
    credentials: [
      { type: 'Specialty certificate (DM)', number: 'DM-CAR-2290', validUntil: '2026-01-31', verified: true },
    ],
    availability: [],
    metrics: {
      utilization: 0.0,
      avgDurationSeconds: 26 * 60,
      avgDocumentationSeconds: 6 * 60,
      noShowRate: 0.05,
      technicalFailureRate: 0.02,
    },
    sourceRef: 'his://practitioner/2290',
    lastRefreshedAt: new Date(Date.now() - 60 * 60_000).toISOString(),
    stale: true,
  },
  'prov-ard': {
    providerId: 'prov-ard',
    displayName: 'Dr. Ananya Rao',
    status: 'ACTIVE',
    specialties: ['Dermatology'],
    languages: ['en', 'kn'],
    modes: ['video', 'chat'],
    credentials: [
      { type: 'NMC registration', number: 'NMC-KA-660912', validUntil: '2028-09-30', verified: true },
    ],
    availability: WEEK_AVAIL.filter((w) => w.dayOfWeek <= 3),
    metrics: {
      utilization: 0.71,
      avgDurationSeconds: 15 * 60,
      avgDocumentationSeconds: 3 * 60,
      noShowRate: 0.08,
      technicalFailureRate: 0.01,
    },
    sourceRef: 'his://practitioner/660912',
    lastRefreshedAt: new Date(Date.now() - 7 * 60_000).toISOString(),
    stale: false,
  },
  'prov-ism': {
    providerId: 'prov-ism',
    displayName: 'Dr. Imran Shaikh',
    status: 'SUSPENDED',
    specialties: ['Internal Medicine'],
    languages: ['en', 'hi'],
    modes: ['video', 'audio'],
    credentials: [
      { type: 'Specialty certificate (MD)', number: 'MD-MI-1180', validUntil: '2024-12-31', verified: false },
    ],
    availability: [],
    metrics: {
      utilization: 0.0,
      avgDurationSeconds: 19 * 60,
      avgDocumentationSeconds: 7 * 60,
      noShowRate: 0.11,
      technicalFailureRate: 0.04,
    },
    sourceRef: 'his://practitioner/1180',
    lastRefreshedAt: new Date(Date.now() - 240 * 60_000).toISOString(),
    stale: true,
  },
};

/* ---- Wrap-up: AI draft, send documents, worklist-after ---- */

export interface AiDraft {
  soap: { s: string; o: string; a: string; p: string };
  summary: string;
  instructions: string;
  model: string;
  version: string;
}

export const MOCK_AI_DRAFT: Record<string, AiDraft> = {
  'AP-20931': {
    soap: {
      s: 'Patient reports resolution of sore throat and fever. Afebrile for 72 h. Good oral tolerance.',
      o: 'Camera-based vital signs within range: HR 72, SpO₂ 98%. No respiratory distress.',
      a: 'Acute pharyngitis (J02.9) resolving. Favourable progression.',
      p: 'Continue hydration. Paracetamol PRN. Discharge, no follow-up unless symptoms return.',
    },
    summary:
      'Teleconsultation follow-up for throat infection treated on 04 Sep. Favourable progression, afebrile. Discharge indicated.',
    instructions:
      'Keep hydrated. If fever > 38°C returns for more than 48 h, book a new consultation.',
    model: 'clinical-scribe',
    version: 'v2.3',
  },
};

export type DocStatus = 'DRAFT' | 'READY' | 'SENDING' | 'CONFIRMED' | 'QUEUED' | 'ERROR';

export interface SendableDoc {
  id: string;
  type: string;
  label: string;
  system: string;
  icon: string;
  status: DocStatus;
  detail?: string;
  required?: boolean;
}

export function sendDocsFor(appointmentId: string): SendableDoc[] {
  if (appointmentId === 'AP-20931') {
    return [
      { id: 'note', type: 'NOTE', label: 'Consultation note', system: 'Clinical records', icon: 'clinical_notes', status: 'READY', required: true },
      { id: 'dx', type: 'DIAGNOSIS', label: 'Diagnosis · J02.9', system: 'HIS', icon: 'coronavirus', status: 'READY', required: true },
      { id: 'rx', type: 'PRESCRIPTION', label: 'Prescription · Paracetamol 650mg', system: 'e-Pharmacy', icon: 'prescriptions', status: 'READY' },
      { id: 'instr', type: 'INSTRUCTIONS', label: 'Patient instructions', system: 'Platform', icon: 'description', status: 'READY' },
      { id: 'fup', type: 'FOLLOW_UP', label: 'Follow-up (optional)', system: 'Platform', icon: 'event_repeat', status: 'DRAFT' },
    ];
  }
  return [
    { id: 'note', type: 'NOTE', label: 'Consultation note', system: 'Clinical records', icon: 'clinical_notes', status: 'READY', required: true },
    { id: 'dx', type: 'DIAGNOSIS', label: 'Diagnosis', system: 'HIS', icon: 'coronavirus', status: 'DRAFT', required: true },
    { id: 'rx', type: 'PRESCRIPTION', label: 'Prescription', system: 'e-Pharmacy', icon: 'prescriptions', status: 'DRAFT' },
    { id: 'leave', type: 'LEAVE', label: 'Medical leave', system: 'HR portal', icon: 'event_busy', status: 'DRAFT' },
  ];
}

/** Worklist after the consultation — completed + at-risk rows. */
export const MOCK_WORKLIST_AFTER: WorklistEntry[] = [
  {
    appointmentId: 'AP-20931',
    patientRef: 'Priya Sharma',
    mode: 'video',
    enteredAt: new Date(Date.now() - 30 * 60_000).toISOString(),
    waitingSeconds: 0,
    readiness: 'READY',
    consultationStatus: 'CLOSED',
    technicalStatus: 'OK',
    telemetryAvailable: true,
    flags: ['FOLLOW_UP'],
    redFlagAcknowledged: true,
  },
  {
    appointmentId: 'AP-20977',
    patientRef: 'Vikram Reddy',
    mode: 'video',
    enteredAt: new Date(Date.now() - 4 * 60_000).toISOString(),
    waitingSeconds: 4 * 60 + 20,
    readiness: 'NOT_READY',
    consultationStatus: 'CHECKED_IN',
    technicalStatus: 'IMPAIRED',
    telemetryAvailable: false,
    flags: ['FIRST_VISIT'],
    redFlagAcknowledged: false,
  },
];

/* ---- Previous (completed) patients: closed consultations with notes & prescriptions ---- */

export interface PrescriptionLine {
  drug: string;
  dosage: string;
  frequency: string;
  duration: string;
}

export interface PreviousPatient {
  appointmentId: string;
  patientName: string;
  abhaId: string;
  age: number;
  sex: string;
  mode: 'video' | 'audio' | 'chat';
  consultedOn: string; // ISO
  durationMinutes: number;
  diagnosis: string;
  diagnosisCode: string;
  consultationNote: string;
  prescription: PrescriptionLine[];
  followUp?: string;
  outcome: 'COMPLETED' | 'CONVERTED_IN_PERSON' | 'NO_SHOW';
}

export const MOCK_PREVIOUS_PATIENTS: PreviousPatient[] = [
  {
    appointmentId: 'AP-20890',
    patientName: 'Ananya Desai',
    abhaId: 'ABHA 11-2048-7781-0012',
    age: 28,
    sex: 'Female',
    mode: 'video',
    consultedOn: new Date(Date.now() - 2 * 24 * 60 * 60_000).toISOString(),
    durationMinutes: 14,
    diagnosis: 'Acute viral upper respiratory infection',
    diagnosisCode: 'J06.9',
    consultationNote:
      'Patient presented with 3 days of sore throat, mild fever and nasal congestion. No breathing difficulty. Throat mildly erythematous on camera exam. Advised symptomatic care and hydration.',
    prescription: [
      { drug: 'Paracetamol 650mg', dosage: '1 tablet', frequency: 'TDS (PRN fever)', duration: '5 days' },
      { drug: 'Cetirizine 10mg', dosage: '1 tablet', frequency: 'OD at night', duration: '5 days' },
    ],
    followUp: 'Review in 5 days if symptoms persist',
    outcome: 'COMPLETED',
  },
  {
    appointmentId: 'AP-20875',
    patientName: 'Karthik Menon',
    abhaId: 'ABHA 17-5521-3390-0087',
    age: 45,
    sex: 'Male',
    mode: 'video',
    consultedOn: new Date(Date.now() - 4 * 24 * 60 * 60_000).toISOString(),
    durationMinutes: 22,
    diagnosis: 'Type 2 diabetes mellitus — routine review',
    diagnosisCode: 'E11.9',
    consultationNote:
      'Known T2DM on metformin. Reports good adherence. Fasting sugar logs reviewed (avg 128 mg/dL). No hypoglycaemic episodes. Continue current regimen, reinforce diet and walking.',
    prescription: [
      { drug: 'Metformin 1000mg', dosage: '1 tablet', frequency: 'BD after meals', duration: '90 days' },
      { drug: 'Atorvastatin 10mg', dosage: '1 tablet', frequency: 'OD at night', duration: '90 days' },
    ],
    followUp: 'HbA1c + lipid profile in 3 months',
    outcome: 'COMPLETED',
  },
  {
    appointmentId: 'AP-20861',
    patientName: 'Fatima Qureshi',
    abhaId: 'ABHA 13-9087-1120-0054',
    age: 33,
    sex: 'Female',
    mode: 'audio',
    consultedOn: new Date(Date.now() - 6 * 24 * 60 * 60_000).toISOString(),
    durationMinutes: 11,
    diagnosis: 'Migraine without aura',
    diagnosisCode: 'G43.0',
    consultationNote:
      'Recurrent unilateral throbbing headaches, 2–3 per month, photophobia. No red-flag neurological features. Advised trigger diary and acute abortive therapy.',
    prescription: [
      { drug: 'Naproxen 250mg', dosage: '1 tablet', frequency: 'at onset, repeat after 8h PRN', duration: 'PRN' },
      { drug: 'Domperidone 10mg', dosage: '1 tablet', frequency: 'with naproxen if nausea', duration: 'PRN' },
    ],
    followUp: 'Neurology referral if frequency increases',
    outcome: 'COMPLETED',
  },
  {
    appointmentId: 'AP-20848',
    patientName: 'Rajesh Pillai',
    abhaId: 'ABHA 20-3341-6650-0190',
    age: 58,
    sex: 'Male',
    mode: 'video',
    consultedOn: new Date(Date.now() - 9 * 24 * 60 * 60_000).toISOString(),
    durationMinutes: 18,
    diagnosis: 'Suspected cardiac chest pain — converted to in-person',
    diagnosisCode: 'R07.9',
    consultationNote:
      'Exertional chest tightness with risk factors (HTN, smoker). Clinically unsuitable for virtual management. Converted to in-person; advised to attend ED and arranged urgent cardiology review.',
    prescription: [
      { drug: 'Aspirin 325mg', dosage: '1 tablet', frequency: 'STAT (chewed)', duration: 'single dose' },
    ],
    followUp: 'Urgent in-person cardiology / ED',
    outcome: 'CONVERTED_IN_PERSON',
  },
  {
    appointmentId: 'AP-20912',
    patientName: 'Priya Sharma',
    abhaId: 'ABHA 14-2847-7312-0045',
    age: 34,
    sex: 'Female',
    mode: 'video',
    consultedOn: new Date(Date.now() - 1 * 24 * 60 * 60_000).toISOString(),
    durationMinutes: 12,
    diagnosis: 'Acute pharyngitis (resolving)',
    diagnosisCode: 'J02.9',
    consultationNote:
      'Follow-up of treated pharyngitis. Afebrile 72h, symptoms nearly resolved. Discharged, no further antibiotics required.',
    prescription: [
      { drug: 'Paracetamol 650mg', dosage: '1 tablet', frequency: 'PRN fever', duration: '3 days' },
    ],
    followUp: 'Return only if symptoms recur',
    outcome: 'COMPLETED',
  },
];

/**
 * Guided flow points shown in the consultation chat panel so a physician can see, end to end,
 * what this service lets them do.
 */
export const CONSULTATION_FLOW_POINTS: { step: number; text: string }[] = [
  { step: 1, text: 'Worklist → review who is waiting, their readiness, mode and any red flags.' },
  { step: 2, text: 'Acknowledge any red flag before you can admit that patient.' },
  { step: 3, text: 'Open "Context" to see demographics, reason, medications, allergies and consents.' },
  { step: 4, text: 'Admit the patient → the consultation opens (status becomes OPEN_DOCTOR).' },
  { step: 5, text: 'Use clinical chat here; request camera vitals; turn on the AI assistant (two consents).' },
  { step: 6, text: 'Start the live transcript once both parties consent to the AI assistant.' },
  { step: 7, text: 'End & review → accept or reject the AI draft note.' },
  { step: 8, text: 'Send documents → each confirmed by its receiving system (note, Rx, leave…).' },
  { step: 9, text: 'Close the consultation → all documents confirmed, or close with an audited exception.' },
  { step: 10, text: 'Previous patients → view closed consultations with their notes and prescriptions.' },
];
