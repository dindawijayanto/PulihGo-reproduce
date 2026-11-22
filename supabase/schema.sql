create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  role text not null check (role in ('PATIENT', 'THERAPIST')),
  username text not null unique,
  full_name text not null,
  email text not null,
  legacy_user_id text unique,
  specialization text,
  affiliation text,
  audio_alerts boolean not null default true,
  auto_sync boolean not null default true,
  language text not null default 'ENGLISH' check (language in ('ENGLISH', 'INDONESIAN')),
  updated_at timestamptz not null default now()
);

create table if not exists public.patients (
  id text primary key,
  profile_id uuid not null unique references public.profiles (id) on delete cascade,
  therapist_id uuid references public.profiles (id) on delete set null,
  age int,
  affected_arm text,
  rehabilitation_goal text,
  notes text
);

create table if not exists public.exercises (
  id text primary key,
  name text not null,
  target_rom_deg double precision not null,
  target_reps int not null,
  rom_ceiling_deg double precision not null
);

create table if not exists public.exercise_sessions (
  id text primary key,
  patient_id text not null references public.patients (id) on delete cascade,
  exercise_id text not null references public.exercises (id),
  started_at timestamptz not null,
  ended_at timestamptz not null,
  repetitions int not null check (repetitions >= 0),
  peak_rom_deg double precision not null check (peak_rom_deg >= 0),
  average_smoothness double precision check (average_smoothness is null or average_smoothness between 0 and 1),
  pain_level text not null check (pain_level in ('NO_PAIN', 'MILD_PAIN', 'STOPPED_DUE_TO_PAIN')),
  source text not null check (source in ('SEEDED', 'DEVICE')),
  target_reps int,
  target_rom_deg double precision,
  repetition_details jsonb not null default '[]'::jsonb,
  synced_at timestamptz not null default now(),
  check (ended_at >= started_at)
);

create index if not exists exercise_sessions_patient_started_idx
  on public.exercise_sessions (patient_id, started_at desc);
create index if not exists patients_therapist_idx on public.patients (therapist_id);

grant usage on schema public to authenticated;
grant select on public.profiles, public.patients, public.exercises, public.exercise_sessions to authenticated;
grant insert on public.exercise_sessions to authenticated;

alter table public.profiles enable row level security;
alter table public.patients enable row level security;
alter table public.exercises enable row level security;
alter table public.exercise_sessions enable row level security;

create or replace function public.is_own_patient_record(pid text)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.patients where id = pid and profile_id = auth.uid());
$$;

create or replace function public.is_assigned_patient(pid text)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.patients where id = pid and therapist_id = auth.uid());
$$;

drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles for select to authenticated using (
  id = auth.uid()
  or exists (select 1 from public.patients p where p.profile_id = profiles.id and p.therapist_id = auth.uid())
  or exists (select 1 from public.patients p where p.profile_id = auth.uid() and p.therapist_id = profiles.id)
);

drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own on public.profiles for update to authenticated
  using (id = auth.uid()) with check (id = auth.uid());

revoke update on public.profiles from authenticated, anon;
grant update (full_name, email, specialization, affiliation, audio_alerts, auto_sync, language, updated_at)
  on public.profiles to authenticated;

drop policy if exists patients_select on public.patients;
create policy patients_select on public.patients for select to authenticated using (
  profile_id = auth.uid() or therapist_id = auth.uid()
);

drop policy if exists exercises_select on public.exercises;
create policy exercises_select on public.exercises for select to authenticated using (true);

drop policy if exists sessions_select on public.exercise_sessions;
create policy sessions_select on public.exercise_sessions for select to authenticated using (
  public.is_own_patient_record(patient_id) or public.is_assigned_patient(patient_id)
);

drop policy if exists sessions_insert_own on public.exercise_sessions;
create policy sessions_insert_own on public.exercise_sessions for insert to authenticated with check (
  public.is_own_patient_record(patient_id) and source = 'DEVICE'
);

revoke update, delete on public.exercise_sessions from authenticated, anon;

grant execute on function public.is_own_patient_record(text) to authenticated;
grant execute on function public.is_assigned_patient(text) to authenticated;
revoke execute on function public.is_own_patient_record(text) from anon, public;
revoke execute on function public.is_assigned_patient(text) from anon, public;
