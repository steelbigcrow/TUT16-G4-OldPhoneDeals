import { useMutation } from '@tanstack/react-query'
import { uploadApi } from '../api'

export function useUploadImage() {
  return useMutation({
    mutationFn: (file: File) => uploadApi.uploadImage(file),
  })
}

