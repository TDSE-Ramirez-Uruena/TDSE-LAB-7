# LAB 7 - Web Framework Extension: Concurrent Execution & Dockerization

**Student:** Julián Ramírez  
**Course:** Enterprise Architecture - Escuela Colombiana de Ingeniería Julio Garavito  
**Docker Hub Repository:** `https://hub.docker.com/r/<TU_USUARIO_DOCKERHUB>/networking-lab2`  
**Cloud Deployment (AWS EC2):** `http://54.242.64.181:35000/`

---

## 1. Project Overview & Current State

This project represents the evolved, production-ready version of our custom web micro-framework. Transitioning from the previous single-threaded sequential architecture, this extension introduces **multithreaded concurrent request processing**, **thread-safe graceful shutdown handling**, full **containerization via Docker**, and deployment onto **Amazon Web Services (AWS EC2)**.

### Key Enhancements:
* **Concurrent Request Handling:** Replaced the sequential single-thread loop with an `ExecutorService` fixed thread pool (`THREAD_POOL_SIZE = 10`), allowing non-blocking parallel client request processing.
* **Thread-Safe Graceful Shutdown:** Enhanced lifecycle management to safely unbind the `ServerSocket` and terminate active worker threads within a grace period (`awaitTermination`).
* **Containerized Infrastructure:** Built using the official **Amazon Corretto Java 21** base image (`21-alpine-full`) and published to **Docker Hub**.
* **Cloud Execution via Docker:** Deployed on AWS EC2 by pulling and running the container image directly.

---

## 2. Commit Evidence of Extension Progress

As required by the assignment criteria, the framework extension is backed by a specific, meaningful commit demonstrating the concurrency implementation:

* **Commit Message:** `Implement concurrent request handling using ThreadPool and graceful shutdown`
* **Commit Hash:** `<TU_COMMIT_HASH_AQUÍ>` *(Replace with output of `git log -1 --format="%h"`)*

---

## 3. Architecture & Concurrency Model

```text
Incoming Client Requests
          │
          ▼
   HttpServer Loop (Main Thread)
          │
          ├──► Delegates Socket Connection to ExecutorService (Thread Pool)
          │
          ▼
   Worker Thread (Thread 1..10)
          │
          ├──► Parse Request Abstraction
          ├──► Check Router (Lambda Handlers)
          ├──► Fallback to StaticFileService (Byte Streams)
          └──► Flush Response & Close Client Socket
```

### Key Differences: Before vs. After Extension

| **Aspect**                    | **Previous Version**                   | **Extended Framework Version**                      |
| ----------------------------- | -------------------------------------- | --------------------------------------------------- |
| **Execution Model**           | Sequential (1 request at a time)       | Concurrent (`Executors.newFixedThreadPool(10)`)     |
| **Shutdown Behavior**         | Closed socket after current iteration  | Closes socket and awaits thread pool termination    |
| **Packaging & Delivery**      | Executable Fat-JAR transferred via SCP | Docker Image pulled from Docker Hub                 |
| **Cloud Runtime Environment** | Host OS Java Process                   | Isolated Docker Container on Amazon Corretto 21     |

## 4. Local Build & Docker Instructions

### Local Execution (Maven)

1. Compile and package the project: 
```bash
   mvn clean package
   ```
2. Run locally using Java21:
```bash
   java -jar target/networking-lab2-1.0-SNAPSHOT.jar
   ```

### Docker Containerization

1. Build Docker Image locally:
```bash
   docker build -t networking-lab2:latest .
   ```
2. Run Docker Container with Environment Variables:
```bash
   docker run -d -p 8080:8080 -e PORT=8080 -e GREETING_PREFIX="HolaDocker" -e APP_ENV=development --name webapp networking-lab2:latest
   ```
3. Publish Image to Docker Hub:
```bash
   docker tag networking-lab2:latest <TU_USUARIO_DOCKERHUB>/networking-lab2:latest
   ```
```bash
   docker push <TU_USUARIO_DOCKERHUB>/networking-lab2:latest   
   ```
## 5. AWS EC2 Cloud Deployment

The containerized framework is live on AWS EC2:

1. **Host Environment:** Amazon Linux 2023 (`t2.micro` instance).

2. **Security Group Rules:** Custom TCP Port `35000` and SSH Port `22` enabled for `0.0.0.0/0`.

3. **Container Launch Command:**

   ```bash
   sudo docker run -d -p 35000:35000 -e PORT=35000 -e GREETING_PREFIX="WelcomeAWS" -e APP_ENV=production --name webapp <TU_USUARIO_DOCKERHUB>/networking-lab2:latest
   ```
4. Endpoint Validation:

- Web Interface: http://54.242.64.181:35000/
- Lambda Endpoint: http://54.242.64.181:35000/hello?name=Julian
- Protected Shutdown: http://54.242.64.181:35000/shutdown (Returns 404 Not Found under APP_ENV=production).

## 6. Demonstration Video Link

A short demonstration video showing local Docker execution, Docker Hub publication, and live AWS EC2 deployment is available here:

- https://youtu.be/HpA7rmRjfqM

## 7. License & Author

- Author: Julián Ramírez
- Acknowledgments: Escuela Colombiana de Ingeniería Julio Garavito and the official AWS EC2 / Java OpenJDK documentation.