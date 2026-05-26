import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { reportsApi } from '../api/reports'
import StatusBadge from '../components/StatusBadge'

export default function ReportDetailPage() {
  const { executionId } = useParams()
  const [downloading, setDownloading] = useState(false)
  const [downloadError, setDownloadError] = useState(null)

  const { data: report, isLoading, isError } = useQuery({
    queryKey: ['report', executionId],
    queryFn: () => reportsApi.get(executionId),
    refetchInterval: query => {
      const status = query.state.data?.status
      return status === 'GENERATING' ? 3000 : false
    },
  })

  const handleDownload = async () => {
    setDownloading(true)
    setDownloadError(null)
    try {
      await reportsApi.download(executionId)
    } catch {
      setDownloadError('Failed to download report. Please try again.')
    } finally {
      setDownloading(false)
    }
  }

  if (isLoading) {
    return <div className="p-6 text-gray-500">Loading report…</div>
  }

  if (isError) {
    return (
      <div className="p-6 max-w-2xl mx-auto space-y-4">
        <p className="text-red-500">Report not found or not yet generated.</p>
        <Link to={`/executions/${executionId}`} className="text-sm text-blue-600 hover:underline">
          ← Back to Execution
        </Link>
      </div>
    )
  }

  return (
    <div className="p-6 max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-xl font-semibold text-gray-800">Test Report</h1>
          <p className="text-xs text-gray-400 mt-1 font-mono">{executionId}</p>
        </div>
        {report && <StatusBadge status={report.status} />}
      </div>

      {/* Metadata */}
      {report && (
        <div className="bg-white border border-gray-200 rounded-lg p-4 text-sm space-y-3">
          {report.generatedAt && (
            <div>
              <span className="text-gray-500">Generated</span>
              <p className="font-medium">{new Date(report.generatedAt).toLocaleString()}</p>
            </div>
          )}
          {report.status === 'GENERATING' && (
            <div className="flex items-center gap-2 text-yellow-600">
              <span className="w-1.5 h-1.5 rounded-full bg-yellow-500 animate-pulse inline-block" />
              PDF is being generated, this may take a moment…
            </div>
          )}
          {report.status === 'FAILED' && (
            <p className="text-red-500">Report generation failed.</p>
          )}
        </div>
      )}

      {/* Actions */}
      <div className="flex flex-wrap gap-3">
        <Link
          to={`/executions/${executionId}`}
          className="text-sm text-blue-600 hover:text-blue-800 border border-blue-200 px-3 py-1.5 rounded"
        >
          ← Back to Execution
        </Link>

        {report?.status === 'READY' && (
          <button
            onClick={handleDownload}
            disabled={downloading}
            className="text-sm bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white px-4 py-1.5 rounded"
          >
            {downloading ? 'Downloading…' : 'Download PDF'}
          </button>
        )}
      </div>

      {downloadError && (
        <p className="text-sm text-red-500">{downloadError}</p>
      )}
    </div>
  )
}
