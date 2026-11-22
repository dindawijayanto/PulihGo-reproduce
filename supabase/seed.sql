insert into public.exercises (id, name, target_rom_deg, target_reps, rom_ceiling_deg) values
  ('EX001', 'Forearm Supination and Pronation', 70.0, 10, 80.0)
on conflict (id) do update set name = excluded.name, target_rom_deg = excluded.target_rom_deg,
  target_reps = excluded.target_reps, rom_ceiling_deg = excluded.rom_ceiling_deg;

insert into public.profiles (id, role, username, full_name, email, legacy_user_id, specialization, affiliation)
select u.id, v.role, v.username, v.full_name, v.email, v.legacy_user_id, v.specialization, v.affiliation
from (values
  ('THERAPIST', 'therapist01', 'Demo Therapist', 'therapist@pulihgo.demo', 'U001', 'Upper Limb Rehabilitation', 'PulihGo Digital Tele-Rehab Hub'),
  ('PATIENT', 'patient01', 'Alya Putri', 'alya@pulihgo.demo', 'U002', null, null),
  ('PATIENT', 'patient02', 'Bima Pratama', 'bima@pulihgo.demo', 'U003', null, null)
) as v (role, username, full_name, email, legacy_user_id, specialization, affiliation)
join auth.users u on lower(u.email) = v.email
on conflict (id) do update set role = excluded.role, username = excluded.username,
  legacy_user_id = excluded.legacy_user_id;

insert into public.patients (id, profile_id, therapist_id, age, affected_arm, rehabilitation_goal, notes)
select v.id, pp.id, tp.id, v.age, v.affected_arm, v.goal, v.notes
from (values
  ('P001', 'U002', 'U001', 24, 'RIGHT', 'Forearm supination and pronation', 'Demo patient for application testing'),
  ('P002', 'U003', 'U001', 31, 'LEFT', 'Forearm supination and pronation', 'Demo patient for application testing')
) as v (id, user_id, therapist_user_id, age, affected_arm, goal, notes)
join public.profiles pp on pp.legacy_user_id = v.user_id
left join public.profiles tp on tp.legacy_user_id = v.therapist_user_id
on conflict (id) do update set profile_id = excluded.profile_id, therapist_id = excluded.therapist_id,
  age = excluded.age, affected_arm = excluded.affected_arm,
  rehabilitation_goal = excluded.rehabilitation_goal, notes = excluded.notes;

insert into public.exercise_sessions (id, patient_id, exercise_id, started_at, ended_at, repetitions,
  peak_rom_deg, average_smoothness, pain_level, source, target_reps, target_rom_deg, repetition_details) values
  ('S001', 'P001', 'EX001', '2026-11-08T09:00:00+07:00', '2026-11-08T09:03:00+07:00', 3, 56.0, 0.82, 'NO_PAIN', 'SEEDED', 10, 70.0, '[{"repetitionNumber": 1, "peakRomDeg": 56.0, "durationMs": 2500, "smoothnessScore": null}, {"repetitionNumber": 2, "peakRomDeg": 52.0, "durationMs": 2400, "smoothnessScore": null}, {"repetitionNumber": 3, "peakRomDeg": 54.0, "durationMs": 2600, "smoothnessScore": null}]'::jsonb),
  ('S002', 'P001', 'EX001', '2026-11-06T09:30:00+07:00', '2026-11-06T09:33:30+07:00', 2, 65.0, 0.88, 'NO_PAIN', 'SEEDED', 10, 70.0, '[{"repetitionNumber": 1, "peakRomDeg": 65.0, "durationMs": 2700, "smoothnessScore": null}, {"repetitionNumber": 2, "peakRomDeg": 62.0, "durationMs": 2500, "smoothnessScore": null}]'::jsonb),
  ('S003', 'P002', 'EX001', '2026-11-07T10:00:00+07:00', '2026-11-07T10:02:40+07:00', 2, 42.0, 0.71, 'STOPPED_DUE_TO_PAIN', 'SEEDED', 10, 70.0, '[{"repetitionNumber": 1, "peakRomDeg": 42.0, "durationMs": 2800, "smoothnessScore": null}, {"repetitionNumber": 2, "peakRomDeg": 38.0, "durationMs": 3000, "smoothnessScore": null}]'::jsonb)
on conflict (id) do nothing;
