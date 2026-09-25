# Usar la imagen oficial de Amazon Corretto Java 21 (Linux)
FROM amazoncorretto:21-alpine-full

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar el ejecutable empaquetado desde la máquina host al contenedor
COPY target/networking-lab2-1.0-SNAPSHOT.jar app.jar

# Puerto por defecto expuesto por el contenedor
EXPOSE 8080

# Variable de entorno por defecto
ENV PORT=8080
ENV APP_ENV=development

# Comando de ejecución
ENTRYPOINT ["java", "-jar", "app.jar"]