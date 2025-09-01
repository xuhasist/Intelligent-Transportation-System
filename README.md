# Intelligent Transportation System
A dynamic traffic signal control system based on real-time vehicle flow, built with **Java 21** and **Spring Boot 3.5.3**.


## 🧩 Features
- RESTful API with Spring Boot Web
- JWT Authentication with Spring Security & JJWT
- Scheduled tasks for real-time dynamic traffic signal control
- MQTT Client for parsing and forwarding traffic control messages
- Socket communication with traffic controllers (TCs)
- MySQL Database Integration via Spring Data JPA
- API Documentation with Springdoc OpenAPI (Swagger UI)
- Discord Notification for sending alerts
- Asynchronous multi-threaded processing of TC messages


## 🚀 Getting Started

### 1. Clone the repository
- git clone https://github.com/xuhasist/Intelligent-Transportation-System.git
- cd Intelligent-Transportation-System

### 2. Set up the environment
- This project requires configurations to be set in application.properties, which are excluded for security reasons.
- Please create your own configuration files such as database, MQTT broker or environment variables as needed.
- You can place the configuration file under:
  ```
  src/main/resources/application.properties
  ```

### 3. Build the project
- This project is primarily developed and run using IntelliJ IDEA (Java 21).
- Import the project as a **Maven** project.
- Build the project and generate the executable jar file.
  - Run ```mvn clean package```
  - The output .jar will be found under:
    ```
    ./target/itsproject-0.0.1-SNAPSHOT.jar
    ```

### 4. Run with Docker
- This project is deployed in a Docker environment.
- Utilize Dockerfile and docker-compose.yml for building and running the application.


## 📁 Project Structure

```
src
 ├── main
 │   ├── java/com/demo/
 │   │    ├── config            # Spring Boot configurations (Async, DataSource, Security, Swagger, etc.)
 │   │    ├── controller        # REST API endpoints (auth, paging, Excel export, etc.)
 │   │    ├── dto               # DTOs for requests and responses
 │   │    ├── enums             # Application constants
 │   │    ├── exception         # Custom exceptions and global exception handling
 │   │    ├── itsproject        # Main application entry point
 │   │    ├── manager           # Dynamic traffic control logic and message handling
 │   │    ├── mapper            # MapStruct mappers for converting between entities and DTOs
 │   │    ├── message           # Definition of traffic control communication messages and protocol processing
 │   │    ├── model             # JPA entities
 │   │    │    ├── dynamic      # Entities for db_dynamic database
 │   │    │    └── its          # Entities for db_its database
 │   │    ├── notification      # Discord notification
 │   │    ├── repository        # JPA repositories
 │   │    │    ├── dynamic      # Repositories for db_dynamic database
 │   │    │    └── its          # Repositories for db_its database
 │   │    ├── scheduler         # Scheduled tasks (connection, thread pool, and dynamic traffic condition monitoring)
 │   │    ├── security          # Security-related classes (UserDetails, etc.) 
 │   │    ├── service           # Business logic (MQTT, socket, REST API, data handling, Caffeine caching, etc.)
 │   │    └── util              # Utility classes (e.g., LocalDateTime serializer/deserializer)
 │   └── resources
 │        ├── application.properties        # Environment-specific configurations (excluded)
 │        └── application-prod.properties   # Configurations for production environment (excluded)        
 └── test  
     └── java/com/demo/
          ├── controller        # Unit tests for REST controllers
          ├── itsproject        # Application-level tests startup
          └── service           # Unit tests for service layer
```

## 🚦 Dynamic Traffic Control Overview

```
                                                 ┌──> TCReceiveMessageManager ──┐ 
ScheduledDynamicTask ──> DynamicControlManager ──┤                              ├──> SocketService <──> Traffic Controllers (TCs)
                                │                └──> TCSendMessageManager ─────┘ 
                                ˅
                         DynamicService
                                ├──> conditionMap
                                ├──> trafficPeriodsMap
                                └──> thresholdMap
```

- **ScheduledDynamicTask**: Regularly checks if the current time falls within any designated dynamic control periods and asynchronously triggers DynamicControlManager for each matching period.
- **DynamicControlManager**: Performs asynchronous traffic flow calculations, retrieves parameters from DynamicService, evaluates if vehicle flow meet defined thresholds and conditions, and applies dynamic control commands.
- **DynamicService**: Loads and organizes dynamic control parameters from database, storing them in map structures.
- **TCReceiveMessageManager & TCSendMessageManager**: Manage parsing and processing of dynamic control protocol messages and communicate with Traffic Controllers via socket.
