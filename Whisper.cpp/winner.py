from collections import defaultdict

# -------------------------------
# INPUT FILE
# -------------------------------
INPUT_FILE = "debate_final_analysis.txt"

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

# -------------------------------
# READ FILE
# -------------------------------
with open(INPUT_FILE, "r", encoding="utf-8") as f:
    lines = f.readlines()

# -------------------------------
# PARSING & SCORING
# -------------------------------
scores = defaultdict(float)
stats = defaultdict(lambda: defaultdict(int))

current_speaker = None
current_sentiment = None
current_argument = None

for line in lines:
    line = line.strip()

    # Detect speaker
    if "User 1:" in line:
        current_speaker = "User 1"
    elif "User 2:" in line:
        current_speaker = "User 2"

    # Detect sentiment
    if line.startswith("Sentiment"):
        current_sentiment = line.split(":")[1].strip()

    # Detect argument type and score
    if line.startswith("Argument Type"):
        current_argument = line.split(":")[1].strip()

        if current_speaker:
            scores[current_speaker] += SENTIMENT_SCORE.get(current_sentiment, 0)
            scores[current_speaker] += ARGUMENT_SCORE.get(current_argument, 0)

            stats[current_speaker][current_sentiment] += 1
            stats[current_speaker][current_argument] += 1

        current_sentiment = None
        current_argument = None

# -------------------------------
# DECIDE WINNER
# -------------------------------
user1_score = scores["User 1"]
user2_score = scores["User 2"]

if user1_score > user2_score:
    winner = "User 1"
elif user2_score > user1_score:
    winner = "User 2"
else:
    winner = "Draw"

# -------------------------------
# WRITE OUTPUT (APPEND)
# -------------------------------
with open(INPUT_FILE, "a", encoding="utf-8") as f:
    f.write("\n" + "=" * 60 + "\n")
    f.write("DEBATE WINNER & PERFORMANCE ANALYSIS\n")
    f.write("=" * 60 + "\n\n")

    for user in ["User 1", "User 2"]:
        f.write(f"{user} PERFORMANCE SUMMARY:\n")
        f.write(f"Total Score : {scores[user]}\n")

        for k, v in stats[user].items():
            f.write(f"{k:<10} : {v}\n")

        f.write("\n" + "-" * 40 + "\n\n")

    f.write(f"🏆 FINAL RESULT: {winner}\n")

print("✅ Winner and performance analysis added successfully.")
print(f"📄 Updated file: {INPUT_FILE}")
