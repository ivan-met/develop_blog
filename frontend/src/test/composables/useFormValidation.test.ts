import { describe, it, expect } from 'vitest'
import { useFormValidation, rules } from '@/composables/useFormValidation'
import { reactive } from 'vue'

describe('useFormValidation', () => {
  it('validates required fields and returns false when empty', () => {
    const fields = reactive({ name: '' })
    const { validate, errors } = useFormValidation(fields)

    const valid = validate({ name: [rules.required('Name')] })

    expect(valid).toBe(false)
    expect(errors.value.name).toBe('Name is required')
  })

  it('returns true when all required fields are filled', () => {
    const fields = reactive({ name: 'Alice' })
    const { validate, errors } = useFormValidation(fields)

    const valid = validate({ name: [rules.required('Name')] })

    expect(valid).toBe(true)
    expect(errors.value.name).toBeUndefined()
  })

  it('clears previous errors on re-validate', () => {
    const fields = reactive({ name: '' })
    const { validate, errors } = useFormValidation(fields)

    validate({ name: [rules.required('Name')] })
    expect(errors.value.name).toBeDefined()

    fields.name = 'Bob'
    validate({ name: [rules.required('Name')] })
    expect(errors.value.name).toBeUndefined()
  })

  it('setServerErrors populates errors from server fieldErrors', () => {
    const fields = reactive({ email: '', username: '' })
    const { setServerErrors, errors } = useFormValidation(fields)

    setServerErrors({ email: 'Email already taken', username: 'Username exists' })

    expect(errors.value.email).toBe('Email already taken')
    expect(errors.value.username).toBe('Username exists')
  })

  it('clearErrors removes all errors', () => {
    const fields = reactive({ name: '' })
    const { validate, clearErrors, errors } = useFormValidation(fields)

    validate({ name: [rules.required('Name')] })
    expect(errors.value.name).toBeDefined()

    clearErrors()
    expect(errors.value.name).toBeUndefined()
  })
})

describe('validation rules', () => {
  describe('required', () => {
    it('fails on empty string', () => {
      expect(rules.required()('')).toBe('This field is required')
    })

    it('fails on whitespace-only string', () => {
      expect(rules.required()('   ')).toBe('This field is required')
    })

    it('passes on non-empty string', () => {
      expect(rules.required()('hello')).toBeNull()
    })
  })

  describe('minLength', () => {
    it('fails when shorter than minimum', () => {
      expect(rules.minLength(8, 'Password')('short')).toContain('at least 8')
    })

    it('passes when equal to minimum', () => {
      expect(rules.minLength(5, 'Field')('hello')).toBeNull()
    })

    it('passes when longer than minimum', () => {
      expect(rules.minLength(3, 'Field')('hello world')).toBeNull()
    })
  })

  describe('maxLength', () => {
    it('fails when exceeds maximum', () => {
      expect(rules.maxLength(5, 'Field')('toolong')).toContain('at most 5')
    })

    it('passes when within maximum', () => {
      expect(rules.maxLength(10, 'Field')('short')).toBeNull()
    })
  })

  describe('email', () => {
    it('fails on invalid email format', () => {
      expect(rules.email()('not-an-email')).toBe('Enter a valid email address')
    })

    it('passes on valid email', () => {
      expect(rules.email()('user@example.com')).toBeNull()
    })

    it('passes on empty string (let required handle it)', () => {
      expect(rules.email()('')).toBeNull()
    })
  })

  describe('matches', () => {
    it('fails when values do not match', () => {
      expect(rules.matches('password123')('different')).toContain('do not match')
    })

    it('passes when values match', () => {
      expect(rules.matches('password123')('password123')).toBeNull()
    })
  })

  describe('noSpaces', () => {
    it('fails when value contains spaces', () => {
      expect(rules.noSpaces()('user name')).toBe('No spaces allowed')
    })

    it('passes when value has no spaces', () => {
      expect(rules.noSpaces()('username')).toBeNull()
    })
  })
})
