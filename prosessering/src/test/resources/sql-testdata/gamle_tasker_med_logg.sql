INSERT INTO task (id, payload, status, opprettet_tid, type, metadata, trigger_tid, avvikstype)
VALUES (1000000, '1002352', 'FERDIG', '2019-10-24 10:38:13.819', 'hentJournalpostIdFraJoarkTask', 'callId=CallId_1571906292358_2053169736
', '2019-10-24 10:38:13.818236', null);
INSERT INTO task (id, payload, status, opprettet_tid, type, metadata, trigger_tid, avvikstype)
VALUES (1000002, '1002351', 'KLAR_TIL_PLUKK', '2019-10-24 10:15:44.061', 'hentSaksnummerFraJoark', 'callId=CallId_1571904940794_1993984421
', '2019-10-24 10:45:59.678770', null);
INSERT INTO task (id, payload, status, opprettet_tid, type, metadata, trigger_tid, avvikstype)
VALUES (1000003, '1002351', 'FERDIG', '2019-10-24 10:15:42.453', 'hentJournalpostIdFraJoarkTask', 'callId=CallId_1571904940794_1993984421
', '2019-10-24 10:15:42.453038', null);

INSERT INTO task_logg (id, task_id, type, node, opprettet_tid, melding, endret_av)
VALUES (40010, 1000000, 'UBEHANDLET', 'familie-ks-mottak-5fd7d6649b-gnx4j', '2019-10-24 10:38:13.819', null, 'VL');
INSERT INTO task_logg (id, task_id, type, node, opprettet_tid, melding, endret_av)
VALUES (40011, 1000000, 'BEHANDLER', 'familie-ks-mottak-5fd7d6649b-k78gn', '2019-10-24 10:38:29.378', null, 'VL');
INSERT INTO task_logg (id, task_id, type, node, opprettet_tid, melding, endret_av)
VALUES (40012, 1000000, 'FERDIG', 'familie-ks-mottak-5fd7d6649b-k78gn', '2019-10-24 10:38:32.398', null, 'VL');
INSERT INTO task_logg (id, task_id, type, node, opprettet_tid, melding, endret_av)
VALUES (40013, 1000002, 'UBEHANDLET', 'familie-ks-mottak-5fd7d6649b-gnx4j', '2019-10-24 10:38:13.819', null, 'VL');
INSERT INTO task_logg (id, task_id, type, node, opprettet_tid, melding, endret_av)
VALUES (40014, 1000002, 'BEHANDLER', 'familie-ks-mottak-5fd7d6649b-k78gn', '2019-10-24 10:38:29.378', null, 'VL');
INSERT INTO task_logg (id, task_id, type, node, opprettet_tid, melding, endret_av)
VALUES (40015, 1000002, 'FERDIG', 'familie-ks-mottak-5fd7d6649b-k78gn', '2019-10-24 10:38:32.398', null, 'VL');
