from infra.adapters.s3_downloader import S3Downloader

class DownloadS3Step:
    def __init__(self, downloader: S3Downloader):
        self.downloader = downloader

    def __call__(self, ctx: dict) -> dict:
        path, fname = self.downloader.download_to_temp(ctx["s3_url"])
        ctx["local_path"] = path
        ctx["filename"] = fname
        return ctx
