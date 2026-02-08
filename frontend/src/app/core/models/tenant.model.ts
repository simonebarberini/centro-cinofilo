export interface TenantSettings {
  name: string;
  slug: string;
  capacityBoxes: number;
  preferences: Record<string, any>;
}

export interface UpdateTenantSettingsRequest {
  capacityBoxes?: number;
  preferences?: Record<string, any>;
}
