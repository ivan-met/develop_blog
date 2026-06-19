import { ref, type Ref } from 'vue'

export type ValidationRule<T> = (value: T) => string | null

export function useFormValidation<T extends Record<string, string>>(
  fields: T,
) {
  const errors = ref<Partial<Record<keyof T, string>>>({}) as Ref<
    Partial<Record<keyof T, string>>
  >

  function setFieldError(field: keyof T, message: string | null) {
    if (message) {
      errors.value[field] = message
    } else {
      delete errors.value[field]
    }
  }

  function clearErrors() {
    errors.value = {}
  }

  function setServerErrors(fieldErrors: Record<string, string>) {
    for (const [key, message] of Object.entries(fieldErrors)) {
      errors.value[key as keyof T] = message
    }
  }

  function validate(rules: {
    [K in keyof T]?: Array<ValidationRule<string>>
  }): boolean {
    clearErrors()
    let valid = true

    for (const [field, fieldRules] of Object.entries(rules)) {
      const value = fields[field as keyof T]
      if (!fieldRules) continue

      for (const rule of fieldRules) {
        const error = rule(value)
        if (error) {
          errors.value[field as keyof T] = error
          valid = false
          break
        }
      }
    }

    return valid
  }

  return { errors, validate, setFieldError, clearErrors, setServerErrors }
}

// ─── Common validation rules ─────────────────────────────────────────────────

export const rules = {
  required:
    (label = 'This field'): ValidationRule<string> =>
    (v) =>
      v.trim() ? null : `${label} is required`,

  minLength:
    (min: number, label = 'This field'): ValidationRule<string> =>
    (v) =>
      v.length >= min ? null : `${label} must be at least ${min} characters`,

  maxLength:
    (max: number, label = 'This field'): ValidationRule<string> =>
    (v) =>
      v.length <= max ? null : `${label} must be at most ${max} characters`,

  email: (): ValidationRule<string> => (v) => {
    if (!v.trim()) return null // let required handle empty
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)
      ? null
      : 'Enter a valid email address'
  },

  matches:
    (other: string, label = 'Passwords'): ValidationRule<string> =>
    (v) =>
      v === other ? null : `${label} do not match`,

  noSpaces: (): ValidationRule<string> => (v) =>
    /^\S+$/.test(v) ? null : 'No spaces allowed',
}
