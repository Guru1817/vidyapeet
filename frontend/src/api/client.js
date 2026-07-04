import axios from 'axios';

// In dev, Vite proxies /api to the backend. In prod, set VITE_API_BASE_URL.
const baseURL = import.meta.env.VITE_API_BASE_URL || '';

const api = axios.create({ baseURL });

export function setAuthToken(token) {
  if (token) {
    localStorage.setItem('token', token);
  } else {
    localStorage.removeItem('token');
  }
}

// Attach the JWT to every request. The backend derives the tenant from it.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Normalize backend error envelopes into a readable message.
export function errorMessage(error, fallback = 'Something went wrong.') {
  const data = error?.response?.data;
  if (data?.fieldErrors) {
    const first = Object.values(data.fieldErrors)[0];
    if (first) return first;
  }
  return data?.message || error?.message || fallback;
}

// Downloads a protected file (sends the JWT) and saves it in the browser.
export async function downloadFile(url, filename) {
  const res = await api.get(url, { responseType: 'blob' });
  const blobUrl = window.URL.createObjectURL(res.data);
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = filename || 'download';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
}

export default api;
