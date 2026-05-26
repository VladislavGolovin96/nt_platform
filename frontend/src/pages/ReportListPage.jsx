import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { reportsApi } from '../api/reports'
import StatusBadge from '../components/StatusBadge'

export default function ReportListPage() {
  const { data: reports, isLoading, isError } = useQuery({
    queryKey: ['reports'],
    queryFn: reportsApi.list,
  })

  if (isLoading) {
    return <div className="p-6 text-gray-500">Loading reports…</div>
  }

  if (isError) {
    return <div className="p-6 text-red-500">Failed to load reports.</div>
  }

  return (
    <div className="p-6 max-w-4xl mx-auto space-y-4">
      <h1 className="text-xl font-semibold text-gray-800">Reports</h1>

      {!reports || reports.length === 0 ? (
        <p className="text-gray-500">No reports yet. Run a test to generate one.</p>
      ) : (
        <div className="bg-white border border-gray-200 rounded-lg divide-y">
          {reports.map(report => (
            <div key={report.id} className="flex items-center justify-between p-4 text-sm">
              <div className="min-w-0">
                <p className="font-mono text-gray-700 text-xs truncate">{report.executionId}</p>
                <p className="text-gray-400 mt-0.5">
                  {report.generatedAt
                    ? new Date(report.generatedAt).toLocaleString()
                    : report.createdAt
                    ? new Date(report.createdAt).toLocaleString()
                    : '—'}
                </p>
              </div>
              <div className="flex items-center gap-3 ml-4 shrink-0">
                <StatusBadge status={report.status} />
                <Link
                  to={`/reports/${report.executionId}`}
                  className="text-sm text-blue-600 hover:text-blue-800 border border-blue-200 px-3 py-1 rounded"
                >
                  View
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
