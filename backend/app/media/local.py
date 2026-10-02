import base64
from pathlib import Path

from app.errors import ImportFailure
from app.schemas import AnalysisInput, Evidence, ResolvedPost


class LocalMediaPreparer:
    """Reads local images and already-extracted frames. No fake video decoding."""

    def __init__(self, media_root: Path):
        self.media_root = media_root.resolve()

    async def prepare(self, post: ResolvedPost) -> AnalysisInput:
        if post.videos:
            raise ImportFailure("VIDEO_NOT_IMPLEMENTED", "视频自动抽帧和转写尚未实现，请先提供 video_frames 和 transcript。")
        evidence = []
        if post.text:
            evidence.append(Evidence(id="text-1", kind="text", text=post.text))
        if post.transcript:
            evidence.append(Evidence(id="transcript-1", kind="transcript", text=post.transcript))
        for kind, items in (("image", post.images), ("video_frame", post.video_frames)):
            for item in items:
                path = (self.media_root / item.path).resolve()
                if not path.is_relative_to(self.media_root):
                    raise ImportFailure("INVALID_MEDIA", "图片必须放在 examples 文件夹内。")
                if not path.is_file() or path.stat().st_size > 5 * 1024 * 1024:
                    raise ImportFailure("INVALID_MEDIA", "图片不存在或超过 5 MB。")
                data = path.read_bytes()
                signatures = {
                    "image/png": data.startswith(b"\x89PNG\r\n\x1a\n"),
                    "image/jpeg": data.startswith(b"\xff\xd8\xff"),
                    "image/webp": data.startswith(b"RIFF") and data[8:12] == b"WEBP",
                }
                if not signatures[item.mime_type]:
                    raise ImportFailure("INVALID_MEDIA", "图片内容与 mime_type 不一致。")
                evidence.append(Evidence(
                    id=item.id, kind=kind, image_base64=base64.b64encode(data).decode("ascii"),
                    mime_type=item.mime_type, timestamp_seconds=item.timestamp_seconds,
                ))
        if not evidence:
            raise ImportFailure("EMPTY_CONTENT", "示例没有可分析的内容。")
        return AnalysisInput(source=post.source, evidence=evidence, is_fixture=post.is_fixture)
