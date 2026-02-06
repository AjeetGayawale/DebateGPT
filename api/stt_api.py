from fastapi import APIRouter, UploadFile, File, HTTPException, Query
from Whispercpp.debate_whispercpp import run_whisper_file
import tempfile
import os

router = APIRouter(prefix="/stt", tags=["Speech To Text"])

TRANSCRIPT_FILE = "debate_transcript.txt"


def _read_full_transcript():
    if os.path.exists(TRANSCRIPT_FILE):
        with open(TRANSCRIPT_FILE, "r", encoding="utf-8") as f:
            return f.read()
    return ""


@router.post("/transcribe")
async def transcribe_audio(
    file: UploadFile = File(...),
    user: int | None = Query(None, description="1 or 2 for User 1 or User 2 turn"),
    reset: bool = Query(False, description="Start fresh debate, clear previous"),
    topic: str | None = Query(None, description="Debate topic for new debate"),
):
    """
    Receives WAV file, runs STT, saves to debate_transcript.txt.
    user=1 or 2: append as User 1/User 2 turn.
    reset=true: start new debate (use with topic=).
    """

    try:
        suffix = os.path.splitext(file.filename)[1] or ".wav"
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
            content = await file.read()
            tmp.write(content)
            wav_path = tmp.name

        transcript_text = run_whisper_file(wav_path)
        os.remove(wav_path)

        transcript_file_path = os.path.abspath(TRANSCRIPT_FILE)

        # When user=2, NEVER reset - always append so User 1's transcript is preserved
        if user == 2:
            reset = False

        if reset or not os.path.exists(TRANSCRIPT_FILE):
            header = "========== FULL DEBATE ==========\n"
            if topic:
                header += f"Topic: {topic}\n\n"
            full_content = header
        else:
            full_content = _read_full_transcript()

        if user in (1, 2):
            full_content += f"User {user}:\n{transcript_text.strip()}\n\n"
        else:
            full_content += transcript_text.strip() + "\n\n"

        with open(TRANSCRIPT_FILE, "w", encoding="utf-8") as f:
            f.write(full_content)

        if not full_content.rstrip().endswith("================================="):
            full_content = full_content.rstrip() + "\n\n=================================\n"

        return {
            "status": "success",
            "message": "Transcription completed",
            "transcript": full_content,
            "transcript_file": TRANSCRIPT_FILE,
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
