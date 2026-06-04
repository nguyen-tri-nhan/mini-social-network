# Test Plan
## Mini Social Network — Microservices

---

## 1. Strategy Overview

```
                        Pyramid
                       ───────────
                      /  Smoke &  \        ← chạy sau deploy
                     / Regression  \
                    /────────────────\
                   /   Integration    \    ← chạy trên CI (PR)
                  /   (@QuarkusTest)   \
                 /──────────────────────\
                /       Unit Tests       \  ← chạy local + CI
               /    (Service layer only)  \
              ────────────────────────────
```

| Layer | Scope | Tool | Tốc độ | Chạy khi |
|---|---|---|---|---|
| Unit | Service methods (mocked repo) | MockK | < 1s/test | Mọi lúc |
| Integration | Full request → DB | `@QuarkusTest` + Testcontainers | 5–30s/test | PR CI |
| Smoke | Endpoint alive sau deploy | Shell script / RestAssured | < 1 phút | Post-deploy |
| Regression | Full suite trên staging | RestAssured / Postman | 5–15 phút | Nightly / Release |

---

## 2. Unit Tests

### Setup

```kotlin
// build.gradle.kts (mỗi service)
testImplementation("io.mockk:mockk:1.13.10")
testImplementation("io.quarkus:quarkus-junit5")
```

### Pattern — Service với mocked repo

```kotlin
class ArticleServiceTest {

    private val repo = mockk<ArticleRepository>()
    private val counterService = mockk<CounterService>()
    private val objectMapper = ObjectMapper()
    private val service = ArticleService(repo, counterService, objectMapper)

    @Test
    fun `listArticles returns paginated result`() {
        val spec  = RsqlQuerySpec.of("visible = true")
        val sort  = Sort.by("createdAt").descending()
        val items = listOf(mockArticle())

        every { repo.findFiltered(any(), any(), any(), 0, 10) } returns items
        every { repo.countFiltered(any(), any()) }              returns 1
        every { counterService.readLiveCounts(any()) }          returns Pair(-1, -1)

        val result = service.listArticles(ArticleQuery(page = 0, size = 10))

        assertThat(result.total).isEqualTo(1)
        assertThat(result.items).hasSize(1)
    }

    @Test
    fun `delete article by non-owner throws ForbiddenException`() {
        val article = mockArticle(authorId = UUID.randomUUID())
        every { repo.findById(any()) } returns article

        assertThrows<ForbiddenException> {
            service.delete(article.id, requesterId = UUID.randomUUID())
        }
    }
}
```

### Test cases per service

#### auth-service
| # | Test | Expected |
|---|---|---|
| U-01 | signup — valid request | credentials saved, JWT returned |
| U-02 | signup — duplicate username | `ConflictException` |
| U-03 | signup — duplicate email | `ConflictException` |
| U-04 | signin — correct credentials | JWT returned |
| U-05 | signin — wrong password | `UnauthorizedException` |
| U-06 | signin — unknown user | `UnauthorizedException` |

#### post-service
| # | Test | Expected |
|---|---|---|
| U-07 | listArticles — no filter | all visible articles paged |
| U-08 | listArticles — RSQL `authorId==<uuid>` | filtered by author |
| U-09 | listArticles — invalid filter field | `BadRequestException` |
| U-10 | listArticles — invalid sort field | `BadRequestException` |
| U-11 | getById — visible article | dto returned |
| U-12 | getById — hidden article | `NotFoundException` |
| U-13 | delete — own article | `visible = false` |
| U-14 | delete — other's article | `ForbiddenException` |

#### interaction-service
| # | Test | Expected |
|---|---|---|
| U-15 | addComment — valid | comment persisted, event published |
| U-16 | deleteComment — own | `visible = false` |
| U-17 | deleteComment — other's | `ForbiddenException` |
| U-18 | castVote — new vote | vote row created |
| U-19 | castVote — existing vote | vote row updated (upsert) |
| U-20 | castVote — retract (value=0) | vote set to 0 |

#### notification-service
| # | Test | Expected |
|---|---|---|
| U-21 | list — owner sees own notifications | paged result |
| U-22 | markSeen — own notification | `seen = true` |
| U-23 | markSeen — other's notification | `ForbiddenException` |
| U-24 | markAllSeen | all owner notifications set seen |

---

## 3. Integration Tests

### Setup — Testcontainers

