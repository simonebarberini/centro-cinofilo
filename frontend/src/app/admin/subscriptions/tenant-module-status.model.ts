export type TenantModuleStatusValue = 'ACTIVE' | 'TRIAL' | 'CANCELLED';

export interface TenantModuleStatusItem {
  moduleKey: string;
  status: TenantModuleStatusValue;
  trialEndsAt: string | null;
}
