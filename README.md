# 🔑 TOTP Generator
Time-based One-Time Password generator with real-time updates.

<img width="1899" height="900" alt="image" src="https://github.com/user-attachments/assets/37c3e1ef-b219-42a5-8d55-d8d77570eb46" />

## ✨ Features

- 🔐 Secure TOTP generation (RFC 6238 compliant)
- ⚡ Real-time updates via Server-Sent Events
- 🎯 Customizable digits (4-10) and time periods (15-300s)
- 📱 Responsive web interface
- 📖 Interactive API documentation

## 🚀 Quick Start

### Local Development
```bash
git clone https://github.com/ita07/TOTPGenerator.git
cd TOTPGenerator
mvn clean package
java -jar target/TOTPGenerator-1.0-SNAPSHOT.jar
```

Access at: http://localhost:8080

### 🐳 Docker
```bash
docker build -t totp-generator .
docker run -p 8080:8080 totp-generator
```

## 🔗 API Endpoints

- `GET /totp-data` - Generate TOTP code with timing info
- `GET /totp-stream` - Real-time TOTP updates (SSE)
- `GET /swagger-ui.html` - API documentation

## 🛠️ Technology Stack

- **Backend**: Java 21, Spring Boot 3
- **Frontend**: HTML5/CSS3/JavaScript
- **Security**: Input validation, CORS protection

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.
