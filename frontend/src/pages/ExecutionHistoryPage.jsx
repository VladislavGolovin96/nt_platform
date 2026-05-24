import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { executionsApi } from '../api/executions'
import StatusBadge from '../components/StatusBadge'

export default function ExecutionHistoryPage() {
  const { data: executions = [], isLoading, error } = useQuery({
    queryKey: ['executions'],
    queryFn: executionsApi.list,
  })

  if (isLoading) return <div className="text-gray-500 p-6">Loading…</div>
  if (error) return <div className="text-red-500 p-6">Failed to load executions.</div>

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <h1 className="text-2xl font-semibold text-gray-800 mb-6">Execution History</h1>

      {executions.length === 0 ? (
        <div className="text-gray-500 bg-gray-50 border border-gray-200 rounded p-8 text-center">
          No executions yet.{' '}
          <Link to="/projects" className="text-blue-600 hover:underline">
            Launch a test from a project
          </Link>
        </div>
      ) : (
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200 text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-500">Simulation</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500">Type</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500">Status</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500">Duration</th>
                <th className="px-4 py-3 text-left font-medium text-gray-500">Started</th>
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {executions.map(exec => (
                <tr key={exec.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-xs text-gray-700 max-w-xs truncate">
                    {exec.simulationClass}
                  </td>
                  <td className="px-4 py-3 text-gray-600">{exec.testType}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={exec.status} />
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    {exec.durationMs != null
                      ? `${(exec.durationMs / 1000).toFixed(1)}s`
                      : '—'}
                  </td>
                  <td className="px-4 py-3 text-gray-500">
                    {exec.startedAt ? new Date(exec.startedAt).toLocaleString() : '—'}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Link
                      to={`/executions/${exec.id}`}
                      className="text-blue-600 hover:underline"
                    >
                      View
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