```kotlin
// build.gradle.kts
testImplementation("io.quarkus:quarkus-junit5")
testImplementation("io.rest-assured:rest-assured")
testImplementation("org.testcontainers:postgresql:1.19.8")
testImplementation("org.testcontainers:kafka:1.19.8")
```

```kotlin
// src/test/resources/application.properties
%test.quarkus.datasource.jdbc.url=jdbc:postgresql://${postgres.host}:${postgres.port}/social
%test.quarkus.liquibase.migrate-at-start=true
%test.quarkus.redis.hosts=redis://localhost:${redis.port}
%test.kafka.bootstrap.servers=localhost:${kafka.port}
```

```kotlin
@QuarkusTest
@TestProfile(IntegrationTestProfile::class)
class ArticleResourceIT {

    @Test
    fun `POST api articles - creates article and returns 201`() {
        given()
            .header("Authorization", "Bearer ${validJwt()}")
            .contentType(ContentType.JSON)
            .body("""{"description": "Hello world"}""")
        .`when`()
            .post("/api/articles")
        .then()
            .statusCode(201)
            .body("data.description", equalTo("Hello world"))
            .body("data.authorId", notNullValue())
    }

    @Test
    fun `GET api articles - filter by authorId returns only author articles`() {
        val authorId = createUserAndGetId()
        createArticle(authorId)
        createArticle(authorId)
        createArticle(anotherUserId())

        given()
            .header("Authorization", "Bearer ${jwtFor(authorId)}")
            .queryParam("filter", "authorId==$authorId")
        .`when`()
            .get("/api/articles")
        .then()
            .statusCode(200)
            .body("data.total", equalTo(2))
    }
}
```

### Test cases per service

#### auth-service
| # | Test | Expected |
|---|---|---|
| I-01 | `POST /api/auth/signup` valid body | 201, `data.accessToken` present |
| I-02 | `POST /api/auth/signup` duplicate username | 409, `error.errorCode = "01-0002"` |
| I-03 | `POST /api/auth/signup` invalid email | 400 |
| I-04 | `POST /api/auth/signin` correct creds | 200, JWT |
| I-05 | `POST /api/auth/signin` wrong password | 401 |

#### post-service
| # | Test | Expected |
|---|---|---|
| I-06 | `GET /api/articles` no params | 200, `data.items` array |
| I-07 | `GET /api/articles?filter=authorId==<uuid>` | 200, filtered |
| I-08 | `GET /api/articles?sort=voteCount,desc` | 200, sorted |
| I-09 | `GET /api/articles?filter=invalid_field==x` | 400 |
| I-10 | `POST /api/articles` valid | 201, article in DB |
| I-11 | `POST /api/articles` no body fields | 400 |
| I-12 | `DELETE /api/articles/{id}` owner | 204, `visible=false` in DB |
| I-13 | `DELETE /api/articles/{id}` non-owner | 403 |
| I-14 | `GET /api/articles/{id}` deleted article | 404 |

#### interaction-service
| # | Test | Expected |
|---|---|---|
| I-15 | `POST /api/comments` valid | 201, comment in DB |
| I-16 | `GET /api/comments?targetId=&targetType=ARTICLE` | 200, paged |
| I-17 | `DELETE /api/comments/{id}` owner | 204 |
| I-18 | `DELETE /api/comments/{id}` non-owner | 403 |
| I-19 | `POST /api/votes` upvote article | 200, vote recorded |
| I-20 | `POST /api/votes` same target twice | 200, value updated |
| I-21 | `POST /api/votes` vote on comment | 200, `targetType=COMMENT` |

#### notification-service
| # | Test | Expected |
|---|---|---|
| I-22 | `GET /api/notifications` | 200, own notifications |
| I-23 | `GET /api/notifications/unread-count` | 200, `data.count` number |
| I-24 | `PATCH /api/notifications/{id}/seen` | 204, `seen=true` in DB |
| I-25 | `PATCH /api/notifications/seen-all` | 204, all `seen=true` |

#### Kafka event flow (cross-service)
| # | Test | Expected |
|---|---|---|
| I-26 | `POST /api/comments` → notification-service consumer | notification row created in DB |
| I-27 | `POST /api/votes` → post-service counter consumer | `article.vote_count` incremented |
| I-28 | `POST /api/articles` → notification-service | `article.created` event published |

---

## 4. Smoke Tests

