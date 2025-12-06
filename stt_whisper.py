import sounddevice as sd
import soundfile as sf
import numpy as np
import noisereduce as nr
from faster_whisper import WhisperModel
import queue
import os
import uuid

def speech_conversion():
   
 sr = 16000
 q = queue.Queue()

 def callback(indata, frames, time, status):
    q.put(indata.copy())

 print("🟢 Start speaking… (Press CTRL + C to stop)")

 audio_data = []

 try:
    # Continuous recording
    with sd.InputStream(samplerate=sr, channels=1, dtype="float32", callback=callback):
        while True:
            audio_data.append(q.get())

 except KeyboardInterrupt:
    print("\n🛑 Recording stopped.")

 # Merge recorded chunks
 audio = np.concatenate(audio_data, axis=0).flatten()

 print("🔇 Reducing noise…")
 cleaned = nr.reduce_noise(y=audio, sr=sr)

 print("🧠 Loading Faster Whisper model…")
 model = WhisperModel("small", device="cpu", compute_type="int8")

 # --- Write cleaned audio to a temporary WAV file because faster-whisper expects a file path ---
 tmp_name = f"temp_{uuid.uuid4().hex}.wav"
 sf.write(tmp_name, cleaned, sr)

 try:
    segments, info = model.transcribe(tmp_name)
    print("\n===== TRANSCRIPT =====")
    for seg in segments:
        print(seg.text)
 finally:
    # remove temporary file
    if os.path.exists(tmp_name):
        os.remove(tmp_name)