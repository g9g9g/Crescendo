# domain/pipeline/steps/transcribe_audio.py
import os, tempfile
from pathlib import Path
from util.mp3_to_musicxml import transcribe_mp3_to_midi, midi_to_musicxml

class TranscribeAudioStep:
    def __init__(self, device: str = "cpu", quant: str = "16th"):
        self.device = device
        self.quant = quant

    def __call__(self, ctx: dict) -> dict:
        mp3_path = ctx["local_path"]  # 이전 스텝(download or upload)에서 세팅
        stem = Path(ctx.get("filename") or "audio").stem
        # 임시 출력 파일 경로
        midi_fd, midi_out = tempfile.mkstemp(prefix=f"{stem}_", suffix=".mid")
        os.close(midi_fd)
        xml_fd, xml_out = tempfile.mkstemp(prefix=f"{stem}_", suffix=".musicxml")
        os.close(xml_fd)

        try:
            transcribe_mp3_to_midi(mp3_path, midi_out, device=self.device)
            midi_to_musicxml(midi_out, xml_out, quantization=self.quant,
                             infer_key_time=True, simplify_notation=True)
            ctx["midi_path"] = midi_out
            ctx["xml_path"] = xml_out
            return ctx
        except Exception:
            # 실패 시 임시물 정리
            try: os.remove(midi_out)
            except: pass
            try: os.remove(xml_out)
            except: pass
            raise