Chạy ngay sau khi deploy để xác nhận service khởi động thành công. Mỗi test < 5 giây.

### Script

```bash
#!/bin/bash
# scripts/smoke-test.sh
BASE_URL=${API_URL:-http://localhost}

check() {
    local name=$1; local expected=$2; local actual=$3
    if [ "$actual" = "$expected" ]; then
        echo "✅ $name"
    else
        echo "❌ $name — expected $expected, got $actual"
        exit 1
    fi
}

# Health checks (Quarkus built-in)
check "auth health"         200 $(curl -so /dev/null -w "%{http_code}" $BASE_URL:8081/q/health)
check "user health"         200 $(curl -so /dev/null -w "%{http_code}" $BASE_URL:8082/q/health)
check "post health"         200 $(curl -so /dev/null -w "%{http_code}" $BASE_URL:8083/q/health)
check "interaction health"  200 $(curl -so /dev/null -w "%{http_code}" $BASE_URL:8084/q/health)
check "notification health" 200 $(curl -so /dev/null -w "%{http_code}" $BASE_URL:8085/q/health)

# API Gateway reachability
check "gateway auth route"   200 $(curl -so /dev/null -w "%{http_code}" -X POST $BASE_URL/api/auth/signin -H "Content-Type: application/json" -d '{"identifier":"x","password":"x"}')
check "gateway post route"   401 $(curl -so /dev/null -w "%{http_code}" $BASE_URL/api/articles)

echo "Smoke tests passed ✅"
```

### Checklist

| # | Check | Tool |
|---|---|---|
| S-01 | `/q/health` của mọi service → `UP` | curl |
| S-02 | `/q/health/live` → 200 (liveness) | curl |
| S-03 | `/q/health/ready` → 200 (readiness) | curl |
| S-04 | `POST /api/auth/signin` qua gateway → 200 hoặc 401 (không phải 502/503) | curl |
| S-05 | `GET /api/articles` không có JWT → 401 (không phải 502/503) | curl |
| S-06 | DB connection (Liquibase chạy không lỗi) | Quarkus startup log |
| S-07 | Kafka connection (consumer/producer connected) | Quarkus startup log |
| S-08 | Redis connection | Quarkus startup log |

---

## 5. Regression Tests

Chạy toàn bộ integration test suite trên môi trường staging sau mỗi merge vào main hoặc trước release.

### CI Pipeline

```yaml
# .github/workflows/regression.yml
name: Regression

on:
  push:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'   # Nightly 2am

jobs:
  regression:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }

      - name: Start infra
        run: docker compose -f infra/docker-compose.dev.yml up -d --wait

      - name: Run all service tests
        run: |
          cd services
          gradle :auth-service:test \
                 :user-api:test \
                 :post-api:test \
                 :interaction-service:test \
                 :notification-api:test \
                 --continue

      - name: Publish test results
        uses: dorny/test-reporter@v1
        if: always()
        with:
          name: Test Results
          path: '**/build/test-results/**/*.xml'
          reporter: java-junit
```

### Regression checklist

| # | Area | Scenario |
|---|---|---|
| R-01 | Auth flow | signup → signin → call protected endpoint |
| R-02 | Post flow | create article → list → filter → delete |
| R-03 | Comment flow | add comment → list → delete |
| R-04 | Vote flow | upvote → check count → retract → re-vote |
| R-05 | Notification flow | comment → verify notification created |
| R-06 | RSQL | filter + sort combinations trên `/api/articles` |
| R-07 | Permissions | cross-user delete → 403 |
| R-08 | Pagination | page/size boundary, hasNext flag |
| R-09 | Validation | thiếu required field → 400 + errorCode |
| R-10 | Counter flush | vote → wait 30s → article.vote_count updated |

---

## 6. Tools Summary

| Tool | Version | Dùng cho |
|---|---|---|
| JUnit 5 | 5.x (via Quarkus) | Test runner |
| MockK | 1.13.x | Unit test mocks (Kotlin-friendly) |
| RestAssured | 5.x (via Quarkus) | HTTP integration tests |
| Testcontainers | 1.19.x | Real PostgreSQL / Kafka / Redis trong test |
| `@QuarkusTest` | Quarkus 3.x | Quarkus context trong integration test |
| GitHub Actions | — | CI runner |
| dorny/test-reporter | — | JUnit XML → PR comment |
