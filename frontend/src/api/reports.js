import client from './client'

export const reportsApi = {
  list: () => client.get('/api/reports').then(r => r.data),

  get: (executionId) => client.get(`/api/reports/${executionId}`).then(r => r.data),

  download: async (executionId) => {
    const response = await client.get(`/api/reports/${executionId}/download`, {
      responseType: 'blob',
    })
    const blob = new Blob([response.data], { type: 'application/pdf' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `report-${executionId}.pdf`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  },
}
