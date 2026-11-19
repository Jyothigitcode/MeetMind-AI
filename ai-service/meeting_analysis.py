import json
import re
from typing import Any

import requests


OLLAMA_URL = "http://127.0.0.1:11434/api/chat"
OLLAMA_MODEL = "qwen3:4b"


# ============================================================
# RULE-BASED FALLBACK PATTERNS
# ============================================================

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


# ============================================================
# LLM SYSTEM PROMPT
# ============================================================

SYSTEM_PROMPT = """
You are MeetMind AI, a meeting intelligence system.

Analyze the meeting transcript and extract accurate, useful,
and action-oriented information.

STRICT GROUNDING RULES:

1. Use only information explicitly supported by the transcript.
2. Never invent facts, people, tasks, deadlines, priorities, or decisions.
3. Do not treat general discussion as a decision or action item.
4. Preserve names and deadlines exactly as stated in the transcript.
5. Return valid JSON only.
6. Do not return Markdown or explanations outside the JSON.

SUMMARY RULES:

1. Write a concise factual summary of the main meeting outcomes.
2. Focus on important topics, confirmed decisions, and planned work.
3. Do not add information that is not present in the transcript.
4. Avoid unnecessary introductory phrases.

KEY DECISION RULES:

1. A key decision must represent a confirmed choice, agreement,
   approval, selection, resolution, or finalized plan.

2. Statements containing words such as "decided", "agreed",
   "approved", "confirmed", "selected", or "finalized"
   are strong decision signals.

3. Do NOT classify tasks, assignments, requests, recommendations,
   or future work as key decisions.

4. A statement using "must", "should", "need to", "please",
   or assigning work to someone is normally an action item,
   NOT a decision.

5. Example:
   "We decided to launch next Friday."
   This is a decision.

6. Example:
   "The team should review all bugs."
   This is an action item, NOT a decision.

ACTION ITEM RULES:

1. An action item must describe specific work that needs to be done.

2. Extract tasks expressed through commitments, assignments,
   requests, or required future work.

3. Statements using "will", "must", "should", "need to",
   "assigned to", or direct task instructions are strong
   action-item signals.

4. If a person is explicitly responsible, preserve their name.

5. If "we" or "the team" is responsible, use "Team" as owner.

6. If no owner is explicitly identified, use "Unassigned".

7. If no due date is explicitly stated, use null.

8. Do not invent owners or due dates.

PRIORITY RULES:

1. HIGH priority requires explicit urgency, immediate business risk,
   critical work, blocking work, severe issues, or words such as
   "urgent", "ASAP", "critical", or "immediately".

2. A deadline by itself does NOT make an action item HIGH priority.

3. MEDIUM is the default priority for normal assigned work,
   including tasks with ordinary deadlines.

4. LOW priority should be used only when the transcript explicitly
   indicates that work is optional, non-urgent, or can be done later.

5. Example:
   "Vivek will complete testing by Wednesday."
   Priority: MEDIUM

6. Example:
   "Fix the critical production issue immediately."
   Priority: HIGH

7. Example:
   "Update the optional documentation later."
   Priority: LOW

EVIDENCE RULES:

1. Every action item must include short evidence copied or closely
   derived from the transcript.

2. The evidence must clearly support the extracted task.

Return exactly this JSON structure:

{
  "summary": "Concise factual meeting summary",
  "keyDecisions": [
    "Confirmed decision"
  ],
  "actionItems": [
    {
      "task": "Specific task",
      "owner": "Owner name, Team, or Unassigned",
      "dueDate": "Explicit due date or null",
      "priority": "HIGH, MEDIUM, or LOW",
      "evidence": "Transcript evidence supporting this action item"
    }
  ]
}

If there are no key decisions, return an empty keyDecisions array.
If there are no action items, return an empty actionItems array.
""".strip()


# ============================================================
# TEXT PROCESSING
# ============================================================

def split_sentences(text: str) -> list[str]:

    sentences = re.split(
        r"(?<=[.!?])\s+|\n+",
        text,
    )

    return [
        " ".join(sentence.split()).strip()
        for sentence in sentences
        if sentence.strip()
    ]


# ============================================================
# RULE-BASED ACTION ITEM EXTRACTION
# ============================================================

def extract_due_date(sentence: str) -> str | None:

    match = DUE_DATE_PATTERN.search(sentence)

    return match.group("due") if match else None


def determine_priority(sentence: str) -> str:

    lower_sentence = sentence.lower()

    if any(
        word in lower_sentence
        for word in [
            "urgent",
            "asap",
            "critical",
            "immediately",
        ]
    ):
        return "HIGH"

    if any(
        word in lower_sentence
        for word in [
            "optional",
            "later",
            "whenever",
        ]
    ):
        return "LOW"

    return "MEDIUM"


def is_action_sentence(sentence: str) -> bool:

    return bool(
        ACTION_SIGNAL_PATTERN.search(sentence)
        or OWNER_COMMITMENT_PATTERN.match(sentence)
        or TASK_VERB_PATTERN.match(sentence)
    )


