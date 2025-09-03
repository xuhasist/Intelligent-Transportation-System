FROM eclipse-temurin:21-jdk-alpine

RUN apk update && apk upgrade && \
    apk add --no-cache bash mysql-client

# Set working directory for the application
WORKDIR /app

# Copy the application JAR file into the /app directory
COPY ${JAR_FILE} .

ENV JAR_PATH=/app/

CMD java $JAVA_OPTS -jar $JAR_PATH$JAR_FILE