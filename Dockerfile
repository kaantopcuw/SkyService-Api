# --- Build Aşaması ---
# Maven ve Java 21 içeren bir temel imaj kullanarak build işlemini başlat
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# pom.xml dosyasını kopyala
COPY pom.xml .

# Bağımlılıkları indirerek Docker katman önbelleklemesinden yararlan
RUN mvn dependency:go-offline

# Proje kaynak kodunu kopyala
COPY src ./src

# Maven ile projeyi paketle (testleri çalıştırmadan)
RUN mvn package -DskipTests

# --- Çalıştırma Aşaması ---
# Daha küçük boyutlu bir JRE imajı kullanarak son imajı oluştur
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Build aşamasında oluşturulan JAR dosyasının yolunu belirt
ARG JAR_FILE=/app/target/api-gateway-0.0.1-SNAPSHOT.jar

# JAR dosyasını build aşamasından kopyala ve adını app.jar olarak değiştir
COPY --from=builder ${JAR_FILE} app.jar

# Spring Boot uygulamasının varsayılan portunu dışarıya aç
EXPOSE 8080

# Veritabanı dosyalarının kalıcı olmasını sağlamak için bir volume tanımla
VOLUME /app/data

# Konteyner başladığında uygulamayı çalıştıracak komut
ENTRYPOINT ["java","-jar","app.jar"]