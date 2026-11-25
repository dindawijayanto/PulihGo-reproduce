import { describe, expect, it } from 'vitest'
import {
  draftForExercise, planDraftChanged, plansForPatient, planValues,
  selectedPlan, validateTherapyPlan,
} from './therapyPlan'
import type { TherapyPlan } from './therapyPlan'
import type { Exercise } from './types'

const exercise: Exercise = {
  id: 'EX001', name: 'Forearm Supination and Pronation',
  target_rom_deg: 70, rom_ceiling_deg: 80, target_reps: 10,
}
const plan: TherapyPlan = {
  patient_id: 'P001', exercise_id: 'EX001',
  target_rom_deg: 60, rom_ceiling_deg: 75, target_reps: 8,
  sets_per_session: 2, sessions_per_week: 4, notes: 'Review carefully',
  is_active: true, updated_by: 'therapist',
  created_at: '2026-11-24T00:00:00Z', updated_at: '2026-11-24T01:00:00Z',
}

const draft = () => draftForExercise(exercise)

describe('PGF-THR-05 therapy plan', () => {
  it('shows exercise defaults but does not fabricate a saved plan', () => {
    expect(draft()).toMatchObject({ targetRomDeg: '70', romCeilingDeg: '80', targetReps: '10' })
    expect(selectedPlan([], 'EX001')).toBeNull()
  })
  it('restores the patient-specific saved prescription', () => {
    expect(draftForExercise(exercise, plan)).toMatchObject({
      targetRomDeg: '60', romCeilingDeg: '75', targetReps: '8',
      setsPerSession: '2', sessionsPerWeek: '4', isActive: true,
    })
  })
  it('never mixes another patient\'s plans', () => {
    const other = { ...plan, patient_id: 'P002' }
    expect(plansForPatient('P001', [other, plan])).toEqual([plan])
  })
  it('rejects zero, negative, missing and non-finite targets', () => {
    for (const targetRomDeg of ['0', '-1', '', 'NaN', 'Infinity', '180']) {
      expect(validateTherapyPlan({ ...draft(), targetRomDeg })).not.toBeNull()
    }
  })
  it('rejects ceilings at or under the target and ceilings above 180', () => {
    for (const romCeilingDeg of ['70', '69', '181', '', 'Infinity']) {
      expect(validateTherapyPlan({ ...draft(), romCeilingDeg })).not.toBeNull()
    }
  })
  it('requires integer reps, sets and weekly sessions in safe ranges', () => {
    expect(validateTherapyPlan({ ...draft(), targetReps: '1.5' })).not.toBeNull()
    expect(validateTherapyPlan({ ...draft(), targetReps: '101' })).not.toBeNull()
    expect(validateTherapyPlan({ ...draft(), setsPerSession: '0' })).not.toBeNull()
    expect(validateTherapyPlan({ ...draft(), setsPerSession: '11' })).not.toBeNull()
    expect(validateTherapyPlan({ ...draft(), sessionsPerWeek: '8' })).not.toBeNull()
  })
  it('validates notes and normalizes values before save', () => {
    expect(validateTherapyPlan({ ...draft(), notes: 'x'.repeat(501) })).not.toBeNull()
    expect(planValues({ ...draft(), targetRomDeg: '70.5', notes: '  review  ' })).toEqual({
      target_rom_deg: 70.5, rom_ceiling_deg: 80, target_reps: 10,
      sets_per_session: 1, sessions_per_week: 3, notes: 'review', is_active: true,
    })
  })
  it('tracks modified inputs and paused vs active plans', () => {
    const initial = draftForExercise(exercise, plan)
    expect(planDraftChanged(initial, initial)).toBe(false)
    expect(planDraftChanged({ ...initial, isActive: false }, initial)).toBe(true)
    expect(planValues({ ...initial, isActive: false }).is_active).toBe(false)
  })
  it('fails closed if an invalid draft reaches serialization', () => {
    expect(() => planValues({ ...draft(), targetReps: '-3' })).toThrow()
  })
})
