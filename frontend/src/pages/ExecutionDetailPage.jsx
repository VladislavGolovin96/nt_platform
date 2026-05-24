import { useEffect, useRef, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { executionsApi } from '../api/executions'
import StatusBadge from '../components/StatusBadge'
import TerminalLog from '../components/TerminalLog'

const ACTIVE_STATUSES = ['PENDING', 'RUNNING']
const GRAFANA_BASE = 'http://localhost:3000'
const GRAFANA_DASHBOARD_ID = 'gatling-tests'

export default function ExecutionDetailPage() {
  const { id } = useParams()
  const queryClient = useQueryClient()
  const [lines, setLines] = useState([])
  const sseRef = useRef(null)

  const { data: execution } = useQuery({
    queryKey: ['execution', id],
    queryFn: () => executionsApi.get(id),
    refetchInterval: query => {
      const status = query.state.data?.status
      return status && ACTIVE_STATUSES.includes(status) ? 3000 : false
    },
  })

  const stopMutation = useMutation({
    mutationFn: () => executionsApi.stop(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['execution', id] })
    },
  })

  // SSE live log streaming via fetch (EventSource doesn't support Authorization header)
  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) return

    let cancelled = false

    const connect = async () => {
      try {
        const response = await fetch(`http://localhost:8080/api/executions/${id}/logs`, {
          headers: { Authorization: `Bearer ${token}` },
        })

        if (!response.ok || !response.body) return

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        while (!cancelled) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })
          const parts = buffer.split('\n')
          buffer = parts.pop()

          for (const part of parts) {
            const trimmed = part.trim()
            if (trimmed.startsWith('data:')) {
              const text = trimmed.slice(5).trim()
              if (text) setLines(prev => [...prev, text])
            }
          }
        }
      } catch {
        // SSE connection closed or error — ignore
      }
    }

    connect()

    return () => {
      cancelled = true
      if (sseRef.current) {
        sseRef.current = null
      }
    }
  }, [id])

  const grafanaUrl = `${GRAFANA_BASE}/d/${GRAFANA_DASHBOARD_ID}?var-executionId=${id}`

  return (
    <div className="p-6 max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-xl font-semibold text-gray-800 font-mono">
            {execution?.simulationClass ?? id}
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Type: <span className="font-medium">{execution?.testType ?? '—'}</span>
            {execution?.targetMode && (
              <> · Target: <span className="font-medium">{execution.targetMode}</span></>
            )}
          </p>
        </div>
        <div className="flex items-center gap-3">
          {execution && <StatusBadge status={execution.status} />}
          {execution && ACTIVE_STATUSES.includes(execution.status) && (
            <button
              onClick={() => stopMutation.mutate()}
              disabled={stopMutation.isPending}
              className="text-sm bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white px-3 py-1 rounded"
            >
              {stopMutation.isPending ? 'Stopping…' : 'Stop'}
            </button>
          )}
        </div>
      </div>

      {/* Metadata */}
      {execution && (
        <div className="bg-white border border-gray-200 rounded-lg p-4 grid grid-cols-2 sm:grid-cols-3 gap-3 text-sm">
          {execution.startedAt && (
            <div>
              <span className="text-gray-500">Started</span>
              <p className="font-medium">{new Date(execution.startedAt).toLocaleString()}</p>
            </div>
          )}
          {execution.finishedAt && (
            <div>
              <span className="text-gray-500">Finished</span>
              <p className="font-medium">{new Date(execution.finishedAt).toLocaleString()}</p>
            </div>
          )}
          {execution.durationMs != null && (
            <div>
              <span className="text-gray-500">Duration</span>
              <p className="font-medium">{(execution.durationMs / 1000).toFixed(1)}s</p>
            </div>
          )}
        </div>
      )}

      {/* Actions */}
      <div className="flex gap-3">
        <a
          href={grafanaUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="text-sm text-blue-600 hover:text-blue-800 border border-blue-200 px-3 py-1.5 rounded"
        >
          Open Grafana ↗
        </a>
        {execution?.status === 'SUCCESS' && (
          <Link
            to={`/reports/${id}`}
            className="text-sm text-green-700 bg-green-50 hover:bg-green-100 border border-green-200 px-3 py-1.5 rounded"
          >
            View Report
          </Link>
        )}
      </div>

      {/* Live terminal */}
      <div>
        <h2 className="text-sm font-medium text-gray-700 mb-2">
          Logs
          {lines.length > 0 && (
            <span className="ml-2 text-gray-400 font-normal">{lines.length} lines</span>
          )}
        </h2>
        <TerminalLog lines={lines} className="h-96" />
      </div>
    </div>
  )
}
