import ollama
import time
from datetime import datetime

print("=== Debate GPT ===\n")

topic = input("Enter debate topic: ")

# Ask stance first
while True:
    stance = input("Choose your side (favor / against): ").lower()
    if stance in ["favor", "against"]:
        break
    print("Please type only: favor or against")

print("\nType 'exit' to stop.\n")

# -----------------------------
# File setup
# -----------------------------
filename = "chatbot_debate_transcript.txt"

log_file = open(filename, "w", encoding="utf-8")
log_file.write("=== DEBATE GPT TRANSCRIPT ===\n")
log_file.write(f"Topic  : {topic}\n")
log_file.write(f"Stance : {stance}\n")
log_file.write("-" * 30 + "\n\n")

# -----------------------------
# Main chat loop
# -----------------------------
while True:
    print("\n" + "-" * 50)
    msg = input(" Your argument:\n> ")
    if msg.lower() == "exit":
        break

    # Save user input
    log_file.write("USER:\n")
    log_file.write(msg + "\n\n")

    prompt = f"""
You are Debate GPT.

STRICT RULES:
- Do NOT use any greetings or formal openings.
- Give ONLY 2–3 short sentences.

- Be clear, simple, and direct.
- No headings, no bullet points.

Debate topic: {topic}
User stance: {stance}
User argument: {msg}

Now write a very short debate response that goes AGAINST the user's stance.
"""

    # -----------------------------
    # STREAMING RESPONSE
    # -----------------------------
    print("\n" + "=" * 50)
    print(" Debate GPT says:\n")

    bot_reply = ""

    for chunk in ollama.chat(
        model="phi3:mini",
        messages=[
            {"role": "system", "content": "You are a debate assistant. Follow rules strictly."},
            {"role": "user", "content": prompt}
        ],
        stream=True
    ):
        if "message" in chunk and "content" in chunk["message"]:
            text = chunk["message"]["content"]
            print(text, end="", flush=True)   # live typing effect
            bot_reply += text

    print("\n" + "=" * 50 + "\n")

    # Save bot output
    log_file.write("DEBATE GPT:\n")
    log_file.write(bot_reply + "\n")
    log_file.write("=" * 60 + "\n\n")

# -----------------------------
# Close file
# -----------------------------
log_file.write("\n=== END OF DEBATE ===\n")
log_file.close()

print(f"\n📄 Debate saved in file: {filename}")
print("Thank you for using Debate GPT!")
