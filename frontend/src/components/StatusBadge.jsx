const COLORS = {
  // project statuses
  PENDING:  'bg-gray-100 text-gray-600',
  CLONING:  'bg-yellow-100 text-yellow-700',
  BUILDING: 'bg-orange-100 text-orange-700',
  READY:    'bg-green-100 text-green-700',
  FAILED:   'bg-red-100 text-red-700',
  // execution statuses
  RUNNING:  'bg-blue-100 text-blue-700',
  SUCCESS:  'bg-green-100 text-green-700',
  STOPPED:  'bg-gray-100 text-gray-600',
  // report statuses
  GENERATING: 'bg-yellow-100 text-yellow-700',
}

export default function StatusBadge({ status }) {
  const cls = COLORS[status] ?? 'bg-gray-100 text-gray-500'
  const isActive = status === 'RUNNING' || status === 'CLONING' || status === 'BUILDING'
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium ${cls}`}>
      {isActive && (
        <span className="w-1.5 h-1.5 rounded-full bg-current animate-pulse" />
      )}
      {status}
    </span>
  )
}
