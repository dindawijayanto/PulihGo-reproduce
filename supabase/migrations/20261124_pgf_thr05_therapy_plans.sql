-- PGF-THR-05: patient-specific clinical Therapy Plans.
-- Apply ONCE with Supabase SQL Editor after schema.sql (uses existing helper functions).
-- No service_role keys are required in any client. RLS allows only assigned therapist writes.
begin;

create table if not exists public.therapy_plans (
  patient_id text not null references public.patients(id) on delete cascade,
  exercise_id text not null references public.exercises(id),
  target_rom_deg double precision not null check (target_rom_deg > 0 and target_rom_deg < 180),
  rom_ceiling_deg double precision not null check (rom_ceiling_deg > target_rom_deg and rom_ceiling_deg <= 180),
  target_reps integer not null check (target_reps between 1 and 100),
  sets_per_session integer not null default 1 check (sets_per_session between 1 and 10),
  sessions_per_week integer not null default 3 check (sessions_per_week between 1 and 7),
  notes text not null default '' check (char_length(notes) <= 500),
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  updated_by uuid not null default auth.uid() references public.profiles(id),
  primary key (patient_id, exercise_id)
);

create index if not exists therapy_plans_patient_idx on public.therapy_plans (patient_id);

-- Server-maintained audit columns. The browser cannot override them.
create or replace function public.touch_therapy_plan_audit()
returns trigger language plpgsql set search_path = public as $$
begin
  new.updated_at := now();
  new.updated_by := auth.uid();
  return new;
end;
$$;

drop trigger if exists therapy_plans_touch_audit on public.therapy_plans;
create trigger therapy_plans_touch_audit
  before update on public.therapy_plans for each row
  execute function public.touch_therapy_plan_audit();

alter table public.therapy_plans enable row level security;

-- A patient can read ONLY their own prescription, and a therapist ONLY their assigned patients.
drop policy if exists therapy_plans_read_assigned_or_own on public.therapy_plans;
create policy therapy_plans_read_assigned_or_own on public.therapy_plans
  for select to authenticated
  using (
    public.is_own_patient_record(patient_id)
    or public.is_assigned_patient(patient_id)
  );

-- A therapist can save ONLY for currently assigned patients. No patient writes.
drop policy if exists therapy_plans_insert_assigned_therapist on public.therapy_plans;
create policy therapy_plans_insert_assigned_therapist on public.therapy_plans
  for insert to authenticated
  with check (
    public.is_assigned_patient(patient_id)
    and exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'THERAPIST')
    and updated_by = auth.uid()
  );

drop policy if exists therapy_plans_update_assigned_therapist on public.therapy_plans;
create policy therapy_plans_update_assigned_therapist on public.therapy_plans
  for update to authenticated
  using (
    public.is_assigned_patient(patient_id)
    and exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'THERAPIST')
  )
  with check (
    public.is_assigned_patient(patient_id)
    and exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'THERAPIST')
    and updated_by = auth.uid()
  );

-- Restrict grants (including update columns); enforce RLS independently of the web UI.
revoke all on table public.therapy_plans from public, anon, authenticated;
grant select on table public.therapy_plans to authenticated;
grant insert (patient_id, exercise_id, target_rom_deg, rom_ceiling_deg, target_reps,
  sets_per_session, sessions_per_week, notes, is_active)
  on table public.therapy_plans to authenticated;
grant update (target_rom_deg, rom_ceiling_deg, target_reps,
  sets_per_session, sessions_per_week, notes, is_active)
  on table public.therapy_plans to authenticated;

grant usage on schema public to authenticated;

commit;
