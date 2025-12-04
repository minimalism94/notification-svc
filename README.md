# Notification Service - Microservice

<div align="center">

**A dedicated microservice for handling notifications in the SmartExpense ecosystem**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)

</div>

---

## 🛠️ Technology Stack

### Backend Technologies
- **Java 17** - Programming language
- **Spring Boot 3.4.0** - Application framework
- **Spring Data JPA** - Data persistence layer
- **Hibernate** - ORM framework
- **MySQL 8.0** - Relational database
- **Lombok** - Boilerplate code reduction
- **Maven** - Build automation and dependency management

### Communication & Integration
- **RESTful API** - HTTP-based service communication
- **Spring Boot Web** - REST controller framework
- **Spring Cloud OpenFeign** - Client-side service communication (used by main app)

### Notification Services
- **JavaMailSender** - Email notifications (SMTP)
- **GreenAPI** - SMS/WhatsApp notifications
- **MIME Message Support** - Email attachments (PDF reports)

### Development Tools
- **SLF4J** - Logging framework
- **JUnit** - Testing framework
- **H2 Database** - In-memory database for testing
- **Spring Boot Validation** - Input validation

---

## 🌐 Service Information

- **Service Name:** `notification-svc`
- **Host:** `localhost`
- **Port:** `9091`
- **Base URL:** `http://localhost:9091`
- **API Base Path:** `/api/v1`
- **Database:** `notification_svc_sept_2025` (separate from main app database)

---

## ⚠️ Important: Integration with Main Application

**This microservice is a critical component of the SmartExpense application.**

### Dependency Relationship

The **SmartExpense main application** (port 9090) **REQUIRES** this notification service to be running for:

1. **User Registration** - Cannot create notification preferences without this service
2. **Notification Management** - All notification settings are managed here
3. **Email Notifications** - Subscription expiry alerts, monthly reports
4. **SMS Notifications** - Subscription expiry alerts via WhatsApp/SMS
5. **Notification History** - All notification records are stored here

### Communication

- Main app uses **Spring Cloud OpenFeign** to communicate with this service
- RESTful API endpoints exposed for service-to-service communication
- Asynchronous notification processing for better performance

### Startup Requirements

**⚠️ CRITICAL:** This service MUST be started **BEFORE** the main SmartExpense application.

**Recommended Startup Order:**
1. ✅ Start Notification Service (port 9091) - **FIRST**
2. ✅ Start SmartExpense Main App (port 9090) - **SECOND**

If the main app starts without this service running, users will not be able to:
- Register new accounts (preference creation fails)
- Manage notification settings
- Receive any notifications

---

## 🎯 Features

### Core Features

- ✅ **Email Notifications**
  - Simple text emails
  - HTML emails with attachments
  - PDF report attachments (Base64 encoded)
  - SMTP configuration support

- ✅ **SMS/WhatsApp Notifications**
  - Integration with GreenAPI
  - WhatsApp message delivery
  - SMS fallback support
  - Delivery status tracking

- ✅ **Notification Preferences Management**
  - Create user notification preferences
  - Update preferences (enable/disable, contact info, type)
  - Automatic type detection (EMAIL/SMS based on contact info)
  - Per-user preference storage

- ✅ **Notification History**
  - Track all sent notifications
  - Status tracking (SUCCEEDED/FAILED)
  - User-specific notification history
  - Soft delete support

### Advanced Features

- **Automatic Type Detection** - Detects EMAIL vs SMS based on contact info format
- **Attachment Support** - PDF attachments in email notifications
- **Error Handling** - Comprehensive exception handling and logging
- **Async Processing** - Asynchronous notification sending
- **Status Tracking** - Track notification delivery status

---

## 📦 Prerequisites

Before running the service, ensure you have:

