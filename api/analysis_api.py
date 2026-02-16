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
    current_speaker = None
    current_sentiment = None

    for raw_line in analysis_text.splitlines():
        line = raw_line.strip()
        if not line:
            continue

        # Detect speaker (appears inside "Corrected Text" blocks)
        # STT format: User 1 / User 2. Chatbot format: USER / DEBATE GPT
        if "User 1:" in line:
            current_speaker = "User 1"
        elif "User 2:" in line:
            current_speaker = "User 2"
        elif "USER:" in line:
            current_speaker = "USER"
        elif "DEBATE GPT:" in line:
            current_speaker = "DEBATE GPT"

        # Detect sentiment
        if line.startswith("Sentiment"):
            parts = line.split(":", 1)
            if len(parts) == 2:
                current_sentiment = parts[1].strip()

        # Detect argument type (commit sentiment count at the same time)
        if line.startswith("Argument Type"):
            parts = line.split(":", 1)
            arg_type = parts[1].strip() if len(parts) == 2 else None

            if current_speaker and arg_type:
                if current_sentiment:
                    stats[current_speaker][current_sentiment] += 1
                stats[current_speaker][arg_type] += 1

            current_sentiment = None

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

