# ongdb-lab-security
>&nbsp;&nbsp;&nbsp;&nbsp;细粒度图数据访问控制组件，数据库管理员通过配置`特殊角色`账户的形式实现，支持`在线自定义配置`进行`热更新`权限操作，不用重启数据库服务。读写权限的控制粒度可以细化到标签、关系、属性，并且区分不同的用户对数据删除权限进行严格管控。

>&nbsp;&nbsp;&nbsp;&nbsp;主要实现`细粒度读写`和`细粒度读`权限的配置。`细粒度写`可以控制用户只能操作某些标签、关系以及属性，是否可以新建与修改属性、标签和关系，取决于权限的细粒度配置。`细粒度读`可以限制用户只能执行某些数据的查询，并支持指定具体的Query，相当于为用户指定了一个Query白名单。

>&nbsp;&nbsp;&nbsp;&nbsp;图数据库基于角色的访问控制体系，在使用时通过将不同角色组合，分配给同一用户多个角色，可以实现更丰富的访问控制功能。例如，某个用户具有部分子图的读取权限，但是也需要具备对某个标签的多个属性拥有修改、删除、创建的权限。 

>图数据库的主要角色权限类型：

|编号|类型|说明|
|---|---|---|
|1|Reader|只读 |
|2|Publisher|读写数据|
|3|Architect|定义索引、限制等数据库模式对象|
|4|Administrator|管理用户和角色，管理数据库运行|
|5|Editor|可以读写但是无法新建标签和关系|
|6|Define|自定义角色|

>&nbsp;&nbsp;&nbsp;&nbsp;其中`ongdb-lab-security`组件主要是实现第六类`Define`权限配置，例如要根据特定标签、关系和属性上的过滤条件对图的一部分子图实现访问控制，则可以通过`ongdb-lab-security`组件实现。生成的配置文件在图数据库安装路径`auth`目录下。

>&nbsp;&nbsp;&nbsp;&nbsp;访问控制的基本思路是定义新用户和角色，为该角色指定只有运行某些扩展过程的权限，然后在扩展过程中根据调用过程的用户及其角色，应用不同的过滤条件，按照标签、关系类型、属性值对结果进行筛选。<br>&nbsp;&nbsp;&nbsp;&nbsp;这些用户因为只有运行特定过程的权利，因此无法通过Browser登陆数据库来运行Cypher查询，而只能通过应用客户端访问数据库。

## 使用前准备
>在图数据库节点执行安装组件、修改配置、重启图数据库操作；如果是集群环境所有节点需要执行相同操作。

- 1.将`ongdb-lab-security-*.jar`放在`plugins`文件夹
- 2.修改`conf`配置
```
// 为角色指定权限
dbms.security.procedures.roles=olab.security.publisher.*:publisher_proc;olab.security.reader*:reader_proc;olab.security.get*:publisher_proc;olab.security.get*:reader_proc;
```
- 3.重启图数据库

## 使用步骤
### 1、创建角色
```
WITH ['reader_proc','publisher_proc'] AS roles
UNWIND roles AS role
CALL dbms.security.createRole(role) RETURN role;
```
### 2、配置账号
```
// 如果需要自定义修改用户则修改`reader_users`和`publisher_users`再进行下一步操作即可
// 预定义特殊Reader用户
WITH ['reader-1','reader-2'] AS reader_users
// 预定义特殊Publisher用户
WITH ['publisher-1','publisher-2'] AS publisher_users,reader_users
WITH apoc.coll.union(reader_users,publisher_users) AS users
UNWIND users AS user
// 默认密码`abc%pro`，初次登陆时不用修改口令
// CALL dbms.security.deleteUser(user) RETURN user;
CALL dbms.security.createUser(user,'abc%pro',false) RETURN user;
```

### 3、指定角色
- 为特殊Reader用户指定角色
```
WITH ['reader-1','reader-2'] AS reader_users
UNWIND reader_users AS user
// 为特殊Reader用户指定角色
CALL dbms.security.addRoleToUser('reader_proc',user) RETURN user;
```

- 特殊Publisher用户指定角色
```
WITH ['publisher-1','publisher-2'] AS publisher_users
UNWIND publisher_users AS user
// 为特殊Publisher用户指定角色
CALL dbms.security.addRoleToUser('publisher_proc',user) RETURN user;
```

### 4、Operator操作权限说明
>&nbsp;&nbsp;&nbsp;&nbsp;`Operator`参数定义了对标签、关系、属性的操作权限的级别定义，使用`olab.security.setPublisher`过程为用户分配数据编辑权限。拥有创建指定数据的特殊权限账户，在创建数据时节点和关系会默认带有系统用户名字段`__system_users`和字段用户归属标记字段`__system_field_users`，存储为一个数组格式。在使用`DELETER_RESTRICT`权限时，会根据该字段来判断用户是否对数据拥有删除权限。

>&nbsp;&nbsp;&nbsp;&nbsp;权限下发机制，当节点拥有一个`PUBLISHER`权限时，用户可以对已有数据进行编辑修改并且可以创建新数据，对所属属性都可以执行`PUBLISHER`操作，但是无法删除。如果这时对节点的属性指定了一个`DELETER_RESTRICT`权限，那么用户则可以对自己创建的属性执行删除操作。如果对节点的属性指定了一个`DELETER`权限，则当前用户可以删除任何其它用户创建的属性数据。

