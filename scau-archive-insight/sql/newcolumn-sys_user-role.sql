 ALTER TABLE sys_user ADD COLUMN role VARCHAR(20) DEFAULT 'user';
 UPDATE sys_user SET role = 'admin' WHERE username = 'admin';