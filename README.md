# ticketing

活动票务 / 预订平台 —— 一个用来**系统学习 Java 后端技术栈 + GCP 部署**的项目。

业务(发布活动、用户抢票)只是载体,刻意选得足够简单;真正的学习目标是 Java 后端工程与云原生设计。核心技术练点是"**抢票**"——它把事务、并发锁、幂等、异步、定时任务全逼了出来。

> 设计细节见 [docs/DESIGN.md](docs/DESIGN.md);上云与成本见 [docs/GCP.md](docs/GCP.md)。

## 技术栈

| 分类 | 选型 |
|------|------|
| 语言 / 框架 | Java 21 + Spring Boot 3.3 |
| 数据库 | PostgreSQL + Spring Data JPA + Flyway |
| 缓存 | Redis |
| 构建 | Maven |
| 本地容器 | Colima + Docker Compose |
| 目标云 | Google Cloud(Cloud Run / Cloud SQL / ...) |

## 前置条件（已在本机装好）

- JDK 21(`JAVA_HOME` 已写入 `~/.zshrc`)
- Maven
- Colima + Docker + Docker Compose(已配国内镜像加速器)

新开终端验证：`java -version`、`mvn -version`、`docker version` 都应正常。

## 本地启动

```bash
# 1. 拉起依赖（PostgreSQL + Redis）
docker compose up -d

# 2. 运行应用（开发模式）
mvn spring-boot:run

# 3. 验证
curl localhost:8080/ping              # {"status":"ok","service":"ticketing"}
curl localhost:8080/actuator/health   # 含 db / redis 各自状态
```

不开发时 `docker compose stop` 停掉容器省资源;数据存在卷里不丢,`docker compose up -d` 再起。

## 项目结构

```
ticketing/
├── pom.xml                      依赖与构建
├── Dockerfile                   多阶段镜像（上云用）
├── docker-compose.yml           本地 PG + Redis
├── docs/
│   ├── DESIGN.md                架构设计
│   └── GCP.md                   上云与成本
└── src/main/
    ├── java/com/example/ticketing/
    │   ├── TicketingApplication.java
    │   └── common/              公共（异常 / 幂等 / 配置）
    │       └── web/PingController.java
    └── resources/
        ├── application.yml       配置（DB / Redis / Flyway）
        └── db/migration/         Flyway 迁移脚本
```

将来按业务域扩展为:`auth/` `event/` `ticket/` `order/` `payment/` `notification/`。

## 阶段进度

- [x] **阶段 0**：脚手架 + 容器化 + 本地依赖
- [x] **阶段 1**：活动 CRUD + 用户认证（JPA / Security / Flyway）
- [ ] **阶段 2**：抢票核心（事务 / 乐观悲观锁 / 幂等）
- [ ] **阶段 3**：占座超时释放 + 海报上传（定时任务 / 对象存储）
- [ ] **阶段 4**：异步通知 + 缓存（消息队列 / Redis）
- [ ] **阶段 5**：部署到 GCP（Cloud Run / Cloud SQL / CI-CD）
- [ ] **阶段 6**：可观测性 + 集成测试（Actuator / Testcontainers）
