import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { projectsApi } from '../api/projects'
import { executionsApi } from '../api/executions'
import StatusBadge from '../components/StatusBadge'

const TEST_TYPES = ['LOAD', 'STRESS', 'RELIABILITY', 'SMOKE', 'SPIKE']

export default function ProjectDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [launchError, setLaunchError] = useState(null)
  const [launchForm, setLaunchForm] = useState({ simulationClass: '', testType: 'LOAD' })

  const { data: project, isLoading, error } = useQuery({
    queryKey: ['project', id],
    queryFn: () => projectsApi.get(id),
    refetchInterval: query => {
      const status = query.state.data?.status
      return status === 'READY' || status === 'FAILED' ? false : 3000
    },
  })

  const { data: simulations = [] } = useQuery({
    queryKey: ['simulations', id],
    queryFn: () => projectsApi.getSimulations(id),
    enabled: project?.status === 'READY',
  })

  const syncMutation = useMutation({
    mutationFn: () => projectsApi.sync(id),
    onSuccess: () => {
      // project query will re-poll automatically as status goes back to BUILDING
    },
  })

  const launchMutation = useMutation({
    mutationFn: executionsApi.create,
    onSuccess: data => {
      navigate(`/executions/${data.id}`)
    },
    onError: err => {
      setLaunchError(err.response?.data?.message || 'Failed to launch test.')
    },
  })

  const handleLaunch = e => {
    e.preventDefault()
    setLaunchError(null)
    launchMutation.mutate({ projectId: id, ...launchForm })
  }

  if (isLoading) return <div className="text-gray-500 p-6">Loading…</div>
  if (error) return <div className="text-red-500 p-6">Failed to load project.</div>

  return (
    <div className="p-6 max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-2xl font-semibold text-gray-800">{project.name}</h1>
          <p className="text-sm text-gray-500 mt-1 font-mono">{project.gitUrl}</p>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={project.status} />
          <button
            onClick={() => syncMutation.mutate()}
            disabled={syncMutation.isPending || project.status === 'BUILDING' || project.status === 'CLONING'}
            className="text-sm text-gray-600 hover:text-gray-900 border border-gray-300 px-3 py-1 rounded disabled:opacity-40"
          >
            {syncMutation.isPending ? 'Syncing…' : 'Sync'}
          </button>
        </div>
      </div>

      {/* Metadata */}
      <div className="bg-white border border-gray-200 rounded-lg p-4 grid grid-cols-2 gap-3 text-sm">
        <div>
          <span className="text-gray-500">Branch</span>
          <p className="font-medium">{project.branch || 'main'}</p>
        </div>
        <div>
          <span className="text-gray-500">Build Tool</span>
          <p className="font-medium">{project.buildTool}</p>
        </div>
        {project.lastSyncedAt && (
          <div>
            <span className="text-gray-500">Last Synced</span>
            <p className="font-medium">{new Date(project.lastSyncedAt).toLocaleString()}</p>
          </div>
        )}
        <div>
          <span className="text-gray-500">Created</span>
          <p className="font-medium">{new Date(project.createdAt).toLocaleString()}</p>
        </div>
      </div>

      {project.status === 'READY' && (
        <>
          {/* Launch test form */}
          <div className="bg-white border border-gray-200 rounded-lg p-5">
            <h2 className="text-lg font-medium text-gray-800 mb-4">Launch Test</h2>
            {launchError && (
              <div className="mb-3 bg-red-50 border border-red-200 text-red-700 rounded p-3 text-sm">{launchError}</div>
            )}
            <form onSubmit={handleLaunch} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Simulation Class</label>
                {simulations.length > 0 ? (
                  <select
                    required
                    value={launchForm.simulationClass}
                    onChange={e => setLaunchForm(f => ({ ...f, simulationClass: e.target.value }))}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">— select simulation —</option>
                    {simulations.map(s => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                ) : (
                  <input
                    required
                    type="text"
                    value={launchForm.simulationClass}
                    onChange={e => setLaunchForm(f => ({ ...f, simulationClass: e.target.value }))}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="com.example.simulations.BasicSimulation"
                  />
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Test Type</label>
                <select
                  value={launchForm.testType}
                  onChange={e => setLaunchForm(f => ({ ...f, testType: e.target.value }))}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {TEST_TYPES.map(t => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </div>

              <button
                type="submit"
                disabled={launchMutation.isPending}
                className="w-full bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white py-2 rounded text-sm font-medium"
              >
                {launchMutation.isPending ? 'Launching…' : 'Launch Test'}
              </button>
            </form>
          </div>
        </>
      )}

      {(project.status === 'BUILDING' || project.status === 'CLONING' || project.status === 'PENDING') && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 text-sm text-yellow-800">
          Project is being built. This page will update automatically when ready.
        </div>
      )}

      {project.status === 'FAILED' && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-sm text-red-700">
          Build failed. Check your repository configuration and click Sync to retry.
        </div>
      )}
    </div>
  )
}
