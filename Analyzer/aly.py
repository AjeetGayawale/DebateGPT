import os
import nltk
from transformers import pipeline
import language_tool_python

# -------------------------------
# FILE PATHS (backend-safe)
# -------------------------------
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(BASE_DIR, ".."))

RAW_TRANSCRIPT_STT = os.path.join(PROJECT_ROOT, "debate_transcript.txt")
FINAL_OUTPUT_STT = os.path.join(BASE_DIR, "debate_final_analysis.txt")

RAW_TRANSCRIPT_CHATBOT = os.path.join(PROJECT_ROOT, "Chatbot", "chatbot_debate_transcript.txt")
FINAL_OUTPUT_CHATBOT = os.path.join(BASE_DIR, "chatbot_final_analysis.txt")


# -------------------------------
# DOWNLOAD REQUIRED NLTK DATA
# -------------------------------
def setup_nltk():
    try:
        nltk.data.find("tokenizers/punkt")
    except LookupError:
        nltk.download("punkt")

    try:
        nltk.data.find("tokenizers/punkt_tab")
    except LookupError:
        nltk.download("punkt_tab")


# -------------------------------
# NLP MODEL for ARGUMENT MINING
# -------------------------------
argument_classifier = pipeline(
    "zero-shot-classification",
    model="typeform/distilbert-base-uncased-mnli"
)

ARGUMENT_LABELS = ["Claim", "Evidence", "Rebuttal", "Statement"]


