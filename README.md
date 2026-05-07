# Voice Driven Prompt Optimization Engine - Backend

A deterministic prompt optimization system that converts raw voice or unstructured input into minimum viable prompts (MVPs) with token efficiency and multilingual support.

## Overview

The backend processes voice input through a sophisticated pipeline:

1. **Speech-to-Text Conversion** - Converts voice to text with language detection
2. **Language Normalization** - Handles English, Hindi, and Hinglish inputs
3. **Intent Extraction** - Identifies user intent, task, domain, constraints, output format, and audience
4. **User Confirmation** - Requires user approval before optimization
5. **Prompt Optimization** - Applies CAVEMAN MODE for token reduction (30-50% savings)
6. **Memory Management** - Stores and reuses optimized prompts via intelligent memory allocation

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.2
- **Database:** PostgreSQL
- **Build Tool:** Maven
- **API Documentation:** RESTful endpoints

## Prerequisites

- Java 17 or higher
- PostgreSQL 14+
- Maven 3.8+
- Node.js 18+ (for frontend integration)

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd engine
```

### 2. Configure Database

Create a PostgreSQL database:

```sql
CREATE DATABASE voice_engine;
```

Update `application.yaml` with your database credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/voice_engine
    username: postgres
    password: your_password
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### 3. Run Database Migrations

Migrations are automatically applied via Flyway on startup. The schema includes:

- **conversations** - Session management
- **messages** - Chat history with voice input details
- **memories** - Optimized prompt storage with session filtering
- **memory_edges** - Relationships between memory nodes
- **prompt_logs** - Transformation audit trail
- **decision_logs** - Pipeline step-by-step decisions

### 4. Build the Application

```bash
mvn clean package
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The server will start at `http://localhost:8080`

## API Endpoints

### Voice Input Processing

**POST** `/api/voice/input`

Process voice or text input for intent extraction and transcription.

**Request:**
```bash
curl -X POST http://localhost:8080/api/voice/input \
  -F "audio=@voice.webm" \
  -F "sessionId=0be96da0-9bf0-4383-af57-c52aa2e6db40"
```

Or with text input:
```bash
curl -X POST http://localhost:8080/api/voice/input \
  -F "text=create a marketing plan for a gym app" \
  -F "sessionId=0be96da0-9bf0-4383-af57-c52aa2e6db40"
```

**Response:**
```json
{
  "transcript": "Create a marketing plan for a gym app",
  "language": "en",
  "confidence": 0.92,
  "intentJson": {
    "intent": "create_marketing_plan",
    "task": "Create a marketing plan for a gym app",
    "domain": "marketing",
    "constraints": [],
    "outputFormat": "bullet_list",
    "audience": "business"
  }
}
```

### Prompt Generation

**POST** `/api/prompt/generate`

Generate optimized prompt after user confirms intent.

**Request:**
```bash
curl -X POST http://localhost:8080/api/prompt/generate \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: 0be96da0-9bf0-4383-af57-c52aa2e6db40" \
  -d '{
    "intent": "create_marketing_plan",
    "task": "Create a marketing plan for a gym app",
    "domain": "marketing",
    "constraints": ["under 100 words"],
    "output_format": "bullet_list",
    "audience": "business"
  }'
```

**Response:**
```json
{
  "optimizedPrompt": "Create a 3-step marketing plan for a gym app. Format: bullet points. Constraint: under 100 words.",
  "tokenInput": 45,
  "tokenOutput": 22,
  "reductionPct": 51.1
}
```

### Chat History

**GET** `/api/chat/history?sessionId={sessionId}`

Retrieve chat history for a specific session.

**Request:**
```bash
curl http://localhost:8080/api/chat/history?sessionId=0be96da0-9bf0-4383-af57-c52aa2e6db40
```

**Response:**
```json
[
  {
    "messageId": "uuid",
    "rawText": "मुझे एक जिम के लिए मार्केटिंग आइडिया बताओ",
    "transcript": "Tell me a marketing idea for a gym",
    "language": "hi",
    "confidence": 0.88,
    "domain": "marketing",
    "optimizedPrompt": "Marketing ideas for a fitness app.",
    "tokenInput": 15,
    "tokenOutput": 8,
    "reductionPct": 46.7,
    "createdAt": "2026-05-07T10:30:45"
  }
]
```

### Memory Cards

**GET** `/api/memory/cards?sessionId={sessionId}`

Get memory cards for current session (filtered by sessionId).

**Request:**
```bash
curl http://localhost:8080/api/memory/cards?sessionId=0be96da0-9bf0-4383-af57-c52aa2e6db40
```

**Response:**
```json
[
  {
    "id": "memory-uuid",
    "domain": "marketing",
    "task": "Marketing plan for gym app",
    "optimizedPrompt": "Create a 3-step marketing plan for a gym app.",
    "useCount": 3,
    "updatedAt": "2026-05-07T12:15:30"
  }
]
```

### Memory Graph

**GET** `/api/memory/graph`

Get knowledge graph visualization data.

**Request:**
```bash
curl http://localhost:8080/api/memory/graph
```

**Response:**
```json
{
  "nodes": [
    {"id": "node1", "label": "Marketing Plan", "domain": "marketing", "useCount": 3},
    {"id": "node2", "label": "Content Strategy", "domain": "marketing", "useCount": 1}
  ],
  "edges": [
    {"from": "node1", "to": "node2", "type": "REFINES", "score": 0.85}
  ]
}
```

