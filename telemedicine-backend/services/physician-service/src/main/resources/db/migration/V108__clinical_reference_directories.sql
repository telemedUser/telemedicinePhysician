-- Clinical reference directories for the D-08 care-plan dropdowns. DB-backed (not hardcoded) so the
-- same data can later be surfaced to the patient side. Three independent lookup tables, each with a
-- search endpoint in physician-service.
--   diagnosis_code   globally recognized ICD-10 codes + titles
--   medication       formulary of medications (name, strength, form)
--   diagnostic_test  catalogue of orderable tests (name, category, specimen)

CREATE TABLE IF NOT EXISTS diagnosis_code (
    code          VARCHAR(16) PRIMARY KEY,   -- ICD-10, e.g. J02.9
    title         VARCHAR(256) NOT NULL,
    system        VARCHAR(16)  NOT NULL DEFAULT 'ICD-10',
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_diagnosis_title ON diagnosis_code (lower(title));

CREATE TABLE IF NOT EXISTS medication (
    id        VARCHAR(64) PRIMARY KEY,
    name      VARCHAR(128) NOT NULL,   -- INN / generic name, e.g. Amoxicillin
    strength  VARCHAR(64),             -- e.g. 500 mg
    form      VARCHAR(64),             -- e.g. tablet, capsule, syrup
    active    BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_medication_name ON medication (lower(name));

CREATE TABLE IF NOT EXISTS diagnostic_test (
    id        VARCHAR(64) PRIMARY KEY,
    name      VARCHAR(160) NOT NULL,   -- e.g. Rapid strep antigen test
    category  VARCHAR(64),             -- e.g. Lab, Imaging, Point-of-care
    specimen  VARCHAR(64),             -- e.g. throat swab, blood
    active    BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_diagnostic_test_name ON diagnostic_test (lower(name));

-- ---------------------------------------------------------------------------
-- Seed: ICD-10 (a practical starter set across common primary-care presentations)
-- ---------------------------------------------------------------------------
INSERT INTO diagnosis_code (code, title) VALUES
    ('J02.9', 'Acute pharyngitis, unspecified'),
    ('J03.90', 'Acute tonsillitis, unspecified'),
    ('J06.9', 'Acute upper respiratory infection, unspecified'),
    ('J00',   'Acute nasopharyngitis (common cold)'),
    ('J01.90', 'Acute sinusitis, unspecified'),
    ('J20.9', 'Acute bronchitis, unspecified'),
    ('J45.909', 'Unspecified asthma, uncomplicated'),
    ('B34.9', 'Viral infection, unspecified'),
    ('R50.9', 'Fever, unspecified'),
    ('R05',   'Cough'),
    ('R51',   'Headache'),
    ('R07.0', 'Pain in throat'),
    ('R42',   'Dizziness and giddiness'),
    ('R11.2', 'Nausea with vomiting, unspecified'),
    ('A09',   'Infectious gastroenteritis and colitis, unspecified'),
    ('K21.9', 'Gastro-oesophageal reflux disease without oesophagitis'),
    ('K59.00', 'Constipation, unspecified'),
    ('N39.0', 'Urinary tract infection, site not specified'),
    ('L23.9', 'Allergic contact dermatitis, unspecified cause'),
    ('L50.9', 'Urticaria, unspecified'),
    ('H66.90', 'Otitis media, unspecified'),
    ('H10.9', 'Unspecified conjunctivitis'),
    ('M54.5', 'Low back pain'),
    ('M25.50', 'Pain in unspecified joint'),
    ('I10',   'Essential (primary) hypertension'),
    ('E11.9', 'Type 2 diabetes mellitus without complications'),
    ('F41.9', 'Anxiety disorder, unspecified'),
    ('F32.9', 'Major depressive disorder, single episode, unspecified'),
    ('G43.909', 'Migraine, unspecified, not intractable'),
    ('Z00.00', 'General adult medical examination without abnormal findings')
ON CONFLICT (code) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Seed: medications (common outpatient formulary)
-- ---------------------------------------------------------------------------
INSERT INTO medication (id, name, strength, form) VALUES
    ('med-amoxicillin-500',  'Amoxicillin',        '500 mg', 'capsule'),
    ('med-amoxiclav-625',    'Amoxicillin/Clavulanate', '625 mg', 'tablet'),
    ('med-azithromycin-500', 'Azithromycin',       '500 mg', 'tablet'),
    ('med-cephalexin-500',   'Cephalexin',         '500 mg', 'capsule'),
    ('med-doxycycline-100',  'Doxycycline',        '100 mg', 'capsule'),
    ('med-paracetamol-500',  'Paracetamol',        '500 mg', 'tablet'),
    ('med-paracetamol-1000', 'Paracetamol',        '1 g',    'tablet'),
    ('med-ibuprofen-400',    'Ibuprofen',          '400 mg', 'tablet'),
    ('med-diclofenac-50',    'Diclofenac',         '50 mg',  'tablet'),
    ('med-cetirizine-10',    'Cetirizine',         '10 mg',  'tablet'),
    ('med-loratadine-10',    'Loratadine',         '10 mg',  'tablet'),
    ('med-salbutamol-inh',   'Salbutamol',         '100 mcg','inhaler'),
    ('med-prednisolone-5',   'Prednisolone',       '5 mg',   'tablet'),
    ('med-omeprazole-20',    'Omeprazole',         '20 mg',  'capsule'),
    ('med-pantoprazole-40',  'Pantoprazole',       '40 mg',  'tablet'),
    ('med-ondansetron-4',    'Ondansetron',        '4 mg',   'tablet'),
    ('med-ors',              'Oral rehydration salts', NULL, 'sachet'),
    ('med-metformin-500',    'Metformin',          '500 mg', 'tablet'),
    ('med-amlodipine-5',     'Amlodipine',         '5 mg',   'tablet'),
    ('med-azithro-susp',     'Azithromycin',       '200 mg/5 ml', 'suspension')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Seed: diagnostic tests
-- ---------------------------------------------------------------------------
INSERT INTO diagnostic_test (id, name, category, specimen) VALUES
    ('test-strep-antigen', 'Rapid strep antigen test',       'Point-of-care', 'throat swab'),
    ('test-throat-culture','Throat swab culture',            'Lab',           'throat swab'),
    ('test-cbc',           'Complete blood count (CBC)',     'Lab',           'blood'),
    ('test-crp',           'C-reactive protein (CRP)',       'Lab',           'blood'),
    ('test-esr',           'Erythrocyte sedimentation rate', 'Lab',           'blood'),
    ('test-blood-glucose', 'Fasting blood glucose',          'Lab',           'blood'),
    ('test-hba1c',         'HbA1c',                          'Lab',           'blood'),
    ('test-lft',           'Liver function test',            'Lab',           'blood'),
    ('test-rft',           'Renal function test',            'Lab',           'blood'),
    ('test-urinalysis',    'Urinalysis',                     'Lab',           'urine'),
    ('test-urine-culture', 'Urine culture',                  'Lab',           'urine'),
    ('test-covid-rat',     'COVID-19 rapid antigen test',    'Point-of-care', 'nasal swab'),
    ('test-covid-pcr',     'COVID-19 RT-PCR',                'Lab',           'nasal swab'),
    ('test-chest-xray',    'Chest X-ray',                    'Imaging',       NULL),
    ('test-ecg',           'Electrocardiogram (ECG)',        'Point-of-care', NULL),
    ('test-stool-routine', 'Stool routine examination',      'Lab',           'stool'),
    ('test-thyroid',       'Thyroid function test (TSH)',    'Lab',           'blood'),
    ('test-lipid',         'Lipid profile',                  'Lab',           'blood'),
    ('test-dengue-ns1',    'Dengue NS1 antigen',             'Lab',           'blood'),
    ('test-malaria-smear', 'Malaria blood smear',            'Lab',           'blood')
ON CONFLICT (id) DO NOTHING;
