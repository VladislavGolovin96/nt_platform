import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { projectsApi } from '../api/projects'
import StatusBadge from '../components/StatusBadge'

export default function ProjectListPage() {
  const { data: projects = [], isLoading, error } = useQuery({
    queryKey: ['projects'],
    queryFn: projectsApi.list,
  })

  if (isLoading) return <div className="text-gray-500 p-6">Loading…</div>
  if (error) return <div className="text-red-500 p-6">Failed to load projects.</div>

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-semibold text-gray-800">Projects</h1>
        <Link
          to="/projects/new"
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded text-sm font-medium"
        >
          New Project
        </Link>
      </div>

      {projects.length === 0 ? (
        <div className="text-gray-500 bg-gray-50 border border-gray-200 rounded p-8 text-center">
          No projects yet.{' '}
          <Link to="/projects/new" className="text-blue-600 hover:underline">
            Create your first project
          </Link>
        </div>
      ) : (
        <div className="grid gap-4">
          {projects.map(project => (
            <Link
              key={project.id}
              to={`/projects/${project.id}`}
              className="block bg-white border border-gray-200 rounded-lg p-5 hover:border-blue-300 hover:shadow-sm transition"
            >
              <div className="flex justify-between items-start">
                <div>
                  <h2 className="text-lg font-medium text-gray-900">{project.name}</h2>
                  <p className="text-sm text-gray-500 mt-1 font-mono">{project.gitUrl}</p>
                  <p className="text-xs text-gray-400 mt-1">
                    Branch: <span className="font-medium">{project.branch || 'main'}</span>
                    {' · '}
                    Build: <span className="font-medium">{project.buildTool}</span>
                  </p>
                </div>
                <StatusBadge status={project.status} />
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
