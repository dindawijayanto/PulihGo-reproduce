import { beforeEach, describe, expect, it, vi } from 'vitest'
import { saveTherapyPlan } from './therapyPlanApi'
import type { TherapyPlanValues } from './therapyPlan'

const mocks = vi.hoisted(() => ({
  getAssignedPatient: vi.fn(),
  from: vi.fn(),
}))
vi.mock('./api', () => ({ getAssignedPatient: mocks.getAssignedPatient }))
vi.mock('./supabase', () => ({ supabase: { from: mocks.from } }))

const values: TherapyPlanValues = {
  target_rom_deg: 60,
  rom_ceiling_deg: 75,
  target_reps: 8,
  sets_per_session: 2,
  sessions_per_week: 4,
  notes: '',
  is_active: true,
}

function chain(data: unknown, error: unknown = null) {
  const api = {
    select: vi.fn(),
    eq: vi.fn(),
    maybeSingle: vi.fn(async () => ({ data, error })),
    single: vi.fn(async () => ({ data, error })),
    insert: vi.fn(),
    update: vi.fn(),
  }
  api.select.mockReturnValue(api)
  api.eq.mockReturnValue(api)
  api.insert.mockReturnValue(api)
  api.update.mockReturnValue(api)
  return api
}

beforeEach(() => {
  vi.resetAllMocks()
  mocks.getAssignedPatient.mockResolvedValue({ id: 'P001' })
})

describe('PGF-THR-05 save API: column-level grants + RLS', () => {
  it('creates an unsaved plan with INSERT, not UPSERT/UPDATE', async () => {
    const lookup = chain(null)
    const saved = { patient_id: 'P001', exercise_id: 'EX001', ...values }
    const write = chain(saved)
    mocks.from.mockReturnValueOnce(lookup).mockReturnValueOnce(write)

    expect(await saveTherapyPlan('THERAPIST', 'P001', 'EX001', values)).toEqual(saved)
    expect(mocks.getAssignedPatient).toHaveBeenCalledWith('THERAPIST', 'P001')
    expect(lookup.select).toHaveBeenCalledWith('patient_id,exercise_id')
    expect(lookup.eq).toHaveBeenNthCalledWith(1, 'patient_id', 'P001')
    expect(lookup.eq).toHaveBeenNthCalledWith(2, 'exercise_id', 'EX001')
    expect(write.insert).toHaveBeenCalledWith({ patient_id: 'P001', exercise_id: 'EX001', ...values })
    expect(write.update).not.toHaveBeenCalled()
  })

  it('edits an existing plan with UPDATE of configurable columns only', async () => {
    const lookup = chain({ patient_id: 'P001', exercise_id: 'EX001' })
    const saved = { patient_id: 'P001', exercise_id: 'EX001', ...values }
    const write = chain(saved)
    mocks.from.mockReturnValueOnce(lookup).mockReturnValueOnce(write)

    expect(await saveTherapyPlan('THERAPIST', 'P001', 'EX001', values)).toEqual(saved)
    expect(write.update).toHaveBeenCalledWith(values)
    expect(write.eq).toHaveBeenNthCalledWith(1, 'patient_id', 'P001')
    expect(write.eq).toHaveBeenNthCalledWith(2, 'exercise_id', 'EX001')
    expect(write.insert).not.toHaveBeenCalled()
    expect(Object.keys(write.update.mock.calls[0][0])).not.toContain('patient_id')
    expect(Object.keys(write.update.mock.calls[0][0])).not.toContain('exercise_id')
  })

  it('does not query or modify a patient who is not assigned', async () => {
    mocks.getAssignedPatient.mockResolvedValue(null)
    await expect(saveTherapyPlan('THERAPIST', 'P404', 'EX001', values)).rejects.toThrow('not assigned')
    expect(mocks.from).not.toHaveBeenCalled()
  })

  it('does not write when the existence lookup is denied or fails', async () => {
    mocks.from.mockReturnValueOnce(chain(null, { code: '42501', message: 'permission denied' }))
    await expect(saveTherapyPlan('THERAPIST', 'P001', 'EX001', values)).rejects.toMatchObject({ code: '42501' })
    expect(mocks.from).toHaveBeenCalledTimes(1)
  })

  it('propagates an INSERT failure without reporting success', async () => {
    mocks.from.mockReturnValueOnce(chain(null)).mockReturnValueOnce(chain(null, { code: '42501' }))
    await expect(saveTherapyPlan('THERAPIST', 'P001', 'EX001', values)).rejects.toMatchObject({ code: '42501' })
  })

  it('rejects a response attributed to the wrong patient', async () => {
    mocks.from.mockReturnValueOnce(chain(null)).mockReturnValueOnce(chain({ patient_id: 'P002', exercise_id: 'EX001' }))
    await expect(saveTherapyPlan('THERAPIST', 'P001', 'EX001', values)).rejects.toThrow('could not be verified')
  })
})
