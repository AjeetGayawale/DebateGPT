import sounddevice as sd
import numpy as np
import noisereduce as nr
from faster_whisper import WhisperModel
import soundfile as sf
import uuid
import os
import keyboard   # to detect CTRL+D on Windows

sr = 16000
model = WhisperModel("small", device="cpu", compute_type="int8")

debate_log = []   # store all user turns


# ---------------------------------------------
# RECORD ONE USER TURN (stops on CTRL + C)
# ---------------------------------------------
def record_turn(user_id):
    print(f"\n🎤 User {user_id}, start speaking…")
    print("➡ Press CTRL + C to end your turn.\n")

    audio_chunks = []

    try:
        with sd.InputStream(samplerate=sr, channels=1, dtype="float32") as stream:
            while True:
                # Each read collects audio chunk
                audio_chunks.append(stream.read(1024)[0])

    except KeyboardInterrupt:
        print("🛑 Turn ended.\n")

    # Merge audio
    audio = np.concatenate(audio_chunks, axis=0).flatten()

    print("🔇 Reducing noise…")
    cleaned = nr.reduce_noise(y=audio, sr=sr)

    # Save temporary wav for whisper
    temp = f"temp_{uuid.uuid4().hex}.wav"
    sf.write(temp, cleaned, sr)

    print("🧠 Transcribing...")
    segments, _ = model.transcribe(temp)
    text = " ".join(seg.text for seg in segments).strip()

    print(f"\n===== USER {user_id} TRANSCRIPT =====")
    print(text)
    print("=====================================\n")

    # Save in log
    debate_log.append({
        "user": f"User {user_id}",
        "text": text
    })

    os.remove(temp)


# ---------------------------------------------
# MAIN LOOP
# ---------------------------------------------
print("\n🎙 TWO-USER DEBATE MODE")
print("CTRL + C → End current user's turn")
print("CTRL + D → End entire debate and print all")
print("User 1 starts.\n")

current_user = 1

while True:
    print(f"👉 Press ENTER to start User {current_user}'s turn…")
    try:
        input()

        # If CTRL+D is pressed BEFORE starting:
        if keyboard.is_pressed("ctrl+d"):
            raise EOFError

        record_turn(current_user)

        # Switch user (1 → 2 → 1 → 2)
        current_user = 2 if current_user == 1 else 1

        # Check if CTRL+D pressed right after turn
        if keyboard.is_pressed("ctrl+d"):
            raise EOFError

    except EOFError:
        # CTRL + D → end debate
        print("\n🟥 CTRL + D detected — Ending entire debate.\n")
        break

# ---------------------------------------------
# SAVE FULL DEBATE TO TXT FILE
# ---------------------------------------------
filename = "debate_transcript.txt"

with open(filename, "w", encoding="utf-8") as f:
    f.write("========== FULL DEBATE ==========\n\n")
    for entry in debate_log:
        f.write(f"{entry['user']}:\n")
        f.write(f"{entry['text']}\n\n")
    f.write("=================================\n")

print(f"📄 Transcript saved as: {filename}")

