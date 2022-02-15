UPDATE BRIDGE SET status='ready' WHERE status='AVAILABLE';
UPDATE BRIDGE SET status='accepted' WHERE status='REQUESTED';
UPDATE BRIDGE SET status='deprovision' WHERE status='DELETION_REQUESTED';

UPDATE PROCESSOR SET status='ready' WHERE status='AVAILABLE';
UPDATE PROCESSOR SET status='accepted' WHERE status='REQUESTED';
UPDATE PROCESSOR SET status='deprovision' WHERE status='DELETION_REQUESTED';