|操作级别|类型|说明|
|---|---|---|
|1|READER_FORBID|禁止读取 |
|2|READER|可以读取 |
|3|EDITOR|对已有数据的编辑修改权限 |
|4|PUBLISHER|对已有数据的编辑修改权限、拥有创建新数据权限|
|5|DELETER_RESTRICT|对已有数据的编辑修改权限、拥有创建新数据权限、拥有删除数据权限<br>【仅允许删除用户自己创建的数据】<br>【当属性设置为该权限时，如果用户对节点或关系存在大于该级别的权限则属性也可以执行一样的权限级别】|
|6|DELETER|对已有数据的编辑修改权限、拥有创建新数据权限、拥有删除数据权限|

### 5、使用Administrator账户分别为reader-*、publisher-*账户配置可用的操作权限
- 为`Publisher-1`配置权限
>说明：
>1. 配置Publisher权限【合并权限列表】【admin】
>2. 对于`properties`配置字段值检查`check`时可以使用`olab.security.getValueTypes`查看可配置的值检查类型
>1. properties不为空时表示对可操作属性进行限制,为空时表示没有配置属性权限

>`olab.security.setPublisher`入参：
>1. @param username:用户名
>2. @param nodeLabels:可操作的标签列表【可为空】【追加】 label properties[<field,operator>] operator
>3. @param relTypes:可操作的关系类型列表【可为空】【追加】 start_label type end_label properties[<field,operator[]>] operator
```
CALL olab.security.setPublisher('publisher-1',[{label:'Person',properties:[{field:'name',operator:'DELETER_RESTRICT',check:'STRING'}],operator:'EDITOR'}],[{start_label:'Person',type:'ACTED_IN',end_label:'Movie',operator:'DELETER_RESTRICT',properties:[{field:'date',operator:'PUBLISHER',check:'LONG'}]}]) YIELD username,currentRole,nodeLabels,relTypes RETURN username,currentRole,nodeLabels,relTypes
// 标签和关系类型配置为NULL时，可查看已有权限
CALL olab.security.setPublisher('publisher-1',NULL,NULL) YIELD username,currentRole,nodeLabels,relTypes RETURN username,currentRole,nodeLabels,relTypes
```

- 为`Reader-1`配置权限
```
// 配置Reader权限【合并权限列表】【admin】
// @param username:用户名
// @param queries:可执行查询列表 query_id query【可为空】【追加】【query_id不可重复】【Query需要返回属性键值格式，不支持直接返回节点和关系】
CALL olab.security.setReader('reader-1',[{query_id:'query001',query:'MATCH (n) RETURN n.name AS name LIMIT 1'},{query_id:'query002',query:'MATCH (n) WITH n LIMIT 10 RETURN olab.result.transfer(n) AS mapList;'},{query_id:'query003',query:'MATCH ()-[r]->() WITH r LIMIT 10 WITH olab.result.transfer(r) AS mapList UNWIND mapList AS map RETURN map;'}]) YIELD username,currentRole,queries RETURN username,currentRole,queries
// 配置的查询参数为NULL时，可查看已有权限
CALL olab.security.setReader('reader-1',NULL) YIELD username,currentRole,queries RETURN username,currentRole,queries
```

- 重置指定用户的权限列表
```
// @param username:用户名
CALL olab.security.clear('reader-1') YIELD username,currentRole RETURN username,currentRole
```

- 获取所有的已配置权限列表
```
CALL olab.security.list() YIELD value RETURN value
```

## 使用案例
### 1、`publisher_proc`角色的账户使用
>以`publisher-1`用户为例

- 1.查看拥有的权限
```
CALL olab.security.get() YIELD value RETURN value
CALL olab.security.getAuth() YIELD operate,level,description RETURN operate,level,description
```

- 2.合并节点
```
CALL olab.security.publisher.merge.node({label},{merge_field},{merge_value},{[other_pros]}) YIELD value RETURN value
```

- 3.删除节点
```
CALL olab.security.publisher.delete.node({node_id}) YIELD value RETURN value
```

- 4.合并关系
```
CALL olab.security.publisher.merge.relationship({start_id},{end_id},{merge_rel_type},{rel_pros}) YIELD value RETURN value
```

- 5.删除关系
```
CALL olab.security.publisher.delete.relationship({rel_id}) YIELD value RETURN value
```

- 6.修改节点的属性值
```
CALL olab.security.publisher.update.node({node_id},{field_name},{field_value}) YIELD value RETURN value
```

- 7.修改关系的属性值
```
CALL olab.security.publisher.update.relationship({rel_id},{field_name},{field_value}) YIELD value RETURN value
```

- 8.删除节点的属性键
```
CALL olab.security.publisher.remove.node.key({node_id},{field_name}) YIELD value RETURN value
```

- 9.删除节点的某个标签
```
CALL olab.security.publisher.remove.node.label({node_id},{label}) YIELD value RETURN value
```

- 10.删除关系的属性键
```
CALL olab.security.publisher.remove.relationship.key({rel_id},{field_name}) YIELD value RETURN value
```

- 11.增加节点的标签
```
CALL olab.security.publisher.add.node.label({node_id},{label}) YIELD value RETURN value
```

### 2、`reader_proc`角色的账户使用
>以`reader-1`用户为例

- 1.查看拥有的权限
```
CALL olab.security.get() YIELD value RETURN value
```

- 2.执行查询
```
CALL olab.security.reader('query001') YIELD value RETURN value;
CALL olab.security.reader('query002') YIELD value RETURN value;
```



