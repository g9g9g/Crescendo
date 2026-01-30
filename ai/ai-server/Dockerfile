# AI Server with OMR (Optical Music Recognition)
# Base image: Python 3.11 slim
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install system dependencies
# - poppler-utils: PDF to image conversion (for OMR)
# - libgl1: OpenCV dependencies
# - libglib2.0-0: OpenCV dependencies
# - wget: Download Audiveris
# - unzip: Extract Audiveris
RUN apt-get update && apt-get install -y \
    poppler-utils \
    libgl1 \
    libglib2.0-0 \
    libsndfile1 \
    ffmpeg \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Install Audiveris 5.7.1 for high-accuracy OMR
# Copy local .deb file and install (ignore desktop menu post-install errors)
COPY Audiveris-5.7.1-ubuntu22.04-x86_64.deb /tmp/
RUN apt-get update \
    && (apt-get install -y /tmp/Audiveris-5.7.1-ubuntu22.04-x86_64.deb || true) \
    && rm -f /tmp/Audiveris-5.7.1-ubuntu22.04-x86_64.deb \
    && rm -rf /var/lib/apt/lists/* \
    && echo "Verifying Audiveris installation..." \
    && if [ -f /opt/audiveris/bin/Audiveris ]; then \
         ln -sf /opt/audiveris/bin/Audiveris /usr/bin/audiveris && echo "Audiveris linked: /opt/audiveris/bin/Audiveris -> /usr/bin/audiveris"; \
         echo "Audiveris installed successfully"; \
       else \
         echo "ERROR: Audiveris binary not found at /opt/audiveris/bin/Audiveris"; \
         exit 1; \
       fi

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Environment variables
ENV PORT=8000 \
    PERFORMANCE_MODE=balanced \
    USE_GPU=False \
    FLASK_ENV=production \
    PYTHONUNBUFFERED=1

# Create necessary directories
RUN mkdir -p uploads outputs

# Expose port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD python -c "import requests; requests.get('http://localhost:8000/api/omr/health', timeout=5)" || exit 1

# Run application
CMD ["python", "wsgi.py"]
