import os
import nltk
from transformers import pipeline
import language_tool_python

# -------------------------------
# FILE PATHS (backend-safe)
# -------------------------------
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(BASE_DIR, ".."))

RAW_TRANSCRIPT_FILE = os.path.join(PROJECT_ROOT, "C://Users//siddh//OneDrive//Desktop//Documents//megaproject//debate_transcript.txt")
FINAL_OUTPUT_FILE = os.path.join(PROJECT_ROOT, "debate_final_analysis.txt")



# -------------------------------
# DOWNLOAD REQUIRED NLTK DATA
# -------------------------------
def setup_nltk():
    try:
        nltk.data.find("tokenizers/punkt")
    except LookupError:
        nltk.download("punkt")

    # required for Python 3.13+
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


# -------------------------------
# MAIN ANALYZER FUNCTION
# -------------------------------
def analyze_debate():
    """
    Reads debate_transcript.txt,
    performs grammar + sentiment + argument analysis,
    saves debate_final_analysis.txt,
    and returns a summary message.
    """

    # ensure nltk is ready
    setup_nltk()

    # -------------------------------
    # 1️⃣ READ RAW TRANSCRIPT
    # -------------------------------
    with open(RAW_TRANSCRIPT_FILE, "r", encoding="utf-8") as f:
        raw_text = f.read()

    # -------------------------------
    # 2️⃣ GRAMMAR CORRECTION
    # -------------------------------
    tool = language_tool_python.LanguageTool("en-US")
    matches = tool.check(raw_text)
    corrected_text = language_tool_python.utils.correct(raw_text, matches)

    # -------------------------------
    # 3️⃣ SENTENCE SEGMENTATION
    # -------------------------------
    sentences = nltk.sent_tokenize(corrected_text)

    # -------------------------------
    # 4️⃣ SENTIMENT ANALYZER
    # -------------------------------
    sentiment_analyzer = pipeline(
        "sentiment-analysis",
        model="distilbert-base-uncased-finetuned-sst-2-english"
    )

    # =====================================================
    # 5️⃣ ARGUMENT TYPE DETECTION (HYBRID SYSTEM)
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
        # 1️⃣ try rule-based first
        label, score = detect_argument_type_rules(sentence)

        if label is not None:
            return label, score, "rule-based"

        # 2️⃣ fallback to NLP
        label, score = detect_argument_type_nlp(sentence)
        return label, score, "nlp-based"

    # -------------------------------
    # 6️⃣ WRITE FINAL OUTPUT
    # -------------------------------
    with open(FINAL_OUTPUT_FILE, "w", encoding="utf-8") as out:
        out.write("DEBATE GRAMMAR, SENTIMENT & ARGUMENT ANALYSIS\n")
        out.write("=" * 55 + "\n\n")

        # ---- Corrected Transcript Section ----
        out.write("CORRECTED TRANSCRIPT:\n")
        out.write("-" * 55 + "\n")
        out.write(corrected_text + "\n\n")

        # ---- Sentence-wise Analysis ----
        out.write("SENTENCE-WISE ANALYSIS:\n")
        out.write("-" * 55 + "\n\n")

        count = 1
        for sentence in sentences:
            if not sentence.strip():
                continue

            sentiment = sentiment_analyzer(sentence)[0]

            # 🔥 HYBRID ARGUMENT DETECTION
            arg_type, arg_conf, method = detect_argument_type(sentence)

            out.write(f"Sentence {count}:\n")
            out.write(f"Corrected Text : {sentence}\n")
            out.write(f"Sentiment      : {sentiment['label']}\n")
            out.write(f"Confidence     : {round(sentiment['score'], 3)}\n")
            out.write(f"Argument Type  : {arg_type}\n")
            out.write(f"Arg Confidence : {arg_conf}\n")
            out.write(f"Detected By    : {method}\n")
            out.write("\n" + "-" * 55 + "\n\n")

            count += 1

    return {
        "message": "FULL ANALYSIS COMPLETED",
        "output_file": FINAL_OUTPUT_FILE,
        "sentences_analyzed": count - 1
    }


# -------------------------------
# CLI MODE (optional)
# -------------------------------
if __name__ == "__main__":
    result = analyze_debate()
    print("✅", result["message"])
    print("📄 Final output saved as:", result["output_file"])