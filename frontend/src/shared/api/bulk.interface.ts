export interface IBulkFailure {
  id: string;
  reason: string;
}

export interface IBulkResult {
  succeeded: string[];
  failed: IBulkFailure[];
}