- **JDK 17 or higher** - [Download Java](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.6+** (or use Maven Wrapper included in project)
- **MySQL 8.0+** - [Download MySQL](https://dev.mysql.com/downloads/mysql/)
- **SMTP Server Access** - For email notifications (Gmail, Outlook, etc.)
- **GreenAPI Account** (Optional) - For SMS/WhatsApp notifications
  - Sign up at [GreenAPI](https://green-api.com/)
  - Get instance ID and API token

---

## 🚀 Quick Start Guide

### Step 1: Clone/Navigate to Service

```bash
cd notification-svc
# or if cloning separately
git clone <repository-url>
cd notification-svc
```

### Step 2: Database Configuration

1. **Start MySQL Server** on your local machine

2. **Update Database Credentials** in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/notification_svc_sept_2025?createDatabaseIfNotExist=true
   spring.datasource.username=root
   spring.datasource.password=your_mysql_password
   ```
   
   **Note:** The database will be created automatically if it doesn't exist.

### Step 3: Email Configuration

Configure SMTP settings in `src/main/resources/application.properties`:

**Gmail Example:**
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

**Note for Gmail:**
- Use an [App Password](https://support.google.com/accounts/answer/185833) instead of your regular password
- Enable 2-Step Verification first

**Outlook Example:**
```properties
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
spring.mail.username=your_email@outlook.com
spring.mail.password=your_password
```

### Step 4: GreenAPI Configuration (Optional)

For SMS/WhatsApp notifications, configure GreenAPI:

1. **Sign up** at [GreenAPI](https://green-api.com/)
2. **Get credentials:**
   - Instance ID
   - API Token
   - API URL (format: `https://{instance-id}.api.green-api.com`)

3. **Update** `src/main/resources/application.properties`:
   ```properties
   green-api.instance-id=your_instance_id
   green-api.api-token=your_api_token
   green-api.api-url=https://your_instance_id.api.green-api.com
   ```

**Note:** SMS notifications will fail if GreenAPI is not configured, but email notifications will still work.

### Step 5: Build the Service

**Windows:**
```bash
mvnw.cmd clean install
```

**Linux/Mac:**
```bash
./mvnw clean install
```

### Step 6: Run the Service

**Windows:**
```bash
mvnw.cmd spring-boot:run
```

**Linux/Mac:**
```bash
./mvnw spring-boot:run
```

**Or using Java directly:**
```bash
java -jar target/notification-svc-0.0.1-SNAPSHOT.jar
```

### Step 7: Verify Service is Running

1. **Check logs** for: `Started Application in X.XXX seconds`
2. **Verify port:** Service should be listening on port `9091`
3. **Test endpoint** (optional):
   ```bash
   curl http://localhost:9091/api/v1/preferences?userId=<test-uuid>
   ```

---

## 📚 API Endpoints

### Notification Preferences

#### Create/Update Preference
```http
POST /api/v1/preferences
Content-Type: application/json

{
  "userId": "uuid-here",
  "notificationEnabled": true,
  "contactInfo": "user@example.com",
  "type": "EMAIL" // Optional: EMAIL or SMS (auto-detected if not provided)
}
```

**Response:**
```json
{
  "userId": "uuid-here",
  "type": "EMAIL",
  "enabled": true,
  "contactInfo": "user@example.com",
  "createdOn": "2025-01-01T10:00:00",
  "updatedOn": "2025-01-01T10:00:00"
}
```

#### Get Preference
```http
GET /api/v1/preferences?userId={uuid}
```

**Response:**
```json
{
  "userId": "uuid-here",
  "type": "EMAIL",
  "enabled": true,
  "contactInfo": "user@example.com",
  "createdOn": "2025-01-01T10:00:00",
  "updatedOn": "2025-01-01T10:00:00"
}
```

### Notifications

#### Send Notification
```http
POST /api/v1/notifications
Content-Type: application/json

{
  "userId": "uuid-here",
  "subject": "Subscription Expiring Soon",
  "body": "Your Netflix subscription expires in 7 days.",
  "type": "EMAIL", // Optional
  "attachmentBase64": "base64-encoded-pdf", // Optional
  "attachmentFileName": "report.pdf", // Optional
  "attachmentContentType": "application/pdf" // Optional
}
```

**Response:**
```json
{
  "id": "notification-uuid",
  "userId": "user-uuid",
  "subject": "Subscription Expiring Soon",
  "body": "Your Netflix subscription expires in 7 days.",
  "type": "EMAIL",
  "status": "SUCCEEDED",
  "createdOn": "2025-01-01T10:00:00"
}
```

#### Get Notification History
```http
GET /api/v1/notifications?userId={uuid}
```

**Response:**
```json
[
  {
    "id": "notification-uuid",
    "userId": "user-uuid",
    "subject": "Subscription Expiring Soon",
    "body": "Your Netflix subscription expires in 7 days.",
    "type": "EMAIL",
    "status": "SUCCEEDED",
    "createdOn": "2025-01-01T10:00:00"
  }
]
```

---

## ⚙️ Configuration

### Application Properties

Key configuration in `src/main/resources/application.properties`:

```properties
# Service Configuration
spring.application.name=notification-svc
server.port=9091

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/notification_svc_sept_2025?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# GreenAPI Configuration (SMS/WhatsApp)
green-api.instance-id=your_instance_id
green-api.api-token=your_api_token
green-api.api-url=https://your_instance_id.api.green-api.com
```

### Database Schema

Tables are auto-created on startup (`spring.jpa.hibernate.ddl-auto=update`):
- `notification_preference` - User notification preferences
- `notification` - Notification history

**Warning:** In production, use `validate` or `none` instead of `update`.

---

## 🏗️ Project Structure

```
notification-svc/
├── src/
│   ├── main/
│   │   ├── java/app/
│   │   │   ├── Application.java              # Main application class
│   │   │   ├── config/                       # Configuration classes
│   │   │   │   └── SmsProviderConfig.java    # SMS provider configuration
│   │   │   ├── Exception/                    # Custom exceptions
│   │   │   │   └── NotifyException.java
│   │   │   ├── model/                        # Entity models
│   │   │   │   ├── Notification.java
│   │   │   │   ├── NotificationPreference.java
│   │   │   │   ├── NotificationStatus.java
│   │   │   │   └── NotificationType.java
│   │   │   ├── repository/                   # JPA repositories
│   │   │   │   ├── NotificationRepository.java
│   │   │   │   └── NotificationPreferenceRepository.java
│   │   │   ├── service/                      # Business logic
│   │   │   │   ├── NotificationService.java
│   │   │   │   ├── NotificationPreferenceService.java
│   │   │   │   ├── GreenApiSmsService.java
│   │   │   │   └── SmsProvider.java
│   │   │   └── web/                           # REST controllers
│   │   │       ├── NotificationController.java
│   │   │       ├── PreferenceController.java
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── dto/                       # Data Transfer Objects
│   │   │       │   ├── NotificationRequest.java
│   │   │       │   ├── NotificationResponse.java
│   │   │       │   ├── PreferenceRequest.java
│   │   │       │   └── PreferenceResponse.java
│   │   │       └── mapper/                    # DTO mappers
│   │   │           └── DtoMapper.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/                                  # Test files
│       ├── java/app/
│       │   ├── ApplicationTests.java
│       │   ├── service/
│       │   └── web/
│       └── resources/
│           └── application-test.properties
├── pom.xml                                    # Maven configuration
├── mvnw                                       # Maven wrapper (Unix)
├── mvnw.cmd                                   # Maven wrapper (Windows)
└── README.md                                  # This file
```

---

## 🔄 Integration with Main Application

### How It Works

1. **Main App Registration:**
   - User registers in SmartExpense (port 9090)
   - Main app calls `POST /api/v1/preferences` to create notification preference
   - Preference stored in notification service database

2. **Notification Sending:**
   - Main app detects event (subscription expiring, monthly report ready)
   - Main app calls `POST /api/v1/notifications` with notification details
   - Notification service:
     - Checks user preference (enabled/disabled, type, contact info)
     - Sends email or SMS based on preference
     - Stores notification in history
     - Returns status

3. **Preference Management:**
   - User updates notification settings in main app
   - Main app calls `POST /api/v1/preferences` to update
   - Notification service updates preference record

### Feign Client Configuration

Main app uses Feign Client to communicate:

```java
@FeignClient(name = "notification-svc", url = "localhost:9091/api/v1")
public interface NotificationClient {
    @PostMapping("/preferences")
    ResponseEntity<Void> upsertPreference(@RequestBody UpsertPreferenceRequest request);
    
    @GetMapping("/preferences")
    ResponseEntity<PreferenceResponse> getPreferences(@RequestParam("userId") UUID userId);
    
    @PostMapping("/notifications")
    ResponseEntity<Void> sendNotification(@RequestBody NotificationRequest request);
    
    @GetMapping("/notifications")
    ResponseEntity<List<NotificationsResponse>> getNotifications(@RequestParam("userId") UUID userId);
}
```

---

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=NotificationServiceTest
```

### Test Database
Uses H2 in-memory database for testing (configured in `src/test/resources/application-test.properties`).

### Test Coverage
- Unit tests for services
- Integration tests for controllers
- API endpoint tests
- SMS provider tests

---

## 🐛 Troubleshooting

### Common Issues

**1. Service Won't Start - Port Already in Use**
- Change port in `application.properties`: `server.port=9092`
- Or stop the process using port 9091

**2. Database Connection Error**
- Ensure MySQL server is running
- Verify database credentials
- Check if database exists or allow auto-creation

**3. Email Not Sending**
- Verify SMTP credentials are correct
- For Gmail, use App Password (not regular password)
- Check firewall/network settings
- Review application logs for detailed error messages

**4. SMS Not Sending**
- Verify GreenAPI credentials (instance ID, API token)
- Check GreenAPI account status
- Ensure phone number format is correct (international format)
- Review GreenAPI dashboard for delivery status

**5. Main App Cannot Connect**
- Verify notification service is running on port 9091
- Check if service started successfully (check logs)
- Verify Feign client URL in main app matches service URL
- Check network/firewall settings

**6. Preference Creation Fails**
- Ensure database is accessible
- Check if user ID is valid UUID format
- Review application logs for detailed errors

---

## 📊 Notification Types

### EMAIL Notifications
- **Format:** Email address (e.g., `user@example.com`)
- **Features:**
  - Text emails
  - HTML emails
  - PDF attachments (Base64 encoded)
  - Subject and body support

### SMS Notifications
- **Format:** Phone number (e.g., `+359123456789`)
- **Features:**
  - WhatsApp delivery (via GreenAPI)
  - SMS fallback
  - International number support
  - Delivery status tracking

### Automatic Type Detection
The service automatically detects notification type based on contact info:
- Email format → `EMAIL`
- Phone number format → `SMS`
- Default → `EMAIL`

---

## 🔒 Security Considerations

- **Database Security:** Use strong passwords, limit database access
- **SMTP Security:** Use TLS/SSL for email (configured via `starttls.enable`)
- **API Security:** Consider adding authentication/authorization for production
- **Sensitive Data:** Store credentials securely (use environment variables or secrets management)

### Production Recommendations

1. **Add Authentication:**
   - Implement API key or JWT authentication
   - Restrict access to known services only

2. **Use Environment Variables:**
   - Don't hardcode credentials in `application.properties`
   - Use Spring profiles for different environments

3. **Enable HTTPS:**
   - Configure SSL/TLS certificates
   - Use secure connections for all communications

4. **Database Security:**
   - Use connection pooling
   - Implement database user with minimal privileges
   - Enable SSL for database connections

---

## 📝 Logging

The service uses SLF4J for logging:

- **INFO:** Service startup, notification sending, preference updates
- **ERROR:** Failed notifications, connection errors, exceptions
- **DEBUG:** Detailed request/response logging (if enabled)

**Log Levels:**
- Default: INFO
- Hibernate: ERROR (to reduce noise)
- Can be configured in `application.properties`

---

## 🎯 Future Enhancements

- Push notifications (Firebase, APNS)
- Notification templates
- Notification scheduling
- Multi-language support
- Notification analytics
- Webhook support
- Rate limiting
- Retry mechanism with exponential backoff
- Notification queuing (RabbitMQ, Kafka)

---

## 📄 License

This project is licensed under the MIT License.

---

## 👥 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

---

## 📞 Support

For issues, questions, or contributions related to this microservice, please open an issue on the repository.

For issues related to the main SmartExpense application, see the [main application README](../ExpenseTracker/README.md).

---

<div align="center">

**Part of the SmartExpense Ecosystem**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)

**Built with ❤️ using Spring Boot**

</div>

