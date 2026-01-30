# Crescendo

> AI 기반 악보 자동 넘김 및 음악 추천 플랫폼

[![GitLab](https://img.shields.io/badge/GitLab-FC6D26?style=flat&logo=gitlab&logoColor=white)](https://gitlab.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Flask](https://img.shields.io/badge/Flask-3.0.0-black?style=flat&logo=flask)](https://flask.palletsprojects.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=flat&logo=kubernetes&logoColor=white)](https://kubernetes.io/)
![Android Jetpack Compose](https://img.shields.io/badge/Android%20Jetpack%20Compose-2024.09.00-green?logo=android&logoColor=white
)

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [프로젝트 구조](#프로젝트-구조)
- [설치 및 실행](#설치-및-실행)
- [API 문서](#api-문서)
- [배포](#배포)
- [팀원](#팀원)

## 프로젝트 소개

**Crescendo**는 악보 연주자들을 위한 스마트 악보 관리 및 자동 넘김 애플리케이션입니다.

### 프로젝트 정보

| 항목 | 내용 |
| --- | --- |
| 팀명 | 브레멘음악대 |
| 서비스명 | 크레센도(Cresecndo) |
| 개발 기간 | 2025.10.10 ~ 2025.11.20 (7주) |
| 개발 인원 | 6명 / 백엔드(2), 모바일(2), AI(1) ,인프라(1) |

### 주요 특징

- **AI 기반 OMR(Optical Music Recognition)**: PDF 또는 이미지 형태의 악보를 MusicXML로 자동 변환
- **자동 악보 스크롤**: AI가 연주를 들으면서 악보를 다음으로 넘길 때가 되면 스크롤
- **음악 임베딩 & 추천**: MERT 모델을 활용한 개인화된 악보 추천
- **난이도 평가**: AI 기반 악보 난이도 자동 분석
- **연주 평가** : AI 및 알고리즘 기반 사용자 연주 평가(WAV -> MIDI)
- **연주 기록 관리**: 연주 기록 저장 및 랭킹 시스템
- **실시간 알림**: Firebase Cloud Messaging을 통한 푸시 알림
- **악보 추천**: CBF를 활용한 사용자 맞춤형 악보 추천
- **악보 검색**: 엘라스틱 서치를 도입하여 다양한 키워드로 검색 가능
- **악보 편곡**: 규칙 기반으로 악보를 해당 장르로 편곡
- **랭킹**: 유저들의 평가 기록을 바탕으로 하여 실력에 게이미피케이션 도입
- **연습 보조**: 메트로놈, BPM 조절 등 악기 연습에 필요한 기능 탑재

## 주요 기능

### 1. 악보 관리
- PDF/이미지 업로드 및 자동 악보 변환
- MusicXML 형식 지원
<img src="/uploads/cf84510839c4d42a47967e2cea4ff8e9/악보업로드_디바이스_.png" alt="악보업로드_디바이스_" width="300">

<br>

- 악보 검색 (Elasticsearch 기반 전문 검색)
<img src="/uploads/881a3066f43c241631b074b62120be7e/스토어악보검색_디바이스_.png" alt="스토어악보검색_디바이스_" width="300">

<br>

- 개인 악보 라이브러리 관리
<img src="/uploads/b29564973dc5be10c8df970fe6791804/라이브러리_디바이스_.png" alt="라이브러리_디바이스_" width="300">


### 2. 자동 악보 스크롤
- CNN 기반 실시간 다중 음 인식
- MusicXML에서 얻은 음표와 AI가 인식한 음이 같다면 커서를 다음으로 이동
![악보음인식](/uploads/67c77bf69ceb2ac8571f5c9db3d7c99b/악보음인식.gif)

### 3. AI 기반 서비스
- **OMR**: 고정밀 악보 인식 (Audiveris 5.7.1)
- **음악 임베딩**: 악보의 음악적 특징 벡터화
- **난이도 평가**: 악보의 연주 난이도 자동 측정
- **추천 시스템**: 사용자 취향 기반 악보 추천

### 4. 음악 연습
- 메트로놈
- 반복 재생
- 박자 늦추기
- 피아노의 경우, 왼손 오른손 따로 듣기
![연습모드_디바이스_](/uploads/73a70cb12fdb2d5f568c96a318423ec3/연습모드_디바이스_.png)

### 5. 소셜 기능
- Google OAuth2 로그인
- 연주 랭킹 시스템

## 기술 스택

### Backend

| Category | Technology |
|----------|-----------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.7 |
| **ORM** | Spring Data JPA |
| **Security** | Spring Security + JWT |
| **Database** | PostgreSQL 15.14 |
| **Cache** | Redis 7.4.7 |
| **Search** | Elasticsearch 8.15.0 |
| **Storage** | AWS S3 |
| **Auth** | Google OAuth2, Firebase Admin SDK |
| **Documentation** | SpringDoc OpenAPI 3 (Swagger) |
| **Build Tool** | Gradle 8.10 |

### AI Server

| Category | Technology |
|----------|-----------|
| **Language** | Python 3.11 |
| **Framework** | Flask 3.0.0 |
| **OMR Engine** | Audiveris 5.7.1, OEMER 0.1.8 |
| **ML Framework** | PyTorch 2.9.0 |
| **Music AI** | MERT (Transformer 기반 음악 임베딩) |
| **Music Library** | music21 9.9.1, librosa 0.11.0 |
| **Image Processing** | OpenCV 4.12.0, pdf2image 1.17.0 |
| **Cloud** | AWS S3 (boto3) |

### Frontend

| Category | Technology |
|----------|-----------|
| **Mobile** | Android (Kotlin) |
| **Target SDK** | Android API 36 |
| **Build** | Gradle 8.13, AGP 8.12.0 |
| **Music Rendering** | OpenSheetMusicDisplay (OSMD) |

### Infrastructure

| Category | Technology |
|----------|-----------|
| **Containerization** | Docker |
| **Orchestration** | Kubernetes |
| **Ingress** | NGINX Ingress Controller |
| **TLS** | Let's Encrypt + cert-manager |
| **CI/CD** | GitLab CI/CD |
| **Registry** | Docker Hub |

## 시스템 아키텍처

```
┌─────────────┐
│   Client    │
│  (Android)  │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│     NGINX Ingress Controller        │
│  (cresd102.duckdns.org - HTTPS)    │
└─────────┬───────────────────────────┘
          │
    ┌─────┴─────┐
    │           │
    ▼           ▼
┌────────┐  ┌────────────┐
│Backend │  │ AI Server  │
│(Spring)│  │  (Flask)   │
│ :8080  │  │   :8000    │
└───┬────┘  └─────┬──────┘
    │             │
    ├─────────┬───┴──────┬──────────┐
    │         │          │          │
    ▼         ▼          ▼          ▼
┌──────┐ ┌──────┐  ┌────────┐  ┌──────┐
│ PG   │ │Redis │  │Elastic │  │AWS S3│
│15.14 │ │7.4.7 │  │ 8.15.0 │  │      │
└──────┘ └──────┘  └────────┘  └──────┘
```

### 데이터 플로우

1. **악보 업로드**
   - 사용자 → Backend → AI Server (OMR) → S3 저장 → DB 메타데이터

2. **음악 추천**
   - Backend → AI Server (임베딩 생성) → Elasticsearch (벡터 검색) → 추천 결과

3. **연주 기록**
   - Android → Backend → PostgreSQL → Elasticsearch (검색 인덱싱)

## 프로젝트 구조

```
S13P31D102/
├── backend/                     # Spring Boot 백엔드
│   ├── src/main/java/com/d102/crescendo/
│   │   ├── domain/
│   │   │   ├── ai/             # AI 서비스 연동
│   │   │   ├── auth/           # 인증/인가
│   │   │   ├── fcm/            # Firebase Cloud Messaging
│   │   │   ├── performance/    # 연주 기록
│   │   │   ├── rank/           # 랭킹
│   │   │   ├── recommendation/ # 추천
│   │   │   ├── sheet/          # 악보 관리
│   │   │   └── user/           # 사용자
│   │   └── resources/
│   │       └── application.yaml
│   ├── build.gradle
│   └── Dockerfile
│
├── ai/                          # AI 마이크로서비스
│   ├── ai-server/
│   │   ├── app/
│   │   │   ├── containers/     # DI 컨테이너
│   │   │   └── routes/         # Flask 라우트
│   │   ├── domain/
│   │   │   ├── pipeline/       # 데이터 처리 파이프라인
│   │   │   └── services/       # AI 서비스
│   │   ├── infra/
│   │   │   └── adapters/       # S3 등 외부 서비스
│   │   ├── requirements.txt
│   │   └── Dockerfile
│   └── k8s/                     # K8s 배포 설정
│
├── android/                     # Android 앱
│   ├── app/
│   └── osmd-kotlin/            # 악보 렌더링 라이브러리
│
├── k8s/                         # Kubernetes 설정
│   ├── backend/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   └── ingress.yaml
│
├── exec/                        # 배포 문서
│   ├── 포팅_메뉴얼.md
│   └── crescendo_db_*.sql
│
└── ai.gitlab-ci.yml            # CI/CD 파이프라인
```

## 설치 및 실행

### 사전 요구사항

- **JDK 21**
- **Gradle 8.10+**
- **Python 3.11**
- **PostgreSQL 15.14**
- **Redis 7.4.7**
- **Docker & Docker Compose** (선택사항)
- **Kubernetes 1.20+** (프로덕션 배포용)


## 팀원

**SSAFY 13기 구미 캠퍼스 D102팀**

- **팀장**: 김동찬 - Android 개발 및 프로젝트 총괄
- **팀원**: 최윤수 - Android 개발 및 UI/UX 디자인
- **팀원**: 박혜은 - Backend 개발
- **팀원**: 홍은솔 - Backend 개발
- **팀원**: 이상헌 - Infra & DevOps 및 AI 기능 개발
- **팀원**: 정보균 - AI 기능 개발

## 라이선스

이 프로젝트는 SSAFY 자율 프로젝트로 개발되었습니다.

## 참고 문서

- [포팅 매뉴얼](exec/포팅_메뉴얼.md)
- [API 명세서](http://cresd102.duckdns.org/swagger-ui.html)
- [Kubernetes 설정](k8s/)
- [AI Server 문서](ai/README.md)

---

**Developed by SSAFY 13th D102 Team**
