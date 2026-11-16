# 多阶段构建：第一阶段编译打包，第二阶段只带 JRE 跑，镜像更小
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# 先拷 pom 单独下依赖，利用 Docker 层缓存（源码变了不必重下依赖）
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/ticketing-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
