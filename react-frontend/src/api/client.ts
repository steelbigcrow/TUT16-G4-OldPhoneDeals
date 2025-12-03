import axios, { type AxiosRequestHeaders } from 'axios';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:3000/api';

const client = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken') || localStorage.getItem('token');
  if (token) {
    const headers: AxiosRequestHeaders = (config.headers as AxiosRequestHeaders) ?? {};
    headers.Authorization = `Bearer ${token}`;
    config.headers = headers;
  }
  return config;
});

export default client;
