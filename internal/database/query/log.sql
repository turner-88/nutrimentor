-- name: UpsertMedicationLog :execresult
INSERT INTO medication_log (user_id, log_date, taken_complete, taken_on_time)
VALUES (?, ?, ?, ?)
ON DUPLICATE KEY UPDATE taken_complete = VALUES(taken_complete), taken_on_time = VALUES(taken_on_time);

-- name: GetMedicationLog :one
SELECT * FROM medication_log WHERE user_id = ? AND log_date = ? LIMIT 1;

-- name: ListMedicationLogs :many
SELECT * FROM medication_log
WHERE user_id = ? AND log_date BETWEEN ? AND ?
ORDER BY log_date DESC;

-- name: UpsertActivityLog :execresult
INSERT INTO activity_log (user_id, log_date, did_activity, per_doctor_advice)
VALUES (?, ?, ?, ?)
ON DUPLICATE KEY UPDATE did_activity = VALUES(did_activity), per_doctor_advice = VALUES(per_doctor_advice);

-- name: GetActivityLog :one
SELECT * FROM activity_log WHERE user_id = ? AND log_date = ? LIMIT 1;

-- name: ListActivityLogs :many
SELECT * FROM activity_log
WHERE user_id = ? AND log_date BETWEEN ? AND ?
ORDER BY log_date DESC;

-- name: UpsertDietLog :execresult
INSERT INTO diet_log (user_id, log_date, per_doctor_advice, on_schedule)
VALUES (?, ?, ?, ?)
ON DUPLICATE KEY UPDATE per_doctor_advice = VALUES(per_doctor_advice), on_schedule = VALUES(on_schedule);

-- name: GetDietLog :one
SELECT * FROM diet_log WHERE user_id = ? AND log_date = ? LIMIT 1;

-- name: ListDietLogs :many
SELECT * FROM diet_log
WHERE user_id = ? AND log_date BETWEEN ? AND ?
ORDER BY log_date DESC;

-- name: CreateGlucoseLog :execresult
INSERT INTO glucose_log (user_id, measured_at, timing, value_mgdl)
VALUES (?, ?, ?, ?);

-- name: ListGlucoseLogs :many
SELECT * FROM glucose_log
WHERE user_id = ? AND measured_at BETWEEN ? AND ?
ORDER BY measured_at DESC;

-- name: CountCompliantDays :one
-- Counts days in [from,to] where the patient completed all three daily pillars fully.
SELECT COUNT(*) FROM (
    SELECT m.log_date
    FROM medication_log m
    JOIN activity_log a ON a.user_id = m.user_id AND a.log_date = m.log_date
    JOIN diet_log d ON d.user_id = m.user_id AND d.log_date = m.log_date
    WHERE m.user_id = ?
      AND m.log_date >= ?
      AND m.log_date <= ?
      AND m.taken_complete = 1 AND a.did_activity = 1 AND d.on_schedule = 1
) t;
