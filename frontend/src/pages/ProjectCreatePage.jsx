import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { projectsApi } from '../api/projects'
import StatusBadge from '../components/StatusBadge'

const TERMINAL_STATUSES = ['READY', 'FAILED']

export default function ProjectCreatePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [createdId, setCreatedId] = useState(null)
  const [form, setForm] = useState({ name: '', gitUrl: '', branch: 'main', buildTool: 'MAVEN' })
  const [error, setError] = useState(null)

  const { data: project } = useQuery({
    queryKey: ['project', createdId],
    queryFn: () => projectsApi.get(createdId),
    enabled: !!createdId,
    refetchInterval: query => {
      const status = query.state.data?.status
      return status && !TERMINAL_STATUSES.includes(status) ? 3000 : false
    },
  })

  const mutation = useMutation({
    mutationFn: projectsApi.create,
    onSuccess: data => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      setCreatedId(data.id)
    },
    onError: err => {
      setError(err.response?.data?.message || 'Failed to create project.')
    },
  })

  const handleSubmit = e => {
    e.preventDefault()
    setError(null)
    mutation.mutate(form)
  }

  if (createdId && project) {
    if (project.status === 'READY') {
      navigate(`/projects/${createdId}`, { replace: true })
      return null
    }
    return (
      <div className="p-6 max-w-lg mx-auto">
        <h1 className="text-2xl font-semibold text-gray-800 mb-6">Creating Project…</h1>
        <div className="bg-white border border-gray-200 rounded-lg p-6">
          <div className="flex items-center justify-between mb-4">
            <span className="text-gray-700 font-medium">{project.name}</span>
            <StatusBadge status={project.status} />
          </div>
          <p className="text-sm text-gray-500">
            {project.status === 'FAILED'
              ? 'Build failed. Please check your repository and try again.'
              : 'Cloning repository and building project…'}
          </p>
          {project.status === 'FAILED' && (
            <button
              onClick={() => { setCreatedId(null) }}
              className="mt-4 text-sm text-blue-600 hover:underline"
            >
              Back to form
            </button>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="p-6 max-w-lg mx-auto">
      <h1 className="text-2xl font-semibold text-gray-800 mb-6">New Project</h1>
      <form onSubmit={handleSubmit} className="bg-white border border-gray-200 rounded-lg p-6 space-y-4">
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 rounded p-3 text-sm">{error}</div>
        )}

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
          <input
            required
            type="text"
            value={form.name}
            onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="My Gatling Project"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Git URL</label>
          <input
            required
            type="url"
            value={form.gitUrl}
            onChange={e => setForm(f => ({ ...f, gitUrl: e.target.value }))}
            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            placeholder="https://github.com/org/gatling-tests.git"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Branch</label>
          <input
            type="text"
            value={form.branch}
            onChange={e => setForm(f => ({ ...f, branch: e.target.value }))}
            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Build Tool</label>
          <select
            value={form.buildTool}
            onChange={e => setForm(f => ({ ...f, buildTool: e.target.value }))}
            className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="MAVEN">Maven</option>
            <option value="GRADLE">Gradle</option>
          </select>
        </div>

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white py-2 rounded text-sm font-medium"
        >
          {mutation.isPending ? 'Creating…' : 'Create Project'}
        </button>
      </form>
    </div>
  )
}
