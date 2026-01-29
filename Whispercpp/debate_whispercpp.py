import sounddevice as sd
import numpy as np
import noisereduce as nr
import wave
import subprocess
import tempfile
import os
import keyboard
import uuid

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(BASE_DIR, ".."))


# -----------------------------
# CONFIG (NO HARD-CODED PATHS)
# -----------------------------
sr = 16000  # sample rate used by whisper.cpp

# Read paths from environment variables
WHISPER_CLI = os.getenv("WHISPER_CLI")
WHISPER_MODEL = os.getenv("WHISPER_MODEL")

if not WHISPER_CLI or not WHISPER_MODEL:
    raise RuntimeError(
        " Whisper paths not configured.\n"
        "Please set environment variables:\n"
        "  WHISPER_CLI   → path to whisper-cli.exe\n"
        "  WHISPER_MODEL → path to model file"
    )

# -----------------------------
# UTILS
# -----------------------------
def write_wav_int16(path: str, samples: np.ndarray, samplerate: int):
    clipped = np.clip(samples, -1.0, 1.0)
    int16 = (clipped * 32767).astype(np.int16)

    with wave.open(path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(samplerate)
        wf.writeframes(int16.tobytes())


def transcribe_with_whispercpp(wav_path: str) -> str:
    cmd = [
        WHISPER_CLI,
        "-m", WHISPER_MODEL,
        "-f", wav_path,
        "--no-timestamps"
    ]

    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, check=True)
        output = proc.stdout.strip()
    except subprocess.CalledProcessError as e:
        output = ((e.stdout or "") + "\n" + (e.stderr or "")).strip()

    lines = [ln.strip() for ln in output.splitlines() if ln.strip()]
    return " ".join(lines)


# -----------------------------
# CLI MODE FUNCTIONS
# -----------------------------
def record_turn(user_id, debate_log):
    print(f"\n User {user_id}, start speaking…")
    print("➡ Press CTRL + C to end your turn.\n")

    audio_chunks = []

    try:
        with sd.InputStream(samplerate=sr, channels=1, dtype="float32") as stream:
            while True:
                audio_chunks.append(stream.read(1024)[0])
    except KeyboardInterrupt:
        print(" Turn ended.\n")

    if not audio_chunks:
        print(" No audio captured.")
        return

    audio = np.concatenate(audio_chunks, axis=0).flatten()

    print("🔇 Reducing noise…")
    cleaned = nr.reduce_noise(y=audio, sr=sr)

    tmp_wav = os.path.join(tempfile.gettempdir(), f"debate_{uuid.uuid4().hex}.wav")
    write_wav_int16(tmp_wav, cleaned, sr)
    print(f" WAV saved: {tmp_wav}")

    print(" Transcribing with whisper.cpp…")
    transcript = transcribe_with_whispercpp(tmp_wav)

    try:
        os.remove(tmp_wav)
    except:
        pass

    print(f"\n===== USER {user_id} TRANSCRIPT =====")
    print(transcript)
    print("=====================================\n")

    debate_log.append({"user": f"User {user_id}", "text": transcript})


def run_stt_session():
    """
    CLI MODE:
    Runs full interactive debate session
    """

    debate_log = []

    topic = input("Enter the debate topic: ").strip()
    print(f"\n🎙 The debate topic is: '{topic}'\n")

    print("🎙 TWO-USER DEBATE MODE (whisper.cpp)")
    print("CTRL + C → End user turn")
    print("CTRL + D → Save & Exit\n")
    print("User 1 starts.\n")

    current_user = 1

    while True:
        print(f" Press ENTER to start User {current_user}'s turn…")

        try:
            input()

            if keyboard.is_pressed("ctrl+d"):
                raise EOFError

            record_turn(current_user, debate_log)

            current_user = 2 if current_user == 1 else 1

            if keyboard.is_pressed("ctrl+d"):
                raise EOFError

        except EOFError:
            print("\nCTRL + D detected — Ending debate.\n")
            break

    # SAVE TRANSCRIPT
    
    filename = os.path.join(PROJECT_ROOT, "debate_transcript.txt")

    with open(filename, "w", encoding="utf-8") as f:
        f.write(f"========== FULL DEBATE ==========\n")
        f.write(f"Topic: {topic}\n\n")
        for entry in debate_log:
            f.write(f"{entry['user']}:\n{entry['text']}\n\n")
        f.write("=================================\n")

    print(f" Transcript saved as: {filename}")

    return filename


# -----------------------------
# API MODE FUNCTION
# -----------------------------
def run_whisper_file(wav_path: str) -> str:
    """
    API MODE:
    Takes a WAV file path and returns transcript.
    No keyboard, no mic, no loops.
    """
    transcript = transcribe_with_whispercpp(wav_path)
    return transcript


# -----------------------------
# ENTRY POINT
# -----------------------------
if __name__ == "__main__":
    run_stt_session()
