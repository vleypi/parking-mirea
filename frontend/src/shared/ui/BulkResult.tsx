import type { IBulkResult } from "@/shared/api/bulk.interface";
import { UI_TEXT } from "@/shared/ui/ui.constant";

interface IBulkResultProps {
  result: IBulkResult;
  getLabel: (id: string) => string;
}

export const BulkResult = ({ result, getLabel }: IBulkResultProps) => (
  <div className="flex flex-col gap-3 text-sm">
    <p className="text-green-700">
      {UI_TEXT.bulkSucceeded}: {result.succeeded.length}
    </p>
    {result.failed.length > 0 && (
      <div>
        <p className="mb-2 text-red-700">
          {UI_TEXT.bulkFailed}: {result.failed.length}
        </p>
        <ul className="flex flex-col gap-1.5">
          {result.failed.map(({ id, reason }) => (
            <li key={id} className="rounded-lg bg-red-50 px-3 py-2">
              <span className="font-medium text-ink">{getLabel(id)}</span>
              <span className="text-red-700">: {reason}</span>
            </li>
          ))}
        </ul>
      </div>
    )}
  </div>
);
