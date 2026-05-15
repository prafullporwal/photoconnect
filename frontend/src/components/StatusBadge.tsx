import type { InquiryStatus } from '../lib/types';

/**
 * Single visual representation of inquiry status, used in list and detail views.
 * Keeping this in one component means a future colour-scheme tweak (e.g. dark
 * mode, brand colours) only touches one file.
 */
export function StatusBadge({ status }: { status: InquiryStatus }) {
  const styles: Record<InquiryStatus, string> = {
    NEW:       'bg-blue-50  text-blue-700  ring-blue-200',
    READ:      'bg-amber-50 text-amber-700 ring-amber-200',
    RESPONDED: 'bg-green-50 text-green-700 ring-green-200',
    CLOSED:    'bg-gray-100 text-gray-600  ring-gray-200',
  };

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ring-inset ${styles[status]}`}
    >
      {status}
    </span>
  );
}
