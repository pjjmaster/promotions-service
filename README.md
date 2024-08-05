
# Promotions Service Application

## Overview

This application is designed to process large CSV files containing promotion data and store the processed data in Redis. The application supports different implementations for handling the CSV processing and provides a RESTful API for interacting with the data.

## Components

### 1. Promotion Service Implementations

There are three implementations of the `PromotionsService` interface, each handling CSV file processing differently:

-   **PromotionServiceEfficient**

      -   **Package**: `com.verve.assessment.service`
      -   **Description**: This implementation uses non-blocking virtual threads and a `ConcurrentLinkedQueue` to process records in batches, providing high performance and efficient resource usage.

-   **PromotionServiceMultiThreaded**

      -   **Package**: `com.verve.assessment.service`
      -   **Description**: This implementation uses a multi-threaded approach with a synchronized `HashMap` to store records in Redis. It balances concurrency and performance.

-   **PromotionsServiceSimple**

      -   **Package**: `com.verve.assessment.service`
      -   **Description**: A simple, single-threaded implementation that processes and stores each record sequentially. It uses a Spring Data repository for Redis interactions.

Currently, PromotionServiceEfficient Bean is enabled by default. You can enable other implementations by 
by annotating those classes with @Service and commenting out the @Service of other implementations
  

### 2. Scheduled Promotions Loader

-   **Class**: `ScheduledPromotionsLoader`
      -   **Package**: `com.verve.assessment.service`
      -   **Description**: A scheduled task that runs every 30 minutes to reload the promotions data by invoking the `processFile` method of the active `PromotionsService` implementation.

### 3. Promotion Repository

-   **Interface**: `PromotionRepository`
      -   **Package**: `com.verve.assessment.repository`
      -   **Description**: Extends `CrudRepository<Promotion, String>` to provide basic CRUD operations for `Promotion` entities.


### 4. Application Configuration

-   **Maven POM**
      -   The project is built using Maven and includes dependencies such as:
            -   `spring-boot-starter-web`: For building the RESTful API.
            -   `spring-boot-starter-data-redis`: For interacting with Redis.
            -   `springdoc-openapi-starter-webmvc-ui`: For generating Swagger documentation.
            -   `opencsv`: For parsing CSV files.
            -   `lombok`: To reduce boilerplate code.
      -   **Java Version**: 21

## Swagger Documentation

-   The application uses SpringDoc for Swagger documentation, enabling easy API exploration and testing.
-   To access the Swagger UI, start the application and navigate to `/swagger-ui.html`.

## How to Use

1.  **Running the Application**:

      -   Build and run the application using Maven: `mvn spring-boot:run`.
2.  **Uploading CSV Files**:

      -   Use the exposed API to upload and process CSV files containing promotion data. The application will parse the file, batch process the records, and store them in Redis.
3.  **Scheduled Data Loading**:

      -   The application automatically reloads the promotions data every 30 minutes, as configured in `ScheduledPromotionsLoader`.

## Error Handling

-   The application logs all exceptions and errors, providing detailed information for debugging. Errors encountered during CSV processing will throw a `RuntimeException` and be logged.


## Questions

1. The .csv file could be very big (billions of entries) - how would your application
   perform? How would you optimize it?
   **Answer:**
   In current implementation, this is achieved using a combination of streaming and batching techniques, where records are read, processed, and stored in Redis in chunks. This approach reduces memory usage and allows the application to handle very large files.
   Also current implementation uses Java 21 virtual threads which are non-blocking and light weight. This parallel processing has improved the performance significantly. I have recorder the response time of POST endpoint for the given csv file which has 200000 records. Below are the results:
   Without using virtual threads:
   |Iteration| Time taken  |
   |1				|1 min 45 seconds|
   |2	     		|1 min 53 seconds|
   |3     		    |1 min 27 seconds|

   After using virtual threads:
   Batch size 3  --> 3.43 seconds
   Batch size 50  --> 4.25 seconds
   Batch size 100  --> 4.84 seconds
   Batch size 1000  --> 5.19 seconds
   Batch size 5000  --> 1132 ms
   Batch size 10000  --> 2.18 seconds /900 ms
   Batch size 50000  --> 2.16 seconds /981 ms
   Batch size 100000 --> 2.41 seconds
   Batch size 1000000 (1million) --> 2.83 seconds


2. How would your application perform in peak periods (millions of requests per
   minute)? How would you optimize it?
   **Answer:**
   In current implementation, we are using Redis, a high-performance, in-memory data store, which is well-suited for handling a high volume of read and write operations. Redis is capable of managing millions of requests per second with low latency.
   We do not need to query the database, redis is a cache as returns the result quickly.
   To further improve the performance, we can use Redis cluster, which allows the Redis workload to be distributed across multiple nodes and we can scale the redis horizontally.


3. How would you operate this app in production (e.g. deployment, scaling, monitoring)?
   **Answer:**
   As per the current implementation, we can create a spring boot executable jar, which can be deployed to any server or AWS EC2 or Azure App Server instance. We should implement the CI/CD pipeline which creates and pushes the jar to artifactory and deploys it to server.
   Additionally, we can containerize the application using dockerFile and creates a docker image using CI pipeline. This docker image can be pushed to kubernetes cluster or any public cloud service.
   We can use Kubernetes or another orchestration tool to manage these instances and handle scaling automatically. Or we can use the auto scaling mechanisms of public clouds.
   We can use Prometheus and Grafana to monitor application performance, resource usage, and request metrics. Or AWS CloudWatch also provides logging and monitoring solution. 
