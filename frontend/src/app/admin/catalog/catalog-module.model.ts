export type CatalogModuleType = 'BASE' | 'OPTIONAL';

export type CatalogModuleActivationStatus = 'INACTIVE' | 'ACTIVE' | 'DEPRECATED';

export interface CatalogModuleEntitlement {
  entitlementKey: string;
  boolValue: boolean | null;
  quotaValue: number | null;
}

export interface CatalogModule {
  moduleKey: string;
  name: string;
  description: string;
  type: CatalogModuleType;
  activationStatus: CatalogModuleActivationStatus;
  entitlements: CatalogModuleEntitlement[];
}
