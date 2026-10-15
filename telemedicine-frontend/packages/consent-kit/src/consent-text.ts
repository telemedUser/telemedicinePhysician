export default {
  platform: {
    version: '1.0.0',
    effectiveFrom: '2026-01-01',
    items: [
      {
        id: 'terms',
        titleKey: 'consent.platform.terms.title',
        bodyKey: 'consent.platform.terms.body',
        required: true,
      },
      {
        id: 'privacy',
        titleKey: 'consent.platform.privacy.title',
        bodyKey: 'consent.platform.privacy.body',
        required: true,
      },
    ],
  },
  clinical: {
    version: '1.0.0',
    effectiveFrom: '2026-01-01',
    procedureKey: 'consent.clinical.procedure',
    risksKeys: [
      'consent.clinical.risks.connection',
      'consent.clinical.risks.diagnosis',
      'consent.clinical.risks.emergency',
    ],
  },
} as const;