def extract_action_items(
    transcript: str,
) -> list[dict[str, str | None]]:

    action_items: list[dict[str, str | None]] = []

    seen_tasks: set[str] = set()


    for sentence in split_sentences(transcript):

        if not is_action_sentence(sentence):
            continue


        owner = "Unassigned"

        task = sentence.rstrip(" .")


        commitment_match = (
            OWNER_COMMITMENT_PATTERN.match(sentence)
        )

        assignment_match = (
            ASSIGNMENT_PATTERN.match(sentence)
        )


        if commitment_match:

            matched_owner = commitment_match.group(
                "owner"
            )

            owner = (
                "Team"
                if matched_owner.lower()
                in {"we", "the team"}
                else matched_owner
            )

            task = (
                commitment_match
                .group("task")
                .rstrip(" .")
            )


        elif assignment_match:

            owner = assignment_match.group(
                "owner"
            )

            task = (
                assignment_match
                .group("task")
                .rstrip(" .")
            )


        normalized_task = task.lower()


        if (
            not task
            or normalized_task in seen_tasks
        ):
            continue


        seen_tasks.add(normalized_task)


        action_items.append(
            {
                "task": task,
                "owner": owner,
                "dueDate":
                    extract_due_date(sentence),
                "priority":
                    determine_priority(sentence),
                "evidence": sentence,
            }
        )


        if len(action_items) == 8:
            break


    return action_items


# ============================================================
# RULE-BASED DECISION EXTRACTION
# ============================================================

def extract_key_decisions(
    transcript: str,
) -> list[str]:

    decisions: list[str] = []

    seen_decisions: set[str] = set()


    for sentence in split_sentences(transcript):

        if not DECISION_SIGNAL_PATTERN.search(
            sentence
        ):
            continue


        decision = re.sub(
            r"^(?:decision|decided)\s*:\s*",
            "",
            sentence,
            flags=re.IGNORECASE,
        ).strip()


        normalized_decision = decision.lower()


        if (
            not decision
            or normalized_decision in seen_decisions
        ):
            continue


        seen_decisions.add(
            normalized_decision
        )

        decisions.append(
            decision
        )


        if len(decisions) == 8:
            break


    return decisions


# ============================================================
# FALLBACK ANALYSIS
# ============================================================

def fallback_analysis(
    transcript: str,
) -> dict[str, Any]:

    sentences = split_sentences(transcript)


    fallback_summary = " ".join(
        sentences[:5]
    )


    return {
        "summary": fallback_summary,

        "keyDecisions":
            extract_key_decisions(transcript),

        "actionItems":
            extract_action_items(transcript),
    }


# ============================================================
# LLM OUTPUT VALIDATION
# ============================================================

def validate_analysis(
    analysis: dict[str, Any],
) -> dict[str, Any]:

    summary = analysis.get(
        "summary"
    )

    key_decisions = analysis.get(
        "keyDecisions",
        [],
    )

    action_items = analysis.get(
        "actionItems",
        [],
    )


    if not isinstance(summary, str):

        raise ValueError(
            "LLM response contains invalid summary"
        )


    summary = summary.strip()


    if not summary:

        raise ValueError(
            "LLM response contains empty summary"
        )


    if not isinstance(key_decisions, list):

        raise ValueError(
            "LLM response contains invalid keyDecisions"
        )


    validated_decisions: list[str] = []


    for decision in key_decisions[:8]:

        if isinstance(decision, str):

            cleaned_decision = decision.strip()

            if cleaned_decision:

                validated_decisions.append(
                    cleaned_decision
                )


    if not isinstance(action_items, list):

        raise ValueError(
            "LLM response contains invalid actionItems"
        )


    validated_action_items = []


    for action_item in action_items[:8]:

        if not isinstance(action_item, dict):
            continue


        task = action_item.get(
            "task"
        )


        if (
            not isinstance(task, str)
            or not task.strip()
        ):
            continue


        owner = action_item.get(
            "owner",
            "Unassigned",
        )


        if (
            not isinstance(owner, str)
            or not owner.strip()
        ):
            owner = "Unassigned"


        due_date = action_item.get(
            "dueDate"
        )


        if (
            due_date is not None
            and not isinstance(due_date, str)
        ):
            due_date = None


        priority = action_item.get(
            "priority",
            "MEDIUM",
        )


        if priority not in {
            "HIGH",
            "MEDIUM",
            "LOW",
        }:
            priority = "MEDIUM"


        evidence = action_item.get(
            "evidence",
            "",
        )


        if not isinstance(evidence, str):
            evidence = ""


        validated_action_items.append(
            {
                "task":
                    task.strip(),

                "owner":
                    owner.strip(),

                "dueDate":
                    due_date.strip()
                    if isinstance(due_date, str)
                    and due_date.strip()
                    else None,

                "priority":
                    priority,

                "evidence":
                    evidence.strip(),
            }
        )


    return {
        "summary":
            summary,

        "keyDecisions":
            validated_decisions,

        "actionItems":
            validated_action_items,
    }


# ============================================================
# OLLAMA LLM ANALYSIS
# ============================================================

def analyze_meeting_with_llm(
    transcript: str,
) -> dict[str, Any]:

    try:

        response = requests.post(
            OLLAMA_URL,

            json={
                "model":
                    OLLAMA_MODEL,

                "messages":
                    [
                        {
                            "role":
                                "system",

                            "content":
                                SYSTEM_PROMPT,
                        },

                        {
                            "role":
                                "user",

                            "content":
                                (
                                    "Analyze the following "
                                    "meeting transcript:\n\n"
                                    + transcript
                                ),
                        },
                    ],

                "stream":
                    False,

                "format":
                    "json",

                "options":
                    {
                        "temperature":
                            0.1,
                    },
            },

            timeout=300,
        )


        response.raise_for_status()


        ollama_response = response.json()


        message = ollama_response.get(
            "message",
            {},
        )


        content = message.get(
            "content",
            "",
        )


        if not content:

            raise ValueError(
                "Ollama returned empty content"
            )


        analysis = json.loads(
            content
        )


        return validate_analysis(
            analysis
        )


    except Exception as exception:

        print(
            "LLM meeting analysis failed. "
            "Using rule-based fallback:",
            exception,
        )


        return fallback_analysis(
            transcript
        )