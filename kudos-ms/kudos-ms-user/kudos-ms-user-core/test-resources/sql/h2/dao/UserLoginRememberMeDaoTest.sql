-- 测试数据：UserLoginRememberMeDaoTest

merge into "user_login_remember_me" ("id", "username", "token", "last_used")
    values ('33333333-0000-0000-0000-000000000001', 'remember-user-1', 'token-1', CURRENT_TIMESTAMP);
