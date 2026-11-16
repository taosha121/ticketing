# 上云与成本

部署目标是 Google Cloud。本文档记录设计原则、服务映射,以及**怎么尽量不花钱 / 不被意外扣费**。

> 注意:GCP 的免费额度和价格会变。真到部署那一步(阶段 5),以 [GCP 官方定价](https://cloud.google.com/pricing) 和 [Always Free 页面](https://cloud.google.com/free) 的当时数字为准。

## 1. 核心原则:应用无状态,状态全外置

能否轻松部署到 Cloud Run 这种 serverless,只取决于一件事:**Java 应用进程自己不存任何状态**。

- "无状态" ≠ "无数据库"。恰恰相反,**数据库是唯一的事实源头**。
- "无状态"指应用进程不往本地内存/本地磁盘塞"重启就丢、换机器就没"的东西。
- 所有要持久化的数据写进外部托管服务;应用只是无记忆的计算单元。

原因:Cloud Run 会把应用动态扩容成多个实例、或缩容到 0 再重启。状态存应用内存里,多实例就各记各的、数据错乱。全放外部服务,多实例读写同一份,才一致。

## 2. 本地 ↔ GCP 服务映射

| 关注点 | 本地(Docker Compose) | GCP | Java 侧 |
|--------|----------------------|-----|---------|
| 应用本身 | Spring Boot jar / 容器 | **Cloud Run** | Dockerfile |
| 关系库 | PostgreSQL 容器 | **Cloud SQL** | Spring Data JPA |
| 缓存 | Redis 容器 | **Memorystore** | Spring Data Redis |
| 文件/图片 | MinIO / 本地盘 | **Cloud Storage** | Spring Cloud GCP Storage |
| 消息队列 | Kafka / RabbitMQ | **Pub/Sub** | Spring Cloud GCP Pub/Sub |
| 定时任务 | Spring Scheduler | **Cloud Scheduler** | REST 触发 |
| 密钥 | `.env` / 环境变量 | **Secret Manager** | Spring Cloud GCP Secret |
| 镜像仓库 | 本地 | **Artifact Registry** | Jib / Dockerfile |
| 日志 | 控制台 | **Cloud Logging** | 结构化 JSON 日志 |

桥梁是 **Spring Cloud GCP(Spring Framework on Google Cloud)**:给 Pub/Sub、Cloud SQL、Storage、Secret Manager 都提供 starter,本地用标准 Spring 抽象写代码,上云几乎零改动。

## 3. 免费额度

GCP 的"免费"分两种:

- **免费试用**:新账号送 **$300 额度,90 天**,期间几乎所有服务随便用。
- **永久免费(Always Free)**:部分服务每月固定免费额度,试用期过后仍免费。

### 有永久免费额度的(放心用)

| 服务 | 永久免费额度(大致) |
|------|---------------------|
| Cloud Run | 每月 200 万次请求,空闲缩容到 0 不计费 |
| Cloud Storage | 5 GB 存储 |
| Pub/Sub | 每月 10 GB 消息量 |
| Cloud Scheduler | 3 个任务 |
| Secret Manager | 6 个密钥版本 |
| Cloud Logging | 每月 50 GiB |

### 没有永久免费额度的(坑!按小时计费,闲着也扣)

| 服务 | 情况 |
|------|------|
| **Cloud SQL**(托管 PG) | 无永久免费,最小规格约 $8–10/月起,**只要实例开着就扣** |
| **Memorystore**(托管 Redis) | 无永久免费,最小规格 $30+/月 |

数据库恰恰是核心,这是最该注意的地方。

## 4. 省钱方案

**路线 A — 吃试用额度(最省心)**
学习期用 $300/90 天额度,把真实架构(Cloud Run + Cloud SQL + Memorystore)完整部署一遍,体验最真。

**路线 B — 长期白嫖(试用过期后)**
- Postgres:不用 Cloud SQL,改用 [Neon](https://neon.tech) 或 [Supabase](https://supabase.com) 的免费 Postgres;或自建在 GCP 免费 e2-micro 小机器上。
- Redis:不用 Memorystore,改用 [Upstash](https://upstash.com) 免费 Redis。
- 应用:照样 Cloud Run(免费额度内)。

## 5. 防止意外扣费(务必做)

上云前先做两件事:

1. **设预算告警**:GCP 控制台配 Budget Alert(比如 $5),超了邮件提醒。
2. **用完就停**:Cloud SQL / Memorystore 按小时计费,不学的时候把实例 **stop 或删掉**(Cloud SQL 支持 stop)。Cloud Run 不用管,自己缩容到 0。
