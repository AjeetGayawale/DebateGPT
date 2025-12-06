import re

class PunctuationEngine:

    def __init__(self):
        self.connectors = [
            "however", "moreover", "therefore", "furthermore",
            "nevertheless", "nonetheless", "in conclusion",
            "on the other hand", "for example", "for instance",
            "as a result", "in fact", "finally", "instead",
            "overall"
        ]

        self.question_words = [
            "why", "what", "how", "when", "where", "who",
            "whom", "whose", "can", "could", "should", "do",
            "does", "did", "is", "are", "will", "would", "may", "might"
        ]

        self.emotion_markers = ["definitely", "absolutely", "must", "important"]

    def punctuate(self, text: str) -> str:
        if not text.strip():
            return ""

        text = text.lower().strip()

        # STEP 1: Add commas before connectors
        for c in self.connectors:
            text = re.sub(fr"\b{c}\b", f", {c}", text)

        # STEP 2: Split using discourse markers
        split_markers = [
            r"\bhowever\b", r"\bmoreover\b", r"\btherefore\b",
            r"\bon the other hand\b", r"\bin conclusion\b",
            r"\bfurthermore\b", r"\bnevertheless\b"
        ]
        pattern = "(" + "|".join(split_markers) + ")"
        parts = re.split(pattern, text)

        sentences = []
        current = ""

        for part in parts:
            part = part.strip()
            if not part:
                continue

            if part in self.connectors:
                if current:
                    sentences.append(current.strip())
                current = part
            else:
                current += " " + part

        if current:
            sentences.append(current.strip())

        final_sentences = []

        for s in sentences:
            words = s.split()

            if not words:
                continue

            # Question detection
            if words[0] in self.question_words:
                sentence = s + "?"
            # Emphasis
            elif any(e in s for e in self.emotion_markers):
                sentence = s + "!"
            else:
                sentence = s + "."

            sentence = sentence[0].upper() + sentence[1:]
            final_sentences.append(sentence)

        final_output = " ".join(final_sentences)
        final_output = re.sub(r",\s*,", ",", final_output)

        for c in self.connectors:
            final_output = final_output.replace(f", {c}", f", {c.capitalize()}")

        return final_output
