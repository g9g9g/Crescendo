# domain/services/omr_service.py
import os
import sys
import tempfile
import shutil
import multiprocessing
from werkzeug.utils import secure_filename
from pdf2image import convert_from_path


class OMRService:
    """OMR (Optical Music Recognition) 서비스"""

    ALLOWED_EXTENSIONS = {'pdf', 'png', 'jpg', 'jpeg'}

    def __init__(
        self,
        upload_folder: str = None,
        output_folder: str = None,
        poppler_path: str = None,
        performance_mode: str = 'balanced',
        s3_uploader = None
    ):
        # 1) 경로/폴더 준비
        base_dir = os.path.join(os.path.dirname(__file__), '..', '..')
        self.upload_folder = upload_folder or os.path.join(base_dir, 'uploads')
        self.output_folder = output_folder or os.path.join(base_dir, 'outputs')
        os.makedirs(self.upload_folder, exist_ok=True)
        os.makedirs(self.output_folder, exist_ok=True)

        # S3 Uploader
        self.s3_uploader = s3_uploader

        # Poppler (Windows 개발환경 전용; 리눅스/도커는 PATH 사용)
        if poppler_path:
            self.poppler_path = poppler_path
        else:
            win_poppler = os.path.join(base_dir, 'poppler-24.08.0', 'Library', 'bin')
            self.poppler_path = win_poppler if os.path.exists(win_poppler) else None

        # 2) 먼저 OMR 엔진 가용성부터 평가 (!!! 순서 중요)
        self.oemer_available = self._check_oemer()
        self.audiveris_available, self.audiveris_path = self._check_audiveris()

        # 3) 워커 수 산정 (메모리/엔진 가용성에 따라)
        cpu = (multiprocessing.cpu_count() or 1)
        available_memory_gb = int(os.getenv("OMR_AVAILABLE_MEMORY_GB", "12"))

        # OEMER는 메모리 사용량이 매우 높으므로 순차 처리만 사용
        oemer_max_workers = 1
        audiveris_max_workers = max(2, cpu - 1)

        # 엔진에 따른 기본 동시성
        default_workers = audiveris_max_workers if self.audiveris_available else oemer_max_workers

        # 4) 퍼포먼스 프로파일
        self.performance_configs = {
            'fast': {
                'dpi': 400, 'grayscale': True, 'skip_deskew': True, 'cache_enabled': True,
                'max_workers': default_workers
            },
            'balanced': {
                'dpi': 400, 'grayscale': True, 'skip_deskew': False, 'cache_enabled': True,
                'max_workers': default_workers
            },
            'accurate': {
                'dpi': 400, 'grayscale': False, 'skip_deskew': False, 'cache_enabled': False,
                'max_workers': 1  # 정확도 모드는 항상 순차처리
            }
        }

        self.performance_mode = performance_mode
        self.current_config = self.performance_configs[performance_mode]

        # 5) 안내 로그
        if self.audiveris_available:
            print(f"[INFO] Audiveris found at: {self.audiveris_path}")
            print("[INFO] Audiveris will be preferred over OEMER for better accuracy")
        elif self.oemer_available:
            print("[INFO] Using OEMER for OMR processing")
        else:
            print("[WARNING] No OMR engine available")

        print(f"[BOOT] OMR engines -> oemer={self.oemer_available}, "
            f"audiveris={self.audiveris_available}, cpu={cpu}, "
            f"default_workers={default_workers}")


    def _check_oemer(self) -> bool:
        """Oemer 사용 가능 여부 확인"""
        try:
            import oemer
            return True
        except ImportError:
            return False

    def _check_audiveris(self) -> tuple[bool, str]:
        """Audiveris 사용 가능 여부 확인"""
        possible_paths = [
            # Linux/Docker 경로 (우선순위 높음)
            '/usr/bin/audiveris',
            '/usr/local/bin/audiveris',
            '/opt/Audiveris/bin/Audiveris.sh',
            # Windows 경로
            r"C:\Program Files\Audiveris\Audiveris.exe",
            r"C:\Program Files\Audiveris\bin\Audiveris.bat",
            r"C:\Program Files (x86)\Audiveris\bin\Audiveris.bat",
        ]

        for path in possible_paths:
            if os.path.exists(path):
                return True, path

        # 시스템 PATH 확인
        audiveris_cmd = shutil.which('audiveris')
        if audiveris_cmd:
            return True, audiveris_cmd

        return False, None

    def allowed_file(self, filename: str) -> bool:
        """허용된 파일 확장자 확인"""
        return '.' in filename and filename.rsplit('.', 1)[1].lower() in self.ALLOWED_EXTENSIONS

    def set_performance_mode(self, mode: str) -> dict:
        """성능 모드 변경"""
        if mode not in self.performance_configs:
            raise ValueError(f'Invalid mode. Choose from: {list(self.performance_configs.keys())}')

        self.performance_mode = mode
        self.current_config = self.performance_configs[mode]

        return {
            'message': f'Performance mode changed to {mode}',
            'config': self.current_config
        }

    def get_performance_info(self) -> dict:
        """현재 성능 설정 정보"""
        return {
            'performance_mode': self.performance_mode,
            'config': self.current_config,
            'system': {
                'cpu_count': multiprocessing.cpu_count(),
                'gpu_available': False,
                'gpu_enabled': False
            },
            'omr_engines': {
                'audiveris_available': self.audiveris_available,
                'oemer_available': self.oemer_available,
                'active_engine': 'Audiveris' if self.audiveris_available else ('OEMER' if self.oemer_available else 'None')
            }
        }

    def convert_pdf_to_images(self, pdf_path: str, output_dir: str) -> list[str]:
        """PDF를 이미지로 변환"""
        try:
            config = self.current_config
            images = convert_from_path(
                pdf_path,
                dpi=config['dpi'],
                poppler_path=self.poppler_path if (self.poppler_path and os.path.exists(self.poppler_path)) else None,
                grayscale=config['grayscale'],
                thread_count=config['max_workers']
            )
            image_paths = []

            for i, image in enumerate(images):
                image_path = os.path.join(output_dir, f'page_{i}.png')
                image.save(image_path, 'PNG', optimize=False)
                image_paths.append(image_path)

            print(f"[INFO] Converted {len(images)} pages to images (DPI: {config['dpi']}, Grayscale: {config['grayscale']})")
            return image_paths

        except Exception as e:
            print(f"Error converting PDF to images: {e}")
            return []

    def download_oemer_models(self) -> bool:
        """Oemer 모델 다운로드"""
        try:
            from oemer import MODULE_PATH
            from oemer.ete import CHECKPOINTS_URL, download_file

            chk_path = os.path.join(MODULE_PATH, "checkpoints/unet_big/model.onnx")
            if os.path.exists(chk_path):
                print("[INFO] Oemer models already downloaded")
                return True

            print("[INFO] Downloading Oemer models (this may take a few minutes)...")

            for idx, (title, url) in enumerate(CHECKPOINTS_URL.items()):
                print(f"[INFO] Downloading {title} ({idx+1}/{len(CHECKPOINTS_URL)})")
                save_dir = "unet_big" if title.startswith("1st") else "seg_net"
                save_dir = os.path.join(MODULE_PATH, "checkpoints", save_dir)
                os.makedirs(save_dir, exist_ok=True)
                save_path = os.path.join(save_dir, title.split("_")[1])

                if not os.path.exists(save_path):
                    download_file(title, url, save_path)
                else:
                    print(f"[INFO] {title} already exists, skipping")

            print("[INFO] All models downloaded successfully")
            return True

        except Exception as e:
            print(f"[ERROR] Failed to download models: {e}")
            import traceback
            traceback.print_exc()
            return False

    def setup_onnx_providers(self) -> list:
        """ONNX Runtime 프로바이더 설정 (메모리 최적화)"""
        providers = [('CPUExecutionProvider', {
            'arena_extend_strategy': 'kSameAsRequested',
            'enable_cpu_mem_arena': True,
            'intra_op_num_threads': 1,  # 메모리 사용량 감소
            'inter_op_num_threads': 1,
        })]
        print("[INFO] Using CPU-only mode for ONNX Runtime (memory-optimized)")
        return providers

    def process_image_with_oemer(self, image_path: str, page_num: int = 0) -> str:
        """Oemer로 이미지 처리"""
        try:
            if not self.oemer_available:
                return None

            print(f"[Page {page_num+1}] Running Oemer OMR on: {image_path}")

            if not self.download_oemer_models():
                print(f"[Page {page_num+1}] [ERROR] Failed to download Oemer models")
                return None

            from oemer.ete import extract, clear_data
            from argparse import Namespace
            import onnxruntime as ort

            clear_data()

            output_dir = os.path.dirname(image_path)

            providers = self.setup_onnx_providers()

            try:
                ort.set_default_logger_severity(3)
            except Exception as e:
                print(f"[Page {page_num+1}] [WARNING] Could not configure ONNX: {e}")

            config = self.current_config
            args = Namespace(
                img_path=image_path,
                output_path=output_dir,
                use_tf=False,
                save_cache=config['cache_enabled'],
                without_deskew=config['skip_deskew']
            )

            print(f"[Page {page_num+1}] Starting Oemer OMR processing...")

            import time
            start_time = time.time()
            musicxml_path = extract(args)
            elapsed_time = time.time() - start_time

            print(f"[Page {page_num+1}] Oemer completed in {elapsed_time:.2f}s. Output: {musicxml_path}")

            if os.path.exists(musicxml_path):
                with open(musicxml_path, 'rb') as f:
                    xml_content = f.read().decode('utf-8')
                print(f"[Page {page_num+1}] [SUCCESS] Successfully converted to MusicXML: {len(xml_content)} bytes")
                return xml_content
            else:
                print(f"[Page {page_num+1}] [ERROR] Oemer did not generate output file")
                return None

        except Exception as e:
            print(f"[Page {page_num+1}] [ERROR] Error processing image with Oemer: {e}")
            import traceback
            traceback.print_exc()
            return None

    def clean_musicxml(self, xml_content: str) -> str:
        """MusicXML 후처리"""
        try:
            import xml.etree.ElementTree as ET

            # 상수 정의
            STEP_ORDER = {'C': 0, 'D': 1, 'E': 2, 'F': 3, 'G': 4, 'A': 5, 'B': 6}
            ENDING_DEFAULT_LENGTH = '56'
            ENDING_DEFAULT_Y = '61'
            SCAN_PREVIOUS_MEASURES = 3

            # 헬퍼 함수들
            def get_pitch_rank(pitch_info):
                """pitch를 비교 가능한 값으로 변환 (octave, step, alter)"""
                step, octave, alter = pitch_info
                return (octave, STEP_ORDER[step], alter)

            def get_or_create_barline(measure, location='left'):
                """마디에서 barline을 찾거나 생성"""
                for bl in measure.findall('barline'):
                    if bl.get('location') == location:
                        return bl

                # 생성
                barline = ET.Element('barline', location=location)
                bar_style = ET.SubElement(barline, 'bar-style')
                bar_style.text = 'regular'

                # 삽입 위치 결정
                attrs = measure.find('attributes')
                if attrs is not None:
                    insert_pos = list(measure).index(attrs) + 1
                    measure.insert(insert_pos, barline)
                else:
                    measure.insert(0, barline)

                return barline

            def add_ending_if_not_exists(barline, number, ending_type, **attrs):
                """barline에 ending 요소 추가 (없으면)"""
                for ending in barline.findall('ending'):
                    if ending.get('number') == number and ending.get('type') == ending_type:
                        return False  # 이미 존재

                ending_elem = ET.SubElement(barline, 'ending', number=number, type=ending_type)
                for key, value in attrs.items():
                    ending_elem.set(key.replace('_', '-'), str(value))

                return True  # 추가됨

            def remove_endings_by_number(barline, number):
                """barline에서 특정 번호의 ending 모두 제거"""
                endings_to_remove = [e for e in barline.findall('ending') if e.get('number') == number]
                for ending in endings_to_remove:
                    barline.remove(ending)
                return len(endings_to_remove)

            def fix_seconda_volta(measures, repeat_idx):
                """2번 엔딩 처리 (repeat backward 다음 마디)"""
                if repeat_idx + 1 >= len(measures):
                    return None

                next_measure = measures[repeat_idx + 1]

                # left barline 생성/찾기
                left_barline = get_or_create_barline(next_measure, 'left')

                # 잘못된 ending 1 제거
                removed = remove_endings_by_number(left_barline, '1')
                if removed > 0:
                    print(f"  Removed incorrect ending 1 from measure {repeat_idx+2} left barline")

                # ending 2 start 추가
                if add_ending_if_not_exists(left_barline, '2', 'start',
                                           end_length=ENDING_DEFAULT_LENGTH,
                                           default_y=ENDING_DEFAULT_Y):
                    print(f"  Added ending 2 type='start' to measure {repeat_idx+2}")

                # right barline에서도 ending 1 제거
                for bl in next_measure.findall('barline'):
                    if bl.get('location') == 'right':
                        removed = remove_endings_by_number(bl, '1')
                        if removed > 0:
                            print(f"  Removed ending 1 from measure {repeat_idx+2} right barline")

                return next_measure

            def fix_prima_volta_stop(measure, backward_barline, barlines, measure_num):
                """1번 엔딩 stop 추가 (repeat backward 마디)"""
                right_barline = backward_barline if backward_barline.get('location') == 'right' else None

                if right_barline is None:
                    for bl in barlines:
                        if bl.get('location') == 'right':
                            right_barline = bl
                            break

                if right_barline is None:
                    return 0

                # ending 1 stop 있는지 확인
                if add_ending_if_not_exists(right_barline, '1', 'stop', default_y=ENDING_DEFAULT_Y):
                    # repeat 앞에 삽입하도록 재배치
                    ending_elem = None
                    for e in right_barline.findall('ending'):
                        if e.get('number') == '1' and e.get('type') == 'stop':
                            ending_elem = e
                            break

                    if ending_elem is not None:
                        right_barline.remove(ending_elem)

                        # repeat 앞에 삽입
                        repeat_idx = None
                        for idx, child in enumerate(right_barline):
                            if child.tag == 'repeat':
                                repeat_idx = idx
                                break

                        if repeat_idx is not None:
                            right_barline.insert(repeat_idx, ending_elem)
                        else:
                            right_barline.append(ending_elem)

                    print(f"  Added ending 1 type='stop' to measure {measure_num}")
                    return 1

                return 0

            def find_and_fix_prima_volta_start(measures, repeat_idx, next_two_notes, get_first_two_notes_func):
                """1번 엔딩 start를 음표 매칭으로 찾아서 추가"""
                if len(next_two_notes) < 2:
                    return

                found_match = False

                # 이전 N마디 스캔해서 첫 2음이 같은 마디 찾기
                for measure_idx in range(repeat_idx, max(-1, repeat_idx - SCAN_PREVIOUS_MEASURES), -1):
                    check_measure = measures[measure_idx]
                    check_notes = get_first_two_notes_func(check_measure)

                    if len(check_notes) >= 2 and check_notes[:2] == next_two_notes[:2]:
                        print(f"  Found matching first 2 notes at measure {measure_idx+1}: {check_notes[:2]}")

                        # left barline 생성/찾기
                        target_barline = get_or_create_barline(check_measure, 'left')

                        # ending 1 start 추가
                        if add_ending_if_not_exists(target_barline, '1', 'start',
                                                   end_length=ENDING_DEFAULT_LENGTH,
                                                   default_y=ENDING_DEFAULT_Y):
                            print(f"  Added ending 1 type='start' to measure {measure_idx+1}")

                        found_match = True
                        break

                # Fallback: 매칭 못 찾으면 바로 이전 마디에 추가
                if not found_match:
                    print(f"  No matching measure found for first 2 notes: {next_two_notes}")

                    if repeat_idx > 0:
                        prev_measure = measures[repeat_idx - 1]
                        print(f"  Adding ending 1 type='start' to previous measure {repeat_idx} as fallback")

                        # left barline 생성/찾기
                        target_barline = get_or_create_barline(prev_measure, 'left')

                        # ending 1 start 추가
                        if add_ending_if_not_exists(target_barline, '1', 'start',
                                                   end_length=ENDING_DEFAULT_LENGTH,
                                                   default_y=ENDING_DEFAULT_Y):
                            print(f"  Added ending 1 type='start' to measure {repeat_idx} (fallback)")

            root = ET.fromstring(xml_content)

            # divisions 값 찾기
            divisions = 12
            divisions_elem = root.find('.//attributes/divisions')
            if divisions_elem is not None:
                divisions = int(divisions_elem.text)

            parts = root.findall('.//part')
            removed_count = 0
            fixed_count = 0
            ending_fixed = 0

            for part in parts:
                measures = part.findall('measure')
                measures_to_remove = []

                # 마디에서 첫 2개 음표 추출 (화음은 가장 높은 음만)
                def get_first_two_notes(measure):
                    """마디에서 첫 2개 음표 그룹의 최고음 pitch 정보 추출 (rest 제외)"""
                    note_groups = []
                    current_group = []

                    for note in measure.findall('note'):
                        if note.find('rest') is not None:
                            continue

                        pitch = note.find('pitch')
                        if pitch is None:
                            continue

                        step = pitch.find('step')
                        octave = pitch.find('octave')
                        alter = pitch.find('alter')

                        if step is None or octave is None:
                            continue

                        # pitch 정보: (step, octave, alter)
                        alter_value = int(alter.text) if alter is not None else 0
                        pitch_info = (step.text, int(octave.text), alter_value)

                        # chord 태그가 있으면 현재 그룹에 추가
                        if note.find('chord') is not None:
                            current_group.append(pitch_info)
                        else:
                            # 새로운 음표 그룹 시작
                            if current_group:
                                highest = max(current_group, key=get_pitch_rank)
                                note_groups.append(highest)

                            current_group = [pitch_info]

                        if len(note_groups) >= 2:
                            break

                    # 마지막 그룹 처리
                    if len(note_groups) < 2 and current_group:
                        highest = max(current_group, key=get_pitch_rank)
                        note_groups.append(highest)

                    return note_groups

                # repeat backward가 있는 마디 찾고 엔딩 수정
                for i, measure in enumerate(measures):
                    barlines = measure.findall('barline')

                    # repeat backward 찾기
                    backward_barline = None
                    for barline in barlines:
                        repeat = barline.find('repeat')
                        if repeat is not None and repeat.get('direction') == 'backward':
                            backward_barline = barline
                            break

                    if not backward_barline:
                        continue

                    print(f"  Found repeat backward at measure {i+1}")

                    # 1. 다음 마디 처리 (2번 엔딩)
                    next_measure = fix_seconda_volta(measures, i)
                    if next_measure is None:
                        continue

                    # 다음 마디의 첫 2음 추출
                    next_two_notes = get_first_two_notes(next_measure)
                    print(f"  Next measure first 2 notes: {next_two_notes}")

                    # 2. repeat backward 마디에 ending 1 stop 추가
                    ending_fixed += fix_prima_volta_stop(measure, backward_barline, barlines, i+1)

                    # 3. 이전 마디 스캔해서 첫 2음이 같은 마디 찾기 (1번 엔딩 start 추가)
                    find_and_fix_prima_volta_start(measures, i, next_two_notes, get_first_two_notes)

                # 마디 duration 확인 및 수정
                for measure in measures:
                    beats = 4
                    beat_type = 4
                    attrs = measure.find('attributes')
                    if attrs is not None:
                        time_elem = attrs.find('time')
                        if time_elem is not None:
                            beats_elem = time_elem.find('beats')
                            beat_type_elem = time_elem.find('beat-type')
                            if beats_elem is not None:
                                beats = int(beats_elem.text)
                            if beat_type_elem is not None:
                                beat_type = int(beat_type_elem.text)

                    expected_duration = divisions * beats * (4 / beat_type)

                    notes = measure.findall('.//note')
                    actual_duration = 0
                    for note in notes:
                        if note.find('chord') is None:
                            duration_elem = note.find('duration')
                            if duration_elem is not None:
                                actual_duration += int(duration_elem.text)

                    # duration 초과 처리
                    if actual_duration > expected_duration:
                        excess_duration = actual_duration - expected_duration

                        for note in notes:
                            if note.find('chord') is not None:
                                continue

                            rest = note.find('rest')
                            if rest is not None:
                                duration_elem = note.find('duration')
                                type_elem = note.find('type')

                                if duration_elem is not None and type_elem is not None:
                                    duration_val = int(duration_elem.text)
                                    type_val = type_elem.text

                                    if type_val == 'whole' and duration_val == divisions * 4:
                                        if excess_duration >= divisions * 2:
                                            duration_elem.text = str(divisions * 2)
                                            type_elem.text = 'half'
                                            fixed_count += 1
                                            break

                    # duration 부족 처리
                    elif actual_duration < expected_duration:
                        missing_duration = int(expected_duration - actual_duration)

                        rest_type = 'quarter'
                        if missing_duration >= divisions * 4:
                            rest_type = 'whole'
                        elif missing_duration >= divisions * 2:
                            rest_type = 'half'
                        elif missing_duration >= divisions:
                            rest_type = 'quarter'
                        elif missing_duration >= divisions / 2:
                            rest_type = 'eighth'
                        else:
                            rest_type = '16th'

                        fill_rest = ET.Element('note')
                        rest_elem = ET.SubElement(fill_rest, 'rest')
                        duration_elem = ET.SubElement(fill_rest, 'duration')
                        duration_elem.text = str(missing_duration)
                        voice_elem = ET.SubElement(fill_rest, 'voice')
                        voice_elem.text = '1'
                        type_elem = ET.SubElement(fill_rest, 'type')
                        type_elem.text = rest_type

                        if notes:
                            first_staff = notes[0].find('staff')
                            if first_staff is not None:
                                staff_elem = ET.SubElement(fill_rest, 'staff')
                                staff_elem.text = first_staff.text

                        measure.append(fill_rest)
                        fixed_count += 1

                    # 빈 마디 확인
                    notes = measure.findall('.//note')
                    actual_notes = [n for n in notes if n.find('rest') is None]

                    has_full_measure_rest = False
                    for note in notes:
                        rest = note.find('rest')
                        if rest is not None and rest.get('measure') == 'yes':
                            has_full_measure_rest = True
                            break

                    if len(actual_notes) == 0 and has_full_measure_rest:
                        attrs = measure.find('attributes')
                        has_important_attrs = False
                        if attrs is not None:
                            if attrs.find('time') is not None or attrs.find('key') is not None:
                                has_important_attrs = True

                        if not has_important_attrs:
                            measures_to_remove.append(measure)
                    elif len(actual_notes) == 0 and len(notes) > 0:
                        all_rests = all(n.find('rest') is not None for n in notes)
                        if all_rests:
                            measures_to_remove.append(measure)
                    elif len(notes) == 0:
                        if measure.find('attributes') is None:
                            measures_to_remove.append(measure)

                # 빈 마디 제거
                for measure in measures_to_remove:
                    part.remove(measure)
                    removed_count += 1

            cleaned_xml = ET.tostring(root, encoding='unicode')

            if not cleaned_xml.startswith('<?xml'):
                cleaned_xml = '<?xml version="1.0" encoding="UTF-8"?>\n' + cleaned_xml

            if ending_fixed > 0:
                print(f"  Second ending automatically added")
            if fixed_count > 0:
                print(f"  {fixed_count} measure durations auto-fixed")
            if removed_count > 0:
                print(f"  {removed_count} empty measures removed")

            return cleaned_xml

        except Exception as e:
            print(f"  Warning: MusicXML post-processing failed: {e}")
            return xml_content

    def process_image_with_audiveris(self, image_path: str, page_num: int = 0) -> str:
        """Audiveris로 이미지 처리"""
        try:
            if not self.audiveris_available:
                return None

            print(f"[Page {page_num+1}] Running Audiveris OMR on: {image_path}")

            import subprocess
            import time

            output_dir = os.path.dirname(image_path)
            basename = os.path.splitext(os.path.basename(image_path))[0]
            output_base = os.path.join(output_dir, basename)

            start_time = time.time()

            try:
                cmd = [
                    self.audiveris_path,
                    "-batch",
                    "-export",
                    "-output", output_dir,
                    image_path
                ]

                print(f"[Page {page_num+1}] Audiveris command: {' '.join(cmd[:3])} ...")

                result = subprocess.run(
                    cmd,
                    capture_output=True,
                    text=True,
                    timeout=300,
                    check=True
                )

                elapsed = time.time() - start_time
                print(f"[Page {page_num+1}] Audiveris completed in {elapsed:.2f}s")

                possible_outputs = [
                    output_base + ".mxl",
                    output_base + ".xml",
                    output_base + ".musicxml"
                ]

                xml_content = None
                for output_file in possible_outputs:
                    if os.path.exists(output_file):
                        print(f"[Page {page_num+1}] Found output: {output_file}")

                        if output_file.endswith('.mxl'):
                            import zipfile
                            with zipfile.ZipFile(output_file, 'r') as zip_ref:
                                for name in zip_ref.namelist():
                                    if name.endswith('.xml') and not name.startswith('META-INF'):
                                        xml_content = zip_ref.read(name).decode('utf-8')
                                        break
                        else:
                            with open(output_file, 'r', encoding='utf-8') as f:
                                xml_content = f.read()

                        break

                if xml_content:
                    print(f"[Page {page_num+1}] [SUCCESS] Audiveris produced MusicXML: {len(xml_content)} bytes")

                    # MusicXML 후처리
                    print(f"[Page {page_num+1}] MusicXML post-processing...")
                    cleaned_xml = self.clean_musicxml(xml_content)

                    return cleaned_xml
                else:
                    print(f"[Page {page_num+1}] [ERROR] No output file found")
                    return None

            except subprocess.TimeoutExpired:
                print(f"[Page {page_num+1}] [ERROR] Audiveris timeout")
                return None
            except subprocess.CalledProcessError as e:
                print(f"[Page {page_num+1}] [ERROR] Audiveris failed: {e}")
                return None

        except Exception as e:
            print(f"[Page {page_num+1}] [ERROR] Error processing with Audiveris: {e}")
            import traceback
            traceback.print_exc()
            return None

    def merge_musicxml_pages(self, xml_contents: list[str]) -> str:
        """여러 페이지의 MusicXML을 하나로 합치기"""
        try:
            import xml.etree.ElementTree as ET

            if not xml_contents:
                return None

            if len(xml_contents) == 1:
                return xml_contents[0]

            print(f"[INFO] Merging {len(xml_contents)} MusicXML pages...")

            # 첫 번째 페이지를 base로 사용
            root = ET.fromstring(xml_contents[0])

            # 첫 번째 part 찾기
            first_part = root.find('.//part')
            if first_part is None:
                print("[WARNING] No part found in first page, returning first page only")
                return xml_contents[0]

            # 첫 번째 페이지의 마지막 measure number 찾기
            measures = first_part.findall('measure')
            if measures:
                last_measure = measures[-1]
                last_measure_number = int(last_measure.get('number', '0'))
            else:
                last_measure_number = 0

            print(f"  First page has {len(measures)} measures (last measure: {last_measure_number})")

            # 나머지 페이지들의 measure를 추가
            for page_idx, xml_content in enumerate(xml_contents[1:], start=2):
                page_root = ET.fromstring(xml_content)
                page_part = page_root.find('.//part')

                if page_part is None:
                    print(f"  [WARNING] No part found in page {page_idx}, skipping")
                    continue

                page_measures = page_part.findall('measure')
                print(f"  Page {page_idx} has {len(page_measures)} measures")

                # measure number를 이어서 증가
                for measure in page_measures:
                    last_measure_number += 1
                    measure.set('number', str(last_measure_number))

                    # 첫 번째 part에 measure 추가
                    first_part.append(measure)

            # XML 문자열로 변환
            merged_xml = ET.tostring(root, encoding='unicode')

            if not merged_xml.startswith('<?xml'):
                merged_xml = '<?xml version="1.0" encoding="UTF-8"?>\n' + merged_xml

            print(f"[SUCCESS] Merged into single MusicXML with {last_measure_number} measures")
            return merged_xml

        except Exception as e:
            print(f"[ERROR] Failed to merge MusicXML pages: {e}")
            import traceback
            traceback.print_exc()
            # 실패하면 첫 번째 페이지만 반환
            return xml_contents[0] if xml_contents else None

    def create_sample_musicxml(self, title: str = "Sample Score") -> str:
        """샘플 MusicXML 생성"""
        return f'''<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE score-partwise PUBLIC "-//Recordare//DTD MusicXML 3.1 Partwise//EN"
                                "http://www.musicxml.org/dtds/partwise.dtd">
<score-partwise version="3.1">
  <work>
    <work-title>{title}</work-title>
  </work>
  <part-list>
    <score-part id="P1">
      <part-name>Music</part-name>
    </score-part>
  </part-list>
  <part id="P1">
    <measure number="1">
      <attributes>
        <divisions>1</divisions>
        <key>
          <fifths>0</fifths>
        </key>
        <time>
          <beats>4</beats>
          <beat-type>4</beat-type>
        </time>
        <clef>
          <sign>G</sign>
          <line>2</line>
        </clef>
      </attributes>
      <note>
        <pitch>
          <step>C</step>
          <octave>4</octave>
        </pitch>
        <duration>4</duration>
        <type>whole</type>
      </note>
    </measure>
  </part>
</score-partwise>'''

    def convert_to_musicxml(self, file_storage) -> dict:
        """PDF/이미지를 MusicXML로 변환"""
        temp_dir = None
        try:
            filename = secure_filename(file_storage.filename)
            print(f"[DEBUG] Received file: {filename}")

            temp_dir = tempfile.mkdtemp()
            file_path = os.path.join(temp_dir, filename)
            file_storage.save(file_path)
            print(f"[DEBUG] File saved to: {file_path}")

            # 파일 타입에 따라 처리
            file_parts = filename.rsplit('.', 1)
            if len(file_parts) > 1:
                file_ext = file_parts[1].lower()
            else:
                # 확장자가 없는 경우 파일 내용으로 추정 (기본값: 이미지)
                print(f"[WARNING] No file extension found in filename: {filename}")
                file_ext = ''

            image_paths = []

            if file_ext == 'pdf':
                print(f"Converting PDF {filename} to images...")
                image_paths = self.convert_pdf_to_images(file_path, temp_dir)
                if not image_paths:
                    raise Exception("Failed to convert PDF to images")
            else:
                image_paths = [file_path]

            print(f"[DEBUG] Total images to process: {len(image_paths)}")

            # OMR 처리
            if not self.audiveris_available and not self.oemer_available:
                musicxml_content = self.create_sample_musicxml(f"Uploaded File - {filename}")
                message = 'No OMR engine available. Install OEMER or Audiveris'
                pages_processed = 0
            else:
                omr_engine = 'Audiveris' if self.audiveris_available else 'OEMER'
                max_workers = self.current_config['max_workers']
                processing_mode = "parallel" if max_workers > 1 and len(image_paths) > 1 else "sequential"

                print(f"Processing {len(image_paths)} image(s) with {omr_engine} ({processing_mode}, workers={max_workers})...")

                xml_results_list = []

                # S3 Uploader가 pickle 불가하므로 모든 엔진에서 순차 처리 사용
                # 순차 처리
                if len(image_paths) > 1:
                    engine_name = "OEMER (sequential to save memory)" if self.oemer_available else "Audiveris"
                    print(f"[INFO] Using sequential processing with {engine_name}")
                    for i, img_path in enumerate(image_paths):
                        if self.audiveris_available:
                            xml_content = self.process_image_with_audiveris(img_path, i)
                        else:
                            xml_content = self.process_image_with_oemer(img_path, i)

                        if xml_content:
                            temp_xml_path = os.path.join(temp_dir, f'page_{i}.xml')
                            with open(temp_xml_path, 'w', encoding='utf-8') as f:
                                f.write(xml_content)
                            xml_results_list.append(temp_xml_path)

                else:
                    # 단일 이미지
                    if self.audiveris_available:
                        xml_content = self.process_image_with_audiveris(image_paths[0], 0)
                    else:
                        xml_content = self.process_image_with_oemer(image_paths[0], 0)

                    if xml_content:
                        temp_xml_path = os.path.join(temp_dir, f'page_0.xml')
                        with open(temp_xml_path, 'w', encoding='utf-8') as f:
                            f.write(xml_content)
                        xml_results_list.append(temp_xml_path)

                if not xml_results_list:
                    raise Exception("OMR processing failed. No music detected in the image(s).")

                # 모든 페이지 읽기
                pages_content = []
                for xml_path in xml_results_list:
                    with open(xml_path, 'r', encoding='utf-8') as f:
                        pages_content.append(f.read())

                # 페이지 합치기
                if len(pages_content) > 1:
                    merged_musicxml = self.merge_musicxml_pages(pages_content)
                else:
                    merged_musicxml = pages_content[0] if pages_content else ''

                pages_processed = len(xml_results_list)
                message = f'Successfully converted {pages_processed} page(s) to MusicXML'

                # S3 업로드
                s3_url = None
                if self.s3_uploader and merged_musicxml:
                    print("[INFO] Uploading merged MusicXML to S3...")
                    # 임시 파일로 저장
                    merged_filename = f"{os.path.splitext(filename)[0]}_merged.musicxml"
                    merged_filepath = os.path.join(temp_dir, merged_filename)
                    with open(merged_filepath, 'w', encoding='utf-8') as f:
                        f.write(merged_musicxml)

                    # S3 업로드
                    s3_url = self.s3_uploader.upload_musicxml(
                        file_path=merged_filepath,
                        use_presigned_url=True,
                        presigned_expiration=86400  # 24시간
                    )

                    if s3_url:
                        print(f"[SUCCESS] Uploaded to S3: {s3_url}")
                        message = f'Successfully converted {pages_processed} page(s) and uploaded to S3'
                    else:
                        print("[WARNING] S3 upload failed, returning MusicXML content")

            return {
                'success': True,
                'musicxml': merged_musicxml if not s3_url else None,  # S3 URL 있으면 내용 제외
                's3_url': s3_url,
                'pages_processed': pages_processed,
                'message': message
            }

        except Exception as e:
            print(f"Conversion error: {e}")
            import traceback
            traceback.print_exc()
            raise

        finally:
            # 임시 디렉토리 정리
            if temp_dir and os.path.exists(temp_dir):
                try:
                    shutil.rmtree(temp_dir)
                except:
                    pass
