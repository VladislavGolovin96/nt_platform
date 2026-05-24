import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

const navLinkClass = ({ isActive }) =>
  isActive
    ? 'text-white font-semibold border-b-2 border-white pb-1'
    : 'text-blue-100 hover:text-white transition-colors'

export default function Layout() {
  const { user, logout } = useAuth()

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-blue-700 px-6 py-3 flex items-center gap-6 shadow">
        <span className="text-white font-bold text-lg mr-4">LoadTest Platform</span>

        <NavLink to="/projects" className={navLinkClass}>Projects</NavLink>
        <NavLink to="/executions" className={navLinkClass}>Executions</NavLink>
        <NavLink to="/reports" className={navLinkClass}>Reports</NavLink>

        <div className="ml-auto flex items-center gap-4">
          {user?.sub && (
            <span className="text-blue-200 text-sm">{user.sub}</span>
          )}
          <button
            onClick={logout}
            className="bg-blue-600 hover:bg-blue-500 text-white text-sm px-3 py-1 rounded transition-colors"
          >
            Logout
          </button>
        </div>
      </nav>

      <main className="max-w-6xl mx-auto px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
