import { Routes, Route, Navigate } from 'react-router-dom'
import PrivateRoute from './components/PrivateRoute'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ProjectListPage from './pages/ProjectListPage'
import ProjectCreatePage from './pages/ProjectCreatePage'
import ProjectDetailPage from './pages/ProjectDetailPage'
import ExecutionHistoryPage from './pages/ExecutionHistoryPage'
import ExecutionDetailPage from './pages/ExecutionDetailPage'
import ReportListPage from './pages/ReportListPage'
import ReportDetailPage from './pages/ReportDetailPage'

export default function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Protected routes wrapped in Layout */}
      <Route element={<PrivateRoute />}>
        <Route element={<Layout />}>
          <Route path="/projects" element={<ProjectListPage />} />
          <Route path="/projects/new" element={<ProjectCreatePage />} />
          <Route path="/projects/:id" element={<ProjectDetailPage />} />
          <Route path="/executions" element={<ExecutionHistoryPage />} />
          <Route path="/executions/:id" element={<ExecutionDetailPage />} />
          <Route path="/reports" element={<ReportListPage />} />
          <Route path="/reports/:executionId" element={<ReportDetailPage />} />
        </Route>
      </Route>

      {/* Default redirect */}
      <Route path="/" element={<Navigate to="/projects" replace />} />
      <Route path="*" element={<Navigate to="/projects" replace />} />
    </Routes>
  )
}
