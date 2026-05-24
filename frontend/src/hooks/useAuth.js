import { useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

const TOKEN_KEY = 'accessToken'
const REFRESH_KEY = 'refreshToken'

function decodeTokenPayload(token) {
  try {
    const payload = token.split('.')[1]
    return JSON.parse(atob(payload))
  } catch {
    return null
  }
}

export function useAuth() {
  const navigate = useNavigate()
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY))

  const isAuthenticated = !!token
  const user = token ? decodeTokenPayload(token) : null

  const login = useCallback(({ accessToken, refreshToken }) => {
    localStorage.setItem(TOKEN_KEY, accessToken)
    if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken)
    setToken(accessToken)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_KEY)
    setToken(null)
    navigate('/login', { replace: true })
  }, [navigate])

  return { user, token, login, logout, isAuthenticated }
}
