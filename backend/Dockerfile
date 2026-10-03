# 城市无主井盖协同治理平台 —— 后端镜像
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
# 上传文件持久化目录（compose 中挂载卷）
RUN mkdir -p /app/uploads
EXPOSE 8080
# 数据库连接等由 docker-compose 通过环境变量注入：
# SPRING_DATASOURCE_URL / SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
