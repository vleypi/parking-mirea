import { apiClient, type IPage } from "@/shared/api";
import type { IAuditEvent, IAuditListRequest } from "@/entities/audit/interfaces/audit.interface";

const AUDIT_PATH = "/admin/audit";

class AuditService {
  async getPage(request: IAuditListRequest): Promise<IPage<IAuditEvent>> {
    const { data } = await apiClient.get<IPage<IAuditEvent>>(AUDIT_PATH, { params: request });
    return data;
  }
}

export const auditService = new AuditService();
