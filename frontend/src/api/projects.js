import client from './client'

export const projectsApi = {
  list: () => client.get('/api/projects').then(r => r.data),
  get: (id) => client.get(`/api/projects/${id}`).then(r => r.data),
  create: (data) => client.post('/api/projects', data).then(r => r.data),
  sync: (id) => client.post(`/api/projects/${id}/sync`).then(r => r.data),
  delete: (id) => client.delete(`/api/projects/${id}`),
  getSimulations: (id) => client.get(`/api/projects/${id}/simulations`).then(r => r.data),
}
