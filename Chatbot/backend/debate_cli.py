import ollama
import threading
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
timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
safe_topic = topic.replace(" ", "_")
filename = f"debate_{safe_topic}_{timestamp}.txt"

log_file = open(filename, "w", encoding="utf-8")
log_file.write("=== DEBATE GPT TRANSCRIPT ===\n")
log_file.write(f"Topic  : {topic}\n")
log_file.write(f"Stance : {stance}\n")
log_file.write("-" * 60 + "\n\n")

# -----------------------------
# Thinking indicator
# -----------------------------
thinking = False

def show_thinking():
    dots = 0
    while thinking:
        print("\rThinking" + "." * dots + " " * 5, end="", flush=True)
        dots = (dots + 1) % 7
        time.sleep(0.5)

# -----------------------------
# Main chat loop
# -----------------------------
while True:
    print("\n" + "-" * 50)
    msg = input("🧑 Your argument:\n> ")
    if msg.lower() == "exit":
        break

    # Save user input
    log_file.write("USER:\n")
    log_file.write(msg + "\n\n")

    prompt = f"""
You are Debate GPT.

STRICT RULES:
- Do NOT start with phrases like "Ladies and gentlemen", "Respected judges", or any greeting.
- Give ONLY ONE paragraph.
- Be direct and professional.
- No headings, no bullet points.

Debate topic: {topic}
User stance: {stance}
User argument: {msg}

Now generate a single-paragraph debate response that supports the user's stance.
"""

    # Start thinking animation
    thinking = True
    t = threading.Thread(target=show_thinking)
    t.start()

    # Call model
    res = ollama.chat(
        model="phi3:mini",
        messages=[
            {"role": "system", "content": "You are a debate assistant. Follow the rules strictly."},
            {"role": "user", "content": prompt}
        ]
    )

    # Stop thinking animation
    thinking = False
    t.join()
    print("\r" + " " * 40, end="\r")  # clear line

    bot_reply = res["message"]["content"]

    # Save bot output
    log_file.write("DEBATE GPT:\n")
    log_file.write(bot_reply + "\n")
    log_file.write("=" * 60 + "\n\n")

    # Show with spacing
    print("\n" + "=" * 50)
    print("🤖 Debate GPT says:\n")
    print(bot_reply)
    print("=" * 50 + "\n")

# -----------------------------
# Close file
# -----------------------------
log_file.write("\n=== END OF DEBATE ===\n")
log_file.close()

print(f"\n📄 Debate saved in file: {filename}")
print("Thank you for using Debate GPT!")
