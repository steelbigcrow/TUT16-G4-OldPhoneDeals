import { apiClient } from './client'
import type { ApiResponse } from '../types/api'
import type { FileUploadResponse } from '../types/upload'

export async function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  const { data } = await apiClient.post<ApiResponse<FileUploadResponse>>(
    '/upload/image',
    formData,
  )
  return data
}

