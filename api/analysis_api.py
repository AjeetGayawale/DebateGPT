from fastapi import APIRouter, HTTPException
from Analyzer.aly import analyze_debate
import os
from collections import defaultdict

router = APIRouter(prefix="/analyze", tags=["Analysis"])

SENTIMENT_SCORE = {
    "POSITIVE": 1.0,
    "NEGATIVE": 1.0,
    "NEUTRAL": 0.0
}

ARGUMENT_SCORE = {
    "Claim": 1.5,
    "Evidence": 1.5,
    "Rebuttal": 1.0,
    "Statement": 0.0
}

def _parse_analysis_stats(analysis_text: str) -> dict:
    """
    Parse Analyzer output (debate_final_analysis.txt) and compute per-user counts.

    Counts include:
    - Sentiment labels (POSITIVE/NEGATIVE/NEUTRAL when present)
    - Argument types (Claim/Evidence/Rebuttal/Statement)

    This mirrors the logic used by Analyzer/winner.py, so the UI matches backend scoring.
    """
    stats = defaultdict(lambda: defaultdict(int))
    lines = analysis_text.splitlines()
    
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
            # Collect "Corrected Text" lines until we hit "Sentiment"
            while i < len(lines):
                current_line = lines[i].strip()
                if current_line.startswith("Corrected Text"):
                    # Get text after "Corrected Text :"
                    parts = current_line.split(":", 1)
                    if len(parts) == 2:
                        corrected_text_lines.append(parts[1].strip())
                elif current_line.startswith("Sentiment"):
                    parts = current_line.split(":", 1)
                    if len(parts) == 2:
                        sentiment = parts[1].strip()
                elif current_line.startswith("Argument Type"):
                    parts = current_line.split(":", 1)
                    if len(parts) == 2:
                        arg_type = parts[1].strip()
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
                # Fallback: search for speaker labels in the text
                # Find which speaker label appears closest before the sentence content
                lines_before_sentiment = corrected_text.split("\n")
                sentence_content = None
                for line in reversed(lines_before_sentiment):
                    line = line.strip()
                    if line and not line.startswith("[") and not line.startswith("==="):
                        # Check if this line itself contains a speaker label
                        if "USER:" in line and "DEBATE GPT:" not in line:
                            speaker = "USER"
                            break
                        elif "DEBATE GPT:" in line:
                            speaker = "DEBATE GPT"
                            break
                        elif "User 1:" in line:
                            speaker = "User 1"
                            break
                        elif "User 2:" in line:
                            speaker = "User 2"
                            break
                        elif ":" not in line or not any(x in line for x in ["USER", "DEBATE GPT", "User 1", "User 2"]):
                            # Found actual sentence content
                            sentence_content = line
                            break
                
                if not speaker and sentence_content:
                    # Find which speaker label appears before this sentence
                    sentence_pos = corrected_text.find(sentence_content)
                    positions = []
                    for label in ["DEBATE GPT:", "USER:", "User 1:", "User 2:"]:
                        pos = corrected_text.find(label)
                        if pos >= 0 and pos < sentence_pos:
                            speaker_name = label.rstrip(":")
                            if speaker_name == "DEBATE GPT":
                                speaker_name = "DEBATE GPT"
                            positions.append((speaker_name, pos))
                    if positions:
                        positions.sort(key=lambda x: x[1], reverse=True)
                        speaker = positions[0][0]
                
                # Final fallback
                if not speaker:
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
            
            # If we found a speaker and argument type, record stats
            if speaker and arg_type:
                if sentiment:
                    stats[speaker][sentiment] += 1
                stats[speaker][arg_type] += 1
        
        i += 1

    return {user: dict(counts) for user, counts in stats.items()}

def _compute_marking_points(stats: dict | None) -> dict | None:
    """
    Compute marking points from stats (same weights as Analyzer/winner.py).
    Returns per-user totals + breakdown.
    """
    if not stats:
        return None

    marking = {}
    for user, counts in stats.items():
        sentiment_points = 0.0
        argument_points = 0.0

        for k, v in counts.items():
            if k in SENTIMENT_SCORE:
                sentiment_points += SENTIMENT_SCORE[k] * float(v)
            if k in ARGUMENT_SCORE:
                argument_points += ARGUMENT_SCORE[k] * float(v)

        total = sentiment_points + argument_points
        marking[user] = {
            "total": round(total, 3),
            "sentiment_points": round(sentiment_points, 3),
            "argument_points": round(argument_points, 3),
        }

    return marking


@router.post("/stt")
def analyze_stt():
    """
    Analyze STT debate transcript
    """
    try:
        result = analyze_debate(mode="stt")
        output_file = result.get("output_file")
        analysis_text = None
        stats = None
        if output_file and os.path.exists(output_file):
            with open(output_file, "r", encoding="utf-8") as f:
                analysis_text = f.read()
            stats = _parse_analysis_stats(analysis_text)
        marking = _compute_marking_points(stats)

        return {
            **result,
            "analysis_text": analysis_text,
            "stats": stats,
            "marking": marking
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/chatbot")
def analyze_chatbot():
    """
    Analyze chatbot debate transcript
    """
    try:
        result = analyze_debate(mode="chatbot")
        output_file = result.get("output_file")
        analysis_text = None
        stats = None
        if output_file and os.path.exists(output_file):
            with open(output_file, "r", encoding="utf-8") as f:
                analysis_text = f.read()
            stats = _parse_analysis_stats(analysis_text)
        marking = _compute_marking_points(stats)

        return {
            **result,
            "analysis_text": analysis_text,
            "stats": stats,
            "marking": marking
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

