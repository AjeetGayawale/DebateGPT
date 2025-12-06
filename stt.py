import sounddevice as sd
from vosk import Model, KaldiRecognizer
import json

# -----------------------------
# CONFIG
# -----------------------------
MODEL_PATH = "vosk-model-small-en-us-0.15"
SAMPLE_RATE = 16000  # Vosk model sample rate

# -----------------------------
# LOAD MODEL
# -----------------------------
model = Model(MODEL_PATH)
rec = KaldiRecognizer(model, SAMPLE_RATE)

# -----------------------------
# GLOBAL TRANSCRIPT
# -----------------------------
transcript = ""

# -----------------------------
# CALLBACK FUNCTION
# -----------------------------
def callback(indata, frames, time, status):
    global transcript
    if status:
        print("Status:", status)

    # Convert audio to bytes
    data = indata.tobytes()

    # Accept waveform and update transcript
    if rec.AcceptWaveform(data):
        result = json.loads(rec.Result())
        text = result.get("text", "")
        if text:
            transcript += text + " "
            print("FULL:", text)
    else:
        partial = json.loads(rec.PartialResult())
        partial_text = partial.get("partial", "")
        if partial_text:
            print("PARTIAL:", partial_text)

# -----------------------------
# START LISTENING
# -----------------------------
print("🎤 Listening... Press Ctrl+C to stop.")

try:
    with sd.InputStream(samplerate=SAMPLE_RATE, channels=1, dtype='int16', callback=callback, blocksize=8000):
        while True:
            pass
except KeyboardInterrupt:
    print("\nStopping microphone...")

# -----------------------------
# FLUSH REMAINING AUDIO
# -----------------------------
rec.AcceptWaveform(b"")  # flush any leftover audio
final_result = json.loads(rec.FinalResult())
final_text = final_result.get("text", "")

if final_text:
    transcript += final_text

# -----------------------------
# PRINT FINAL TRANSCRIPT
# -----------------------------
print("\n===========================")
print("📝 FINAL TRANSCRIPT:")
print(transcript.strip())
print("===========================")