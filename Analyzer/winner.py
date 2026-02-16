from collections import defaultdict
import os

# -------------------------------
# PATHS
# -------------------------------
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.abspath(os.path.join(BASE_DIR, ".."))

# STT files
INPUT_FILE_STT = os.path.join(BASE_DIR, "debate_final_analysis.txt")
OUTPUT_FILE_STT = os.path.join(BASE_DIR, "debate_final_winner.txt")

# Chatbot files
INPUT_FILE_CHATBOT = os.path.join(BASE_DIR, "chatbot_final_analysis.txt")
OUTPUT_FILE_CHATBOT = os.path.join(BASE_DIR, "chatbot_final_winner.txt")


# -------------------------------
# SCORING RULES
# -------------------------------
SENTIMENT_SCORE = {
    "POSITIVE": 1,
    "NEGATIVE": 1,
    "NEUTRAL": 0
}

ARGUMENT_SCORE = {
    "Claim": 1.5,
    "Evidence": 1.5,
    "Rebuttal": 1,
    "Statement": 0
}


# =====================================================
# MAIN FUNCTION (DUAL MODE)
# =====================================================
def run_winner_analysis(mode: str = "stt"):
    """
    mode:
      - 'stt'     → analyze debate_final_analysis.txt
      - 'chatbot' → analyze chatbot_final_analysis.txt
    """

    # -------------------------------
    # SELECT FILES BASED ON MODE
    # -------------------------------
    if mode == "chatbot":
        INPUT_FILE = INPUT_FILE_CHATBOT
        OUTPUT_FILE = OUTPUT_FILE_CHATBOT
    else:
        INPUT_FILE = INPUT_FILE_STT
        OUTPUT_FILE = OUTPUT_FILE_STT

    if not os.path.exists(INPUT_FILE):
        raise FileNotFoundError(f"Input file not found: {INPUT_FILE}")

    # -------------------------------
    # READ FILE
    # -------------------------------
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        lines = f.readlines()

    # -------------------------------
    # PARSING & SCORING
    # -------------------------------
    # STT uses "User 1" / "User 2"; chatbot uses "USER" / "DEBATE GPT"
    speaker_keys = ["User 1", "User 2"] if mode == "stt" else ["USER", "DEBATE GPT"]
    scores = defaultdict(float)
    stats = defaultdict(lambda: defaultdict(int))

    i = 0
    while i < len(lines):
        line = lines[i].strip()
        
        # Look for sentence blocks: "Sentence N:"
        if line.startswith("Sentence ") and line.endswith(":"):
            # Collect the entire sentence block
            corrected_text_lines = []
            sentiment = None
            arg_type = None
            
            i += 1
            # Collect "Corrected Text" lines until we hit "Argument Type"
            while i < len(lines):
                current_line = lines[i].strip()
                if current_line.startswith("Corrected Text"):
                    # Get text after "Corrected Text :"
                    parts = current_line.split(":", 1)
                    if len(parts) == 2:
                        corrected_text_lines.append(parts[1].strip())
                elif current_line.startswith("Sentiment") and ":" in current_line:
                    sentiment = current_line.split(":", 1)[1].strip()
                elif current_line.startswith("Argument Type") and ":" in current_line:
                    arg_type = current_line.split(":", 1)[1].strip()
                    # Found argument type - determine speaker from corrected text
                    break
                elif current_line and not current_line.startswith("-"):
                    # Continuation of corrected text
                    corrected_text_lines.append(current_line)
                i += 1
            
            # Determine speaker from corrected text
            # With the updated analyzer, corrected text should start with speaker label
            corrected_text = "\n".join(corrected_text_lines)
            speaker = None
            
            # Check if corrected text starts with a speaker label (new format)
            if corrected_text.startswith("USER:"):
                speaker = "USER"
            elif corrected_text.startswith("DEBATE GPT:"):
                speaker = "DEBATE GPT"
            elif corrected_text.startswith("User 1:"):
                speaker = "User 1"
            elif corrected_text.startswith("User 2:"):
                speaker = "User 2"
            else:
                # Fallback: search for speaker labels
                if "DEBATE GPT:" in corrected_text:
                    debate_gpt_pos = corrected_text.find("DEBATE GPT:")
                    user_pos = corrected_text.find("USER:")
                    if user_pos >= 0 and (debate_gpt_pos < 0 or user_pos > debate_gpt_pos):
                        speaker = "USER"
                    else:
                        speaker = "DEBATE GPT"
                elif "USER:" in corrected_text:
                    speaker = "USER"
                elif "User 1:" in corrected_text:
                    speaker = "User 1"
                elif "User 2:" in corrected_text:
                    speaker = "User 2"
            
            # Score this sentence
            if speaker and arg_type:
                scores[speaker] += SENTIMENT_SCORE.get(sentiment, 0)
                scores[speaker] += ARGUMENT_SCORE.get(arg_type, 0)
                
                stats[speaker][sentiment] += 1
                stats[speaker][arg_type] += 1
        
        i += 1

    # Ensure all speaker keys exist in scores (for consistent response)
    for k in speaker_keys:
        if k not in scores:
            scores[k] = 0.0

    # -------------------------------
    # DECIDE WINNER
    # -------------------------------
    first_key, second_key = speaker_keys[0], speaker_keys[1]
    first_score = scores[first_key]
    second_score = scores[second_key]

    if first_score > second_score:
        winner = first_key
    elif second_score > first_score:
        winner = second_key
    else:
        winner = "Draw"

    # -------------------------------
    # WRITE OUTPUT
    # -------------------------------
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write("=" * 60 + "\n")
        f.write("DEBATE WINNER & PERFORMANCE ANALYSIS\n")
        f.write("=" * 60 + "\n\n")

        for user in speaker_keys:
            f.write(f"{user} PERFORMANCE SUMMARY:\n")
            f.write(f"Total Score : {scores[user]}\n")
            for k, v in stats[user].items():
                f.write(f"{k:<10} : {v}\n")
            f.write("\n" + "-" * 40 + "\n\n")
        f.write(f"🏆 FINAL RESULT: {winner}\n")

    return {
        "mode": mode,
        "winner": winner,
        "scores": {k: round(scores[k], 3) for k in speaker_keys},
        "output_file": OUTPUT_FILE
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
        result = run_winner_analysis(mode="chatbot")
    else:
        result = run_winner_analysis(mode="stt")

    print("✅ Winner analysis completed")
    print("🏆 Winner:", result["winner"])
    print("📄 Output file:", result["output_file"])
