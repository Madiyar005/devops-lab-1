# ------------------------------
# 1-кезең: Құрастыру (builder)
# ------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

# Жұмыс папкасы
WORKDIR build/

# БҮКІЛ app папкасын көшіру (pom.xml бар)
COPY app/ .

# Dependency-лерді жүктеу (кэшке сақталады)
# Бұл тек pom.xml өзгермесе қайта жүктелмейді
RUN mvn dependency:go-offline

# Қолданбаны құрастыру
RUN mvn clean package -DskipTests

# Құрастырылған jar файлын табу
RUN ls -la target/ && echo "Jar файлын тексеру"

# ------------------------------
# 2-кезең: Іске қосу (runtime)
# ------------------------------
FROM eclipse-temurin:17-jre-jammy

# Жұмыс папкасы
WORKDIR /app

# 1-кезеңнен құрастырылған jar файлын көшіру
# Кеңейтілген жол: кез келген jar файлын көшіру
#COPY --from=builder /build/target/*.jar app.jar
COPY --from=builder /build/target/*-jar-with-dependencies.jar app.jar

# Қолданба жұмыс істейтін порт
EXPOSE 5000

# Қолданбаны іске қосу
CMD ["java", "-jar", "app.jar"]
