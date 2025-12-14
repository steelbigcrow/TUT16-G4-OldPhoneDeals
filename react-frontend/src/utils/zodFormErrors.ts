import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { ZodError } from 'zod'

export function setZodFormErrors<TFieldValues extends FieldValues>(
  zodError: ZodError,
  setError: UseFormSetError<TFieldValues>,
) {
  for (const issue of zodError.issues) {
    const path = issue.path.join('.') as Path<TFieldValues>
    setError(path, { type: 'validate', message: issue.message })
  }
}

