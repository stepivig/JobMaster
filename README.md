# JobMaster

## Overview

JobMaster is a robust job scheduling and execution system built with Spring Boot. It allows you to create, manage, and
execute scheduled jobs with support for multiple runtime environments including Java and Kotlin.

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

2. Build the application
   ```bash
   cd JobMaster
   ./gradlew clean bootJar
   ```

3. Build the Docker image

   ```bash
   docker build -t jobmaster:latest .
   ```

4. Run the container
   ```bash
   docker run -d -p 8080:8080 --name jobmaster-container jobmaster:latest
   ```
