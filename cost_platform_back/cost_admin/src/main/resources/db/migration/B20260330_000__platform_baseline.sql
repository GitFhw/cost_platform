-- Flyway baseline migration for cost_platform
-- Generated from legacy initialization scripts

-- ----------------------------
-- 1、部门表
-- ----------------------------
drop table if exists sys_dept;
create table sys_dept (
  dept_id           bigint(20)      not null auto_increment    comment '部门id',
  parent_id         bigint(20)      default 0                  comment '父部门id',
  ancestors         varchar(50)     default ''                 comment '祖级列表',
  dept_name         varchar(30)     default ''                 comment '部门名称',
  order_num         int(4)          default 0                  comment '显示顺序',
  leader            varchar(20)     default null               comment '负责人',
  phone             varchar(11)     default null               comment '联系电话',
  email             varchar(50)     default null               comment '邮箱',
  status            char(1)         default '0'                comment '部门状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time 	    datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  primary key (dept_id)
) engine=innodb auto_increment=200 comment = '部门表';

-- ----------------------------
-- 初始化-部门表数据
-- ----------------------------
insert into sys_dept values(100,  0,   '0',          '若依科技',   0, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(101,  100, '0,100',      '深圳总公司', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(102,  100, '0,100',      '长沙分公司', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(103,  101, '0,100,101',  '研发部门',   1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(104,  101, '0,100,101',  '市场部门',   2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(105,  101, '0,100,101',  '测试部门',   3, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(106,  101, '0,100,101',  '财务部门',   4, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(107,  101, '0,100,101',  '运维部门',   5, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(108,  102, '0,100,102',  '市场部门',   1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);
insert into sys_dept values(109,  102, '0,100,102',  '财务部门',   2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', sysdate(), '', null);


-- ----------------------------
-- 2、用户信息表
-- ----------------------------
drop table if exists sys_user;
create table sys_user (
  user_id           bigint(20)      not null auto_increment    comment '用户ID',
  dept_id           bigint(20)      default null               comment '部门ID',
  user_name         varchar(30)     not null                   comment '用户账号',
  nick_name         varchar(30)     not null                   comment '用户昵称',
  user_type         varchar(2)      default '00'               comment '用户类型（00系统用户）',
  email             varchar(50)     default ''                 comment '用户邮箱',
  phonenumber       varchar(11)     default ''                 comment '手机号码',
  sex               char(1)         default '0'                comment '用户性别（0男 1女 2未知）',
  avatar            varchar(100)    default ''                 comment '头像地址',
  password          varchar(100)    default ''                 comment '密码',
  status            char(1)         default '0'                comment '账号状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  login_ip          varchar(128)    default ''                 comment '最后登录IP',
  login_date        datetime                                   comment '最后登录时间',
  pwd_update_date   datetime                                   comment '密码最后更新时间',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (user_id)
) engine=innodb auto_increment=100 comment = '用户信息表';

-- ----------------------------
-- 初始化-用户信息表数据
-- ----------------------------
insert into sys_user values(1,  103, 'admin', '若依', '00', 'ry@163.com', '15888888888', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, '管理员');
insert into sys_user values(2,  105, 'ry',    '若依', '00', 'ry@qq.com',  '15666666666', '1', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', '127.0.0.1', sysdate(), sysdate(), 'admin', sysdate(), '', null, '测试员');


-- ----------------------------
-- 3、岗位信息表
-- ----------------------------
drop table if exists sys_post;
create table sys_post
(
  post_id       bigint(20)      not null auto_increment    comment '岗位ID',
  post_code     varchar(64)     not null                   comment '岗位编码',
  post_name     varchar(50)     not null                   comment '岗位名称',
  post_sort     int(4)          not null                   comment '显示顺序',
  status        char(1)         not null                   comment '状态（0正常 1停用）',
  create_by     varchar(64)     default ''                 comment '创建者',
  create_time   datetime                                   comment '创建时间',
  update_by     varchar(64)     default ''			       comment '更新者',
  update_time   datetime                                   comment '更新时间',
  remark        varchar(500)    default null               comment '备注',
  primary key (post_id)
) engine=innodb comment = '岗位信息表';

-- ----------------------------
-- 初始化-岗位信息表数据
-- ----------------------------
insert into sys_post values(1, 'ceo',  '董事长',    1, '0', 'admin', sysdate(), '', null, '');
insert into sys_post values(2, 'se',   '项目经理',  2, '0', 'admin', sysdate(), '', null, '');
insert into sys_post values(3, 'hr',   '人力资源',  3, '0', 'admin', sysdate(), '', null, '');
insert into sys_post values(4, 'user', '普通员工',  4, '0', 'admin', sysdate(), '', null, '');


-- ----------------------------
-- 4、角色信息表
-- ----------------------------
drop table if exists sys_role;
create table sys_role (
  role_id              bigint(20)      not null auto_increment    comment '角色ID',
  role_name            varchar(30)     not null                   comment '角色名称',
  role_key             varchar(100)    not null                   comment '角色权限字符串',
  role_sort            int(4)          not null                   comment '显示顺序',
  data_scope           char(1)         default '1'                comment '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限）',
  menu_check_strictly  tinyint(1)      default 1                  comment '菜单树选择项是否关联显示',
  dept_check_strictly  tinyint(1)      default 1                  comment '部门树选择项是否关联显示',
  status               char(1)         not null                   comment '角色状态（0正常 1停用）',
  del_flag             char(1)         default '0'                comment '删除标志（0代表存在 2代表删除）',
  create_by            varchar(64)     default ''                 comment '创建者',
  create_time          datetime                                   comment '创建时间',
  update_by            varchar(64)     default ''                 comment '更新者',
  update_time          datetime                                   comment '更新时间',
  remark               varchar(500)    default null               comment '备注',
  primary key (role_id)
) engine=innodb auto_increment=100 comment = '角色信息表';

-- ----------------------------
-- 初始化-角色信息表数据
-- ----------------------------
insert into sys_role values('1', '超级管理员',  'admin',  1, 1, 1, 1, '0', '0', 'admin', sysdate(), '', null, '超级管理员');
insert into sys_role values('2', '普通角色',    'common', 2, 2, 1, 1, '0', '0', 'admin', sysdate(), '', null, '普通角色');


-- ----------------------------
-- 5、菜单权限表
-- ----------------------------
drop table if exists sys_menu;
create table sys_menu (
  menu_id           bigint(20)      not null auto_increment    comment '菜单ID',
  menu_name         varchar(50)     not null                   comment '菜单名称',
  parent_id         bigint(20)      default 0                  comment '父菜单ID',
  order_num         int(4)          default 0                  comment '显示顺序',
  path              varchar(200)    default ''                 comment '路由地址',
  component         varchar(255)    default null               comment '组件路径',
  query             varchar(255)    default null               comment '路由参数',
  route_name        varchar(50)     default ''                 comment '路由名称',
  is_frame          int(1)          default 1                  comment '是否为外链（0是 1否）',
  is_cache          int(1)          default 0                  comment '是否缓存（0缓存 1不缓存）',
  menu_type         char(1)         default ''                 comment '菜单类型（M目录 C菜单 F按钮）',
  visible           char(1)         default 0                  comment '菜单状态（0显示 1隐藏）',
  status            char(1)         default 0                  comment '菜单状态（0正常 1停用）',
  perms             varchar(100)    default null               comment '权限标识',
  icon              varchar(100)    default '#'                comment '菜单图标',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default ''                 comment '备注',
  primary key (menu_id)
) engine=innodb auto_increment=2000 comment = '菜单权限表';

-- ----------------------------
-- 初始化-菜单信息表数据
-- ----------------------------
-- 一级菜单
insert into sys_menu values('1', '系统管理', '0', '1', 'system',           null, '', '', 1, 0, 'M', '0', '0', '', 'system',   'admin', sysdate(), '', null, '系统管理目录');
insert into sys_menu values('2', '系统监控', '0', '2', 'monitor',          null, '', '', 1, 0, 'M', '0', '0', '', 'monitor',  'admin', sysdate(), '', null, '系统监控目录');
insert into sys_menu values('3', '系统工具', '0', '3', 'tool',             null, '', '', 1, 0, 'M', '0', '0', '', 'tool',     'admin', sysdate(), '', null, '系统工具目录');
insert into sys_menu values('4', '若依官网', '0', '4', 'http://ruoyi.vip', null, '', '', 0, 0, 'M', '0', '0', '', 'guide',    'admin', sysdate(), '', null, '若依官网地址');
-- 二级菜单
insert into sys_menu values('100',  '用户管理', '1',   '1', 'user',       'system/user/index',        '', '', 1, 0, 'C', '0', '0', 'system:user:list',        'user',          'admin', sysdate(), '', null, '用户管理菜单');
insert into sys_menu values('101',  '角色管理', '1',   '2', 'role',       'system/role/index',        '', '', 1, 0, 'C', '0', '0', 'system:role:list',        'peoples',       'admin', sysdate(), '', null, '角色管理菜单');
insert into sys_menu values('102',  '菜单管理', '1',   '3', 'menu',       'system/menu/index',        '', '', 1, 0, 'C', '0', '0', 'system:menu:list',        'tree-table',    'admin', sysdate(), '', null, '菜单管理菜单');
insert into sys_menu values('103',  '部门管理', '1',   '4', 'dept',       'system/dept/index',        '', '', 1, 0, 'C', '0', '0', 'system:dept:list',        'tree',          'admin', sysdate(), '', null, '部门管理菜单');
insert into sys_menu values('104',  '岗位管理', '1',   '5', 'post',       'system/post/index',        '', '', 1, 0, 'C', '0', '0', 'system:post:list',        'post',          'admin', sysdate(), '', null, '岗位管理菜单');
insert into sys_menu values('105',  '字典管理', '1',   '6', 'dict',       'system/dict/index',        '', '', 1, 0, 'C', '0', '0', 'system:dict:list',        'dict',          'admin', sysdate(), '', null, '字典管理菜单');
insert into sys_menu values('106',  '参数设置', '1',   '7', 'config',     'system/config/index',      '', '', 1, 0, 'C', '0', '0', 'system:config:list',      'edit',          'admin', sysdate(), '', null, '参数设置菜单');
insert into sys_menu values('107',  '通知公告', '1',   '8', 'notice',     'system/notice/index',      '', '', 1, 0, 'C', '0', '0', 'system:notice:list',      'message',       'admin', sysdate(), '', null, '通知公告菜单');
insert into sys_menu values('108',  '日志管理', '1',   '9', 'log',        '',                         '', '', 1, 0, 'M', '0', '0', '',                        'log',           'admin', sysdate(), '', null, '日志管理菜单');
insert into sys_menu values('109',  '在线用户', '2',   '1', 'online',     'monitor/online/index',     '', '', 1, 0, 'C', '0', '0', 'monitor:online:list',     'online',        'admin', sysdate(), '', null, '在线用户菜单');
insert into sys_menu values('110',  '定时任务', '2',   '2', 'job',        'monitor/job/index',        '', '', 1, 0, 'C', '0', '0', 'monitor:job:list',        'job',           'admin', sysdate(), '', null, '定时任务菜单');
insert into sys_menu values('111',  '数据监控', '2',   '3', 'druid',      'monitor/druid/index',      '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list',      'druid',         'admin', sysdate(), '', null, '数据监控菜单');
insert into sys_menu values('112',  '服务监控', '2',   '4', 'server',     'monitor/server/index',     '', '', 1, 0, 'C', '0', '0', 'monitor:server:list',     'server',        'admin', sysdate(), '', null, '服务监控菜单');
insert into sys_menu values('113',  '缓存监控', '2',   '5', 'cache',      'monitor/cache/index',      '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',      'redis',         'admin', sysdate(), '', null, '缓存监控菜单');
insert into sys_menu values('114',  '缓存列表', '2',   '6', 'cacheList',  'monitor/cache/list',       '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',      'redis-list',    'admin', sysdate(), '', null, '缓存列表菜单');
insert into sys_menu values('115',  '表单构建', '3',   '1', 'build',      'tool/build/index',         '', '', 1, 0, 'C', '0', '0', 'tool:build:list',         'build',         'admin', sysdate(), '', null, '表单构建菜单');
insert into sys_menu values('116',  '代码生成', '3',   '2', 'gen',        'tool/gen/index',           '', '', 1, 0, 'C', '0', '0', 'tool:gen:list',           'code',          'admin', sysdate(), '', null, '代码生成菜单');
insert into sys_menu values('117',  '系统接口', '3',   '3', 'swagger',    'tool/swagger/index',       '', '', 1, 0, 'C', '0', '0', 'tool:swagger:list',       'swagger',       'admin', sysdate(), '', null, '系统接口菜单');
-- 三级菜单
insert into sys_menu values('500',  '操作日志', '108', '1', 'operlog',    'monitor/operlog/index',    '', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list',    'form',          'admin', sysdate(), '', null, '操作日志菜单');
insert into sys_menu values('501',  '登录日志', '108', '2', 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor',    'admin', sysdate(), '', null, '登录日志菜单');
-- 用户管理按钮
insert into sys_menu values('1000', '用户查询', '100', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1001', '用户新增', '100', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1002', '用户修改', '100', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1003', '用户删除', '100', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1004', '用户导出', '100', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1005', '用户导入', '100', '6',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1006', '重置密码', '100', '7',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd',       '#', 'admin', sysdate(), '', null, '');
-- 角色管理按钮
insert into sys_menu values('1007', '角色查询', '101', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1008', '角色新增', '101', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1009', '角色修改', '101', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1010', '角色删除', '101', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1011', '角色导出', '101', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:export',         '#', 'admin', sysdate(), '', null, '');
-- 菜单管理按钮
insert into sys_menu values('1012', '菜单查询', '102', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1013', '菜单新增', '102', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1014', '菜单修改', '102', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1015', '菜单删除', '102', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove',         '#', 'admin', sysdate(), '', null, '');
-- 部门管理按钮
insert into sys_menu values('1016', '部门查询', '103', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1017', '部门新增', '103', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1018', '部门修改', '103', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1019', '部门删除', '103', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove',         '#', 'admin', sysdate(), '', null, '');
-- 岗位管理按钮
insert into sys_menu values('1020', '岗位查询', '104', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1021', '岗位新增', '104', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1022', '岗位修改', '104', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1023', '岗位删除', '104', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1024', '岗位导出', '104', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:export',         '#', 'admin', sysdate(), '', null, '');
-- 字典管理按钮
insert into sys_menu values('1025', '字典查询', '105', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1026', '字典新增', '105', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1027', '字典修改', '105', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1028', '字典删除', '105', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1029', '字典导出', '105', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:export',         '#', 'admin', sysdate(), '', null, '');
-- 参数设置按钮
insert into sys_menu values('1030', '参数查询', '106', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:query',        '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1031', '参数新增', '106', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:add',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1032', '参数修改', '106', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:edit',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1033', '参数删除', '106', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:remove',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1034', '参数导出', '106', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:export',       '#', 'admin', sysdate(), '', null, '');
-- 通知公告按钮
insert into sys_menu values('1035', '公告查询', '107', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:query',        '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1036', '公告新增', '107', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:add',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1037', '公告修改', '107', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1038', '公告删除', '107', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove',       '#', 'admin', sysdate(), '', null, '');
-- 操作日志按钮
insert into sys_menu values('1039', '操作查询', '500', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query',      '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1040', '操作删除', '500', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove',     '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1041', '日志导出', '500', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export',     '#', 'admin', sysdate(), '', null, '');
-- 登录日志按钮
insert into sys_menu values('1042', '登录查询', '501', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1043', '登录删除', '501', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1044', '日志导出', '501', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export',  '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1045', '账户解锁', '501', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock',  '#', 'admin', sysdate(), '', null, '');
-- 在线用户按钮
insert into sys_menu values('1046', '在线查询', '109', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query',       '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1047', '批量强退', '109', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1048', '单条强退', '109', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', sysdate(), '', null, '');
-- 定时任务按钮
insert into sys_menu values('1049', '任务查询', '110', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query',          '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1050', '任务新增', '110', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1051', '任务修改', '110', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1052', '任务删除', '110', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove',         '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1053', '状态修改', '110', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus',   '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1054', '任务导出', '110', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export',         '#', 'admin', sysdate(), '', null, '');
-- 代码生成按钮
insert into sys_menu values('1055', '生成查询', '116', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query',             '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1056', '生成修改', '116', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit',              '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1057', '生成删除', '116', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1058', '导入代码', '116', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import',            '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1059', '预览代码', '116', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview',           '#', 'admin', sysdate(), '', null, '');
insert into sys_menu values('1060', '生成代码', '116', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code',              '#', 'admin', sysdate(), '', null, '');


-- ----------------------------
-- 6、用户和角色关联表  用户N-1角色
-- ----------------------------
drop table if exists sys_user_role;
create table sys_user_role (
  user_id   bigint(20) not null comment '用户ID',
  role_id   bigint(20) not null comment '角色ID',
  primary key(user_id, role_id)
) engine=innodb comment = '用户和角色关联表';

-- ----------------------------
-- 初始化-用户和角色关联表数据
-- ----------------------------
insert into sys_user_role values ('1', '1');
insert into sys_user_role values ('2', '2');


-- ----------------------------
-- 7、角色和菜单关联表  角色1-N菜单
-- ----------------------------
drop table if exists sys_role_menu;
create table sys_role_menu (
  role_id   bigint(20) not null comment '角色ID',
  menu_id   bigint(20) not null comment '菜单ID',
  primary key(role_id, menu_id)
) engine=innodb comment = '角色和菜单关联表';

-- ----------------------------
-- 初始化-角色和菜单关联表数据
-- ----------------------------
insert into sys_role_menu values ('2', '1');
insert into sys_role_menu values ('2', '2');
insert into sys_role_menu values ('2', '3');
insert into sys_role_menu values ('2', '4');
insert into sys_role_menu values ('2', '100');
insert into sys_role_menu values ('2', '101');
insert into sys_role_menu values ('2', '102');
insert into sys_role_menu values ('2', '103');
insert into sys_role_menu values ('2', '104');
insert into sys_role_menu values ('2', '105');
insert into sys_role_menu values ('2', '106');
insert into sys_role_menu values ('2', '107');
insert into sys_role_menu values ('2', '108');
insert into sys_role_menu values ('2', '109');
insert into sys_role_menu values ('2', '110');
insert into sys_role_menu values ('2', '111');
insert into sys_role_menu values ('2', '112');
insert into sys_role_menu values ('2', '113');
insert into sys_role_menu values ('2', '114');
insert into sys_role_menu values ('2', '115');
insert into sys_role_menu values ('2', '116');
insert into sys_role_menu values ('2', '117');
insert into sys_role_menu values ('2', '500');
insert into sys_role_menu values ('2', '501');
insert into sys_role_menu values ('2', '1000');
insert into sys_role_menu values ('2', '1001');
insert into sys_role_menu values ('2', '1002');
insert into sys_role_menu values ('2', '1003');
insert into sys_role_menu values ('2', '1004');
insert into sys_role_menu values ('2', '1005');
insert into sys_role_menu values ('2', '1006');
insert into sys_role_menu values ('2', '1007');
insert into sys_role_menu values ('2', '1008');
insert into sys_role_menu values ('2', '1009');
insert into sys_role_menu values ('2', '1010');
insert into sys_role_menu values ('2', '1011');
insert into sys_role_menu values ('2', '1012');
insert into sys_role_menu values ('2', '1013');
insert into sys_role_menu values ('2', '1014');
insert into sys_role_menu values ('2', '1015');
insert into sys_role_menu values ('2', '1016');
insert into sys_role_menu values ('2', '1017');
insert into sys_role_menu values ('2', '1018');
insert into sys_role_menu values ('2', '1019');
insert into sys_role_menu values ('2', '1020');
insert into sys_role_menu values ('2', '1021');
insert into sys_role_menu values ('2', '1022');
insert into sys_role_menu values ('2', '1023');
insert into sys_role_menu values ('2', '1024');
insert into sys_role_menu values ('2', '1025');
insert into sys_role_menu values ('2', '1026');
insert into sys_role_menu values ('2', '1027');
insert into sys_role_menu values ('2', '1028');
insert into sys_role_menu values ('2', '1029');
insert into sys_role_menu values ('2', '1030');
insert into sys_role_menu values ('2', '1031');
insert into sys_role_menu values ('2', '1032');
insert into sys_role_menu values ('2', '1033');
insert into sys_role_menu values ('2', '1034');
insert into sys_role_menu values ('2', '1035');
insert into sys_role_menu values ('2', '1036');
insert into sys_role_menu values ('2', '1037');
insert into sys_role_menu values ('2', '1038');
insert into sys_role_menu values ('2', '1039');
insert into sys_role_menu values ('2', '1040');
insert into sys_role_menu values ('2', '1041');
insert into sys_role_menu values ('2', '1042');
insert into sys_role_menu values ('2', '1043');
insert into sys_role_menu values ('2', '1044');
insert into sys_role_menu values ('2', '1045');
insert into sys_role_menu values ('2', '1046');
insert into sys_role_menu values ('2', '1047');
insert into sys_role_menu values ('2', '1048');
insert into sys_role_menu values ('2', '1049');
insert into sys_role_menu values ('2', '1050');
insert into sys_role_menu values ('2', '1051');
insert into sys_role_menu values ('2', '1052');
insert into sys_role_menu values ('2', '1053');
insert into sys_role_menu values ('2', '1054');
insert into sys_role_menu values ('2', '1055');
insert into sys_role_menu values ('2', '1056');
insert into sys_role_menu values ('2', '1057');
insert into sys_role_menu values ('2', '1058');
insert into sys_role_menu values ('2', '1059');
insert into sys_role_menu values ('2', '1060');

-- ----------------------------
-- 8、角色和部门关联表  角色1-N部门
-- ----------------------------
drop table if exists sys_role_dept;
create table sys_role_dept (
  role_id   bigint(20) not null comment '角色ID',
  dept_id   bigint(20) not null comment '部门ID',
  primary key(role_id, dept_id)
) engine=innodb comment = '角色和部门关联表';

-- ----------------------------
-- 初始化-角色和部门关联表数据
-- ----------------------------
insert into sys_role_dept values ('2', '100');
insert into sys_role_dept values ('2', '101');
insert into sys_role_dept values ('2', '105');


-- ----------------------------
-- 9、用户与岗位关联表  用户1-N岗位
-- ----------------------------
drop table if exists sys_user_post;
create table sys_user_post
(
  user_id   bigint(20) not null comment '用户ID',
  post_id   bigint(20) not null comment '岗位ID',
  primary key (user_id, post_id)
) engine=innodb comment = '用户与岗位关联表';

-- ----------------------------
-- 初始化-用户与岗位关联表数据
-- ----------------------------
insert into sys_user_post values ('1', '1');
insert into sys_user_post values ('2', '2');


-- ----------------------------
-- 10、操作日志记录
-- ----------------------------
drop table if exists sys_oper_log;
create table sys_oper_log (
  oper_id           bigint(20)      not null auto_increment    comment '日志主键',
  title             varchar(50)     default ''                 comment '模块标题',
  business_type     int(2)          default 0                  comment '业务类型（0其它 1新增 2修改 3删除）',
  method            varchar(200)    default ''                 comment '方法名称',
  request_method    varchar(10)     default ''                 comment '请求方式',
  operator_type     int(1)          default 0                  comment '操作类别（0其它 1后台用户 2手机端用户）',
  oper_name         varchar(50)     default ''                 comment '操作人员',
  dept_name         varchar(50)     default ''                 comment '部门名称',
  oper_url          varchar(255)    default ''                 comment '请求URL',
  oper_ip           varchar(128)    default ''                 comment '主机地址',
  oper_location     varchar(255)    default ''                 comment '操作地点',
  oper_param        varchar(2000)   default ''                 comment '请求参数',
  json_result       varchar(2000)   default ''                 comment '返回参数',
  status            int(1)          default 0                  comment '操作状态（0正常 1异常）',
  error_msg         varchar(2000)   default ''                 comment '错误消息',
  oper_time         datetime                                   comment '操作时间',
  cost_time         bigint(20)      default 0                  comment '消耗时间',
  primary key (oper_id),
  key idx_sys_oper_log_bt (business_type),
  key idx_sys_oper_log_s  (status),
  key idx_sys_oper_log_ot (oper_time)
) engine=innodb auto_increment=100 comment = '操作日志记录';


-- ----------------------------
-- 11、字典类型表
-- ----------------------------
drop table if exists sys_dict_type;
create table sys_dict_type
(
  dict_id          bigint(20)      not null auto_increment    comment '字典主键',
  dict_name        varchar(100)    default ''                 comment '字典名称',
  dict_type        varchar(100)    default ''                 comment '字典类型',
  status           char(1)         default '0'                comment '状态（0正常 1停用）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (dict_id),
  unique (dict_type)
) engine=innodb auto_increment=100 comment = '字典类型表';

insert into sys_dict_type values(1,  '用户性别', 'sys_user_sex',        '0', 'admin', sysdate(), '', null, '用户性别列表');
insert into sys_dict_type values(2,  '菜单状态', 'sys_show_hide',       '0', 'admin', sysdate(), '', null, '菜单状态列表');
insert into sys_dict_type values(3,  '系统开关', 'sys_normal_disable',  '0', 'admin', sysdate(), '', null, '系统开关列表');
insert into sys_dict_type values(4,  '任务状态', 'sys_job_status',      '0', 'admin', sysdate(), '', null, '任务状态列表');
insert into sys_dict_type values(5,  '任务分组', 'sys_job_group',       '0', 'admin', sysdate(), '', null, '任务分组列表');
insert into sys_dict_type values(6,  '系统是否', 'sys_yes_no',          '0', 'admin', sysdate(), '', null, '系统是否列表');
insert into sys_dict_type values(7,  '通知类型', 'sys_notice_type',     '0', 'admin', sysdate(), '', null, '通知类型列表');
insert into sys_dict_type values(8,  '通知状态', 'sys_notice_status',   '0', 'admin', sysdate(), '', null, '通知状态列表');
insert into sys_dict_type values(9,  '操作类型', 'sys_oper_type',       '0', 'admin', sysdate(), '', null, '操作类型列表');
insert into sys_dict_type values(10, '系统状态', 'sys_common_status',   '0', 'admin', sysdate(), '', null, '登录状态列表');


-- ----------------------------
-- 12、字典数据表
-- ----------------------------
drop table if exists sys_dict_data;
create table sys_dict_data
(
  dict_code        bigint(20)      not null auto_increment    comment '字典编码',
  dict_sort        int(4)          default 0                  comment '字典排序',
  dict_label       varchar(100)    default ''                 comment '字典标签',
  dict_value       varchar(100)    default ''                 comment '字典键值',
  dict_type        varchar(100)    default ''                 comment '字典类型',
  css_class        varchar(100)    default null               comment '样式属性（其他样式扩展）',
  list_class       varchar(100)    default null               comment '表格回显样式',
  is_default       char(1)         default 'N'                comment '是否默认（Y是 N否）',
  status           char(1)         default '0'                comment '状态（0正常 1停用）',
  create_by        varchar(64)     default ''                 comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        varchar(64)     default ''                 comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (dict_code)
) engine=innodb auto_increment=100 comment = '字典数据表';

insert into sys_dict_data values(1,  1,  '男',       '0',       'sys_user_sex',        '',   '',        'Y', '0', 'admin', sysdate(), '', null, '性别男');
insert into sys_dict_data values(2,  2,  '女',       '1',       'sys_user_sex',        '',   '',        'N', '0', 'admin', sysdate(), '', null, '性别女');
insert into sys_dict_data values(3,  3,  '未知',     '2',       'sys_user_sex',        '',   '',        'N', '0', 'admin', sysdate(), '', null, '性别未知');
insert into sys_dict_data values(4,  1,  '显示',     '0',       'sys_show_hide',       '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '显示菜单');
insert into sys_dict_data values(5,  2,  '隐藏',     '1',       'sys_show_hide',       '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '隐藏菜单');
insert into sys_dict_data values(6,  1,  '正常',     '0',       'sys_normal_disable',  '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '正常状态');
insert into sys_dict_data values(7,  2,  '停用',     '1',       'sys_normal_disable',  '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '停用状态');
insert into sys_dict_data values(8,  1,  '正常',     '0',       'sys_job_status',      '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '正常状态');
insert into sys_dict_data values(9,  2,  '暂停',     '1',       'sys_job_status',      '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '停用状态');
insert into sys_dict_data values(10, 1,  '默认',     'DEFAULT', 'sys_job_group',       '',   '',        'Y', '0', 'admin', sysdate(), '', null, '默认分组');
insert into sys_dict_data values(11, 2,  '系统',     'SYSTEM',  'sys_job_group',       '',   '',        'N', '0', 'admin', sysdate(), '', null, '系统分组');
insert into sys_dict_data values(12, 1,  '是',       'Y',       'sys_yes_no',          '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '系统默认是');
insert into sys_dict_data values(13, 2,  '否',       'N',       'sys_yes_no',          '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '系统默认否');
insert into sys_dict_data values(14, 1,  '通知',     '1',       'sys_notice_type',     '',   'warning', 'Y', '0', 'admin', sysdate(), '', null, '通知');
insert into sys_dict_data values(15, 2,  '公告',     '2',       'sys_notice_type',     '',   'success', 'N', '0', 'admin', sysdate(), '', null, '公告');
insert into sys_dict_data values(16, 1,  '正常',     '0',       'sys_notice_status',   '',   'primary', 'Y', '0', 'admin', sysdate(), '', null, '正常状态');
insert into sys_dict_data values(17, 2,  '关闭',     '1',       'sys_notice_status',   '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '关闭状态');
insert into sys_dict_data values(18, 99, '其他',     '0',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', sysdate(), '', null, '其他操作');
insert into sys_dict_data values(19, 1,  '新增',     '1',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', sysdate(), '', null, '新增操作');
insert into sys_dict_data values(20, 2,  '修改',     '2',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', sysdate(), '', null, '修改操作');
insert into sys_dict_data values(21, 3,  '删除',     '3',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '删除操作');
insert into sys_dict_data values(22, 4,  '授权',     '4',       'sys_oper_type',       '',   'primary', 'N', '0', 'admin', sysdate(), '', null, '授权操作');
insert into sys_dict_data values(23, 5,  '导出',     '5',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', sysdate(), '', null, '导出操作');
insert into sys_dict_data values(24, 6,  '导入',     '6',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', sysdate(), '', null, '导入操作');
insert into sys_dict_data values(25, 7,  '强退',     '7',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '强退操作');
insert into sys_dict_data values(26, 8,  '生成代码', '8',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', sysdate(), '', null, '生成操作');
insert into sys_dict_data values(27, 9,  '清空数据', '9',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '清空操作');
insert into sys_dict_data values(28, 1,  '成功',     '0',       'sys_common_status',   '',   'primary', 'N', '0', 'admin', sysdate(), '', null, '正常状态');
insert into sys_dict_data values(29, 2,  '失败',     '1',       'sys_common_status',   '',   'danger',  'N', '0', 'admin', sysdate(), '', null, '停用状态');


-- ----------------------------
-- 13、参数配置表
-- ----------------------------
drop table if exists sys_config;
create table sys_config (
  config_id         int(5)          not null auto_increment    comment '参数主键',
  config_name       varchar(100)    default ''                 comment '参数名称',
  config_key        varchar(100)    default ''                 comment '参数键名',
  config_value      varchar(500)    default ''                 comment '参数键值',
  config_type       char(1)         default 'N'                comment '系统内置（Y是 N否）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (config_id)
) engine=innodb auto_increment=100 comment = '参数配置表';

insert into sys_config values(1, '主框架页-默认皮肤样式名称',     'sys.index.skinName',               'skin-blue',     'Y', 'admin', sysdate(), '', null, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow' );
insert into sys_config values(2, '用户管理-账号初始密码',         'sys.user.initPassword',            '123456',        'Y', 'admin', sysdate(), '', null, '初始化密码 123456' );
insert into sys_config values(3, '主框架页-侧边栏主题',           'sys.index.sideTheme',              'theme-dark',    'Y', 'admin', sysdate(), '', null, '深色主题theme-dark，浅色主题theme-light' );
insert into sys_config values(4, '账号自助-验证码开关',           'sys.account.captchaEnabled',       'true',          'Y', 'admin', sysdate(), '', null, '是否开启验证码功能（true开启，false关闭）');
insert into sys_config values(5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser',         'false',         'Y', 'admin', sysdate(), '', null, '是否开启注册用户功能（true开启，false关闭）');
insert into sys_config values(6, '用户登录-黑名单列表',           'sys.login.blackIPList',            '',              'Y', 'admin', sysdate(), '', null, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');
insert into sys_config values(7, '用户管理-初始密码修改策略',     'sys.account.initPasswordModify',   '1',             'Y', 'admin', sysdate(), '', null, '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框');
insert into sys_config values(8, '用户管理-账号密码更新周期',     'sys.account.passwordValidateDays', '0',             'Y', 'admin', sysdate(), '', null, '密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框');


-- ----------------------------
-- 14、系统访问记录
-- ----------------------------
drop table if exists sys_logininfor;
create table sys_logininfor (
  info_id        bigint(20)     not null auto_increment   comment '访问ID',
  user_name      varchar(50)    default ''                comment '用户账号',
  ipaddr         varchar(128)   default ''                comment '登录IP地址',
  login_location varchar(255)   default ''                comment '登录地点',
  browser        varchar(50)    default ''                comment '浏览器类型',
  os             varchar(50)    default ''                comment '操作系统',
  status         char(1)        default '0'               comment '登录状态（0成功 1失败）',
  msg            varchar(255)   default ''                comment '提示消息',
  login_time     datetime                                 comment '访问时间',
  primary key (info_id),
  key idx_sys_logininfor_s  (status),
  key idx_sys_logininfor_lt (login_time)
) engine=innodb auto_increment=100 comment = '系统访问记录';


-- ----------------------------
-- 15、定时任务调度表
-- ----------------------------
drop table if exists sys_job;
create table sys_job (
  job_id              bigint(20)    not null auto_increment    comment '任务ID',
  job_name            varchar(64)   default ''                 comment '任务名称',
  job_group           varchar(64)   default 'DEFAULT'          comment '任务组名',
  invoke_target       varchar(500)  not null                   comment '调用目标字符串',
  cron_expression     varchar(255)  default ''                 comment 'cron执行表达式',
  misfire_policy      varchar(20)   default '3'                comment '计划执行错误策略（1立即执行 2执行一次 3放弃执行）',
  concurrent          char(1)       default '1'                comment '是否并发执行（0允许 1禁止）',
  status              char(1)       default '0'                comment '状态（0正常 1暂停）',
  create_by           varchar(64)   default ''                 comment '创建者',
  create_time         datetime                                 comment '创建时间',
  update_by           varchar(64)   default ''                 comment '更新者',
  update_time         datetime                                 comment '更新时间',
  remark              varchar(500)  default ''                 comment '备注信息',
  primary key (job_id, job_name, job_group)
) engine=innodb auto_increment=100 comment = '定时任务调度表';

insert into sys_job values(1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams',        '0/10 * * * * ?', '3', '1', '1', 'admin', sysdate(), '', null, '');
insert into sys_job values(2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(\'ry\')',  '0/15 * * * * ?', '3', '1', '1', 'admin', sysdate(), '', null, '');
insert into sys_job values(3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(\'ry\', true, 2000L, 316.50D, 100)',  '0/20 * * * * ?', '3', '1', '1', 'admin', sysdate(), '', null, '');


-- ----------------------------
-- 16、定时任务调度日志表
-- ----------------------------
drop table if exists sys_job_log;
create table sys_job_log (
  job_log_id          bigint(20)     not null auto_increment    comment '任务日志ID',
  job_name            varchar(64)    not null                   comment '任务名称',
  job_group           varchar(64)    not null                   comment '任务组名',
  invoke_target       varchar(500)   not null                   comment '调用目标字符串',
  job_message         varchar(500)                              comment '日志信息',
  status              char(1)        default '0'                comment '执行状态（0正常 1失败）',
  exception_info      varchar(2000)  default ''                 comment '异常信息',
  start_time          datetime                                  comment '执行开始时间',
  end_time            datetime                                  comment '执行结束时间',
  create_time         datetime                                  comment '创建时间',
  primary key (job_log_id)
) engine=innodb comment = '定时任务调度日志表';


-- ----------------------------
-- 17、通知公告表
-- ----------------------------
drop table if exists sys_notice;
create table sys_notice (
  notice_id         int(4)          not null auto_increment    comment '公告ID',
  notice_title      varchar(50)     not null                   comment '公告标题',
  notice_type       char(1)         not null                   comment '公告类型（1通知 2公告）',
  notice_content    longblob        default null               comment '公告内容',
  status            char(1)         default '0'                comment '公告状态（0正常 1关闭）',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(255)    default null               comment '备注',
  primary key (notice_id)
) engine=innodb auto_increment=10 comment = '通知公告表';

-- ----------------------------
-- 初始化-公告信息表数据
-- ----------------------------
insert into sys_notice values('1', '温馨提醒：2018-07-01 若依新版本发布啦', '2', '新版本内容', '0', 'admin', sysdate(), '', null, '管理员');
insert into sys_notice values('2', '维护通知：2018-07-01 若依系统凌晨维护', '1', '维护内容',   '0', 'admin', sysdate(), '', null, '管理员');
insert into sys_notice values('3', '若依开源框架介绍', '1', '<p><span style=\"color: rgb(230, 0, 0);\">项目介绍</span></p><p><font color=\"#333333\">RuoYi开源项目是为企业用户定制的后台脚手架框架，为企业打造的一站式解决方案，降低企业开发成本，提升开发效率。主要包括用户管理、角色管理、部门管理、菜单管理、参数管理、字典管理、</font><span style=\"color: rgb(51, 51, 51);\">岗位管理</span><span style=\"color: rgb(51, 51, 51);\">、定时任务</span><span style=\"color: rgb(51, 51, 51);\">、</span><span style=\"color: rgb(51, 51, 51);\">服务监控、登录日志、操作日志、代码生成等功能。其中，还支持多数据源、数据权限、国际化、Redis缓存、Docker部署、滑动验证码、第三方认证登录、分布式事务、</span><font color=\"#333333\">分布式文件存储</font><span style=\"color: rgb(51, 51, 51);\">、分库分表处理等技术特点。</span></p><p><img src=\"https://foruda.gitee.com/images/1773931848342439032/a4d22313_1815095.png\" style=\"width: 64px;\"><br></p><p><span style=\"color: rgb(230, 0, 0);\">官网及演示</span></p><p><span style=\"color: rgb(51, 51, 51);\">若依官网地址：&nbsp;</span><a href=\"http://ruoyi.vip\" target=\"_blank\">http://ruoyi.vip</a><a href=\"http://ruoyi.vip\" target=\"_blank\"></a></p><p><span style=\"color: rgb(51, 51, 51);\">若依文档地址：&nbsp;</span><a href=\"http://doc.ruoyi.vip\" target=\"_blank\">http://doc.ruoyi.vip</a><br></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【不分离版】：&nbsp;</span><a href=\"http://demo.ruoyi.vip\" target=\"_blank\">http://demo.ruoyi.vip</a></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【分离版本】：&nbsp;</span><a href=\"http://vue.ruoyi.vip\" target=\"_blank\">http://vue.ruoyi.vip</a></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【微服务版】：&nbsp;</span><a href=\"http://cloud.ruoyi.vip\" target=\"_blank\">http://cloud.ruoyi.vip</a></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【移动端版】：&nbsp;</span><a href=\"http://h5.ruoyi.vip\" target=\"_blank\">http://h5.ruoyi.vip</a></p><p><br style=\"color: rgb(48, 49, 51); font-family: &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif; font-size: 12px;\"></p>', '0', 'admin', sysdate(), '', null, '管理员');


-- ----------------------------
-- 18、公告已读记录表
-- ----------------------------
drop table if exists sys_notice_read;
create table sys_notice_read (
  read_id          bigint(20)       not null auto_increment    comment '已读主键',
  notice_id        int(4)           not null                   comment '公告id',
  user_id          bigint(20)       not null                   comment '用户id',
  read_time        datetime         not null                   comment '阅读时间',
  primary key (read_id),
  unique key uk_user_notice (user_id, notice_id)   comment '同一用户同一公告只记录一次'
) engine=innodb auto_increment=1 comment='公告已读记录表';


-- ----------------------------
-- 19、代码生成业务表
-- ----------------------------
drop table if exists gen_table;
create table gen_table (
  table_id          bigint(20)      not null auto_increment    comment '编号',
  table_name        varchar(200)    default ''                 comment '表名称',
  table_comment     varchar(500)    default ''                 comment '表描述',
  sub_table_name    varchar(64)     default null               comment '关联子表的表名',
  sub_table_fk_name varchar(64)     default null               comment '子表关联的外键名',
  class_name        varchar(100)    default ''                 comment '实体类名称',
  tpl_category      varchar(200)    default 'crud'             comment '使用的模板（crud单表操作 tree树表操作）',
  tpl_web_type      varchar(30)     default ''                 comment '前端模板类型（element-ui模版 element-plus模版）',
  package_name      varchar(100)                               comment '生成包路径',
  module_name       varchar(30)                                comment '生成模块名',
  business_name     varchar(30)                                comment '生成业务名',
  function_name     varchar(50)                                comment '生成功能名',
  function_author   varchar(50)                                comment '生成功能作者',
  gen_type          char(1)         default '0'                comment '生成代码方式（0zip压缩包 1自定义路径）',
  gen_path          varchar(200)    default '/'                comment '生成路径（不填默认项目路径）',
  options           varchar(1000)                              comment '其它生成选项',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time 	    datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (table_id)
) engine=innodb auto_increment=1 comment = '代码生成业务表';


-- ----------------------------
-- 20、代码生成业务表字段
-- ----------------------------
drop table if exists gen_table_column;
create table gen_table_column (
  column_id         bigint(20)      not null auto_increment    comment '编号',
  table_id          bigint(20)                                 comment '归属表编号',
  column_name       varchar(200)                               comment '列名称',
  column_comment    varchar(500)                               comment '列描述',
  column_type       varchar(100)                               comment '列类型',
  java_type         varchar(500)                               comment 'JAVA类型',
  java_field        varchar(200)                               comment 'JAVA字段名',
  is_pk             char(1)                                    comment '是否主键（1是）',
  is_increment      char(1)                                    comment '是否自增（1是）',
  is_required       char(1)                                    comment '是否必填（1是）',
  is_insert         char(1)                                    comment '是否为插入字段（1是）',
  is_edit           char(1)                                    comment '是否编辑字段（1是）',
  is_list           char(1)                                    comment '是否列表字段（1是）',
  is_query          char(1)                                    comment '是否查询字段（1是）',
  query_type        varchar(200)    default 'EQ'               comment '查询方式（等于、不等于、大于、小于、范围）',
  html_type         varchar(200)                               comment '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  dict_type         varchar(200)    default ''                 comment '字典类型',
  sort              int                                        comment '排序',
  create_by         varchar(64)     default ''                 comment '创建者',
  create_time 	    datetime                                   comment '创建时间',
  update_by         varchar(64)     default ''                 comment '更新者',
  update_time       datetime                                   comment '更新时间',
  primary key (column_id)
) engine=innodb auto_increment=1 comment = '代码生成业务表字段';

-- =========================================================
-- Legacy cost platform business schema baseline
-- =========================================================

-- =========================================================
-- 企业级成本核算平台完整数据库设计脚本
-- 项目：cost_platform
-- 说明：
-- 1. 本脚本用于定义企业级核算平台核心业务表
-- 2. 所有业务表统一使用 cost_ 前缀
-- 3. 所有表和字段均带中文注释
-- 4. 设计目标包含多行业、多场景、多费用、多规则、多版本
-- 5. 同时考虑月近 100 万条计费数据的正式核算性能要求
-- =========================================================

set names utf8mb4;
set foreign_key_checks = 0;

-- =========================================================
-- 一、场景与费用主数据
-- =========================================================

drop table if exists cost_scene;
create table cost_scene (
  scene_id                  bigint          not null auto_increment comment '场景主键',
  scene_code                varchar(64)     not null comment '场景编码，用于唯一标识一个核算主题或合同场景',
  scene_name                varchar(128)    not null comment '场景名称，供业务人员识别核算主题',
  business_domain           varchar(64)     not null comment '业务域字典值，对应 cost_business_domain',
  org_code                  varchar(64)     default '' comment '所属组织编码，用于按组织隔离场景',
  scene_type                varchar(32)     default 'CONTRACT' comment '场景类型，例如合同、方案、公司级核算域',
  active_version_id         bigint          default null comment '当前生效版本主键，对应 cost_publish_version.version_id',
  status                    char(1)         not null default '0' comment '场景状态（0正常 1停用 2草稿）',
  remark                    varchar(500)    default null comment '场景说明，用于补充业务口径和适用边界',
  create_by                 varchar(64)     default '' comment '创建人',
  create_time               datetime        default current_timestamp comment '创建时间',
  update_by                 varchar(64)     default '' comment '更新人',
  update_time               datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (scene_id),
  unique key uk_cost_scene_code (scene_code),
  key idx_cost_scene_domain_status (business_domain, status),
  key idx_cost_scene_org_status (org_code, status)
) engine=innodb default charset=utf8mb4 comment='核算平台-场景主数据表';

drop table if exists cost_fee_item;
create table cost_fee_item (
  fee_id                    bigint          not null auto_increment comment '费用主键',
  scene_id                  bigint          not null comment '所属场景主键，对应 cost_scene.scene_id',
  fee_code                  varchar(64)     not null comment '费用编码，用于唯一标识一个费用项',
  fee_name                  varchar(128)    not null comment '费用名称，供业务人员识别费用项',
  fee_category              varchar(64)     default '' comment '费用分类，例如固定薪资、补贴、港杂费、附加费',
  unit_code                 varchar(32)     default '' comment '计价单位编码，例如人、吨、箱、元',
  factor_summary            varchar(255)    default '' comment '影响因素摘要，用于帮助业务快速理解费用依赖',
  scope_description         varchar(255)    default '' comment '适用范围说明，补充当前场景下该费用的适用边界',
  object_dimension          varchar(64)     default '' comment '核算对象维度，例如人、班组、协力公司、船舶',
  sort_no                   int             not null default 0 comment '排序号，用于列表和工作台展示顺序',
  status                    char(1)         not null default '0' comment '费用状态（0正常 1停用）',
  remark                    varchar(500)    default null comment '备注',
  create_by                 varchar(64)     default '' comment '创建人',
  create_time               datetime        default current_timestamp comment '创建时间',
  update_by                 varchar(64)     default '' comment '更新人',
  update_time               datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (fee_id),
  unique key uk_cost_fee_scene_code (scene_id, fee_code),
  key idx_cost_fee_scene_status (scene_id, status),
  key idx_cost_fee_scene_sort (scene_id, sort_no),
  constraint fk_cost_fee_scene foreign key (scene_id) references cost_scene (scene_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-费用主数据表';

-- =========================================================
-- 二、变量与影响因素
-- =========================================================

drop table if exists cost_variable_group;
create table cost_variable_group (
  group_id                  bigint          not null auto_increment comment '变量分组主键',
  scene_id                  bigint          not null comment '所属场景主键',
  group_code                varchar(64)     not null comment '变量分组编码',
  group_name                varchar(128)    not null comment '变量分组名称',
  sort_no                   int             not null default 0 comment '排序号',
  status                    char(1)         not null default '0' comment '分组状态（0正常 1停用）',
  remark                    varchar(500)    default null comment '备注',
  create_by                 varchar(64)     default '' comment '创建人',
  create_time               datetime        default current_timestamp comment '创建时间',
  update_by                 varchar(64)     default '' comment '更新人',
  update_time               datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (group_id),
  unique key uk_cost_var_group_scene_code (scene_id, group_code),
  key idx_cost_var_group_scene_sort (scene_id, sort_no),
  constraint fk_cost_var_group_scene foreign key (scene_id) references cost_scene (scene_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-变量分组表';

drop table if exists cost_variable;
create table cost_variable (
  variable_id               bigint          not null auto_increment comment '变量主键',
  scene_id                  bigint          not null comment '所属场景主键',
  group_id                  bigint          default null comment '所属变量分组主键',
  variable_code             varchar(64)     not null comment '变量编码，用于规则、公式、快照引用',
  variable_name             varchar(128)    not null comment '变量名称，供业务人员识别',
  variable_type             varchar(32)     not null comment '变量类型，例如NUMBER、TEXT、DICT、REMOTE、FORMULA、BOOLEAN、DATE',
  source_type               varchar(32)     not null comment '变量来源，例如INPUT、DICT、REMOTE、FORMULA',
  dict_type                 varchar(64)     default '' comment '字典类型，当变量来源为字典时使用',
  remote_api                varchar(255)    default '' comment '远程接口地址或标识，当变量来源为接口时使用',
  data_path                 varchar(255)    default '' comment '取值路径，用于从外部数据中提取字段值',
  formula_expr              varchar(2000)   default null comment '公式表达式，当变量为公式变量时使用',
  data_type                 varchar(32)     default 'STRING' comment '数据类型，例如STRING、NUMBER、BOOLEAN、DATE',
  default_value             varchar(255)    default '' comment '默认值',
  precision_scale           int             default 2 comment '数值精度，用于金额或数量变量',
  status                    char(1)         not null default '0' comment '变量状态（0正常 1停用）',
  sort_no                   int             not null default 0 comment '排序号',
  remark                    varchar(500)    default null comment '备注',
  create_by                 varchar(64)     default '' comment '创建人',
  create_time               datetime        default current_timestamp comment '创建时间',
  update_by                 varchar(64)     default '' comment '更新人',
  update_time               datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (variable_id),
  unique key uk_cost_variable_scene_code (scene_id, variable_code),
  key idx_cost_variable_scene_group (scene_id, group_id),
  key idx_cost_variable_scene_status (scene_id, status),
  constraint fk_cost_variable_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_variable_group foreign key (group_id) references cost_variable_group (group_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-变量主数据表';

drop table if exists cost_fee_variable_rel;
create table cost_fee_variable_rel (
  rel_id                    bigint          not null auto_increment comment '费用与变量关系主键',
  scene_id                  bigint          not null comment '所属场景主键',
  fee_id                    bigint          not null comment '费用主键',
  variable_id               bigint          not null comment '变量主键',
  relation_type             varchar(32)     not null default 'OPTIONAL' comment '关系类型，例如REQUIRED、OPTIONAL、TIER_BASIS、FORMULA_INPUT',
  source_type               varchar(32)     not null default 'RULE_DERIVED' comment '来源类型，例如RULE_DERIVED、MANUAL_REQUIRED',
  source_rule_id            bigint          default null comment '来源规则主键，规则派生关系使用',
  source_code               varchar(128)    not null default '' comment '来源编码，例如规则编码或手工配置编码',
  sort_no                   int             not null default 0 comment '排序号',
  remark                    varchar(500)    default null comment '备注',
  create_by                 varchar(64)     default '' comment '创建人',
  create_time               datetime        default current_timestamp comment '创建时间',
  update_by                 varchar(64)     default '' comment '更新人',
  update_time               datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (rel_id),
  unique key uk_cost_fee_var_rel (fee_id, variable_id, relation_type, source_type, source_code),
  key idx_cost_fee_var_scene_fee (scene_id, fee_id),
  key idx_cost_fee_var_scene_var (scene_id, variable_id),
  key idx_cost_fee_var_source (source_type, source_rule_id, source_code),
  constraint fk_cost_fee_var_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_fee_var_fee foreign key (fee_id) references cost_fee_item (fee_id),
  constraint fk_cost_fee_var_variable foreign key (variable_id) references cost_variable (variable_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-费用与变量适用关系表';

-- =========================================================
-- 三、规则与条件
-- =========================================================

drop table if exists cost_rule;
create table cost_rule (
  rule_id                    bigint          not null auto_increment comment '规则主键',
  scene_id                   bigint          not null comment '所属场景主键',
  fee_id                     bigint          not null comment '所属费用主键',
  rule_code                  varchar(64)     not null comment '规则编码',
  rule_name                  varchar(128)    default '' comment '规则名称，便于业务识别',
  rule_type                  varchar(32)     not null comment '规则类型，例如FIXED_RATE、FIXED_AMOUNT、FORMULA、TIER_RATE、ALLOCATE',
  condition_logic            varchar(16)     not null default 'AND' comment '条件逻辑，例如AND、OR',
  priority                   int             not null default 0 comment '优先级，数值越大越优先',
  quantity_variable_code     varchar(64)     default '' comment '阶梯或公式依赖的数量变量编码',
  pricing_mode               varchar(32)     not null default 'TYPED' comment '定价模式，例如TYPED、ADVANCED_JSON',
  pricing_json               json            default null comment '规则定价结构化配置，承接固定费率、固定金额、阶梯明细、公式等',
  amount_formula             varchar(2000)   default null comment '金额公式表达式',
  note_template              varchar(500)    default '' comment '结果备注模板',
  status                     char(1)         not null default '0' comment '规则状态（0正常 1停用）',
  sort_no                    int             not null default 0 comment '排序号',
  remark                     varchar(500)    default null comment '备注',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_by                  varchar(64)     default '' comment '更新人',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (rule_id),
  unique key uk_cost_rule_scene_code (scene_id, rule_code),
  key idx_cost_rule_scene_fee (scene_id, fee_id),
  key idx_cost_rule_scene_status_priority (scene_id, status, priority),
  constraint fk_cost_rule_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_rule_fee foreign key (fee_id) references cost_fee_item (fee_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-费率规则主表';

drop table if exists cost_rule_condition;
create table cost_rule_condition (
  condition_id               bigint          not null auto_increment comment '规则条件主键',
  scene_id                   bigint          not null comment '所属场景主键',
  rule_id                    bigint          not null comment '所属规则主键',
  group_no                   int             not null default 1 comment '组号，用于表达同组条件',
  sort_no                    int             not null default 0 comment '排序号',
  variable_code              varchar(64)     not null comment '变量编码，指向规则判断使用的变量',
  display_name               varchar(128)    default '' comment '显示名称，用于业务页展示',
  operator_code              varchar(32)     not null comment '操作符，例如EQ、IN、GT、GTE、LT、LTE、BETWEEN',
  compare_value              varchar(1000)   default '' comment '比较值，统一以字符串存储并由前端/服务层解析',
  status                     char(1)         not null default '0' comment '条件状态（0正常 1停用）',
  remark                     varchar(500)    default null comment '备注',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_by                  varchar(64)     default '' comment '更新人',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (condition_id),
  key idx_cost_rule_cond_rule_group (rule_id, group_no, sort_no),
  key idx_cost_rule_cond_scene_var (scene_id, variable_code),
  constraint fk_cost_rule_condition_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_rule_condition_rule foreign key (rule_id) references cost_rule (rule_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-规则条件表';

drop table if exists cost_rule_tier;
create table cost_rule_tier (
  tier_id                    bigint          not null auto_increment comment '阶梯主键',
  scene_id                   bigint          not null comment '所属场景主键',
  rule_id                    bigint          not null comment '所属规则主键',
  start_value                decimal(18,4)   default null comment '阶梯起始值，允许为空表示首档负无穷起始',
  end_value                  decimal(18,4)   default null comment '阶梯截止值，允许为空表示末档无上限',
  rate_value                 decimal(18,6)   not null comment '阶梯费率或阶梯单价',
  interval_mode              varchar(32)     not null default 'LEFT_CLOSED_RIGHT_OPEN' comment '区间模式，例如LEFT_CLOSED_RIGHT_OPEN、LEFT_OPEN_RIGHT_CLOSED',
  tier_no                    int             not null default 1 comment '阶梯序号',
  status                     char(1)         not null default '0' comment '阶梯状态（0正常 1停用）',
  remark                     varchar(500)    default null comment '备注',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_by                  varchar(64)     default '' comment '更新人',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (tier_id),
  key idx_cost_rule_tier_rule_no (rule_id, tier_no),
  key idx_cost_rule_tier_scene_rule (scene_id, rule_id),
  constraint fk_cost_rule_tier_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_rule_tier_rule foreign key (rule_id) references cost_rule (rule_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-规则阶梯明细表';

-- =========================================================
-- 四、发布版本与快照
-- =========================================================

drop table if exists cost_publish_version;
create table cost_publish_version (
  version_id                 bigint          not null auto_increment comment '发布版本主键',
  scene_id                   bigint          not null comment '所属场景主键',
  version_no                 varchar(64)     not null comment '发布版本号，例如 V2026.03.001',
  version_status             varchar(32)     not null default 'PUBLISHED' comment '版本状态，例如DRAFT、PUBLISHED、ACTIVE、ROLLED_BACK',
  publish_desc               varchar(1000)   default '' comment '发布说明',
  validation_result_json     json            default null comment '发布校验结果快照',
  snapshot_hash              varchar(128)    default '' comment '快照哈希，用于快速识别配置是否变化',
  published_by               varchar(64)     default '' comment '发布人',
  published_time             datetime        default null comment '发布时间',
  activated_by               varchar(64)     default '' comment '生效操作人',
  activated_time             datetime        default null comment '生效时间',
  rollback_by                varchar(64)     default '' comment '回滚操作人',
  rollback_time              datetime        default null comment '回滚时间',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_by                  varchar(64)     default '' comment '更新人',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (version_id),
  unique key uk_cost_publish_scene_ver (scene_id, version_no),
  key idx_cost_publish_scene_status (scene_id, version_status),
  key idx_cost_publish_scene_time (scene_id, published_time),
  constraint fk_cost_publish_scene foreign key (scene_id) references cost_scene (scene_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-场景发布版本表';

drop table if exists cost_publish_snapshot;
create table cost_publish_snapshot (
  snapshot_id                bigint          not null auto_increment comment '快照明细主键',
  version_id                 bigint          not null comment '所属发布版本主键',
  snapshot_type              varchar(32)     not null comment '快照对象类型，例如SCENE、FEE、VARIABLE、RULE、RULE_CONDITION、RULE_TIER',
  object_code                varchar(64)     not null comment '对象编码，用于识别被快照的业务对象',
  object_name                varchar(128)    default '' comment '对象名称，用于工作台和审计展示',
  snapshot_json              json            not null comment '业务对象快照 JSON',
  sort_no                    int             not null default 0 comment '排序号',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  primary key (snapshot_id),
  key idx_cost_snapshot_version_type (version_id, snapshot_type),
  key idx_cost_snapshot_version_code (version_id, object_code),
  constraint fk_cost_snapshot_version foreign key (version_id) references cost_publish_version (version_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-发布快照明细表';

-- =========================================================
-- 五、试算与正式核算任务
-- =========================================================

drop table if exists cost_simulation_record;
create table cost_simulation_record (
  simulation_id              bigint          not null auto_increment comment '试算记录主键',
  scene_id                   bigint          not null comment '场景主键',
  version_id                 bigint          default null comment '试算使用的版本主键，优先记录发布版本',
  simulation_no              varchar(64)     not null comment '试算编号',
  input_json                 json            not null comment '试算输入数据',
  variable_json              json            default null comment '变量计算结果',
  explain_json               json            default null comment '试算解释结果，包括命中规则、阶梯、公式等',
  result_json                json            default null comment '试算输出结果',
  status                     varchar(32)     not null default 'SUCCESS' comment '试算状态，例如SUCCESS、FAILED',
  error_message              varchar(1000)   default '' comment '失败信息',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  primary key (simulation_id),
  unique key uk_cost_simulation_no (simulation_no),
  key idx_cost_simulation_scene_time (scene_id, create_time),
  constraint fk_cost_simulation_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_simulation_version foreign key (version_id) references cost_publish_version (version_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-试算记录表';

drop table if exists cost_calc_task;
create table cost_calc_task (
  task_id                    bigint          not null auto_increment comment '核算任务主键',
  task_no                    varchar(64)     not null comment '任务编号',
  scene_id                   bigint          not null comment '场景主键',
  version_id                 bigint          not null comment '运行使用的发布版本主键',
  task_type                  varchar(32)     not null comment '任务类型，例如FORMAL_SINGLE、FORMAL_BATCH、SIMULATION_BATCH',
  bill_month                 varchar(16)     default '' comment '账期，例如2026-03',
  source_count               int             not null default 0 comment '输入数据总量',
  success_count              int             not null default 0 comment '成功处理数量',
  fail_count                 int             not null default 0 comment '失败数量',
  task_status                varchar(32)     not null default 'INIT' comment '任务状态，例如INIT、RUNNING、SUCCESS、PART_SUCCESS、FAILED',
  progress_percent           decimal(5,2)    not null default 0 comment '任务进度百分比',
  started_time               datetime        default null comment '开始时间',
  finished_time              datetime        default null comment '结束时间',
  duration_ms                bigint          default 0 comment '任务总耗时，单位毫秒',
  request_no                 varchar(64)     default '' comment '幂等请求号',
  execute_node               varchar(128)    default '' comment '执行节点标识，用于分布式任务追踪',
  error_message              varchar(1000)   default '' comment '任务失败摘要',
  remark                     varchar(500)    default null comment '备注',
  create_by                  varchar(64)     default '' comment '创建人',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_by                  varchar(64)     default '' comment '更新人',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (task_id),
  unique key uk_cost_calc_task_no (task_no),
  key idx_cost_calc_task_scene_month (scene_id, bill_month),
  key idx_cost_calc_task_scene_status (scene_id, task_status),
  key idx_cost_calc_task_version (version_id),
  constraint fk_cost_calc_task_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_calc_task_version foreign key (version_id) references cost_publish_version (version_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-正式核算任务表';

drop table if exists cost_calc_task_detail;
create table cost_calc_task_detail (
  detail_id                  bigint          not null auto_increment comment '任务明细主键',
  task_id                    bigint          not null comment '所属任务主键',
  task_no                    varchar(64)     not null comment '任务编号冗余字段，便于快速过滤',
  biz_no                     varchar(128)    not null comment '业务单号，用于唯一标识一条待计费业务数据',
  partition_no               int             not null default 1 comment '分片号，用于批量任务并行处理',
  detail_status              varchar(32)     not null default 'INIT' comment '明细状态，例如INIT、SUCCESS、FAILED',
  retry_count                int             not null default 0 comment '重试次数',
  input_json                 json            not null comment '输入业务数据',
  result_summary             varchar(1000)   default '' comment '结果摘要，用于列表快速展示',
  error_message              varchar(1000)   default '' comment '失败信息',
  create_time                datetime        default current_timestamp comment '创建时间',
  update_time                datetime        default current_timestamp on update current_timestamp comment '更新时间',
  primary key (detail_id),
  unique key uk_cost_calc_task_detail (task_id, biz_no),
  key idx_cost_calc_task_detail_task_status (task_id, detail_status),
  key idx_cost_calc_task_detail_task_partition (task_id, partition_no),
  key idx_cost_calc_task_detail_task_no (task_no),
  constraint fk_cost_calc_task_detail_task foreign key (task_id) references cost_calc_task (task_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-正式核算任务明细表';

-- =========================================================
-- 六、结果台账与追溯
-- =========================================================

drop table if exists cost_result_ledger;
create table cost_result_ledger (
  result_id                  bigint          not null auto_increment comment '结果台账主键',
  task_id                    bigint          not null comment '所属任务主键',
  task_no                    varchar(64)     not null comment '任务编号冗余字段，便于查询',
  scene_id                   bigint          not null comment '场景主键',
  version_id                 bigint          not null comment '发布版本主键',
  fee_id                     bigint          not null comment '费用主键',
  fee_code                   varchar(64)     not null comment '费用编码',
  fee_name                   varchar(128)    not null comment '费用名称',
  biz_no                     varchar(128)    not null comment '业务单号',
  bill_month                 varchar(16)     not null comment '账期，例如2026-03',
  object_dimension           varchar(64)     default '' comment '核算对象维度，例如人、班组、协力公司、船舶',
  object_code                varchar(128)    default '' comment '核算对象编码',
  object_name                varchar(128)    default '' comment '核算对象名称',
  quantity_value             decimal(18,4)   default null comment '参与费率计算的数量值',
  unit_price                 decimal(18,6)   default null comment '最终命中的单价或费率',
  amount_value               decimal(18,2)   not null comment '最终金额',
  currency_code              varchar(32)     default 'CNY' comment '币种编码',
  result_status              varchar(32)     not null default 'SUCCESS' comment '结果状态，例如SUCCESS、FAILED、ADJUSTED',
  trace_id                   bigint          default null comment '追溯记录主键，对应 cost_result_trace.trace_id',
  create_time                datetime        default current_timestamp comment '创建时间',
  primary key (result_id),
  key idx_cost_result_scene_month (scene_id, bill_month),
  key idx_cost_result_task_fee (task_id, fee_id),
  key idx_cost_result_biz_fee (biz_no, fee_code),
  key idx_cost_result_version (version_id),
  constraint fk_cost_result_task foreign key (task_id) references cost_calc_task (task_id),
  constraint fk_cost_result_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_result_version foreign key (version_id) references cost_publish_version (version_id),
  constraint fk_cost_result_fee foreign key (fee_id) references cost_fee_item (fee_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-结果台账表';

drop table if exists cost_result_trace;
create table cost_result_trace (
  trace_id                   bigint          not null auto_increment comment '结果追溯主键',
  scene_id                   bigint          not null comment '场景主键',
  version_id                 bigint          not null comment '发布版本主键',
  rule_id                    bigint          default null comment '命中的规则主键',
  tier_id                    bigint          default null comment '命中的阶梯主键',
  variable_json              json            default null comment '变量计算结果，用于解释命中过程',
  condition_json             json            default null comment '规则条件匹配结果',
  pricing_json               json            default null comment '定价过程结果，包括单价来源、阶梯摘要等',
  timeline_json              json            default null comment '执行时间线，用于回放输入、变量、规则、金额和结果过程',
  create_time                datetime        default current_timestamp comment '创建时间',
  primary key (trace_id),
  key idx_cost_trace_scene_ver (scene_id, version_id),
  key idx_cost_trace_rule (rule_id),
  key idx_cost_trace_tier (tier_id),
  constraint fk_cost_trace_scene foreign key (scene_id) references cost_scene (scene_id),
  constraint fk_cost_trace_version foreign key (version_id) references cost_publish_version (version_id),
  constraint fk_cost_trace_rule foreign key (rule_id) references cost_rule (rule_id),
  constraint fk_cost_trace_tier foreign key (tier_id) references cost_rule_tier (tier_id)
) engine=innodb default charset=utf8mb4 comment='核算平台-结果追溯解释表';

-- =========================================================
-- 七、治理与审计
-- =========================================================

drop table if exists cost_audit_log;
create table cost_audit_log (
  audit_id                   bigint          not null auto_increment comment '审计日志主键',
  scene_id                   bigint          default null comment '所属场景主键',
  object_type                varchar(32)     not null comment '对象类型，例如SCENE、FEE、VARIABLE、RULE、PUBLISH、DICT',
  object_code                varchar(64)     default '' comment '对象编码',
  action_type                varchar(32)     not null comment '操作类型，例如CREATE、UPDATE、DELETE、DISABLE、PUBLISH、ACTIVATE、ROLLBACK',
  action_summary             varchar(500)    default '' comment '操作摘要',
  before_json                json            default null comment '变更前快照',
  after_json                 json            default null comment '变更后快照',
  operator_code              varchar(64)     default '' comment '操作人编码',
  operator_name              varchar(128)    default '' comment '操作人名称',
  operate_time               datetime        default current_timestamp comment '操作时间',
  request_no                 varchar(64)     default '' comment '请求流水号',
  primary key (audit_id),
  key idx_cost_audit_scene_time (scene_id, operate_time),
  key idx_cost_audit_object (object_type, object_code),
  key idx_cost_audit_action (action_type, operate_time)
) engine=innodb default charset=utf8mb4 comment='核算平台-配置审计日志表';

set foreign_key_checks = 1;
