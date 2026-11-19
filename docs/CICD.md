# CI/CD 概念与实践

## CI 和 CD 的范围

### CI（持续集成）

核心定义：频繁合并代码，自动验证每次变更是否正确。

```
触发（push / PR）
   ↓
编译 / 构建
   ↓
静态分析          Checkstyle、SonarQube、ESLint
安全扫描          OWASP dependency-check、Snyk、Dependabot
   ↓
单元测试          JUnit、Jest、pytest
集成测试          Testcontainers、Cypress、Playwright
   ↓
构建产物          打 Docker image、打 jar 包
推到 Registry     ← CI 与 CD 的分界线
```

测试只是 CI 的验证手段之一，编译失败、lint 报错同样是 CI 阻断合并的理由。

### CD（持续交付 / 持续部署）

```
Registry 有新镜像
   ↓
部署到 staging
   ↓
自动化验收测试
   ↓
部署到 prod
```

### 推镜像到 Registry 算 CI 还是 CD？

严格来说是 CD 的起点，但处于边界上，业界有分歧。

- **CI 视角**：测试通过后构建镜像是 CI 的最后一步，产出可部署产物
- **CD 视角**：镜像产出之后，后续的部署流程才是 CD

比较清晰的划分方式是拆成两个 workflow 文件：`ci.yml`（测试）和 `deploy.yml`（部署）。

面试中说"推 Registry 是 CI/CD 的分界点"是最稳妥的答法。

---

## GitHub Actions vs Jenkins

### 本质区别

| | GitHub Actions | Jenkins |
|---|---|---|
| Runner 位置 | GitHub 云上 | 公司自己的服务器 |
| 能访问内网资源 | 不能（除非 self-hosted runner） | 能 |
| 配置文件位置 | repo 里的 `.github/workflows/` | repo 里的 `Jenkinsfile` + Jenkins 服务器配置 |
| 触发方式 | GitHub 直接触发 | GitHub Webhook 通知 Jenkins |
| 维护成本 | 低（GitHub 托管） | 高（需要自己运维服务器） |

### 代码在 GitHub 但用 Jenkins 的原因

核心原因只有一个：**需要访问公司内网资源**。

- 内网 Kubernetes 集群
- 内网 Docker Registry
- 内网数据库
- 公司 VPN 内的服务

GitHub Actions 的 runner 在 GitHub 的云上，物理上无法直接连接公司内网。Jenkins 服务器架在公司内网，可以直接访问一切内网资源。

> GitHub Actions 也有 self-hosted runner 方案，可以在公司内网机器上跑，但维护成本和 Jenkins 差不多，不如直接用 Jenkins。

---

## Jenkinsfile

Jenkinsfile 用 **Groovy** 语言编写，有两种语法格式。

### 格式一：Declarative Pipeline（主流）

2016 年后推出，结构固定，易读，新项目推荐使用。

```groovy
pipeline {
    agent { label 'java-agent' }

    environment {
        DOCKER_REGISTRY = 'registry.company.internal'
        IMAGE_NAME = 'my-service'
    }

    stages {
        stage('Build & Test') {
            steps {
                sh 'mvn test --no-transfer-progress'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.BUILD_NUMBER} ."
            }
        }

        stage('Push to Registry') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'registry-creds',
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                    sh "docker login ${DOCKER_REGISTRY} -u $USER -p $PASS"
                    sh "docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.BUILD_NUMBER}"
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                sh "kubectl set image deployment/my-service my-service=${DOCKER_REGISTRY}/${IMAGE_NAME}:${env.BUILD_NUMBER}"
            }
        }
    }

    post {
        failure {
            sh "curl -X POST https://qyapi.weixin.qq.com/... -d '{\"content\":\"Build failed\"}'"
        }
    }
}
```

如果需要写复杂逻辑，用 `script {}` 块：

```groovy
stage('Conditional Deploy') {
    steps {
        script {
            if (env.BRANCH_NAME == 'main') {
                sh 'kubectl apply -f k8s/'
            }
        }
    }
}
```

### 格式二：Scripted Pipeline（老格式）

2016 年之前的主流写法，纯 Groovy 脚本，灵活但难维护。老项目中常见。

```groovy
node {
    stage('Test') {
        sh 'mvn test'
    }

    if (env.BRANCH_NAME == 'main') {
        stage('Deploy') {
            sh 'kubectl apply -f k8s/'
        }
    }
}
```

---

## 本项目的 CI 配置

本项目使用 GitHub Actions，配置文件见 [.github/workflows/ci.yml](../.github/workflows/ci.yml)。

**当前覆盖范围**：
- 编译 ✓
- 单元测试（domain 逻辑、JwtService）✓
- Service 层测试（Mockito mock）✓
- 集成测试（Testcontainers + 真实 Postgres + Redis）✓

**测试分层**：
- `@Tag("unit")` — 纯领域逻辑，无 Spring 上下文，毫秒级
- `@Tag("service")` — Service 层，Mockito mock 依赖，无 Docker
- `@Tag("integration")` — 完整 Spring Boot + Testcontainers，需要 Docker

**Pre-commit hook**：本地提交前自动运行 unit + service 测试（不需要 Docker），集成测试只在 CI 上跑。
