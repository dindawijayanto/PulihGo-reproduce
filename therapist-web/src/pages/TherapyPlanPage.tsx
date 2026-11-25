import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getAssignedPatient, listExercises } from '../lib/api'
import { useAuth } from '../lib/auth'
import { initials } from '../lib/logic'
import { listTherapyPlans, saveTherapyPlan } from '../lib/therapyPlanApi'
import {
  draftForExercise, planDraftChanged, planValues, plansForPatient,
  selectedPlan, validateTherapyPlan,
  type TherapyPlan, type TherapyPlanDraft,
} from '../lib/therapyPlan'
import type { Exercise, Patient } from '../lib/types'
import './therapyPlan.css'

type PageState =
  | { kind: 'loading'; scope: string }
  | { kind: 'missing'; scope: string }
  | { kind: 'error'; scope: string; reason: string }
  | { kind: 'ready'; scope: string; patient: Patient; exercises: Exercise[]; plans: TherapyPlan[] }

/** PGF-THR-05. Patient-specific prescriptions; Android consumption is PGF-INT-02. */
export function TherapyPlanPage() {
  const { patientId = '' } = useParams()
  const { profile } = useAuth()
  const therapistId = profile?.id ?? ''
  const scope = `${therapistId}:${patientId}`
  const scopeRef = useRef(scope)
  scopeRef.current = scope

  const [state, setState] = useState<PageState>({ kind: 'loading', scope: '' })
  const [reloadTick, setReloadTick] = useState(0)
  const [selectedExerciseId, setSelectedExerciseId] = useState('')
  const [draft, setDraft] = useState<TherapyPlanDraft | null>(null)
  const [baseDraft, setBaseDraft] = useState<TherapyPlanDraft | null>(null)
  const [saving, setSaving] = useState(false)
  const savingRef = useRef(false)
  const [message, setMessage] = useState<{ kind: 'ok' | 'error'; text: string } | null>(null)

  useEffect(() => {
    let active = true
    setState({ kind: 'loading', scope })
    setSelectedExerciseId('')
    setDraft(null)
    setBaseDraft(null)
    setMessage(null)
    setSaving(false)
    if (!therapistId || !patientId) {
      setState({ kind: 'missing', scope })
      return () => { active = false }
    }

    void (async () => {
      try {
        const patient = await getAssignedPatient(therapistId, patientId)
        if (!active) return
        if (!patient) {
          setState({ kind: 'missing', scope })
          return
        }
        const [allExercises, allPlans] = await Promise.all([
          listExercises(), listTherapyPlans(therapistId, patient.id),
        ])
        if (!active) return
        const plans = plansForPatient(patient.id, allPlans)
        const exercises = [...allExercises].sort((a, b) => a.name.localeCompare(b.name))
        const first = exercises[0]
        const next = first ? draftForExercise(first, selectedPlan(plans, first.id)) : null
        setSelectedExerciseId(first?.id ?? '')
        setDraft(next)
        setBaseDraft(next)
        setState({ kind: 'ready', scope, patient, exercises, plans })
      } catch {
        if (active) setState({
          kind: 'error', scope,
          reason: 'Unable to load Therapy Plans. Check Supabase access and confirm the THR-05 migration was applied.',
        })
      }
    })()
    return () => { active = false }
  }, [patientId, therapistId, scope, reloadTick])

  const shown = state.scope === scope ? state : { kind: 'loading' as const, scope }
  const ready = shown.kind === 'ready' ? shown : null
  const exercise = ready?.exercises.find((entry) => entry.id === selectedExerciseId) ?? null
  const persisted = ready ? selectedPlan(ready.plans, selectedExerciseId) : null
  const changed = draft != null && baseDraft != null && planDraftChanged(draft, baseDraft)
  const validationError = draft ? validateTherapyPlan(draft) : null

  useEffect(() => {
    const warn = (event: BeforeUnloadEvent) => {
      if (!changed) return
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [changed])

  function choose(exerciseId: string) {
    if (!ready || savingRef.current) return
    if (changed && !window.confirm('Discard unsaved changes to this exercise plan?')) return
    const chosen = ready.exercises.find((entry) => entry.id === exerciseId)
    if (!chosen) return
    const next = draftForExercise(chosen, selectedPlan(ready.plans, exerciseId))
    setSelectedExerciseId(exerciseId)
    setDraft(next)
    setBaseDraft(next)
    setMessage(null)
  }

  function refresh() {
    if (savingRef.current) return
    if (changed && !window.confirm('Discard unsaved changes and reload the plan?')) return
    setReloadTick((n) => n + 1)
  }

  async function save() {
    if (!ready || !exercise || !draft || savingRef.current) return
    const validation = validateTherapyPlan(draft)
    if (validation) {
      setMessage({ kind: 'error', text: validation })
      return
    }
    const currentScope = scope
    savingRef.current = true
    setSaving(true)
    setMessage(null)
    try {
      const saved = await saveTherapyPlan(therapistId, ready.patient.id, exercise.id, planValues(draft))
      if (scopeRef.current !== currentScope) return
      setState((before) => {
        if (before.kind !== 'ready' || before.scope !== currentScope) return before
        const others = before.plans.filter((item) => item.exercise_id !== exercise.id)
        return { ...before, plans: plansForPatient(before.patient.id, [...others, saved]) }
      })
      const next = draftForExercise(exercise, saved)
      setDraft(next)
      setBaseDraft(next)
      setMessage({ kind: 'ok', text: 'Therapy Plan saved to Supabase for this patient.' })
    } catch {
      if (scopeRef.current === currentScope) {
        setMessage({ kind: 'error', text: 'Could not save this plan. Verify your access, connection, and database migration, then retry.' })
      }
    } finally {
      savingRef.current = false
      if (scopeRef.current === currentScope) setSaving(false)
    }
  }

  const back = <Link className="link-btn" to={`/patients/${encodeURIComponent(patientId)}`}>← Patient Detail</Link>

  if (shown.kind === 'loading') return <div className="card empty" role="status">Loading Therapy Plan…</div>
  if (shown.kind !== 'ready') {
    return (
      <div className="card empty" role="alert">
        <h2>{shown.kind === 'missing' ? 'Patient not available' : 'Therapy Plan unavailable'}</h2>
        <p className="tp-empty-message">
          {shown.kind === 'missing'
            ? 'This patient is not assigned to your therapist account.'
            : shown.reason}
        </p>
        <div className="tp-actions">{back}{shown.kind === 'error' && <button className="btn-chip" onClick={refresh}>Retry</button>}</div>
      </div>
    )
  }

  const name = shown.patient.profile?.full_name || shown.patient.id
  const savedCount = shown.plans.length
  const activeCount = shown.plans.filter((item) => item.is_active).length

  return (
    <>
      <div className="page-head">
        <div>
          {back}
          <div className="crumb" style={{ marginTop: 8 }}>THERAPY / PATIENTS / <b>THERAPY PLAN</b></div>
          <h1>Therapy Plan</h1>
          <p className="muted" style={{ marginTop: 5 }}>Manage exercise targets and safety limits for one patient.</p>
        </div>
        <div className="head-actions">
          <span className="date-chip">{savedCount} saved • {activeCount} active</span>
          <button className="btn-chip" disabled={saving} onClick={refresh}>Refresh</button>
        </div>
      </div>

      <section className="card tp-patient" aria-label="Selected patient">
        <div className="tp-avatar">{initials(name)}</div>
        <div className="tp-patient-body">
          <strong>{name}</strong>
          <p className="muted">#{shown.patient.id} • {shown.patient.affected_arm?.toLowerCase() ?? 'Unspecified'} arm</p>
          <p className="faint">{shown.patient.rehabilitation_goal || 'Rehabilitation goal not specified'}</p>
        </div>
        <span className="pill idle">Assigned patient</span>
      </section>

      {shown.exercises.length === 0 ? (
        <section className="card empty" role="status">
          <h2>No exercises available</h2>
          <p style={{ marginTop: 8 }}>No exercise is currently listed in the Supabase exercise catalog.</p>
        </section>
      ) : (
        <div className="tp-grid">
          <section className="card tp-exercises" aria-label="Available prescribed exercises">
            <div className="tp-card-heading">
              <span className="label">Prescribed exercises</span>
              <span className="faint">{shown.exercises.length} available</span>
            </div>
            <p className="faint tp-context">Select an available exercise to view or configure its patient-specific prescription.</p>
            <div className="tp-exercise-list">
              {shown.exercises.map((item) => {
                const plan = selectedPlan(shown.plans, item.id)
                return (
                  <button
                    type="button"
                    key={item.id}
                    className={`tp-exercise ${item.id === selectedExerciseId ? 'selected' : ''}`}
                    aria-pressed={item.id === selectedExerciseId}
                    disabled={saving}
                    onClick={() => choose(item.id)}
                  >
                    <span className="tp-exercise-name">{item.name}<small>{item.id}</small></span>
                    {plan ? <span className={`pill ${plan.is_active ? 'ok' : 'idle'}`}>{plan.is_active ? 'Active' : 'Paused'}</span>
                      : <span className="pill idle">Not saved</span>}
                  </button>
                )
              })}
            </div>
            <div className="tp-note">
              <strong>Patient-specific plan</strong>
              <p>Only saved prescriptions apply to this patient. Catalog defaults shown in a new form have not been published.</p>
            </div>
          </section>

          <section className="card tp-prescription" aria-labelledby="tp-prescription-title">
            <div className="tp-card-heading">
              <span className="label" id="tp-prescription-title">Clinical prescription</span>
              {persisted
                ? <span className={`pill ${persisted.is_active ? 'ok' : 'idle'}`}>{persisted.is_active ? 'Saved / active' : 'Saved / paused'}</span>
                : <span className="pill warn">Unsaved defaults</span>}
            </div>
            {exercise && draft && (
              <form onSubmit={(event) => { event.preventDefault(); void save() }}>
                <label className="tp-field">
                  <span>Exercise</span>
                  <div className="tp-readonly">{exercise.name}</div>
                </label>
                <div className="tp-fields-two">
                  <label className="tp-field">
                    <span>Target ROM (°)</span>
                    <input aria-label="Target ROM (degrees)" type="number" step="any" min="0.1" max="179.9" required
                      value={draft.targetRomDeg} onChange={(e) => setDraft({ ...draft, targetRomDeg: e.target.value })} />
                  </label>
                  <label className="tp-field">
                    <span>Safe ROM ceiling (°)</span>
                    <input aria-label="Safe ROM ceiling (degrees)" type="number" step="any" min="0.1" max="180" required
                      value={draft.romCeilingDeg} onChange={(e) => setDraft({ ...draft, romCeilingDeg: e.target.value })} />
                  </label>
                  <label className="tp-field">
                    <span>Target repetitions</span>
                    <input aria-label="Target repetitions" type="number" step="1" min="1" max="100" required
                      value={draft.targetReps} onChange={(e) => setDraft({ ...draft, targetReps: e.target.value })} />
                  </label>
                  <label className="tp-field">
                    <span>Sets per session</span>
                    <input aria-label="Sets per session" type="number" step="1" min="1" max="10" required
                      value={draft.setsPerSession} onChange={(e) => setDraft({ ...draft, setsPerSession: e.target.value })} />
                  </label>
                  <label className="tp-field">
                    <span>Sessions per week</span>
                    <input aria-label="Sessions per week" type="number" step="1" min="1" max="7" required
                      value={draft.sessionsPerWeek} onChange={(e) => setDraft({ ...draft, sessionsPerWeek: e.target.value })} />
                  </label>
                  <label className="tp-field">
                    <span>Prescription status</span>
                    <select aria-label="Prescription status" value={draft.isActive ? 'ACTIVE' : 'PAUSED'}
                      onChange={(e) => setDraft({ ...draft, isActive: e.target.value === 'ACTIVE' })}>
                      <option value="ACTIVE">Active</option>
                      <option value="PAUSED">Paused</option>
                    </select>
                  </label>
                </div>
                <label className="tp-field">
                  <span>Therapist notes (optional)</span>
                  <textarea rows={3} maxLength={500} value={draft.notes} aria-label="Therapist notes"
                    onChange={(e) => setDraft({ ...draft, notes: e.target.value })}
                    placeholder="Instructions for this patient's next sessions" />
                  <small className="faint">{draft.notes.length}/500 characters</small>
                </label>
                <div className="tp-warning" role="note">
                  <b>Safety guard</b>
                  <p>The ROM ceiling must be above the target. Review all limits before saving. A plan does not replace clinical judgment.</p>
                </div>
                {validationError && <p className="error-text tp-message" role="alert">{validationError}</p>}
                {message && <p className={`${message.kind === 'ok' ? 'success-text' : 'error-text'} tp-message`} role="status">{message.text}</p>}
                <button type="submit" className="btn-primary tp-save"
                  disabled={saving || !!validationError || (!!persisted && !changed)}>
                  {saving ? 'Saving…' : persisted ? (changed ? 'Save changes' : 'Up to date') : 'Save Therapy Plan'}
                </button>
                {persisted && <p className="faint tp-saved-on">Last updated {new Date(persisted.updated_at).toLocaleString()}</p>}
              </form>
            )}
          </section>
        </div>
      )}
      <p className="faint tp-footer-note">
        Plans are saved in Supabase with therapist-assignment access control. Applying a saved plan in the Android Patient exercise flow is implemented separately in PGF-INT-02.
      </p>
    </>
  )
}
