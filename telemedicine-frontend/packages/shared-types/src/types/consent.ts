export type ConsentType = 'platform' | 'clinical';

export type ConsentStatus = 'pending' | 'granted' | 'withdrawn' | 'outdated';

export interface ConsentRecord {
  type: ConsentType;
  version: string;
  status: ConsentStatus;
  grantedAt?: string;
  withdrawnAt?: string;
}

export interface ConsentVersion {
  type: ConsentType;
  version: string;
  textKey: string;
  effectiveFrom: string;
}