# =====================================================
# MAIN ANALYZER FUNCTION (DUAL MODE)
# =====================================================
def analyze_debate(mode: str = "stt"):
    """
    mode:
      - 'stt'     → analyze debate_transcript.txt
      - 'chatbot' → analyze chatbot_debate_transcript.txt
    """

    setup_nltk()

    # -------------------------------
    # SELECT FILES BASED ON MODE
    # -------------------------------
    if mode == "chatbot":
        RAW_FILE = RAW_TRANSCRIPT_CHATBOT
        FINAL_FILE = FINAL_OUTPUT_CHATBOT
    else:
        RAW_FILE = RAW_TRANSCRIPT_STT
        FINAL_FILE = FINAL_OUTPUT_STT

    if not os.path.exists(RAW_FILE):
        raise FileNotFoundError(f"Input file not found: {RAW_FILE}")

    # -------------------------------
    # 1.READ RAW TRANSCRIPT
    # -------------------------------
    with open(RAW_FILE, "r", encoding="utf-8") as f:
        raw_text = f.read()

    # For chatbot mode, the transcript file can contain multiple debates
    # separated by '============================================================'.
    # To avoid mixing old debates, only analyze the *last* debate block.
    if mode == "chatbot":
        parts = [p.strip() for p in raw_text.split("============================================================") if p.strip()]
        if parts:
            last_block = parts[-1]
            raw_text = "=== DEBATE GPT TRANSCRIPT ===\n\n" + last_block + "\n"

    # -------------------------------
    # 2.GRAMMAR CORRECTION
    # -------------------------------
    tool = language_tool_python.LanguageTool("en-US")
    matches = tool.check(raw_text)
    # Apply grammar corrections - this fixes spelling, grammar, punctuation
    # Use the correct() function which applies all corrections automatically
    corrected_text = language_tool_python.utils.correct(raw_text, matches)
    
    # Log if corrections were made (for debugging)
    if len(matches) > 0:
        print(f"Grammar check found {len(matches)} issues")
        if corrected_text != raw_text:
            print("Grammar corrections were applied")
        else:
            print("Warning: Grammar check found issues but no corrections were applied")

    # -------------------------------
    # 3.SENTENCE SEGMENTATION (with speaker tracking for chatbot)
    # -------------------------------
    # For chatbot mode, split by speaker first, then tokenize each speaker's text
    if mode == "chatbot":
        # Split corrected_text by speaker labels
        speaker_sentences = []
        current_speaker = None
        current_text = []
        
        for line in corrected_text.split("\n"):
            line_stripped = line.strip()
            if "USER:" in line:
                # Save previous speaker's text if any
                if current_speaker and current_text:
                    text_block = "\n".join(current_text)
                    sentences = nltk.sent_tokenize(text_block)
                    for sent in sentences:
                        if sent.strip():
                            speaker_sentences.append((current_speaker, sent.strip()))
                current_speaker = "USER"
                current_text = []
                # Extract text after USER:
                parts = line.split("USER:", 1)
                if len(parts) > 1 and parts[1].strip():
                    current_text.append(parts[1].strip())
            elif "DEBATE GPT:" in line:
                # Save previous speaker's text if any
                if current_speaker and current_text:
                    text_block = "\n".join(current_text)
                    sentences = nltk.sent_tokenize(text_block)
                    for sent in sentences:
                        if sent.strip():
                            speaker_sentences.append((current_speaker, sent.strip()))
                current_speaker = "DEBATE GPT"
                current_text = []
                # Extract text after DEBATE GPT:
                parts = line.split("DEBATE GPT:", 1)
                if len(parts) > 1 and parts[1].strip():
                    current_text.append(parts[1].strip())
            elif current_speaker and line_stripped and not line_stripped.startswith("[") and not line_stripped.startswith("==="):
                # Continuation of current speaker's text
                current_text.append(line_stripped)
        
        # Don't forget the last speaker's text
        if current_speaker and current_text:
            text_block = "\n".join(current_text)
            sentences = nltk.sent_tokenize(text_block)
            for sent in sentences:
                if sent.strip():
                    speaker_sentences.append((current_speaker, sent.strip()))
        
        sentences_with_speaker = speaker_sentences
    else:
        # STT mode: just tokenize normally
        sentences_list = nltk.sent_tokenize(corrected_text)
        sentences_with_speaker = [(None, s.strip()) for s in sentences_list if s.strip()]

    # -------------------------------
    # 4.SENTIMENT ANALYZER
    # -------------------------------
    sentiment_analyzer = pipeline(
        "sentiment-analysis",
        model="distilbert-base-uncased-finetuned-sst-2-english"
    )

    # =====================================================
    # 5.ARGUMENT TYPE DETECTION (HYBRID SYSTEM)
    # =====================================================

    # ---------- RULE BASED ----------
    def detect_argument_type_rules(sentence):
        s = sentence.lower()

        if any(k in s for k in ["i think", "i believe", "in my opinion", "i feel that"]):
            return "Claim", 0.95

        if any(k in s for k in ["because", "since", "as a result", "this shows", "for example"]):
            return "Evidence", 0.9

        if any(k in s for k in ["but", "however", "on the other hand", "although"]):
            return "Rebuttal", 0.9

        return None, None


    # ---------- NLP BASED ----------
    def detect_argument_type_nlp(sentence):
        result = argument_classifier(sentence, ARGUMENT_LABELS)
        label = result["labels"][0]
        score = round(result["scores"][0], 3)
        return label, score


    # ---------- HYBRID CONTROLLER ----------
    def detect_argument_type(sentence):
        label, score = detect_argument_type_rules(sentence)
        if label is not None:
            return label, score, "rule-based"

        label, score = detect_argument_type_nlp(sentence)
        return label, score, "nlp-based"


    # -------------------------------
    # 6.WRITE FINAL OUTPUT
    # -------------------------------
    with open(FINAL_FILE, "w", encoding="utf-8") as out:
        out.write("DEBATE GRAMMAR, SENTIMENT & ARGUMENT ANALYSIS\n")
        out.write("=" * 55 + "\n\n")

        out.write("CORRECTED TRANSCRIPT:\n")
        out.write("-" * 55 + "\n")
        out.write(corrected_text + "\n\n")

        out.write("SENTENCE-WISE ANALYSIS:\n")
        out.write("-" * 55 + "\n\n")

        count = 1
        for speaker, sentence in sentences_with_speaker:
            if not sentence.strip():
                continue

            sentiment = sentiment_analyzer(sentence)[0]
            arg_type, arg_conf, method = detect_argument_type(sentence)

            out.write(f"Sentence {count}:\n")
            # Include speaker label in corrected text for chatbot mode
            if speaker:
                corrected_text_line = f"{speaker}: {sentence}"
            else:
                corrected_text_line = sentence
            out.write(f"Corrected Text : {corrected_text_line}\n")
            out.write(f"Sentiment      : {sentiment['label']}\n")
            out.write(f"Confidence     : {round(sentiment['score'], 3)}\n")
            out.write(f"Argument Type  : {arg_type}\n")
            out.write(f"Arg Confidence : {arg_conf}\n")
            out.write(f"Detected By    : {method}\n")
            out.write("\n" + "-" * 55 + "\n\n")

            count += 1

    return {
        "mode": mode,
        "message": "FULL ANALYSIS COMPLETED",
        "output_file": FINAL_FILE,
        "sentences_analyzed": count - 1
    }


# -------------------------------
# CLI MODE
# -------------------------------
if __name__ == "__main__":
    print("Choose mode:")
    print("1 → STT Debate")
    print("2 → Chatbot Debate")

    choice = input("Enter choice (1/2): ").strip()

    if choice == "2":
        result = analyze_debate(mode="chatbot")
    else:
        result = analyze_debate(mode="stt")

    print("✅", result["message"])
    print(" Output:", result["output_file"])
