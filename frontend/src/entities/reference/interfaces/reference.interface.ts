export interface ICodeTitle {
  code: string;
  title: string;
}

export interface ISpotType {
  id: number;
  code: string;
  title: string;
  active: boolean;
  sortOrder: number;
}

export interface IReferenceCategory {
  id: number;
  code: string;
  title: string;
  active: boolean;
}

export interface IReferences {
  spotTypes: ISpotType[];
  requestStatuses: ICodeTitle[];
  clientCategories: IReferenceCategory[];
  ownerStatuses: ICodeTitle[];
  vehicleStatuses: ICodeTitle[];
  spotStatuses: ICodeTitle[];
}

export interface ISpotTypePayload {
  code: string;
  title: string;
  sortOrder: number | null;
}
