export interface GrantResponse {
  id: string;
  tenantId: string;
  moduleKey: string;
  grantedBy: string;
  note: string | null;
  createdAt: string;
}
