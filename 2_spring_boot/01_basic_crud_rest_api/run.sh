kill -9 $(lsof -t -i :8080)
mvn spring-boot:run