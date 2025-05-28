CREATE TABLE `users` (
     `id`	BIGINT	NOT NULL,
     `username`	VARCHAR(100)	NULL,
     `password`	VARCHAR(60)	NULL	COMMENT 'BcryptEncoder의 암호화시 변수 길이는 항상 60고정',
     `roles`	VARCHAR(100)	NULL	COMMENT '유저 권한 항상 ADMIN이지만 SpringSecurity에서 필요한 필드',
     primary key (id)
);

-- id: monitory / pw: monitory 에 해당되는 값 bcrypt with round 10 (스프링 bcrypt 기준)
insert into users (id,username,password,roles) values
   (0,"monitory","$2a$10$MrmGAxIYU5xybtejh.0KXu6o6vI8iOaTe1IN1Cuer4Kl6zprZ.51C","ADMIN");