import api from './axios';

// Each function = one backend endpoint. Pages never write URLs themselves,
// so if an endpoint changes there is exactly one place to fix.
const data = (request) => request.then((res) => res.data);

// Leave out empty filters so we never send ?status=&from=
const clean = (obj) => Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== '' && v != null));

export const authApi = {
  login: (body) => data(api.post('/auth/login', body)),
  register: (body) => data(api.post('/auth/register', body)),
  me: () => data(api.get('/auth/me')),
};

export const patientApi = {
  getMe: () => data(api.get('/patients/me')),
  createProfile: (body) => data(api.post('/patients/profile', body)),
  updateProfile: (body) => data(api.put('/patients/me', body)),
  // doctor
  list: ({ page = 0, size = 10, sort = 'registeredAt,desc' } = {}) =>
    data(api.get('/admin/patients', { params: { page, size, sort } })),
  search: ({ query, page = 0, size = 10, sort = 'registeredAt,desc' }) =>
    data(api.get('/admin/patients/search', { params: { query, page, size, sort } })),
  getById: (id) => data(api.get(`/admin/patients/${id}`)),
  updateMedicalNotes: (id, body) => data(api.put(`/admin/patients/${id}/medical-notes`, body)),
};

export const appointmentApi = {
  book: (body) => data(api.post('/appointments/book', body)),
  mine: () => data(api.get('/appointments/my')),
  availability: (from, days = 14) => data(api.get('/appointments/availability', { params: { from, days } })),
  cancel: (id) => data(api.put(`/appointments/${id}/cancel`)),
  reschedule: (id, appointmentTime) => data(api.put(`/appointments/${id}/reschedule`, { appointmentTime })),
  // doctor
  today: () => data(api.get('/admin/appointments/today')),
  listAll: ({ page = 0, size = 10, sort = 'appointmentTime,desc', status, from, to } = {}) =>
    data(api.get('/admin/appointments', { params: clean({ page, size, sort, status, from, to }) })),
  forPatient: (patientId) => data(api.get(`/admin/patients/${patientId}/appointments`)),
  confirm: (id) => data(api.put(`/admin/appointments/${id}/confirm`)),
  complete: (id) => data(api.put(`/admin/appointments/${id}/complete`)),
};

export const prescriptionApi = {
  write: (body) => data(api.post('/admin/prescriptions', body)),
  historyForPatient: (patientId) => data(api.get(`/admin/patients/${patientId}/prescriptions`)),
  forAppointment: (appointmentId) => data(api.get(`/admin/appointments/${appointmentId}/prescription`)),
  mineByAppointment: (appointmentId) => data(api.get(`/prescriptions/appointment/${appointmentId}`)),
};

export const reportApi = {
  overview: () => data(api.get('/admin/reports/overview')),
  appointments: (period) => data(api.get('/admin/reports/appointments', { params: { period } })),
};
