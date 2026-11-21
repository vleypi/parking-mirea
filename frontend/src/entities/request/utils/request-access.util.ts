import { ACTIVE_REQUEST_STATUSES } from "@/entities/request/constants/request.constant";
import type { IRequest } from "@/entities/request/interfaces/request.interface";

export const isRequestEditable = ({ archived, status }: IRequest): boolean => !archived && ACTIVE_REQUEST_STATUSES.includes(status.code);
