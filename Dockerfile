# 第一階段：使用 Maven 進行編譯與打包
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
# 切換到 translate 子資料夾進行打包
WORKDIR /app/translate
RUN mvn clean package -DskipTests

# 第二階段：使用輕量 JDK 執行打包好的 jar
FROM eclipse-temurin:17-jre
WORKDIR /app
# 從 translate 子資料夾內抓出打包好的 jar 檔
COPY --from=build /app/translate/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]