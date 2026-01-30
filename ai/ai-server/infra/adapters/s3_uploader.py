# infra/adapters/s3_uploader.py
"""
S3 파일 업로더
"""

import os
import logging
import mimetypes
from typing import Optional
from datetime import datetime
import boto3
from botocore.exceptions import ClientError, NoCredentialsError

logger = logging.getLogger(__name__)


class S3Uploader:
    """
    S3에 파일 업로드
    """

    def __init__(
        self,
        bucket_name: Optional[str] = None,
        region: str = 'ap-northeast-2',
        aws_access_key_id: Optional[str] = None,
        aws_secret_access_key: Optional[str] = None
    ):
        """
        Args:
            bucket_name: S3 버킷 이름 (환경변수 S3_BUCKET_NAME 또는 직접 지정)
            region: AWS 리전
            aws_access_key_id: AWS Access Key (환경변수 AWS_ACCESS_KEY_ID 또는 직접 지정)
            aws_secret_access_key: AWS Secret Key (환경변수 AWS_SECRET_ACCESS_KEY 또는 직접 지정)
        """
        # 환경변수에서 읽기
        self.bucket_name = bucket_name or os.getenv('S3_BUCKET_NAME')
        self.region = region or os.getenv('AWS_REGION', 'ap-northeast-2')

        # boto3 클라이언트 생성
        try:
            # 환경변수나 IAM Role 사용
            if aws_access_key_id and aws_secret_access_key:
                self.s3_client = boto3.client(
                    's3',
                    region_name=self.region,
                    aws_access_key_id=aws_access_key_id,
                    aws_secret_access_key=aws_secret_access_key
                )
            else:
                # 환경변수 또는 IAM Role 사용
                self.s3_client = boto3.client('s3', region_name=self.region)

            logger.info(f"S3Uploader initialized (bucket: {self.bucket_name}, region: {self.region})")

        except NoCredentialsError:
            logger.error("AWS credentials not found")
            self.s3_client = None
        except Exception as e:
            logger.error(f"Failed to initialize S3 client: {e}")
            self.s3_client = None

    def upload_file(
        self,
        file_path: str,
        s3_key: Optional[str] = None,
        content_type: Optional[str] = None,
        public_read: bool = False,
        use_presigned_url: bool = False,
        presigned_expiration: int = 3600
    ) -> Optional[str]:
        """
        파일을 S3에 업로드

        Args:
            file_path: 업로드할 로컬 파일 경로
            s3_key: S3 객체 키 (경로), 없으면 타임스탬프 기반 자동 생성
            content_type: MIME type, 없으면 자동 감지
            public_read: 공개 읽기 권한 부여 여부 (deprecated, 사용 안 함)
            use_presigned_url: Presigned URL 생성 여부 (권장)
            presigned_expiration: Presigned URL 만료 시간 (초, 기본 1시간)

        Returns:
            업로드된 파일의 S3 URL 또는 Presigned URL, 실패 시 None
        """
        if not self.s3_client:
            logger.error("S3 client not initialized")
            return None

        if not self.bucket_name:
            logger.error("S3 bucket name not configured")
            return None

        if not os.path.exists(file_path):
            logger.error(f"File not found: {file_path}")
            return None

        try:
            # S3 키 생성
            if not s3_key:
                timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
                filename = os.path.basename(file_path)
                s3_key = f"musicxml/converted/{timestamp}_{filename}"

            # Content-Type 자동 감지
            if not content_type:
                content_type, _ = mimetypes.guess_type(file_path)
                if not content_type:
                    # MusicXML 기본값
                    if file_path.endswith('.musicxml') or file_path.endswith('.xml'):
                        content_type = 'application/vnd.recordare.musicxml+xml'
                    else:
                        content_type = 'application/octet-stream'

            # 업로드 설정
            extra_args = {
                'ContentType': content_type
            }

            # ACL 대신 버킷 정책 사용 (modern S3)
            # public_read는 버킷 정책에서 설정해야 함
            # if public_read:
            #     extra_args['ACL'] = 'public-read'

            # 파일 크기 확인
            file_size = os.path.getsize(file_path)
            logger.info(f"Uploading to S3: {s3_key} ({file_size:,} bytes)")

            # 업로드
            self.s3_client.upload_file(
                file_path,
                self.bucket_name,
                s3_key,
                ExtraArgs=extra_args
            )

            # URL 생성
            if use_presigned_url:
                # Presigned URL 생성 (임시 접근 URL)
                s3_url = self.s3_client.generate_presigned_url(
                    'get_object',
                    Params={
                        'Bucket': self.bucket_name,
                        'Key': s3_key
                    },
                    ExpiresIn=presigned_expiration
                )
                logger.info(f"Upload successful with presigned URL (expires in {presigned_expiration}s)")
            else:
                # 일반 S3 URL (버킷이 public이어야 접근 가능)
                s3_url = f"https://{self.bucket_name}.s3.{self.region}.amazonaws.com/{s3_key}"
                logger.info(f"Upload successful: {s3_url}")

            return s3_url

        except ClientError as e:
            logger.error(f"S3 upload failed: {e}")
            return None
        except Exception as e:
            logger.error(f"Unexpected error during upload: {e}")
            return None

    def upload_musicxml(
        self,
        file_path: str,
        s3_key: Optional[str] = None,
        public_read: bool = False,
        use_presigned_url: bool = True,
        presigned_expiration: int = 86400
    ) -> Optional[str]:
        """
        MusicXML 파일을 S3에 업로드

        Args:
            file_path: 업로드할 MusicXML 파일 경로
            s3_key: S3 객체 키, 없으면 자동 생성
            public_read: 공개 읽기 권한 (deprecated, 사용 안 함)
            use_presigned_url: Presigned URL 생성 여부 (기본 True, 권장)
            presigned_expiration: Presigned URL 만료 시간 (초, 기본 24시간)

        Returns:
            업로드된 파일의 S3 URL 또는 Presigned URL, 실패 시 None
        """
        return self.upload_file(
            file_path=file_path,
            s3_key=s3_key,
            content_type='application/vnd.recordare.musicxml+xml',
            public_read=public_read,
            use_presigned_url=use_presigned_url,
            presigned_expiration=presigned_expiration
        )

    def delete_file(self, s3_key: str) -> bool:
        """
        S3에서 파일 삭제

        Args:
            s3_key: 삭제할 S3 객체 키

        Returns:
            성공 여부
        """
        if not self.s3_client:
            logger.error("S3 client not initialized")
            return False

        if not self.bucket_name:
            logger.error("S3 bucket name not configured")
            return False

        try:
            self.s3_client.delete_object(
                Bucket=self.bucket_name,
                Key=s3_key
            )
            logger.info(f"Deleted from S3: {s3_key}")
            return True

        except ClientError as e:
            logger.error(f"S3 delete failed: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error during delete: {e}")
            return False

    def file_exists(self, s3_key: str) -> bool:
        """
        S3에 파일이 존재하는지 확인

        Args:
            s3_key: 확인할 S3 객체 키

        Returns:
            존재 여부
        """
        if not self.s3_client or not self.bucket_name:
            return False

        try:
            self.s3_client.head_object(
                Bucket=self.bucket_name,
                Key=s3_key
            )
            return True
        except ClientError:
            return False
