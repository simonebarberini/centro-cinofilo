import { CatalogModuleType, CatalogModuleActivationStatus } from '../catalog/catalog-module.model';
import { TenantModuleStatusValue } from '../subscriptions/tenant-module-status.model';

export interface TenantModuleViewModel {
  moduleKey: string;
  name: string;
  description: string;
  type: CatalogModuleType;
  catalogActivationStatus: CatalogModuleActivationStatus;
  tenantStatus: TenantModuleStatusValue | null;
  trialEndsAt: string | null;
}
