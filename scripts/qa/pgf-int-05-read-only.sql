-- PGF-INT-05 — READ ONLY, for Supabase SQL Editor
-- No migrations, no role changes, no writes, no credentials.

-- 1) Therapy Plans used by the demo (P001 / P002); no free-text notes.
select patient_id, exercise_id, target_rom_deg, rom_ceiling_deg,
       target_reps, sets_per_session, sessions_per_week, is_active, updated_at
from public.therapy_plans
where patient_id in ('P001', 'P002')
order by patient_id, exercise_id;

-- 2) Latest device sessions. Older records may have NULL snapshot targets.
select id, patient_id, exercise_id, target_rom_deg, target_reps,
       repetitions, peak_rom_deg, pain_level, source, started_at
from public.exercise_sessions
where patient_id in ('P001', 'P002') and source = 'DEVICE'
order by started_at desc
limit 12;

-- 3) Check for duplicate session IDs and source counts in test cohort.
select patient_id, id, count(*) as duplicate_count
from public.exercise_sessions
where patient_id in ('P001', 'P002')
group by patient_id, id
having count(*) > 1;

-- 4) Number of device sessions with/without target snapshots.
select patient_id,
       count(*) filter (where source = 'DEVICE') as device_sessions,
       count(*) filter (where source = 'DEVICE' and target_rom_deg is not null and target_reps is not null) as with_target_snapshot,
       count(*) filter (where source = 'DEVICE' and (target_rom_deg is null or target_reps is null)) as missing_target_snapshot
from public.exercise_sessions
where patient_id in ('P001', 'P002')
group by patient_id
order by patient_id;
