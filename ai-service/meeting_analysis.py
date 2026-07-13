import re


ACTION_SIGNAL_PATTERN = re.compile(
    r"\b(?:action item|follow[- ]?up|assigned to|responsible for|"
    r"need(?:s)? to|must|should|please|deadline|due)\b",
    re.IGNORECASE,
)

DECISION_SIGNAL_PATTERN = re.compile(
    r"\b(?:decided|decision|agreed|approved|selected|chosen|"
    r"confirmed|finalized|resolved|will use|will adopt|"
    r"will proceed with)\b",
    re.IGNORECASE,
)

OWNER_COMMITMENT_PATTERN = re.compile(
    r"^(?P<owner>(?:[A-Z][a-z]+(?:\s+[A-Z][a-z]+){0,2}|"
    r"[Tt]he team|[Ww]e))\s+"
    r"(?:will|should|must|need(?:s)? to|has to|is responsible for)\s+"
    r"(?P<task>.+)$"
)

ASSIGNMENT_PATTERN = re.compile(
    r"^(?P<task>.+?)\s+(?:was|is|has been)?\s*assigned to\s+"
    r"(?P<owner>[A-Z][a-z]+(?:\s+[A-Z][a-z]+){0,2})",
    re.IGNORECASE,
)

DUE_DATE_PATTERN = re.compile(
    r"\b(?:by|before|on)\s+(?P<due>(?:(?:next|this|coming)\s+)?"
    r"(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday|"
    r"today|tomorrow|\d{1,2}(?:st|nd|rd|th)?(?:\s+[A-Za-z]+){0,2}))",
    re.IGNORECASE,
)

TASK_VERB_PATTERN = re.compile(
    r"^(?:please\s+)?(?:complete|prepare|send|share|review|schedule|"
    r"deliver|create|finish|update|test|investigate|coordinate|organize|"
    r"draft|design|implement|deploy)\b",
    re.IGNORECASE,
)


def split_sentences(text: str) -> list[str]:
    sentences = re.split(r"(?<=[.!?])\s+|\n+", text)

    return [
        " ".join(sentence.split()).strip()
        for sentence in sentences
        if sentence.strip()
    ]


def extract_due_date(sentence: str) -> str | None:
    match = DUE_DATE_PATTERN.search(sentence)

    return match.group("due") if match else None


def determine_priority(sentence: str) -> str:
    lower_sentence = sentence.lower()

    if any(word in lower_sentence for word in ["urgent", "asap", "critical", "immediately"]):
        return "HIGH"

    if any(word in lower_sentence for word in ["optional", "later", "whenever"]):
        return "LOW"

    return "MEDIUM"


def is_action_sentence(sentence: str) -> bool:
    return bool(
        ACTION_SIGNAL_PATTERN.search(sentence)
        or OWNER_COMMITMENT_PATTERN.match(sentence)
        or TASK_VERB_PATTERN.match(sentence)
    )


def extract_action_items(transcript: str) -> list[dict[str, str | None]]:
    action_items: list[dict[str, str | None]] = []
    seen_tasks: set[str] = set()

    for sentence in split_sentences(transcript):
        if not is_action_sentence(sentence):
            continue

        owner = "Unassigned"
        task = sentence.rstrip(" .")

        commitment_match = OWNER_COMMITMENT_PATTERN.match(sentence)
        assignment_match = ASSIGNMENT_PATTERN.match(sentence)

        if commitment_match:
            matched_owner = commitment_match.group("owner")
            owner = "Team" if matched_owner.lower() in {"we", "the team"} else matched_owner
            task = commitment_match.group("task").rstrip(" .")
        elif assignment_match:
            owner = assignment_match.group("owner")
            task = assignment_match.group("task").rstrip(" .")

        normalized_task = task.lower()

        if not task or normalized_task in seen_tasks:
            continue

        seen_tasks.add(normalized_task)

        action_items.append(
            {
                "task": task,
                "owner": owner,
                "dueDate": extract_due_date(sentence),
                "priority": determine_priority(sentence),
                "evidence": sentence,
            }
        )

        if len(action_items) == 8:
            break

    return action_items


def extract_key_decisions(transcript: str) -> list[str]:
    decisions: list[str] = []
    seen_decisions: set[str] = set()

    for sentence in split_sentences(transcript):
        if not DECISION_SIGNAL_PATTERN.search(sentence):
            continue

        decision = re.sub(
            r"^(?:decision|decided)\s*:\s*",
            "",
            sentence,
            flags=re.IGNORECASE,
        ).strip()

        normalized_decision = decision.lower()

        if not decision or normalized_decision in seen_decisions:
            continue

        seen_decisions.add(normalized_decision)
        decisions.append(decision)

        if len(decisions) == 8:
            break

    return decisions