### Decision Logs

**GET** `/api/logs?messageId={messageId}`

Get detailed pipeline decision logs for a message.

**Request:**
```bash
curl http://localhost:8080/api/logs?messageId=message-uuid
```

**Response:**
```json
[
  {
    "messageId": "message-uuid",
    "stepName": "STT",
    "decision": "COMPLETED",
    "detail": "Speech-to-text conversion complete",
    "createdAt": "2026-05-07T10:30:45"
  },
  {
    "messageId": "message-uuid",
    "stepName": "LANGUAGE_DETECTION",
    "decision": "COMPLETED",
    "detail": "Detected: Hindi | Normalized to: English",
    "createdAt": "2026-05-07T10:30:46"
  },
  {
    "messageId": "message-uuid",
    "stepName": "PROMPT_OPTIMIZATION",
    "decision": "COMPLETED",
    "detail": "Applied CAVEMAN MODE - token reduction optimized",
    "createdAt": "2026-05-07T10:30:47"
  }
]
```

## Database Schema

### conversations
```sql
CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### messages
```sql
CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID REFERENCES conversations(id),
    raw_text TEXT,
    transcript TEXT,
    confidence FLOAT,
    language VARCHAR(10),
    optimized_prompt TEXT,
    token_input INT,
    token_output INT,
    reduction_pct FLOAT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### memories
```sql
CREATE TABLE memories (
    id UUID PRIMARY KEY,
    session_id VARCHAR(100),
    domain VARCHAR(50),
    task TEXT,
    optimized_prompt TEXT,
    use_count INT DEFAULT 1,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_memories_session_id ON memories(session_id);
```

### memory_edges
```sql
CREATE TABLE memory_edges (
    id UUID PRIMARY KEY,
    from_id UUID REFERENCES memories(id),
    to_id UUID REFERENCES memories(id),
    edge_type VARCHAR(20),
    score FLOAT
);
```

### decision_logs
```sql
CREATE TABLE decision_logs (
    id UUID PRIMARY KEY,
    message_id UUID REFERENCES messages(id),
    step_name VARCHAR(50),
    decision VARCHAR(20),
    detail TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Pipeline Architecture

```
Input (Voice/Text)
    ↓
[STT Module] → Speech-to-Text + Language Detection
    ↓
[Language Detection] → Detect language (Hindi/Hinglish/English)
    ↓
[Filter] → Remove filler words, clean noise
    ↓
[Normalization] → Translate to English if needed
    ↓
[Intent Extraction] → Extract intent, task, domain, constraints, format, audience
    ↓
[User Confirmation] → Wait for user approval
    ↓
[Prompt Optimization] → Apply CAVEMAN MODE (remove unnecessary tokens)
    ↓
[Validation] → Verify output quality
    ↓
[Memory] → Store in memory with session tracking
    ↓
Output (Optimized Prompt)
```

## System Features

### Multilingual Support
- English detection and processing
- Hindi language handling
- Hinglish (mixed language) support
- Automatic normalization to English

### Intent Extraction
- Structured intent identification
- Task extraction from raw input
- Domain classification (marketing, code, writing, data, research, other)
- Constraint identification
- Output format specification (bullet_list, paragraph, table, code, numbered_list, short_answer)
- Audience targeting (general, developer, business, student, expert)

### Memory Management
- Session-scoped memory cards
- Intelligent memory allocation (merge, save_new, save_child, skip)
- Memory similarity scoring
- Knowledge graph with relationship tracking
- Smart memory reuse for similar prompts

### Token Optimization
- 30-50% token reduction (CAVEMAN MODE)
- Removes unnecessary verbose language
- Maintains semantic meaning
- Deterministic output for consistent results

## Running Tests

```bash
mvn test
```

## Logs

Application logs are located at: `logs/application.log`

Check logs for debugging pipeline steps and decision-making.

## Example Usage Flow

### 1. User speaks Hindi request:
```
"मुझे एक जिम के लिए मार्केटिंग आइडिया बताओ"
```

### 2. Backend processes:
- STT converts to text
- Detects Hindi language
- Normalizes to English
- Extracts marketing intent
- Returns to frontend for confirmation

### 3. User confirms
- Frontend sends confirmation with parsed intent
- Backend optimizes prompt
- Stores in memory with session ID

### 4. Output:
```
Optimized Prompt: "Create marketing ideas for a gym app."
Tokens: 15 → 8 (47% reduction)
```

## Troubleshooting

### Database Connection Issues
- Verify PostgreSQL is running
- Check credentials in `application.yaml`
- Ensure database exists

### Migration Failures
- Check Flyway migration files are present
- Verify database permissions
- Clear migration history if needed: `TRUNCATE schema_version;`

### Low Confidence Scores
- Ensure clear audio input
- Check for background noise
- Try speaking more clearly

## Performance Optimization

- Memory queries use session_id index for fast filtering
- Connection pooling configured with HikariCP
- JPA lazy loading for relationships
- Query optimization for memory graph generation

## Contributing

Follow the existing code structure:
- `controller/` - REST endpoints
- `service/` - Business logic
- `model/` - JPA entities
- `repository/` - Data access
- `pipeline/` - Processing pipeline
- `memory/` - Memory management
- `filter/` - Input filtering and validation

## Support

For issues or questions, refer to the main project README or contact the developer
