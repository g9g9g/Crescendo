# Crescendo AI Services

AI 마이크로서비스 모음: PDF to MusicXML 변환 및 실시간 피아노 음 인식

## 📋 목차

- [서비스 개요](#서비스-개요)
- [아키텍처](#아키텍처)
- [로컬 개발 환경 설정](#로컬-개발-환경-설정)
- [Docker 빌드](#docker-빌드)
- [GitLab CI/CD 설정](#gitlab-cicd-설정)
- [Kubernetes 배포](#kubernetes-배포)
- [트러블슈팅](#트러블슈팅)

---

## 🎯 서비스 개요

### 1. PDF Parse Service (Python/Flask)

PDF 악보를 MusicXML로 변환하는 OMR (Optical Music Recognition) 서비스

**기술 스택:**
- Python 3.11
- Flask 3.0.0
- OEMER (OMR 엔진)
- Poppler (PDF 변환)
- OpenCV (이미지 전처리)

**주요 기능:**
- PDF → 이미지 변환
- 광학 악보 인식 (OMR)
- MusicXML 출력
- 3가지 성능 모드 (Fast, Balanced, Accurate)

**엔드포인트:**
- `GET /health` - 헬스 체크
- `GET /performance` - 성능 설정 조회
- `POST /performance/mode/<mode>` - 성능 모드 변경
- `POST /convert` - PDF/이미지 → MusicXML 변환

**포트:** 5002

---

### 2. Piano Recognition Service (C#/gRPC)

실시간 피아노 음 인식을 위한 gRPC 서비스

**기술 스택:**
- C# / .NET 9.0
- gRPC (HTTP/2)
- ONNX Runtime (AI 추론)
- NAudio (오디오 처리)
- MathNet.Numerics (신호 처리)

**주요 기능:**
- 실시간 오디오 스트리밍 인식
- Mel Spectrogram 변환
- ONNX 모델 추론 (88건반 피아노)
- Unary/Streaming RPC 지원

**RPC 메서드:**
- `Predict` (Unary) - 단발 예측
- `StreamRecognition` (Bidirectional Streaming) - 실시간 스트리밍

**포트:** 5001

---

## 🏗️ 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                   Kubernetes Cluster                        │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Ingress (NGINX)                        │    │
│  │         cresd102.duckdns.org (HTTPS)                │    │
│  └───────────┬────────────────────────┬─────────────────┘    │
│              │                        │                      │
│       /ai/pdf                   /ai/piano                   │
│              │                        │                      │
│  ┌───────────▼──────────┐  ┌─────────▼────────────┐         │
│  │  PDF Parse Service   │  │ Piano Recognition    │         │
│  │   (crescendo-pdf)    │  │  (crescendo-piano)   │         │
│  │   Port: 5002         │  │   Port: 5001         │         │
│  │   ClusterIP          │  │   ClusterIP          │         │
│  └──────────────────────┘  └──────────────────────┘         │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ 로컬 개발 환경 설정

### PDF Parse Service

#### 요구사항
- Python 3.11+
- Poppler (PDF 변환 라이브러리)

#### 설치 방법

```bash
# 1. 가상환경 생성
cd ai/pdfparse
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 2. 의존성 설치
pip install -r requirements.txt

# 3. 환경 변수 설정 (선택)
export PORT=5002
export PERFORMANCE_MODE=accurate  # fast, balanced, accurate

# 4. 실행
python app.py
```

**Poppler 설치:**
- **Windows:** [Poppler for Windows](https://github.com/oschwartz10612/poppler-windows/releases) 다운로드
- **Ubuntu/Debian:** `sudo apt-get install poppler-utils`
- **macOS:** `brew install poppler`

#### 테스트

```bash
curl http://localhost:5002/health
```

---

### Piano Recognition Service

#### 요구사항
- .NET 9.0 SDK
- ONNX 모델 파일: `piano_recognition.onnx`

#### 설치 방법

```bash
# 1. .NET SDK 확인
dotnet --version  # 9.0 이상 필요

# 2. 의존성 복원
cd ai/pianorecognition
dotnet restore

# 3. 빌드
dotnet build

# 4. ONNX 모델 파일 준비 (중요!)
# Models/piano_recognition.onnx 파일이 있어야 합니다
# 없는 경우 학습된 모델 파일을 다운로드하거나 학습하세요

# 5. 실행
dotnet run --project pianorecognition/pianorecognition.csproj
```

#### 환경 변수

```bash
export PORT=5001
```

#### 테스트

```bash
curl http://localhost:5001/health
```

---

## 🐳 Docker 빌드

### PDF Parse Service

```bash
cd ai/pdfparse
docker build -t sangheon95/crescendo-pdf:latest .
docker run -p 5002:5002 \
  -e PERFORMANCE_MODE=accurate \
  sangheon95/crescendo-pdf:latest
```

### Piano Recognition Service

⚠️ **중요: ONNX 모델 파일 준비**

빌드 전에 ONNX 모델 파일을 프로젝트에 배치해야 합니다:

```bash
# 1. 모델 파일을 Models/ 디렉토리에 복사
mkdir -p ai/pianorecognition/pianorecognition/Models
# piano_recognition.onnx 파일을 위 디렉토리에 배치

# 2. Docker 빌드
cd ai/pianorecognition
docker build -t sangheon95/crescendo-piano:latest .

# 3. 실행
docker run -p 5001:5001 \
  -e PORT=5001 \
  sangheon95/crescendo-piano:latest
```

---

## 🚀 GitLab CI/CD 설정

### 필수 환경 변수

GitLab 프로젝트 Settings > CI/CD > Variables에 다음 변수를 추가하세요:

| 변수명 | 설명 | 예시 |
|--------|------|------|
| `DOCKER_HUB_USER` | Docker Hub 사용자명 | `sangheon95` |
| `DOCKER_HUB_TOKEN` | Docker Hub Access Token | `dckr_pat_...` |
| `KUBE_CONFIG` | Kubernetes 설정 파일 내용 | `cat ~/.kube/config` |

### Docker Hub Access Token 생성

1. [Docker Hub](https://hub.docker.com/) 로그인
2. Account Settings > Security > New Access Token
3. Token 이름: `gitlab-ci`
4. Access permissions: `Read, Write, Delete`
5. 생성된 토큰 복사 → GitLab CI/CD 변수에 추가

### Kubernetes Config 가져오기

```bash
# 1. Kubernetes 클러스터에 접속할 수 있는 kubeconfig 파일 내용 복사
cat ~/.kube/config

# 2. GitLab CI/CD 변수에 KUBE_CONFIG로 추가
# Type: File or Variable
# Protected: Yes (권장)
# Masked: No (YAML 형식이므로 masking 불가)
```

---

## ☸️ Kubernetes 배포

### 사전 준비

1. **Kubernetes 클러스터** 준비 (버전 1.20+)
2. **NGINX Ingress Controller** 설치
3. **cert-manager** 설치 (HTTPS용)

### 수동 배포

```bash
# 1. Namespace 생성 (없는 경우)
kubectl create namespace apps

# 2. Docker Hub Secret 생성
kubectl create secret docker-registry dockerhub-secret \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=<DOCKER_HUB_USER> \
  --docker-password=<DOCKER_HUB_TOKEN> \
  -n apps

# 3. 배포 적용
kubectl apply -f ai/k8s/pdfparse/
kubectl apply -f ai/k8s/pianorecognition/
kubectl apply -f ai/k8s/ingress.yaml

# 4. 배포 상태 확인
kubectl get pods -n apps
kubectl rollout status deployment/crescendo-pdf -n apps
kubectl rollout status deployment/crescendo-piano -n apps
```

### 서비스 확인

```bash
# Pod 로그 확인
kubectl logs -f deployment/crescendo-pdf -n apps
kubectl logs -f deployment/crescendo-piano -n apps

# 서비스 테스트
curl https://cresd102.duckdns.org/ai/pdf/health
curl https://cresd102.duckdns.org/ai/piano/health
```

---

## 🔧 환경 설정

### PDF Parse 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `PORT` | `5002` | 서비스 포트 |
| `PERFORMANCE_MODE` | `accurate` | 성능 모드 (fast, balanced, accurate) |
| `USE_GPU` | `False` | GPU 가속 사용 여부 |
| `FLASK_ENV` | `production` | Flask 환경 |

**성능 모드 설명:**
- **fast**: DPI 250, 그레이스케일, 빠른 처리 (정확도 낮음)
- **balanced**: DPI 400, 그레이스케일, 균형잡힌 성능 (기본값)
- **accurate**: DPI 600, 컬러, 최고 품질 (느림)

### Piano Recognition 환경 변수

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `PORT` | `5001` | 서비스 포트 |
| `ASPNETCORE_URLS` | `http://+:5001` | Kestrel 바인딩 URL |
| `ASPNETCORE_ENVIRONMENT` | `Production` | ASP.NET Core 환경 |

---

## 📊 리소스 요구사항

### PDF Parse Service

**최소 요구사항:**
- CPU: 250m (0.25 코어)
- 메모리: 512Mi

**권장 사항:**
- CPU: 1000m (1 코어)
- 메모리: 1Gi
- Storage: 2Gi (임시 파일용)

### Piano Recognition Service

**최소 요구사항:**
- CPU: 250m (0.25 코어)
- 메모리: 512Mi

**권장 사항:**
- CPU: 1000m (1 코어)
- 메모리: 1Gi
- Storage: 500Mi (모델 파일용)

---

## 🐛 트러블슈팅

### PDF Parse

#### 문제: "Poppler not found"
```bash
# Docker 이미지 빌드 시 Poppler가 설치되지 않음
# 해결: Dockerfile에 poppler-utils 설치 확인
RUN apt-get update && apt-get install -y poppler-utils
```

#### 문제: "OMR processing failed"
```bash
# OEMER 모델이 다운로드되지 않음
# 해결: 첫 실행 시 모델 자동 다운로드 (인터넷 연결 필요)
# 또는 수동 다운로드: python -c "from oemer.ete import download_models; download_models()"
```

#### 문제: 메모리 부족
```bash
# 높은 DPI 설정으로 인한 메모리 부족
# 해결: PERFORMANCE_MODE를 'fast' 또는 'balanced'로 변경
kubectl set env deployment/crescendo-pdf PERFORMANCE_MODE=balanced -n apps
```

---

### Piano Recognition

#### 문제: "Model file not found"
```bash
# ONNX 모델 파일이 없음
# 해결: piano_recognition.onnx 파일을 Models/ 디렉토리에 배치
# 파일 위치: ai/pianorecognition/pianorecognition/Models/piano_recognition.onnx
```

#### 문제: gRPC 연결 실패
```bash
# HTTP/2가 아닌 HTTP/1.1로 연결 시도
# 해결: gRPC 클라이언트에서 HTTP/2 프로토콜 사용 확인
# 예시 (C#):
var channel = GrpcChannel.ForAddress("http://localhost:5001");
```

#### 문제: Pod 재시작 반복
```bash
# 헬스 체크 실패로 인한 재시작
# 확인: kubectl logs <pod-name> -n apps
# 해결: StartupProbe 타이밍 조정 (현재: 30s + 18*10s = 210s)
```

---

### GitLab CI/CD

#### 문제: Docker login failed
```bash
# Docker Hub 자격증명 오류
# 확인: DOCKER_HUB_USER, DOCKER_HUB_TOKEN 변수 설정 확인
# 해결: Access Token 재생성 및 변수 업데이트
```

#### 문제: kubectl command not found
```bash
# KUBE_CONFIG가 올바르게 설정되지 않음
# 확인: echo "$KUBE_CONFIG" 값 확인
# 해결: kubeconfig 파일 내용을 정확히 복사하여 변수에 추가
```

#### 문제: 빌드는 성공하지만 배포가 트리거되지 않음
```bash
# only.changes 경로가 잘못됨
# 확인: .gitlab-ci.yml의 경로가 실제 디렉토리 구조와 일치하는지 확인
# 해결: ai/pdfparse/**/* 형식으로 경로 수정 완료
```

---

## 📚 추가 리소스

### API 문서

- **PDF Parse API**: `http://localhost:5002/performance` (Swagger 없음, 수동 테스트)
- **Piano Recognition gRPC**: `pianorecognition/protos/piano_recognition.proto` 참조

### 모델 파일

**Piano Recognition ONNX 모델:**
- 파일명: `piano_recognition.onnx`
- 위치: `ai/pianorecognition/pianorecognition/Models/`
- 크기: ~수백 MB (Git에서 제외됨)
- **다운로드 방법:**
  - 학습된 모델이 있는 경우 해당 위치에 배치
  - 없는 경우 모델 학습 후 배치
  - 팀 내부 저장소에서 다운로드

### 관련 문서

- [OEMER Documentation](https://github.com/BreezeWhite/oemer)
- [ONNX Runtime](https://onnxruntime.ai/)
- [gRPC Documentation](https://grpc.io/docs/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)

---

## 🤝 기여

문제 발견 시:
1. GitHub Issues에 버그 리포트 작성
2. 기능 개선 제안은 Discussion에 작성
3. PR 환영 (코드 리뷰 필수)

---

## 📝 라이센스

프로젝트 라이센스에 따름

---

## 📞 문의

- 프로젝트 관리자: Crescendo Team
- GitLab: [프로젝트 링크]
- 이슈 트래커: [GitLab Issues]
