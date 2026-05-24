import client from './client'

export const executionsApi = {
  list: () => client.get('/api/executions').then(r => r.data),
  get: (id) => client.get(`/api/executions/${id}`).then(r => r.data),
  create: (data) => client.post('/api/executions', data).then(r => r.data),
  stop: (id) => client.post(`/api/executions/${id}/stop`),
}
