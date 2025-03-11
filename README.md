# JobMaster

## Overview

JobMaster is a reliable job scheduler built with Spring Boot. It lets you create, manage, and run scheduled tasks in
Java or Kotlin.

The system uses an H2 database to save data to a file. You can set up one-time or repeated jobs using cron expressions.
Jobs are small pieces of Java or Kotlin code with a `Main` class and a static `main` method. Each job can take input
parameters and run multiple times. Every execution is saved to the database along with results and additional details.
Jobs can be changed or removed easily. When the application restarts, it automatically schedules any existing recurring
jobs.

## Prerequisites

Ensure you have the following installed on your system:

- **Java Development Kit (JDK) 17 or later**
- **Gradle**
- **Docker Desktop**

## Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/stepivig/JobMaster.git
   ```
   or
   ```bash
   git clone git@github.com:stepivig/JobMaster.git
   ```

2. **Build the application**
   ```bash
   cd JobMaster
   ./gradlew clean bootJar
   ```

3. **Build the Docker image**
   ```bash
   docker build -t jobmaster:latest .
   ```

4. **Run the container**
   ```bash
   docker run -d -p 8080:8080 --name jobmaster-container jobmaster:latest
   ```

Alternatively, you can run the application using Spring Boot directly or through IntelliJ IDEA:

- **Run with Spring Boot**
  ```bash
  ./gradlew bootRun
  ```

By default, the application runs on port `8080`.

## Usage

### API Endpoints

JobMaster exposes a set of RESTful API endpoints to manage and execute jobs. Below are some key endpoints:

- **Create a Job**
    - **Endpoint:** `POST /jobs`
    - **Description:** Creates a new job with the specified parameters.
    - **Request Body Example:**
      ```json
      {
        "name": "example-job",
        "description": "A simple example job",
        "memorySize": 128,
        "runtime": "JAVA",
        "timeout": 60000,
        "active": true,
        "codeType": "RAW",
        "codeRaw": "public class Main { public static void main(String[] args) { System.out.println(\"Hello, World!\"); } }",
        "scheduleType": "ONCE",
        "codeInput": ["param1", "param2"],
        "createdAt": "2025-01-01T00:00:00.182974",
        "updatedAt": "2025-01-01T00:00:00.182974"
      }
      ```

- **Get All Jobs**
    - **Endpoint:** `GET /jobs`
    - **Description:** Retrieves a list of all jobs.
    - **Response Example:**
      ```json
      [
        {
          "id": 1,
          "name": "example-job",
          "description": "A simple example job",
          "memorySize": 128,
          "runtime": "JAVA",
          "timeout": 60000,
          "active": true,
          "codeType": "RAW",
          "codeRaw": "public class Main { public static void main(String[] args) { System.out.println(\"Hello, World!\"); } }",
          "scheduleType": "ONCE",
          "codeInput": ["param1", "param2"],
          "createdAt": "2025-01-01T00:00:00.182974",
          "updatedAt": "2025-01-01T00:00:00.182974"
        }
      ]
      ```

- **Get Job by ID**
    - **Endpoint:** `GET /jobs/{jobId}`
    - **Description:** Retrieves the details of a specific job by its ID.
    - **Response Example:**
      ```json
      {
        "id": 1,
        "name": "example-job",
        "description": "A simple example job",
        "memorySize": 128,
        "runtime": "JAVA",
        "timeout": 60000,
        "active": true,
        "codeType": "RAW",
        "codeRaw": "public class Main { public static void main(String[] args) { System.out.println(\"Hello, World!\"); } }",
        "scheduleType": "ONCE",
        "codeInput": ["param1", "param2"],
        "createdAt": "2025-01-01T00:00:00.182974",
        "updatedAt": "2025-01-01T00:00:00.182974"
      }
      ```

- **Update a Job**
    - **Endpoint:** `PUT /jobs/{jobId}`
    - **Description:** Updates the details of an existing job.
    - **Request Body Example:**
      ```json
      {
        "name": "updated-job",
        "description": "An updated example job",
        "memorySize": 256,
        "runtime": "KOTLIN",
        "timeout": 120000,
        "active": true,
        "codeType": "RAW",
        "codeRaw": "fun main(args: Array<String>) { println(\"Hello, Kotlin World!\") }",
        "scheduleType": "ONCE",
        "codeInput": ["param1", "param2"],
        "createdAt": "2025-01-01T00:00:00.182974",
        "updatedAt": "2025-01-01T00:00:00.182974"
      }
      ```

- **Delete a Job**
    - **Endpoint:** `DELETE /jobs/{jobId}`
    - **Description:** Deletes a specific job by its ID.

- **Get Executions by Job ID**
    - **Endpoint:** `GET /jobs/{jobId}/executions`
    - **Description:** Retrieves a list of executions for a specific job.
    - **Response Example:**
      ```json
      [
        {
          "id": 1,
          "status": "COMPLETED",
          "creationTime": "2025-01-01T12:00:00",
          "startTime": "2025-01-01T12:05:00",
          "endTime": "2025-01-01T12:10:00",
          "customInput": {"param1": "value1"},
          "output": "Hello, World!"
        }
      ]
      ```

- **Get Execution by Job ID and Execution ID**
    - **Endpoint:** `GET /jobs/{jobId}/executions/{executionId}`
    - **Description:** Retrieves the details of a specific execution by job ID and execution ID.
    - **Response Example:**
      ```json
      {
        "id": 1,
        "status": "COMPLETED",
        "creationTime": "2025-01-01T12:00:00",
        "startTime": "2025-01-01T12:05:00",
        "endTime": "2025-01-01T12:10:00",
        "customInput": {"param1": "value1"},
        "output": "Hello, World!"
      }
      ```